package com.yogida.meditation.service.api;

import com.yogida.meditation.dto.MediaBulkDeleteRequest;
import com.yogida.meditation.dto.MediaCreateRequest;
import com.yogida.meditation.dto.MediaDto;
import com.yogida.meditation.dto.MediaFileUpdateRequest;

import java.util.List;
import java.util.Optional;

public interface MediaFacadeApi {

    /**
     * Returns all media records regardless of status.
     */
    List<MediaDto> findAll();

    /**
     * Returns all media records with ACTIVE status.
     */
    List<MediaDto> findAllActive();

    /**
     * Returns a single media entry by ID.
     */
    Optional<MediaDto> findById(Long id);

    /**
     * Uploads the S3 object and creates the media catalog entry in one step.
     */
    MediaDto create(MediaCreateRequest request);

    /**
     * Updates metadata. If a new file is provided and the object key differs,
     * uploads the new S3 object and deletes the old one.
     */
    MediaDto update(Long id, MediaFileUpdateRequest request);

    /**
     * Deletes the media record and its associated S3 object.
     */
    void delete(Long id);

    /**
     * Deletes multiple media records and their associated S3 objects,
     * using bulk S3 deletion grouped by bucket.
     */
    void bulkDelete(MediaBulkDeleteRequest request);
}
