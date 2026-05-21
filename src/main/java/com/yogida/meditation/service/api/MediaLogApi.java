package com.yogida.meditation.service.api;

import com.yogida.meditation.dto.MediaLogDto;
import com.yogida.meditation.entity.MediaEntity;
import com.yogida.meditation.enums.MediaLogAction;

import java.util.List;

public interface MediaLogApi {

    /**
     * Appends an audit log entry for the given media entity.
     */
    MediaLogDto log(MediaEntity media, MediaLogAction action, String message);

    /**
     * Returns all log entries for a specific media item, newest first.
     */
    List<MediaLogDto> findByMediaId(Long mediaId);
}

