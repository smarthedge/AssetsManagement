package com.assetsmanagement.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record AssetResponse(
        Long id,
        String name,
        String description,
        String category,
        String serialNumber,
        LocalDate purchaseDate,
        BigDecimal value,
        Boolean status,
        Long createdByUserId,
        String createdByUsername,
        LocalDateTime createdDateTime,
        Long lastChangedByUserId,
        String lastChangedByUsername,
        LocalDateTime lastChangedDateTime,
        Integer version
) {}
