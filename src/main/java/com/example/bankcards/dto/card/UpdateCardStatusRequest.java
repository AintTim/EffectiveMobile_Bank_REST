package com.example.bankcards.dto.card;

import com.example.bankcards.entity.CardStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class UpdateCardStatusRequest {

    @NotNull(message = "Card status must be provided")
    private CardStatus status;
}
