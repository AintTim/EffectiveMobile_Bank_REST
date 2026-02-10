package com.example.bankcards.dto.card;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CreateCardRequest {

    @ValidNumber
    private String number;

    @NotNull(message = "User ID must be provided")
    private Long userId;

    @NotNull(message = "Expiration date must be provided")
    @FutureOrPresent
    @JsonFormat(pattern = "dd.MM.yyyy")
    private LocalDate expirationDate;

    @NotNull(message = "Balance must be provided")
    @DecimalMin(value = "0.01", message = "Balance must be greater than 0")
    private BigDecimal balance;
}
