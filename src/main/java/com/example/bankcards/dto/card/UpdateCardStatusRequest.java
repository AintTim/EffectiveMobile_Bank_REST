package com.example.bankcards.dto.card;

import com.example.bankcards.entity.CardStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdateCardStatusRequest {

    @NotNull(message = "Card status must be provided")
    private CardStatus status;
}
