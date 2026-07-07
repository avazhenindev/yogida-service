package com.yogida.meditation.service;

import com.yogida.meditation.dto.MediaDto;
import com.yogida.meditation.dto.MediaUpdateRequest;
import com.yogida.meditation.entity.FavouriteEntity;
import com.yogida.meditation.entity.MediaEntity;
import com.yogida.meditation.entity.MediaCategoryEntity;
import com.yogida.meditation.entity.S3ObjectEntity;
import com.yogida.meditation.entity.TagEntity;
import com.yogida.meditation.enums.ContentType;
import com.yogida.meditation.enums.MediaStatus;
import com.yogida.meditation.exception.EntityNotFoundException;
import com.yogida.meditation.mapper.MediaMapper;
import com.yogida.meditation.repository.FavouriteRepository;
import com.yogida.meditation.repository.MediaCategoryRepository;
import com.yogida.meditation.repository.MediaRepository;
import com.yogida.meditation.repository.S3ObjectRepository;
import com.yogida.meditation.repository.TagRepository;
import com.yogida.meditation.service.api.MediaApi;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Log4j2
@Service
@RequiredArgsConstructor
public class MediaService implements MediaApi {

    private final MediaRepository mediaRepository;
    private final MediaCategoryRepository mediaCategoryRepository;
    private final S3ObjectRepository s3ObjectRepository;
    private final TagRepository tagRepository;
    private final FavouriteRepository favouriteRepository;
    private final MediaMapper mediaMapper;

    @Override
    @Transactional(readOnly = true)
    public List<MediaDto> findAll() {
        return mediaMapper.toDtoList(mediaRepository.findAll());
    }

