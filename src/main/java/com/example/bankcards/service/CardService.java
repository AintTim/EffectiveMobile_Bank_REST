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
import jakarta.transaction.Transactional;
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
    private final UserService userService;
    private final CardMapper mapper;
    private final CardNumberMasker masker;

    public List<CardDto> getAllCards() {
        var cards = repository.findAll();
        return cards.stream().map(this::toMaskedCardDto).toList();
    }

    public Page<CardDto> getUserCards(Pageable pageable) {
        var currentUser = authService.getCurrentUser();

        Specification<Card> userCardsSpec = (root, query, cb) ->
                cb.equal(root.get("user").get("id"), currentUser.getId());

        return repository.findAll(userCardsSpec, pageable)
                .map(this::toMaskedCardDto);
    }

    public CardDto getCardDto(UUID id) {
        var card = findCardById(id);
        validateCardAccess(card);

        return toMaskedCardDto(card);
    }

    @Transactional
    public CardDto createCard(CreateCardRequest request) {
        validateCardNumberUniqueness(request.getNumber());

        var user = userService.findUserById(request.getUserId());
        var card = mapper.toEntity(request);
        card.setUser(user);

        var savedCard = repository.save(card);

        return toMaskedCardDto(savedCard);
    }

    @Transactional
    public void removeCard(UUID id) {
        var card = findCardById(id);
        repository.delete(card);
    }

    @Transactional
    public CardDto updateCardStatus(UUID id, UpdateCardStatusRequest request) {
        var card = findCardById(id);
        card.setStatus(request.getStatus());

        repository.save(card);

        return toMaskedCardDto(card);
    }

    @Transactional
    public CardDto updateCardBalance(UUID id, BigDecimal balance) {
        var card = findCardById(id);
        card.setBalance(balance);

        var updatedCard = repository.save(card);

        return toMaskedCardDto(updatedCard);
    }

    public BigDecimal getCardBalance(UUID id) {
        var card = getCardDto(id);
        return card.getBalance();
    }

    private Card findCardById(UUID id) {
        return repository.findById(id).orElseThrow(CardNotFoundException::new);
    }

    private void validateCardNumberUniqueness(String cardNumber) {
        if (repository.existsByNumber(cardNumber)) {
            throw new DuplicateCardException();
        }
    }

    private void validateCardAccess(Card card) {
        var currentUser = authService.getCurrentUser();

        if (currentUser.isAdmin() || card.isOwnedBy(currentUser)) {
            return;
        }

        throw new AccessDeniedException("You do not have access to this card");
    }

    private CardDto toMaskedCardDto(Card card) {
        var cardDto = mapper.toDto(card);
        cardDto.setNumber(masker.mask(cardDto.getNumber()));
        return cardDto;
    }
}
