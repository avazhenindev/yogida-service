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

    @Operation(
        summary = "Save review and/or rating",
        description = """
            Creates or updates the review/rating row for the given user and media item.
            `rating` updates the star rating (1–5) on every call.
            `reviewText` is written on first save only; subsequent saves do not change it.
            """
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Review saved"),
        @ApiResponse(responseCode = "400", description = "Invalid request"),
        @ApiResponse(responseCode = "404", description = "Media or user not found")
    })
    @PutMapping("/{mediaId}/review")
    ResponseEntity<MediaReviewResponse> save(
            @Parameter(description = "Media ID", required = true) @PathVariable Long mediaId,
            @Valid @RequestBody MediaReviewSaveRequest request);

    @Operation(
        summary = "Get rating summary",
        description = """
            Returns the average rating, total rating count, and per-star breakdown (keys 1–5) for a media item.
            All keys 1–5 are present in `starBreakdown` (missing stars have a count of 0).
            """
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Rating summary returned"),
        @ApiResponse(responseCode = "404", description = "Media not found")
    })
    @GetMapping("/{mediaId}/rating-summary")
    ResponseEntity<MediaRatingSummaryResponse> getRatingSummary(
            @Parameter(description = "Media ID", required = true) @PathVariable Long mediaId);

    @Operation(
        summary = "List reviews",
        description = """
            Returns a paginated list of reviews for a media item.
            Each entry may have a null `rating` (text-only review) or null `reviewText` (rating-only entry).
            `userName` is derived from the user email prefix; `userInitial` is the first uppercase character.
            Supported sort fields: `createdAt` (default, desc), `rating` (asc or desc).
            Example: GET /media/1/reviews?page=0&size=10&sort=rating,desc
            """
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Reviews page returned"),
        @ApiResponse(responseCode = "400", description = "Unsupported sort field"),
        @ApiResponse(responseCode = "404", description = "Media not found")
    })
    @GetMapping("/{mediaId}/reviews")
    ResponseEntity<Page<MediaReviewResponse>> getReviews(
            @Parameter(description = "Media ID", required = true) @PathVariable Long mediaId,
            @PageableDefault(size = 10, sort = "createdAt") Pageable pageable);
}
