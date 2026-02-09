package com.example.bankcards.controller;

import com.example.bankcards.dto.card.CardDto;
import com.example.bankcards.dto.card.CreateCardRequest;
import com.example.bankcards.dto.card.UpdateCardStatusRequest;
import com.example.bankcards.dto.transfer.TransferRequest;
import com.example.bankcards.exception.*;
import com.example.bankcards.service.CardService;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@AllArgsConstructor
@RestController
@RequestMapping("/api/cards")
public class CardController {

    private final CardService service;

    @PostMapping
    public ResponseEntity<CardDto> createCard(
            @Valid @RequestBody CreateCardRequest request,
            UriComponentsBuilder uriBuilder) {
        var card = service.createCard(request);
        var uri = uriBuilder.path("/cards/{id}").buildAndExpand(card.getId()).toUri();

        return ResponseEntity.created(uri).body(card);
    }

    @PostMapping("/block/{id}")
    public void blockOwnCard(@PathVariable(name = "id") UUID id) {
        service.blockCard(id);
    }

    @PostMapping("/transfer")
    public void transferBetweenOwnCards(@Valid @RequestBody TransferRequest request) {
        service.transferFundsBetweenOwnCards(request);
    }

    @GetMapping("/{id}")
    public CardDto getCard(@PathVariable(name = "id") UUID id) {
        return service.getCardDto(id);
    }

    @PatchMapping("/{id}")
    public CardDto updateCardStatus(
            @PathVariable(name = "id") UUID id,
            @Valid @RequestBody UpdateCardStatusRequest request) {
        return service.updateCardStatus(id, request);
    }

    @DeleteMapping("/{id}")
    public void deleteCard(@PathVariable(name = "id") UUID id) {
        service.removeCard(id);
    }

    @GetMapping
    public List<CardDto> getCards() {
        return service.getAllCards();
    }

    @GetMapping("/my")
    public Page<CardDto> getUserCards(
            @PageableDefault(sort = "expirationDate", direction = Sort.Direction.ASC) Pageable pageable) {
        return service.getUserCards(pageable);
    }

    @GetMapping("/{id}/balance")
    public BigDecimal getCardBalance(@PathVariable(name = "id") UUID id) {
        return service.getCardBalance(id);
    }

    @ExceptionHandler(DuplicateCardException.class)
    public ResponseEntity<ErrorDto> handleDuplicateCardException() {
        return ResponseEntity.badRequest()
                .body(new ErrorDto("Card number is already registered."));
    }

    @ExceptionHandler(CardNotFoundException.class)
    public ResponseEntity<Void> handleCardNotFound() {
        return ResponseEntity.notFound().build();
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorDto> handleAccessDenied(Exception ex) {
        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(new ErrorDto(ex.getMessage()));
    }

    @ExceptionHandler(IllegalTransferException.class)
    public ResponseEntity<ErrorDto> handleIllegalTransfer(Exception ex) {
        return ResponseEntity.badRequest().body(new ErrorDto(ex.getMessage()));
    }

    @ExceptionHandler(NotEnoughFundsException.class)
    public ResponseEntity<ErrorDto> handleNotEnoughFunds(Exception ex) {
        return ResponseEntity.badRequest().body(new ErrorDto(ex.getMessage()));
    }
}
