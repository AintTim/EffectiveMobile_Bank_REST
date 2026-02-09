package com.example.bankcards.service;

import com.example.bankcards.dto.card.CardDto;
import com.example.bankcards.dto.card.CreateCardRequest;
import com.example.bankcards.dto.card.UpdateCardStatusRequest;
import com.example.bankcards.dto.transfer.TransferRequest;
import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.CardStatus;
import com.example.bankcards.exception.CardNotFoundException;
import com.example.bankcards.exception.DuplicateCardException;
import com.example.bankcards.exception.IllegalTransferException;
import com.example.bankcards.exception.NotEnoughFundsException;
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

    public BigDecimal getCardBalance(UUID id) {
        var card = getCardDto(id);
        return card.getBalance();
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
    public void blockCard(UUID id) {
        var card = findCardById(id);
        validateCardAccess(card);

        card.setStatus(CardStatus.BLOCKED);
        repository.save(card);
    }

    @Transactional
    public void transferFundsBetweenOwnCards(TransferRequest request) {
        validateTransferRequest(request);

        var fromCard = findCardById(request.getFromCard());
        var toCard = findCardById(request.getToCard());
        validateCardsAccess(fromCard, toCard);
        validateCardsStatus(fromCard, toCard);

        executeTransfer(fromCard, toCard, request.getAmount());
    }

    private Card findCardById(UUID id) {
        return repository.findById(id).orElseThrow(CardNotFoundException::new);
    }

    private void executeTransfer(Card sourceCard, Card targetCard, BigDecimal amount) {
        if (!sourceCard.hasSufficientBalance(amount)) {
            throw new NotEnoughFundsException("Not enough funds on source card for transfer");
        }
        sourceCard.withdraw(amount);
        targetCard.deposit(amount);

        repository.saveAll(List.of(sourceCard, targetCard));
    }

    private void validateCardNumberUniqueness(String cardNumber) {
        if (repository.existsByNumber(cardNumber)) {
            throw new DuplicateCardException();
        }
    }

    private void validateCardsStatus(Card from, Card to) {
        if (from.isBlocked()) {
            throw new IllegalTransferException("Cannot transfer from blocked card");
        }
        if (to.isBlocked()) {
            throw new IllegalTransferException("Cannot transfer to blocked card");
        }
    }

    private void validateCardsAccess(Card from, Card to) {
        validateCardAccess(from);
        validateCardAccess(to);
    }

    private void validateCardAccess(Card card) {
        var currentUser = authService.getCurrentUser();

        if (currentUser.isAdmin() || card.isOwnedBy(currentUser)) {
            return;
        }

        throw new AccessDeniedException("You do not have access to this card " + card.getId());
    }

    private void validateTransferRequest(TransferRequest request) {
        if (request.getFromCard().equals(request.getToCard())) {
            throw new IllegalTransferException("Cannot transfer to the same card");
        }
    }

    private CardDto toMaskedCardDto(Card card) {
        var cardDto = mapper.toDto(card);
        cardDto.setNumber(masker.mask(cardDto.getNumber()));
        return cardDto;
    }
}
