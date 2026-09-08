package com.yogida.meditation.controller;

import com.yogida.meditation.controller.api.MediaRatingControllerApi;
import com.yogida.meditation.dto.*;
import com.yogida.meditation.service.CurrentUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.Set;
import com.yogida.meditation.service.MediaReviewService;

@RestController
@RequiredArgsConstructor
public class MediaRatingController implements MediaRatingControllerApi {

    private final MediaReviewService mediaReviewApi;
    private final CurrentUserService currentUserService;

    @Override
    public ResponseEntity<MediaReviewResponse> save(Long mediaId, MediaReviewSaveRequest request) {
        // The author is whoever holds the token, never whoever the body claims.
        Long authorId = currentUserService.getCurrentUserOrThrow().getUserId();
        return ResponseEntity.ok(
                mediaReviewApi.save(mediaId, authorId, request.rating(), request.reviewText()));
    }

    @Override
    public ResponseEntity<MediaRatingSummaryResponse> getRatingSummary(Long mediaId) {
        return ResponseEntity.ok(mediaReviewApi.getRatingSummary(mediaId));
    }

    @Override
    public ResponseEntity<Page<MediaReviewResponse>> getReviews(Long mediaId, Pageable pageable) {
        Set<String> allowed = Set.of("createdAt", "rating");
        pageable.getSort().forEach(order -> {
            if (!allowed.contains(order.getProperty())) {
                throw new IllegalArgumentException("Unsupported sort field: " + order.getProperty());
            }
        });
        return ResponseEntity.ok(mediaReviewApi.findReviewsByMediaId(mediaId, pageable));
    }
}
