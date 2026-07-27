package com.yogida.meditation.controller.api;

import com.yogida.meditation.dto.BreathingDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

@Tag(name = "Breathing", description = "Breathing exercises for authenticated mobile users")
@RequestMapping("/breathing")
public interface BreathingControllerApi {

    @Operation(summary = "List all breathing exercises", description = "Returns all exercises ordered by displayOrder ascending.")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "Exercises retrieved"))
    @GetMapping
    ResponseEntity<List<BreathingDto>> getAll();

    @Operation(summary = "Get a breathing exercise by ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Exercise found"),
            @ApiResponse(responseCode = "404", description = "Exercise not found")
    })
    @GetMapping("/{id}")
    ResponseEntity<BreathingDto> getById(
            @Parameter(description = "Breathing exercise ID", required = true) @PathVariable Long id);
}