    @Override
    @Transactional(readOnly = true)
    public List<MediaDto> findAllActive() {
        return mediaMapper.toDtoList(mediaRepository.findAllByStatus(MediaStatus.ACTIVE));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<MediaDto> findById(Long id) {
        return mediaRepository.findById(id).map(mediaMapper::toDto);
    }



    @Override
    @Transactional
    public MediaDto create(MediaUpdateRequest request) {
        MediaEntity entity = new MediaEntity();
        entity.setName(request.name());
        S3ObjectEntity mediaObject = resolveS3Object(request.mediaObjectId());
        entity.setMediaObject(mediaObject);
        entity.setBucketName(mediaObject.getBucketName());
        entity.setDescription(request.description());
        entity.setPictureObject(resolveS3ObjectOrNull(request.pictureObjectId()));
        entity.setCategory(resolveCategory(request.categoryId()));
        entity.setStatus(request.status() != null ? request.status() : MediaStatus.ACTIVE);
        entity.setDurationSeconds(request.durationSeconds());
        entity.setTags(resolveTags(request.tagIds()));
        entity.setCreatedAt(LocalDateTime.now());
        return mediaMapper.toDto(mediaRepository.save(entity));
    }

    @Override
    @Transactional
    public MediaDto update(Long id, MediaUpdateRequest request) {
        MediaEntity entity = findEntityById(id);
        entity.setName(request.name());
        S3ObjectEntity mediaObject = resolveS3Object(request.mediaObjectId());
        entity.setMediaObject(mediaObject);
        entity.setBucketName(mediaObject.getBucketName());
        entity.setDescription(request.description());
        entity.setPictureObject(resolveS3ObjectOrNull(request.pictureObjectId()));
        entity.setCategory(resolveCategory(request.categoryId()));
        if (request.status() != null) {
            entity.setStatus(request.status());
        }
        entity.setDurationSeconds(request.durationSeconds());
        if (request.tagIds() != null) {
            entity.setTags(resolveTags(request.tagIds()));
        }
        entity.setUpdatedAt(LocalDateTime.now());
        return mediaMapper.toDto(mediaRepository.save(entity));
    }

    @Override
    @Transactional
    public void delete(Long id) {
        if (!mediaRepository.existsById(id)) {
            throw new EntityNotFoundException("Media", id);
        }
        mediaRepository.deleteById(id);
        log.info("MediaService > Deleted media id={}", id);
    }

    @Override
    @Transactional
    public void updateStatus(Long id, MediaStatus status) {
        MediaEntity entity = findEntityById(id);
        entity.setStatus(status);
        entity.setUpdatedAt(LocalDateTime.now());
        mediaRepository.save(entity);
    }

    /** Package-visible helper used by the scheduler. */
    @Transactional(readOnly = true)
    public List<MediaEntity> findAllEntities() {
        return mediaRepository.findAllWithMediaObjectBy();
    }

    /**
     * Enrich a single MediaDto with favourite information for the given user.
     * If userId is null (unauthenticated), isFavourite is set to false and favouriteId is null.
     */
    private void enrichWithFavouriteData(MediaDto mediaDto, Long userId) {
        if (userId == null) {
            mediaDto.setIsFavourite(false);
            mediaDto.setFavouriteId(null);
            return;
        }

        var favourite = favouriteRepository.findByUserUserIdAndContentTypeAndContentId(
                userId,
                ContentType.MEDIA.value(),
                mediaDto.getId()
        );

        if (favourite.isPresent()) {
            mediaDto.setIsFavourite(true);
            mediaDto.setFavouriteId(favourite.get().getFavouriteId());
        } else {
            mediaDto.setIsFavourite(false);
            mediaDto.setFavouriteId(null);
        }
    }

    /**
     * Enrich a list of MediaDtos with favourite information for the given user.
     * If userId is null (unauthenticated), all favourite fields are set to false/null.
     */
    private void enrichWithFavouriteData(List<MediaDto> mediaDtos, Long userId) {
        if (userId == null) {
            mediaDtos.forEach(dto -> {
                dto.setIsFavourite(false);
                dto.setFavouriteId(null);
            });
            return;
        }

        // Fetch all favourites for this user and media content type
        List<FavouriteEntity> favourites = favouriteRepository.findByUserUserId(userId).stream()
                .filter(f -> ContentType.MEDIA.value().equals(f.getContentType()))
                .toList();

        // Build a map of media ID -> favourite ID for quick lookup
        Map<Long, Long> favouriteMap = favourites.stream()
                .collect(Collectors.toMap(FavouriteEntity::getContentId, FavouriteEntity::getFavouriteId));

        // Enrich each DTO
        mediaDtos.forEach(dto -> {
            Long mediaId = dto.getId();
            if (favouriteMap.containsKey(mediaId)) {
                dto.setIsFavourite(true);
                dto.setFavouriteId(favouriteMap.get(mediaId));
            } else {
                dto.setIsFavourite(false);
                dto.setFavouriteId(null);
            }
        });
    }

    private MediaEntity findEntityById(Long id) {
        return mediaRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Media", id));
    }

    private MediaCategoryEntity resolveCategory(Long categoryId) {
        if (categoryId == null) {
            return null;
        }
        return mediaCategoryRepository.findById(categoryId)
                .orElseThrow(() -> new EntityNotFoundException("MediaCategory", categoryId));
    }

    private S3ObjectEntity resolveS3Object(Long s3ObjectId) {
        return s3ObjectRepository.findById(s3ObjectId)
                .orElseThrow(() -> new EntityNotFoundException("S3Object", s3ObjectId));
    }

    private S3ObjectEntity resolveS3ObjectOrNull(Long s3ObjectId) {
        if (s3ObjectId == null) {
            return null;
        }
        return resolveS3Object(s3ObjectId);
    }

    private Set<TagEntity> resolveTags(List<Long> tagIds) {
        if (tagIds == null || tagIds.isEmpty()) {
            return Collections.emptySet();
        }
        Set<TagEntity> resolved = new HashSet<>();
        for (Long tagId : tagIds) {
            resolved.add(tagRepository.findById(tagId)
                    .orElseThrow(() -> new EntityNotFoundException("Tag", tagId)));
        }
        return resolved;
    }
}
