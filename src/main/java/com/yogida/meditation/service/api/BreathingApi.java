package com.yogida.meditation.service.api;

import com.yogida.meditation.dto.*;
import org.springframework.web.multipart.MultipartFile;


import java.util.List;

/**
 * Service contract for breathing exercise management.
 */
public interface BreathingApi {

    List<BreathingDto> findAll();

    BreathingDto findById(Long id);

    BreathingDto create(BreathingCreateRequest request, MultipartFile iconFile, List<MultipartFile> audioFiles);

    BreathingDto update(Long id, BreathingUpdateRequest request, MultipartFile iconFile, List<MultipartFile> audioFiles);

    void delete(Long id);

    void reorder(List<BreathingReorderItem> items);

    BreathingDto addAudioToPhase(Long phaseId, MultipartFile audioFile);

    BreathingDto removeAudioFromPhase(Long phaseId, Long audioObjectId);
}
