package com.assetsmanagement.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;
import java.time.LocalDate;

public record AssetRequest(
        @NotBlank(message = "Asset name is required")
        String name,

        String description,

        String category,

        String serialNumber,

        @PastOrPresent(message = "Purchase date must be in the past or present")
        LocalDate purchaseDate,

        @PositiveOrZero(message = "Value must be zero or positive")
        BigDecimal value
) {}
