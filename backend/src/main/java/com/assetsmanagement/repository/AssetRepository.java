package com.assetsmanagement.repository;

import com.assetsmanagement.entity.Asset;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AssetRepository extends JpaRepository<Asset, Long> {

    Optional<Asset> findBySerialNumber(String serialNumber);

    Page<Asset> findByStatusTrue(Pageable pageable);

    Page<Asset> findByCategoryAndStatusTrue(String category, Pageable pageable);
}
