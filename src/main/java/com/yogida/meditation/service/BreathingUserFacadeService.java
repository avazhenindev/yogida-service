package com.yogida.meditation.service;

import com.yogida.meditation.dto.BreathingDto;
import com.yogida.meditation.dto.BreathingPhaseAudioDto;
import com.yogida.meditation.dto.BreathingPhaseDto;
import com.yogida.meditation.entity.AppUserEntity;
import com.yogida.meditation.entity.BreathingEntity;
import com.yogida.meditation.entity.BreathingPhaseAudioEntity;
import com.yogida.meditation.entity.BreathingPhaseEntity;
import com.yogida.meditation.entity.S3ObjectEntity;
import com.yogida.meditation.exception.BreathingNotFoundException;
import com.yogida.meditation.mapper.BreathingMapper;
import com.yogida.meditation.repository.BreathingRepository;
import com.yogida.meditation.service.api.R2StorageApi;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The user-facing read path for breathing exercises: the same data the admin path returns, with
 * entitlement applied.
 *
 * <p>Before this existed, {@code BreathingService.findAll()} handed every exercise's audio URL
 * to every authenticated caller, and those URLs pointed at a public bucket that needed no
 * signature. The {@code premium} flag was written by the admin UI and read by nothing, so
 * premium breathing content was free to anyone with an account.
 *
 * <p>The gate has two halves and both are necessary. Audio now lives in the private bucket, so
 * a URL is required to reach it at all; and a URL is only minted here, after the check. An
 * unentitled caller still receives the exercise — name, colour, icon, phase structure and
 * durations — so the paywall can show what is being offered, but every {@code audioFiles.url}
 * comes back null and {@code locked} is true.
 *
 * <p>URLs are presigned during the listing rather than through a separate stream endpoint.
 * Presigning is a local HMAC over the request, not a call to R2, so doing it per audio object is
 * cheap; and it keeps the client unchanged, since it already reads {@code audio.url}. The cost
 * is that a URL handed out here expires on the normal presigned-URL schedule, so a client that
 * holds a listing for longer than that must refetch before playing — which the mobile app's
 * RTK Query cache already does on screen focus.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BreathingUserFacadeService {

    private final BreathingRepository breathingRepository;
    private final BreathingMapper breathingMapper;
    private final CurrentUserService currentUserService;
    private final EntitlementService entitlementService;
    private final R2StorageApi r2StorageApi;

    @Transactional(readOnly = true)
    public List<BreathingDto> findAllForCurrentUser() {
        AppUserEntity user = currentUserService.getCurrentUserOrThrow();
        // Resolved once for the whole listing rather than per exercise: entitlement is a
        // property of the user, and asking per item would repeat the same lookup N times.
        boolean premiumUser = entitlementService.isUserPremium(user.getKeycloakUserId());

        return breathingRepository.findAllByOrderByDisplayOrderAsc().stream()
                .map(entity -> applyEntitlement(entity, premiumUser))
                .toList();
    }

    @Transactional(readOnly = true)
    public BreathingDto findByIdForCurrentUser(Long id) {
        AppUserEntity user = currentUserService.getCurrentUserOrThrow();
        BreathingEntity entity = breathingRepository.findWithGraphById(id)
                .orElseThrow(() -> new BreathingNotFoundException(id));
        boolean premiumUser = entitlementService.isUserPremium(user.getKeycloakUserId());
        return applyEntitlement(entity, premiumUser);
    }

    /**
     * Maps an exercise and fills in audio URLs only when the caller may have them.
     */
    private BreathingDto applyEntitlement(BreathingEntity entity, boolean premiumUser) {
        BreathingDto dto = breathingMapper.toDto(entity);
        boolean locked = entity.isPremium() && !premiumUser;

        if (locked) {
            // The mapper already leaves every audio url null; nothing to strip.
            return withLocked(dto, true);
        }
        return withPhases(withLocked(dto, false), signedPhases(dto, entity));
    }

    /**
     * Rebuilds the phase list with presigned URLs, matching audio DTOs to their entities by S3
     * object id rather than by list position — the mapper's ordering is not a contract.
     */
    private List<BreathingPhaseDto> signedPhases(BreathingDto dto, BreathingEntity entity) {
        Map<Long, S3ObjectEntity> objectsById = new HashMap<>();
        for (BreathingPhaseEntity phase : entity.getPhases()) {
            if (phase.getAudioFiles() == null) {
                continue;
            }
            for (BreathingPhaseAudioEntity audio : phase.getAudioFiles()) {
                S3ObjectEntity object = audio.getAudioObject();
                if (object != null && object.getId() != null) {
                    objectsById.put(object.getId(), object);
                }
            }
        }

        return dto.phases().stream()
                .map(phase -> new BreathingPhaseDto(
                        phase.id(), phase.name(), phase.label(), phase.duration(),
                        phase.color(), phase.displayOrder(),
                        phase.audioFiles() == null ? List.of() : phase.audioFiles().stream()
                                .map(audio -> new BreathingPhaseAudioDto(
                                        audio.id(), presign(objectsById.get(audio.id()))))
                                .toList()))
                .toList();
    }

    /**
     * Presigns one audio object.
     *
     * <p>Signs against whatever bucket the row records, not a hard-coded one. Objects uploaded
     * before the private-bucket switch still carry {@code public}, and presigning those works —
     * though it does not make them private, since the unsigned public URL keeps working until
     * the objects are physically moved.
     *
     * <p>A signing failure yields null rather than propagating: one bad object should degrade to
     * a silent exercise, not a failed catalogue.
     */
    private String presign(S3ObjectEntity object) {
        if (object == null || object.getBucketName() == null || object.getObjectUri() == null) {
            return null;
        }
        try {
            return r2StorageApi.generateStreamingUrl(object.getBucketName(), object.getObjectUri());
        } catch (RuntimeException e) {
            log.warn("BreathingUserFacadeService > Failed to presign audio object {} ({}): {}",
                    object.getId(), object.getObjectUri(), e.getMessage());
            return null;
        }
    }

    private BreathingDto withLocked(BreathingDto dto, boolean locked) {
        return new BreathingDto(dto.id(), dto.name(), dto.icon(), dto.description(), dto.color(),
                dto.cycles(), dto.isPremium(), locked, dto.displayOrder(),
                dto.createdAt(), dto.updatedAt(), dto.phases());
    }

    private BreathingDto withPhases(BreathingDto dto, List<BreathingPhaseDto> phases) {
        return new BreathingDto(dto.id(), dto.name(), dto.icon(), dto.description(), dto.color(),
                dto.cycles(), dto.isPremium(), dto.locked(), dto.displayOrder(),
                dto.createdAt(), dto.updatedAt(), phases);
    }
}
