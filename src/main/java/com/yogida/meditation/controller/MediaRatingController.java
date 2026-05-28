package com.yogida.meditation.controller;

import com.yogida.meditation.controller.api.MediaRatingControllerApi;
import com.yogida.meditation.dto.*;
import com.yogida.meditation.service.api.MediaRatingApi;
import com.yogida.meditation.service.api.MediaReviewApi;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class MediaRatingController implements MediaRatingControllerApi {

    private final MediaRatingApi mediaRatingApi;
    private final MediaReviewApi mediaReviewApi;

    @Override
    public ResponseEntity<Void> upsertRating(Long mediaId, MediaRatingRequest request) {
        mediaRatingApi.upsertRating(mediaId, request.userId(), request.rating());
        return ResponseEntity.ok().build();
    }

    @Override
    public ResponseEntity<MediaRatingSummaryResponse> getRatingSummary(Long mediaId) {
        return ResponseEntity.ok(mediaReviewApi.getRatingSummary(mediaId));
    }

    @Override
    public ResponseEntity<MediaReviewResponse> upsertReview(Long mediaId, MediaReviewRequest request) {
        return ResponseEntity.ok(mediaReviewApi.upsertReview(mediaId, request.userId(), request.reviewText()));
    }

    @Override
    public ResponseEntity<Page<MediaReviewResponse>> getReviews(Long mediaId, Pageable pageable) {
        return ResponseEntity.ok(mediaReviewApi.findReviewsByMediaId(mediaId, pageable));
    }
}
