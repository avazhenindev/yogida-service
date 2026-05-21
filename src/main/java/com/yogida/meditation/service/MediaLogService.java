package com.yogida.meditation.service;

import com.yogida.meditation.dto.MediaLogDto;
import com.yogida.meditation.entity.MediaEntity;
import com.yogida.meditation.entity.MediaLogEntity;
import com.yogida.meditation.enums.MediaLogAction;
import com.yogida.meditation.mapper.MediaLogMapper;
import com.yogida.meditation.repository.MediaLogRepository;
import com.yogida.meditation.service.api.MediaLogApi;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MediaLogService implements MediaLogApi {

    private final MediaLogRepository mediaLogRepository;
    private final MediaLogMapper mediaLogMapper;

    @Override
    @Transactional
    public MediaLogDto log(MediaEntity media, MediaLogAction action, String message) {
        MediaLogEntity entry = new MediaLogEntity();
        entry.setMedia(media);
        entry.setMediaName(media.getName());
        entry.setAction(action);
        entry.setMessage(message);
        entry.setCreatedAt(LocalDateTime.now());
        return mediaLogMapper.toDto(mediaLogRepository.save(entry));
    }

    @Override
    @Transactional(readOnly = true)
    public List<MediaLogDto> findByMediaId(Long mediaId) {
        return mediaLogMapper.toDtoList(
                mediaLogRepository.findAllByMediaIdOrderByCreatedAtDesc(mediaId));
    }
}

