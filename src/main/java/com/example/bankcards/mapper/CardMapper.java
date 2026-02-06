package com.example.bankcards.mapper;

import com.example.bankcards.dto.card.CardDto;
import com.example.bankcards.dto.card.CreateCardRequest;
import com.example.bankcards.entity.Card;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface CardMapper {

    Card toEntity(CreateCardRequest request);
    List<CardDto> toDtos(List<Card> cards);
    CardDto toDto(Card card);
    // update() - для обновления баланса
}
