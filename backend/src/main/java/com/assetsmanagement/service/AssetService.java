package com.assetsmanagement.service;

import com.assetsmanagement.dto.PageResponse;
import com.assetsmanagement.dto.request.AssetRequest;
import com.assetsmanagement.dto.response.AssetResponse;
import com.assetsmanagement.entity.Asset;
import com.assetsmanagement.exception.BadRequestException;
import com.assetsmanagement.exception.ResourceNotFoundException;
import com.assetsmanagement.repository.AssetRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Manages asset lifecycle with audit tracking, soft delete, and optimistic locking.
 */
@Service
@Transactional
public class AssetService {

    private static final Logger log = LoggerFactory.getLogger(AssetService.class);

    private final AssetRepository assetRepository;

    public AssetService(AssetRepository assetRepository) {
        this.assetRepository = assetRepository;
    }

    /**
     * Retrieves a paginated list of active assets, optionally filtered by category.
     */
    @Transactional(readOnly = true)
    public PageResponse<AssetResponse> getAllAssets(int page, int size, String category) {
        log.info("Fetching assets — page: {}, size: {}, category: {}", page, size, category);
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by("name").ascending());

        Page<Asset> assetPage = (category != null && !category.isBlank())
                ? assetRepository.findByCategoryAndStatusTrue(category, pageRequest)
                : assetRepository.findByStatusTrue(pageRequest);

        log.debug("Found {} assets (total: {})", assetPage.getNumberOfElements(), assetPage.getTotalElements());

        return new PageResponse<>(
                assetPage.map(this::toResponse).getContent(),
                assetPage.getNumber(),
                assetPage.getSize(),
                assetPage.getTotalElements(),
                assetPage.getTotalPages()
        );
    }

    /**
     * Retrieves a single asset by ID. Throws 404 if not found or soft-deleted.
     */
    @Transactional(readOnly = true)
    public AssetResponse getAssetById(Long id) {
        log.info("Fetching asset by ID: {}", id);
        Asset asset = findActiveAsset(id);
        return toResponse(asset);
    }

    /**
     * Creates a new asset. Checks for duplicate serial numbers.
     */
    public AssetResponse createAsset(AssetRequest request) {
        log.info("Creating asset: {}", request.name());

        if (request.serialNumber() != null && !request.serialNumber().isBlank()) {
            assetRepository.findBySerialNumber(request.serialNumber()).ifPresent(a -> {
                throw new BadRequestException("Serial number already exists: " + request.serialNumber());
            });
        }

        Asset asset = Asset.builder()
                .name(request.name())
                .description(request.description())
                .category(request.category())
                .serialNumber(request.serialNumber())
                .purchaseDate(request.purchaseDate())
                .value(request.value())
                .build();

        Asset saved = assetRepository.save(asset);
        log.info("Asset created: ID={}, name={}", saved.getId(), saved.getName());
        return toResponse(saved);
    }

    /**
     * Updates an existing asset. Checks for optimistic locking conflicts.
     */
    public AssetResponse updateAsset(Long id, AssetRequest request) {
        log.info("Updating asset ID: {}", id);
        Asset asset = findActiveAsset(id);

        if (request.serialNumber() != null && !request.serialNumber().isBlank()
                && !request.serialNumber().equals(asset.getSerialNumber())) {
            assetRepository.findBySerialNumber(request.serialNumber()).ifPresent(a -> {
                throw new BadRequestException("Serial number already exists: " + request.serialNumber());
            });
        }

        asset.setName(request.name());
        asset.setDescription(request.description());
        asset.setCategory(request.category());
        asset.setSerialNumber(request.serialNumber());
        asset.setPurchaseDate(request.purchaseDate());
        asset.setValue(request.value());

        Asset saved = assetRepository.save(asset);
        log.info("Asset updated: ID={}, name={}, version={}", saved.getId(), saved.getName(), saved.getVersion());
        return toResponse(saved);
    }

    /**
     * Soft-deletes an asset by setting status=false. The record remains in the database for audit.
     */
    public void deleteAsset(Long id) {
        log.info("Soft-deleting asset ID: {}", id);
        Asset asset = findActiveAsset(id);
        asset.setStatus(false);
        assetRepository.save(asset);
        log.info("Asset soft-deleted: ID={}, name={}", asset.getId(), asset.getName());
    }

    /**
     * Finds an active (non-deleted) asset by ID. Throws ResourceNotFoundException if not found or deleted.
     */
    private Asset findActiveAsset(Long id) {
        Asset asset = assetRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Asset", "id", id));
        if (Boolean.FALSE.equals(asset.getStatus())) {
            log.warn("Attempted to access soft-deleted asset ID: {}", id);
            throw new ResourceNotFoundException("Asset", "id", id);
        }
        return asset;
    }

    private AssetResponse toResponse(Asset asset) {
        return new AssetResponse(
                asset.getId(), asset.getName(), asset.getDescription(), asset.getCategory(),
                asset.getSerialNumber(), asset.getPurchaseDate(), asset.getValue(),
                asset.getStatus(), asset.getCreatedByUsername(),
                asset.getCreatedDateTime(), asset.getLastChangedByUsername(),
                asset.getLastChangedDateTime(), asset.getVersion());
    }
}
