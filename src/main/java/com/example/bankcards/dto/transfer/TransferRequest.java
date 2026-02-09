package com.example.bankcards.dto.transfer;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
public class TransferRequest {

    @NotNull(message = "Source card ID must be provided")
    private UUID fromCard;

    @NotNull(message = "Target card ID must be provided")
    private UUID toCard;

    @NotNull(message = "Transfer amount must be provided")
    @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
    private BigDecimal amount;
}
