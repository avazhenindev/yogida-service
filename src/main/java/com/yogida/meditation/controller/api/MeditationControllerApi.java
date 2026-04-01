package com.yogida.meditation.controller.api;

import com.yogida.meditation.dto.ErrorResponse;
import com.yogida.meditation.dto.MediaDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Map;

@Tag(name = "Meditation", description = "Endpoints for meditation media streaming and catalog browsing")
@RequestMapping("/meditation/")
public interface MeditationControllerApi {

    @Operation(
        summary = "Get a presigned streaming URL",
        description = "Generates a time-limited presigned URL for streaming the specified media object from a Cloudflare R2 bucket."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Presigned streaming URL generated successfully",
            content = @Content(mediaType = "application/json",
                schema = @Schema(type = "object", example = """
                    {"url": "https://r2.example.com/bucket/media.mp3?X-Amz-Signature=..."}""")))
    })
    @GetMapping("/stream-link")
    Map<String, String> getStreamLink(
        @Parameter(description = "Name of the R2 bucket containing the media", required = true, example = "meditations")
        @RequestParam("bucketName") String bucketName,
        @Parameter(description = "Name of the media object to stream", required = true, example = "morning-calm.mp3")
        @RequestParam("mediaName") String mediaName
    );

    @Operation(
        summary = "List all media objects grouped by bucket",
        description = "Returns a map where each key is a bucket name and the value is the list of media object names stored in that bucket."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Media catalog retrieved successfully",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = Map.class),
                examples = @ExampleObject(value = """
                    {
                      "meditations": [
                        {
                          "id": 1,
                          "name": "morning-calm.mp3",
                          "bucketName": "meditations",
                          "description": "A calming meditation for the morning.",
                          "category": "calm",
                          "createdAt": "2024-01-01T10:00:00",
                          "updatedAt": "2024-01-02T12:00:00"
                        }
                      ]
                    }"""
                )))
    })
    @GetMapping(value = "/media", produces = "application/json")
    ResponseEntity<Map<String, List<MediaDto>>> getAllStorageObject();

}
