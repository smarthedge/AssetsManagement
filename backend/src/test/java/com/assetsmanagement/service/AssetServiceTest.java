package com.assetsmanagement.service;

import com.assetsmanagement.dto.PageResponse;
import com.assetsmanagement.dto.request.AssetRequest;
import com.assetsmanagement.dto.response.AssetResponse;
import com.assetsmanagement.entity.Asset;
import com.assetsmanagement.exception.BadRequestException;
import com.assetsmanagement.exception.ResourceNotFoundException;
import com.assetsmanagement.repository.AssetRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AssetServiceTest {

    @Mock
    private AssetRepository assetRepository;

    @InjectMocks
    private AssetService assetService;

    private Asset asset;

    @BeforeEach
    void setUp() {
        asset = Asset.builder()
                .id(1L)
                .name("Test Laptop")
                .description("A test asset")
                .category("Laptop")
                .serialNumber("SN-TEST-001")
                .purchaseDate(LocalDate.now())
                .value(new BigDecimal("1500.00"))
                .build();
        asset.setStatus(true);
        asset.setCreatedByUsername("admin");
        asset.setCreatedDateTime(LocalDateTime.now());
        asset.setLastChangedByUsername("admin");
        asset.setLastChangedDateTime(LocalDateTime.now());
        asset.setVersion(0);
    }

    @Test
    void getAssetById_shouldReturnAsset() {
        when(assetRepository.findById(1L)).thenReturn(Optional.of(asset));

        AssetResponse response = assetService.getAssetById(1L);

        assertNotNull(response);
        assertEquals("Test Laptop", response.name());
        assertEquals("SN-TEST-001", response.serialNumber());
    }

    @Test
    void getAssetById_shouldThrowWhenNotFound() {
        when(assetRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> assetService.getAssetById(99L));
    }

    @Test
    void getAssetById_shouldThrowWhenSoftDeleted() {
        asset.setStatus(false);
        when(assetRepository.findById(1L)).thenReturn(Optional.of(asset));

        assertThrows(ResourceNotFoundException.class, () -> assetService.getAssetById(1L));
    }

    @Test
    void createAsset_shouldSaveAndReturnAsset() {
        when(assetRepository.findBySerialNumber("SN-TEST-001")).thenReturn(Optional.empty());
        when(assetRepository.save(any(Asset.class))).thenReturn(asset);

        AssetRequest request = new AssetRequest("Test Laptop", "A test asset", "Laptop",
                "SN-TEST-001", LocalDate.now(), new BigDecimal("1500.00"));
        AssetResponse response = assetService.createAsset(request);

        assertNotNull(response);
        assertEquals("Test Laptop", response.name());
        verify(assetRepository).save(any(Asset.class));
    }

    @Test
    void createAsset_shouldThrowWhenSerialNumberExists() {
        when(assetRepository.findBySerialNumber("SN-TEST-001")).thenReturn(Optional.of(asset));

        AssetRequest request = new AssetRequest("Another Asset", "Desc", "Laptop",
                "SN-TEST-001", LocalDate.now(), new BigDecimal("1500.00"));

        assertThrows(BadRequestException.class, () -> assetService.createAsset(request));
    }

    @Test
    void updateAsset_shouldUpdateFields() {
        when(assetRepository.findById(1L)).thenReturn(Optional.of(asset));
        when(assetRepository.findBySerialNumber("SN-NEW")).thenReturn(Optional.empty());
        when(assetRepository.save(any(Asset.class))).thenReturn(asset);

        AssetRequest request = new AssetRequest("Updated Laptop", "Updated desc", "Desktop",
                "SN-NEW", LocalDate.now().minusDays(1), new BigDecimal("2000.00"));
        AssetResponse response = assetService.updateAsset(1L, request);

        assertNotNull(response);
        verify(assetRepository).save(any(Asset.class));
    }

    @Test
    void deleteAsset_shouldSoftDelete() {
        when(assetRepository.findById(1L)).thenReturn(Optional.of(asset));
        when(assetRepository.save(any(Asset.class))).thenReturn(asset);

        assetService.deleteAsset(1L);

        assertEquals(false, asset.getStatus());
        verify(assetRepository).save(asset);
    }

    @Test
    void getAllAssets_shouldReturnPaginatedResults() {
        Page<Asset> assetPage = new PageImpl<>(List.of(asset), PageRequest.of(0, 20, Sort.by("name")), 1);
        when(assetRepository.findByStatusTrue(any(PageRequest.class))).thenReturn(assetPage);

        PageResponse<AssetResponse> response = assetService.getAllAssets(0, 20, null);

        assertEquals(1, response.totalElements());
        assertEquals(1, response.content().size());
        assertEquals("Test Laptop", response.content().getFirst().name());
    }
}
