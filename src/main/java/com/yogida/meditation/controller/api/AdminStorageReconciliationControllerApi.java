package com.yogida.meditation.controller.api;

import com.yogida.meditation.dto.StorageReconciliationReport;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Tag(name = "Admin — Storage reconciliation",
     description = "Read-only comparison of the s3_object table against what the buckets hold")
@RequestMapping("/admin/storage")
public interface AdminStorageReconciliationControllerApi {

    @Operation(
            summary = "Reconcile s3_object rows against bucket contents",
            description = "Reports three things: s3_object rows nothing references (each one "
                    + "permanently blocks its own key, because of the unique constraint on "
                    + "bucket/base_url/uri), object keys in a bucket with no row (wasted storage), "
                    + "and rows pointing at objects that are not there (a 404 at playback). "
                    + "Changes nothing — deleting storage on the strength of a query is how a bug "
                    + "turns into data loss.",
            operationId = "reconcileStorage"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Reconciliation report"),
            @ApiResponse(responseCode = "403", description = "Caller is not an administrator")
    })
    @GetMapping("/reconciliation")
    ResponseEntity<StorageReconciliationReport> reconcile();
}
