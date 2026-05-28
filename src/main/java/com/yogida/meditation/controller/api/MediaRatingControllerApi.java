package com.yogida.meditation.controller.api;

import com.yogida.meditation.dto.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Media — Ratings & Reviews", description = "Endpoints for submitting and retrieving media ratings and written reviews")
@RequestMapping("/media")
public interface MediaRatingControllerApi {

    @Operation(summary = "Submit or update a rating", description = "Creates a new rating or updates the existing one for the given user and media item.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Rating saved"),
            @ApiResponse(responseCode = "400", description = "Invalid request"),
            @ApiResponse(responseCode = "404", description = "Media or user not found")
    })
    @PutMapping("/{mediaId}/rating")
    ResponseEntity<Void> upsertRating(
            @Parameter(description = "Media ID", required = true) @PathVariable Long mediaId,
            @Valid @RequestBody MediaRatingRequest request);

    @Operation(summary = "Get rating summary", description = "Returns the average rating and total count for a media item.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Rating summary returned"),
            @ApiResponse(responseCode = "404", description = "Media not found")
    })
    @GetMapping("/{mediaId}/rating-summary")
    ResponseEntity<MediaRatingSummaryResponse> getRatingSummary(
            @Parameter(description = "Media ID", required = true) @PathVariable Long mediaId);

    @Operation(summary = "Submit or update a written review", description = "Creates a new review or updates the existing one for the given user and media item.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Review saved"),
            @ApiResponse(responseCode = "400", description = "Invalid request"),
            @ApiResponse(responseCode = "404", description = "Media or user not found")
    })
    @PutMapping("/{mediaId}/review")
    ResponseEntity<MediaReviewResponse> upsertReview(
            @Parameter(description = "Media ID", required = true) @PathVariable Long mediaId,
            @Valid @RequestBody MediaReviewRequest request);

    @Operation(summary = "List written reviews", description = "Returns a paginated list of written reviews for a media item, newest first.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Reviews page returned"),
            @ApiResponse(responseCode = "404", description = "Media not found")
    })
    @GetMapping("/{mediaId}/reviews")
    ResponseEntity<Page<MediaReviewResponse>> getReviews(
            @Parameter(description = "Media ID", required = true) @PathVariable Long mediaId,
            @PageableDefault(size = 20) Pageable pageable);
}
