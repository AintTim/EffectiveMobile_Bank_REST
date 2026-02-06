package com.example.bankcards.service;

import com.example.bankcards.dto.card.CardDto;
import com.example.bankcards.dto.card.CreateCardRequest;
import com.example.bankcards.dto.card.UpdateCardStatusRequest;
import com.example.bankcards.entity.Card;
import com.example.bankcards.exception.CardNotFoundException;
import com.example.bankcards.exception.DuplicateCardException;
import com.example.bankcards.mapper.CardMapper;
import com.example.bankcards.repository.CardRepository;
import com.example.bankcards.util.CardNumberMasker;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
@AllArgsConstructor
public class CardService {

    private final AuthService authService;
    private final CardRepository repository;
    private final CardMapper mapper;
    private final CardNumberMasker masker;

    public List<CardDto> getAllCards() {
        var cards = repository.findAll();
        return cards.stream().map(this::getMaskedCard).toList();
    }

    public Page<CardDto> getUserCards(Pageable pageable) {
        var user = authService.getCurrentUser();
        Specification<Card> spec = Specification.unrestricted();
        spec.and(
                ((root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get("user"), user.getId()))
        );

        Page<Card> cards = repository.findAll(spec, pageable);
        return cards.map(this::getMaskedCard);
    }

    public CardDto getCardDto(UUID id) {
        var card = getCard(id);
        var user = authService.getCurrentUser();
        if (!user.isAdmin() && !card.isOwnedBy(user)) {
            throw new AccessDeniedException("You do not have access to this card");
        }

        return getMaskedCard(card);
    }

    public CardDto createCard(CreateCardRequest request) {
        if (repository.existsByNumber(request.getNumber())) {
            throw new DuplicateCardException();
        }

        var card = mapper.toEntity(request);
        repository.save(card);

        return getMaskedCard(card);
    }

    public void removeCard(UUID id) {
        var card = getCard(id);
        repository.delete(card);
    }

    public CardDto updateCardStatus(UUID id, UpdateCardStatusRequest request) {
        var card = getCard(id);
        card.setStatus(request.getStatus());

        repository.save(card);

        return getMaskedCard(card);
    }

    public CardDto updateCardBalance(UUID id, BigDecimal balance) {
        var card = getCard(id);
        card.setBalance(balance);

        repository.save(card);

        return getMaskedCard(card);
    }

    public BigDecimal getCardBalance(UUID id) {
        var card = getCardDto(id);
        return card.getBalance();
    }

    private Card getCard(UUID id) {
        return repository.findById(id).orElseThrow(CardNotFoundException::new);
    }

    private CardDto getMaskedCard(Card card) {
        var cardDto = mapper.toDto(card);
        cardDto.setNumber(masker.mask(cardDto.getNumber()));
        return cardDto;
    }
}
