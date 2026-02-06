package com.example.bankcards.dto.card;

import com.example.bankcards.entity.CardStatus;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Data
public class CardDto {

    private UUID id;
    private Long userId;
    private String number;
    private LocalDate expirationDate;
    private CardStatus status;
    private BigDecimal balance;
}
