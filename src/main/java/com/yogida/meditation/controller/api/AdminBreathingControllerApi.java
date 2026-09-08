package com.yogida.meditation.controller.api;

import com.yogida.meditation.dto.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Tag(name = "Admin — Breathing", description = "Admin CRUD for breathing exercises including icon/audio upload and display-order management")
@RequestMapping("/admin/breathing")
public interface AdminBreathingControllerApi {

    @Operation(summary = "List all breathing exercises",
            operationId = "listAdminBreathingExercises")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "Exercises retrieved"))
    @GetMapping
    ResponseEntity<List<BreathingDto>> getAll();

    @Operation(summary = "Get a breathing exercise by ID",
            operationId = "getAdminBreathingExerciseById")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Exercise found"),
            @ApiResponse(responseCode = "404", description = "Exercise not found")
    })
    @GetMapping("/{id}")
    ResponseEntity<BreathingDto> getById(
            @Parameter(description = "Breathing exercise ID", required = true) @PathVariable Long id);

    @Operation(summary = "Create a breathing exercise",
               description = "Creates a new exercise. Send as multipart/form-data: " +
                             "part 'meta' (JSON, BreathingCreateRequest) + part 'iconFile' (required image). " +
                             "Audio files are added separately via POST /admin/breathing/phases/{phaseId}/audio.",
            operationId = "createBreathingExercise")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Exercise created"),
            @ApiResponse(responseCode = "400", description = "Validation error")
    })
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    ResponseEntity<BreathingDto> create(
            @RequestPart("meta") @Valid BreathingCreateRequest meta,
            @RequestPart("iconFile") MultipartFile iconFile,
            @RequestPart(value = "audioFiles", required = false) List<MultipartFile> audioFiles);

    @Operation(summary = "Update a breathing exercise",
               description = "Partially updates the exercise. Send as multipart/form-data: " +
                             "part 'meta' (JSON, BreathingUpdateRequest) + optional part 'iconFile' (replaces current icon). " +
                             "If phases are provided they fully replace the current phase list.",
            operationId = "updateBreathingExercise")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Exercise updated"),
            @ApiResponse(responseCode = "404", description = "Exercise not found")
    })
    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    ResponseEntity<BreathingDto> update(
            @Parameter(description = "Breathing exercise ID", required = true) @PathVariable Long id,
            @RequestPart("meta") @Valid BreathingUpdateRequest meta,
            @RequestPart(value = "iconFile", required = false) MultipartFile iconFile,
            @RequestPart(value = "audioFiles", required = false) List<MultipartFile> audioFiles);

    @Operation(summary = "Delete a breathing exercise",
            operationId = "deleteBreathingExercise")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Exercise deleted"),
            @ApiResponse(responseCode = "404", description = "Exercise not found")
    })
    @DeleteMapping("/{id}")
    ResponseEntity<Void> delete(
            @Parameter(description = "Breathing exercise ID", required = true) @PathVariable Long id);

    @Operation(summary = "Reorder breathing exercises",
               description = "Bulk-updates the displayOrder of exercises. Send the full ordered list with new positions.",
            operationId = "reorderBreathingExercises")
    @ApiResponses(@ApiResponse(responseCode = "204", description = "Reorder applied"))
    @PutMapping("/reorder")
    ResponseEntity<Void> reorder(@Valid @RequestBody List<BreathingReorderItem> items);

    @Operation(summary = "Upload an audio file to a phase",
               description = "Uploads one audio file and links it to the specified phase. " +
                             "Multiple calls add multiple audio options; the mobile client picks one at random.",
            operationId = "addBreathingPhaseAudio")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Audio added; returns updated exercise"),
            @ApiResponse(responseCode = "404", description = "Phase not found")
    })
    @PostMapping(value = "/phases/{phaseId}/audio", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    ResponseEntity<BreathingDto> addAudio(
            @Parameter(description = "Phase ID", required = true) @PathVariable Long phaseId,
            @RequestPart("audioFile") MultipartFile audioFile);

    @Operation(summary = "Remove an audio file from a phase",
            operationId = "removeBreathingPhaseAudio")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Audio removed; returns updated exercise"),
            @ApiResponse(responseCode = "404", description = "Phase or audio object not found")
    })
    @DeleteMapping("/phases/{phaseId}/audio/{audioObjectId}")
    ResponseEntity<BreathingDto> removeAudio(
            @Parameter(description = "Phase ID", required = true) @PathVariable Long phaseId,
            @Parameter(description = "S3 audio object ID to remove", required = true) @PathVariable Long audioObjectId);

    @Operation(
            summary = "Move breathing phase audio into the private bucket",
            description = "One-off migration. Breathing phase audio was originally uploaded to the "
                    + "PUBLIC bucket and served by unsigned URL, so premium exercises were readable "
                    + "by anyone holding the link. New uploads now go to the private bucket; this "
                    + "moves the objects that predate that change and repoints their s3_object rows. "
                    + "Idempotent and safe to re-run. Run it with dryRun=true first — the delete of "
                    + "the public copy is not reversible.",
            operationId = "migrateBreathingAudioToPrivateBucket"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Migration report"),
            @ApiResponse(responseCode = "403", description = "Caller is not an administrator"),
            @ApiResponse(responseCode = "500", description = "Private bucket is not configured")
    })
    @PostMapping("/audio/migrate-to-private")
    ResponseEntity<BreathingAudioMigrationResult> migrateAudioToPrivateBucket(
            @Parameter(description = "Report what would move without changing anything. Defaults to true.")
            @RequestParam(defaultValue = "true") boolean dryRun);
}
