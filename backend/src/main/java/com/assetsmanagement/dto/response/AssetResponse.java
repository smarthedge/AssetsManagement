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
        String createdByUsername,
        LocalDateTime createdDateTime,
        String lastChangedByUsername,
        LocalDateTime lastChangedDateTime,
        Integer version
) {}
