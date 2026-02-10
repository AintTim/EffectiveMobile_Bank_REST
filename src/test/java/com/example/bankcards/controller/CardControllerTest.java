package com.example.bankcards.controller;

import com.example.bankcards.dto.card.CardDto;
import com.example.bankcards.dto.card.CreateCardRequest;
import com.example.bankcards.dto.card.UpdateCardStatusRequest;
import com.example.bankcards.dto.transfer.TransferRequest;
import com.example.bankcards.entity.CardStatus;
import com.example.bankcards.exception.*;
import com.example.bankcards.service.CardService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class CardControllerTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @Mock
    private CardService cardService;

    @InjectMocks
    private CardController cardController;

    private UUID cardId1;
    private UUID cardId2;
    private CardDto cardDto1;
    private CreateCardRequest createCardRequest;
    private TransferRequest transferRequest;
    private UpdateCardStatusRequest updateCardStatusRequest;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules();

        mockMvc = MockMvcBuilders.standaloneSetup(cardController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        cardId1 = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");
        cardId2 = UUID.fromString("123e4567-e89b-12d3-a456-426614174001");

        cardDto1 = CardDto.builder()
                .id(cardId1)
                .number("**** **** **** 5678")
                .userId(1L)
                .expirationDate(LocalDate.now().plusYears(3))
                .status(CardStatus.ACTIVE)
                .balance(new BigDecimal("1000.00"))
                .build();

        createCardRequest = CreateCardRequest.builder()
                .number("1111222233334444")
                .userId(1L)
                .expirationDate(LocalDate.now().plusYears(4))
                .build();

        transferRequest = TransferRequest.builder()
                .fromCard(cardId1)
                .toCard(cardId2)
                .amount(new BigDecimal("100.00"))
                .build();

        updateCardStatusRequest = new UpdateCardStatusRequest(CardStatus.BLOCKED);
    }

    @Test
    void createCard_ValidRequest_ShouldReturnCreated() throws Exception {
        when(cardService.createCard(any(CreateCardRequest.class))).thenReturn(cardDto1);

        mockMvc.perform(post("/api/cards")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createCardRequest)))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(header().string("Location", containsString("/cards/" + cardId1)))
                .andExpect(jsonPath("$.id", is(cardId1.toString())))
                .andExpect(jsonPath("$.number", is("**** **** **** 5678")))
                .andExpect(jsonPath("$.status", is("ACTIVE")))
                .andExpect(jsonPath("$.balance", is(1000.00)));

        verify(cardService, times(1)).createCard(any(CreateCardRequest.class));
    }

    @Test
    void createCard_InvalidRequest_ShouldReturnBadRequest() throws Exception {
        CreateCardRequest invalidRequest = CreateCardRequest.builder()
                .number("")
                .userId(null)
                .build();

        mockMvc.perform(post("/api/cards")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());

        verify(cardService, never()).createCard(any());
    }

    @Test
    void createCard_DuplicateCardNumber_ShouldReturnBadRequest() throws Exception {
        when(cardService.createCard(any(CreateCardRequest.class)))
                .thenThrow(new DuplicateCardException());

        mockMvc.perform(post("/api/cards")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createCardRequest)))
                .andExpect(status().isBadRequest());

        verify(cardService, times(1)).createCard(any(CreateCardRequest.class));
    }

    @Test
    void transferBetweenOwnCards_ValidRequest_ShouldReturn() throws Exception {
        doNothing().when(cardService).transferFundsBetweenOwnCards(any(TransferRequest.class));

        mockMvc.perform(post("/api/cards/transfer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(transferRequest)))
                .andExpect(status().isOk());

        verify(cardService, times(1)).transferFundsBetweenOwnCards(any(TransferRequest.class));
    }

    @Test
    void transferBetweenOwnCards_InvalidRequest_ShouldReturnBadRequest() throws Exception {
        TransferRequest invalidRequest = TransferRequest.builder()
                .fromCard(null)
                .toCard(cardId2)
                .amount(new BigDecimal("-100.00"))
                .build();

        mockMvc.perform(post("/api/cards/transfer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());

        verify(cardService, never()).transferFundsBetweenOwnCards(any());
    }

    @Test
    void transferBetweenOwnCards_IllegalTransfer_ShouldReturnBadRequest() throws Exception {
        String errorMessage = "Cannot transfer to the same card";
        doThrow(new IllegalTransferException(errorMessage))
                .when(cardService).transferFundsBetweenOwnCards(any(TransferRequest.class));

        mockMvc.perform(post("/api/cards/transfer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(transferRequest)))
                .andExpect(status().isBadRequest());

        verify(cardService, times(1)).transferFundsBetweenOwnCards(any(TransferRequest.class));
    }

    @Test
    void transferBetweenOwnCards_NotEnoughFunds_ShouldReturnBadRequest() throws Exception {
        String errorMessage = "Not enough funds on source card";
        doThrow(new NotEnoughFundsException(errorMessage))
                .when(cardService).transferFundsBetweenOwnCards(any(TransferRequest.class));

        mockMvc.perform(post("/api/cards/transfer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(transferRequest)))
                .andExpect(status().isBadRequest());

        verify(cardService, times(1)).transferFundsBetweenOwnCards(any(TransferRequest.class));
    }

    @Test
    void transferBetweenOwnCards_AccessDenied_ShouldReturnForbidden() throws Exception {
        String errorMessage = "You do not have access to one of the cards";
        doThrow(new AccessDeniedException(errorMessage))
                .when(cardService).transferFundsBetweenOwnCards(any(TransferRequest.class));

        mockMvc.perform(post("/api/cards/transfer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(transferRequest)))
                .andExpect(status().isForbidden());

        verify(cardService, times(1)).transferFundsBetweenOwnCards(any(TransferRequest.class));
    }

    @Test
    void getCard_ValidId_ShouldReturnCard() throws Exception {
        when(cardService.getCardDto(cardId1)).thenReturn(cardDto1);

        mockMvc.perform(get("/api/cards/{id}", cardId1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(cardId1.toString())))
                .andExpect(jsonPath("$.number", is("**** **** **** 5678")))
                .andExpect(jsonPath("$.balance", is(1000.00)));

        verify(cardService, times(1)).getCardDto(cardId1);
    }

    @Test
    void getCard_CardNotFound_ShouldReturnNotFound() throws Exception {
        when(cardService.getCardDto(cardId1)).thenThrow(new CardNotFoundException());

        mockMvc.perform(get("/api/cards/{id}", cardId1))
                .andExpect(status().isNotFound());

        verify(cardService, times(1)).getCardDto(cardId1);
    }

    @Test
    void getCard_AccessDenied_ShouldReturnForbidden() throws Exception {
        String errorMessage = "You do not have access to this card";
        when(cardService.getCardDto(cardId1)).thenThrow(new AccessDeniedException(errorMessage));

        mockMvc.perform(get("/api/cards/{id}", cardId1))
                .andExpect(status().isForbidden());

        verify(cardService, times(1)).getCardDto(cardId1);
    }

    @Test
    void getCardBalance_ValidId_ShouldReturnBalance() throws Exception {
        when(cardService.getCardBalance(cardId1)).thenReturn(new BigDecimal("1000.00"));

        mockMvc.perform(get("/api/cards/{id}/balance", cardId1))
                .andExpect(status().isOk())
                .andExpect(content().string("1000.00"));

        verify(cardService, times(1)).getCardBalance(cardId1);
    }

    @Test
    void getCardBalance_CardNotFound_ShouldReturnNotFound() throws Exception {
        when(cardService.getCardBalance(cardId1)).thenThrow(new CardNotFoundException());

        mockMvc.perform(get("/api/cards/{id}/balance", cardId1))
                .andExpect(status().isNotFound());

        verify(cardService, times(1)).getCardBalance(cardId1);
    }

    @Test
    void updateCardStatus_ValidRequest_ShouldReturnUpdatedCard() throws Exception {
        CardDto blockedCardDto = CardDto.builder()
                .id(cardId1)
                .status(CardStatus.BLOCKED)
                .build();

        when(cardService.updateCardStatus(eq(cardId1), any(UpdateCardStatusRequest.class)))
                .thenReturn(blockedCardDto);

        mockMvc.perform(patch("/api/cards/{id}", cardId1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateCardStatusRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(cardId1.toString())))
                .andExpect(jsonPath("$.status", is("BLOCKED")));

        verify(cardService, times(1)).updateCardStatus(eq(cardId1), any(UpdateCardStatusRequest.class));
    }

    @Test
    void updateCardStatus_CardNotFound_ShouldReturnNotFound() throws Exception {
        when(cardService.updateCardStatus(eq(cardId1), any(UpdateCardStatusRequest.class)))
                .thenThrow(new CardNotFoundException());

        mockMvc.perform(patch("/api/cards/{id}", cardId1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateCardStatusRequest)))
                .andExpect(status().isNotFound());

        verify(cardService, times(1)).updateCardStatus(eq(cardId1), any(UpdateCardStatusRequest.class));
    }

    @Test
    void deleteCard_ValidId_ShouldReturnNoContent() throws Exception {
        doNothing().when(cardService).removeCard(cardId1);

        mockMvc.perform(delete("/api/cards/{id}", cardId1))
                .andExpect(status().isNoContent());

        verify(cardService, times(1)).removeCard(cardId1);
    }

    @Test
    void deleteCard_CardNotFound_ShouldReturnNotFound() throws Exception {
        doThrow(new CardNotFoundException()).when(cardService).removeCard(cardId1);

        mockMvc.perform(delete("/api/cards/{id}", cardId1))
                .andExpect(status().isNotFound());

        verify(cardService, times(1)).removeCard(cardId1);
    }
}