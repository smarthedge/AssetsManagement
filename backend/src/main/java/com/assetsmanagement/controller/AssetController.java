package com.assetsmanagement.controller;

import com.assetsmanagement.dto.PageResponse;
import com.assetsmanagement.dto.request.AssetRequest;
import com.assetsmanagement.dto.response.AssetResponse;
import com.assetsmanagement.dto.response.ErrorResponse;
import com.assetsmanagement.service.AssetService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for asset CRUD operations.
 * All endpoints require authentication. Create/Update/Delete require ADMIN role.
 */
@RestController
@RequestMapping("/api/assets")
@Tag(name = "Assets", description = "Asset management endpoints")
@SecurityRequirement(name = "Bearer")
public class AssetController {

    private static final Logger log = LoggerFactory.getLogger(AssetController.class);
    private final AssetService assetService;

    public AssetController(AssetService assetService) {
        this.assetService = assetService;
    }

    @Operation(summary = "List all assets", description = "Returns a paginated list of active assets. Optionally filter by category.")
    @GetMapping
    public ResponseEntity<PageResponse<AssetResponse>> getAllAssets(
            @Parameter(description = "Page number (0-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "Category filter") @RequestParam(required = false) String category) {
        log.info("GET /api/assets — page: {}, size: {}, category: {}", page, size, category);
        return ResponseEntity.ok(assetService.getAllAssets(page, size, category));
    }

    @Operation(summary = "Get asset by ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Asset found"),
            @ApiResponse(responseCode = "404", description = "Asset not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/{id}")
    public ResponseEntity<AssetResponse> getAsset(@PathVariable Long id) {
        log.info("GET /api/assets/{}", id);
        return ResponseEntity.ok(assetService.getAssetById(id));
    }

    @Operation(summary = "Create a new asset")
    @ApiResponses(@ApiResponse(responseCode = "201", description = "Asset created"))
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
//    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<AssetResponse> createAsset(@Valid @RequestBody AssetRequest request) {
        log.info("POST /api/assets — name: {}", request.name());
        AssetResponse created = assetService.createAsset(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @Operation(summary = "Update an existing asset")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Asset updated"),
            @ApiResponse(responseCode = "404", description = "Asset not found"),
            @ApiResponse(responseCode = "409", description = "Concurrent modification conflict")
    })
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AssetResponse> updateAsset(@PathVariable Long id, @Valid @RequestBody AssetRequest request) {
        log.info("PUT /api/assets/{}", id);
        return ResponseEntity.ok(assetService.updateAsset(id, request));
    }

    @Operation(summary = "Soft-delete an asset", description = "Sets the asset status to false. Record is preserved for audit.")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteAsset(@PathVariable Long id) {
        log.info("DELETE /api/assets/{}", id);
        assetService.deleteAsset(id);
        return ResponseEntity.noContent().build();
    }
}
