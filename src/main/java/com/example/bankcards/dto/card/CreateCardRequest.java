package com.example.bankcards.dto.card;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public class CreateCardRequest {

    @ValidNumber
    private String number;

    @NotNull(message = "User ID must be provided")
    private Long userId;

    @NotNull(message = "Expiration date must be provided")
    @FutureOrPresent
    @JsonFormat(pattern = "dd.MM.yyyy")
    private LocalDate expirationDate;
}
