package com.example.bankcards.controller;

import com.example.bankcards.dto.card.CardDto;
import com.example.bankcards.dto.card.CreateCardRequest;
import com.example.bankcards.dto.card.UpdateCardStatusRequest;
import com.example.bankcards.dto.transfer.TransferRequest;
import com.example.bankcards.exception.*;
import com.example.bankcards.service.CardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
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

@Tag(
        name = "Банковские карты",
        description = "API для управления банковскими картами, блокировки и переводов"
)
@AllArgsConstructor
@RestController
@RequestMapping("/api/cards")
public class CardController {

    private final CardService service;

    @Operation(
            summary = "Создать новую карту",
            description = "Только для администраторов. Создает новую банковскую карту."
    )
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping
    public ResponseEntity<CardDto> createCard(
            @Valid @RequestBody CreateCardRequest request,
            UriComponentsBuilder uriBuilder) {
        var card = service.createCard(request);
        var uri = uriBuilder.path("/cards/{id}").buildAndExpand(card.getId()).toUri();

        return ResponseEntity.created(uri).body(card);
    }

    @Operation(
            summary = "Заблокировать свою карту",
            description = "Позволяет владельцу карты заблокировать её."
    )
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Карта успешно заблокирована"),
            @ApiResponse(responseCode = "403", description = "Нет доступа к карте"),
            @ApiResponse(responseCode = "404", description = "Карта не найдена")
    })
    @PostMapping("/block/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void blockOwnCard(@PathVariable(name = "id") UUID id) {
        service.blockCard(id);
    }

    @Operation(
            summary = "Перевод между своими картами",
            description = "Перевод средств между картами одного пользователя."
    )
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Перевод выполнен успешно"),
            @ApiResponse(responseCode = "400", description = "Недостаточно средств или некорректные данные"),
            @ApiResponse(responseCode = "403", description = "Нет доступа к одной из карт")
    })
    @PostMapping("/transfer")
    public void transferBetweenOwnCards(@Valid @RequestBody TransferRequest request) {
        service.transferFundsBetweenOwnCards(request);
    }

    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/{id}")
    public CardDto getCard(@PathVariable(name = "id") UUID id) {
        return service.getCardDto(id);
    }

    @SecurityRequirement(name = "bearerAuth")
    @GetMapping
    public List<CardDto> getCards() {
        return service.getAllCards();
    }

    @Operation(
            summary = "Получить свои карты",
            description = """
        Получить пагинированный список карт текущего пользователя.
        
        **Примеры запросов:**
        - `GET /api/cards/my` - первая страница, 20 карт, сортировка по expirationDate ASC
        - `GET /api/cards/my?page=1&size=10` - вторая страница, 10 карт на странице
        - `GET /api/cards/my?sort=balance,desc` - сортировка по балансу по убыванию
        - `GET /api/cards/my?sort=status,asc&sort=expirationDate,desc` - множественная сортировка
        
        **Поддерживаемые поля для сортировки:**
        - `expirationDate` (по умолчанию) - дата истечения срока
        - `balance` - баланс карты
        - `status` - статус карты
        """
    )
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/my")
    public Page<CardDto> getUserCards(
            @Parameter(description = "Параметры пагинации и сортировки")
            @PageableDefault(sort = "expirationDate", direction = Sort.Direction.ASC) Pageable pageable) {
        return service.getUserCards(pageable);
    }

    @Operation(
            summary = "Получить баланс карты",
            description = "Получить текущий баланс карты."
    )
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/{id}/balance")
    public BigDecimal getCardBalance(@PathVariable(name = "id") UUID id) {
        return service.getCardBalance(id);
    }

    @SecurityRequirement(name = "bearerAuth")
    @PatchMapping("/{id}")
    public CardDto updateCardStatus(
            @PathVariable(name = "id") UUID id,
            @Valid @RequestBody UpdateCardStatusRequest request) {
        return service.updateCardStatus(id, request);
    }

    @SecurityRequirement(name = "bearerAuth")
    @DeleteMapping("/{id}")
    public void deleteCard(@PathVariable(name = "id") UUID id) {
        service.removeCard(id);
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
