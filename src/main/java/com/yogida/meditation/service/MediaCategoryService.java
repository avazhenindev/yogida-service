package com.yogida.meditation.service;

import com.yogida.meditation.dto.MediaCategoryCreateRequest;
import com.yogida.meditation.dto.MediaCategoryDto;
import com.yogida.meditation.dto.MediaCategoryUpdateRequest;
import com.yogida.meditation.entity.MediaCategoryEntity;
import com.yogida.meditation.exception.EntityNotFoundException;
import com.yogida.meditation.mapper.MediaCategoryMapper;
import com.yogida.meditation.repository.MediaCategoryRepository;
import com.yogida.meditation.service.api.MediaCategoryApi;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Log4j2
@Service
@RequiredArgsConstructor
public class MediaCategoryService implements MediaCategoryApi {

    private final MediaCategoryRepository mediaCategoryRepository;
    private final MediaCategoryMapper mediaCategoryMapper;

    @Override
    @Transactional(readOnly = true)
    public List<MediaCategoryDto> findAll() {
        return mediaCategoryMapper.toDtoList(mediaCategoryRepository.findAll());
    }

    @Override
    @Transactional(readOnly = true)
    public MediaCategoryDto findById(Long id) {
        return mediaCategoryRepository.findById(id)
                .map(mediaCategoryMapper::toDto)
                .orElseThrow(() -> new EntityNotFoundException("MediaCategory", id));
    }

    @Override
    @Transactional
    public MediaCategoryDto create(MediaCategoryCreateRequest request) {
        MediaCategoryEntity entity = mediaCategoryMapper.toEntity(request);
        MediaCategoryEntity saved = mediaCategoryRepository.save(entity);
        log.info("MediaCategoryService > Created media category id={}", saved.getId());
        return mediaCategoryMapper.toDto(saved);
    }

    @Override
    @Transactional
    public MediaCategoryDto update(Long id, MediaCategoryUpdateRequest request) {
        MediaCategoryEntity entity = mediaCategoryRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("MediaCategory", id));
        mediaCategoryMapper.updateEntity(request, entity);
        MediaCategoryEntity saved = mediaCategoryRepository.save(entity);
        log.info("MediaCategoryService > Updated media category id={}", saved.getId());
        return mediaCategoryMapper.toDto(saved);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        if (!mediaCategoryRepository.existsById(id)) {
            throw new EntityNotFoundException("MediaCategory", id);
        }
        mediaCategoryRepository.deleteById(id);
        log.info("MediaCategoryService > Deleted media category id={}", id);
    }
}

