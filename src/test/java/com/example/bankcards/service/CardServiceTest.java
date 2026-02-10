package com.example.bankcards.service;

import com.example.bankcards.dto.card.CardDto;
import com.example.bankcards.dto.card.CreateCardRequest;
import com.example.bankcards.dto.transfer.TransferRequest;
import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.CardStatus;
import com.example.bankcards.entity.Role;
import com.example.bankcards.entity.User;
import com.example.bankcards.exception.CardNotFoundException;
import com.example.bankcards.exception.DuplicateCardException;
import com.example.bankcards.exception.IllegalTransferException;
import com.example.bankcards.exception.NotEnoughFundsException;
import com.example.bankcards.mapper.CardMapper;
import com.example.bankcards.repository.CardRepository;
import com.example.bankcards.util.CardNumberMasker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

import static org.hibernate.validator.internal.util.Contracts.assertNotNull;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CardServiceTest {

    @Mock
    private AuthService authService;

    @Mock
    private CardRepository cardRepository;

    @Mock
    private UserService userService;

    @Mock
    private CardMapper cardMapper;

    @Mock
    private CardNumberMasker cardNumberMasker;

    @InjectMocks
    private CardService cardService;

    private User user;
    private Card card1;
    private Card card2;
    private CardDto cardDto1;
    private CardDto cardDto2;

    @BeforeEach
    void setUp() {
        user = new User(1L, "user@example.com", Role.USER);

        card1 = Card.builder()
                .id(UUID.randomUUID())
                .number("1234567812345678")
                .user(user)
                .expirationDate(LocalDate.now().plusYears(3))
                .status(CardStatus.ACTIVE)
                .balance(new BigDecimal("1000.00"))
                .build();

        card2 = Card.builder()
                .id(UUID.randomUUID())
                .number("8765432187654321")
                .user(user)
                .expirationDate(LocalDate.now().plusYears(2))
                .status(CardStatus.ACTIVE)
                .balance(new BigDecimal("500.00"))
                .build();

        cardDto1 = CardDto.builder()
                .id(card1.getId())
                .number("**** **** **** 5678")
                .userId(user.getId())
                .expirationDate(card1.getExpirationDate())
                .status(CardStatus.ACTIVE)
                .balance(new BigDecimal("1000.00"))
                .build();

        cardDto2 = CardDto.builder()
                .id(card2.getId())
                .number("**** **** **** 4321")
                .userId(user.getId())
                .expirationDate(card2.getExpirationDate())
                .status(CardStatus.ACTIVE)
                .balance(new BigDecimal("500.00"))
                .build();
    }

    @Test
    void getAllCards_ShouldReturnAllCardsWithMaskedNumbers() {
        List<Card> cards = Arrays.asList(card1, card2);
        when(cardRepository.findAll()).thenReturn(cards);
        when(cardMapper.toDto(card1)).thenReturn(cardDto1);
        when(cardMapper.toDto(card2)).thenReturn(cardDto2);
        when(cardNumberMasker.mask(cardDto1.getNumber())).thenReturn("**** **** **** 5678");
        when(cardNumberMasker.mask(cardDto2.getNumber())).thenReturn("**** **** **** 4321");

        List<CardDto> result = cardService.getAllCards();

        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("**** **** **** 5678", result.get(0).getNumber());
        assertEquals("**** **** **** 4321", result.get(1).getNumber());
        verify(cardRepository, times(1)).findAll();
        verify(cardMapper, times(2)).toDto(any(Card.class));
        verify(cardNumberMasker, times(2)).mask(anyString());
    }

    @Test
    void getAllCards_ShouldReturnEmptyList_WhenNoCards() {
        when(cardRepository.findAll()).thenReturn(Collections.emptyList());

        List<CardDto> result = cardService.getAllCards();

        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(cardRepository, times(1)).findAll();
    }

    @Test
    void getUserCards_ShouldReturnPaginatedUserCards() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Card> cardPage = new PageImpl<>(Collections.singletonList(card1), pageable, 1);

        when(authService.getCurrentUser()).thenReturn(user);
        when(cardRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(cardPage);
        when(cardMapper.toDto(card1)).thenReturn(cardDto1);
        when(cardNumberMasker.mask(anyString())).thenReturn("**** **** **** 5678");

        Page<CardDto> result = cardService.getUserCards(pageable);

        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        assertEquals(cardDto1.getId(), result.getContent().get(0).getId());
        verify(authService, times(1)).getCurrentUser();
        verify(cardRepository, times(1)).findAll(any(Specification.class), eq(pageable));
    }

    @Test
    void getCardDto_ShouldThrowAccessDeniedException_WhenUserNotOwnerAndNotAdmin() {
        User otherUser = new User(999L, "some@mail.ru", Role.USER);
        when(authService.getCurrentUser()).thenReturn(otherUser);
        when(cardRepository.findById(card1.getId())).thenReturn(Optional.of(card1));

        assertThrows(AccessDeniedException.class, () ->
                cardService.getCardDto(card1.getId()));
    }

    @Test
    void getCardDto_ShouldThrowCardNotFoundException_WhenCardNotFound() {
        UUID nonExistentId = UUID.randomUUID();
        when(cardRepository.findById(nonExistentId)).thenReturn(Optional.empty());

        assertThrows(CardNotFoundException.class, () ->
                cardService.getCardDto(nonExistentId));
    }

    @Test
    void getCardBalance_ShouldReturnCorrectBalance() {
        when(authService.getCurrentUser()).thenReturn(user);
        when(cardRepository.findById(card1.getId())).thenReturn(Optional.of(card1));
        when(cardMapper.toDto(card1)).thenReturn(cardDto1);
        when(cardNumberMasker.mask(anyString())).thenReturn("**** **** **** 5678");

        BigDecimal result = cardService.getCardBalance(card1.getId());

        assertEquals(new BigDecimal("1000.00"), result);
    }

    @Test
    void createCard_ShouldCreateCard_WithValidRequest() {
        CreateCardRequest request = CreateCardRequest.builder()
                .number("1111222233334444")
                .userId(user.getId())
                .expirationDate(LocalDate.now().plusYears(3))
                .build();

        Card newCard = Card.builder()
                .id(UUID.randomUUID())
                .number(request.getNumber())
                .user(user)
                .expirationDate(request.getExpirationDate())
                .status(CardStatus.ACTIVE)
                .balance(BigDecimal.ZERO)
                .build();

        CardDto expectedDto = CardDto.builder()
                .id(newCard.getId())
                .number("**** **** **** 4444")
                .userId(user.getId())
                .expirationDate(request.getExpirationDate())
                .status(CardStatus.ACTIVE)
                .balance(BigDecimal.ZERO)
                .build();

        when(cardRepository.existsByNumber(request.getNumber())).thenReturn(false);
        when(userService.findUserById(user.getId())).thenReturn(user);
        when(cardMapper.toEntity(request)).thenReturn(newCard);
        when(cardRepository.save(newCard)).thenReturn(newCard);
        when(cardMapper.toDto(newCard)).thenReturn(expectedDto);
        when(cardNumberMasker.mask(expectedDto.getNumber())).thenReturn("**** **** **** 4444");

        CardDto result = cardService.createCard(request);

        assertNotNull(result);
        assertEquals(newCard.getId(), result.getId());
        assertEquals("**** **** **** 4444", result.getNumber());
        verify(cardRepository, times(1)).existsByNumber(request.getNumber());
        verify(userService, times(1)).findUserById(user.getId());
        verify(cardRepository, times(1)).save(newCard);
    }

    @Test
    void createCard_ShouldThrowDuplicateCardException_WithDuplicateCardNumber() {
        CreateCardRequest request = CreateCardRequest.builder()
                .number("1111222233334444")
                .userId(user.getId())
                .build();

        when(cardRepository.existsByNumber(request.getNumber())).thenReturn(true);

        assertThrows(DuplicateCardException.class, () ->
                cardService.createCard(request));
        verify(cardRepository, never()).save(any(Card.class));
    }

    @Test
    void removeCard_ShouldDeleteCard_WithExistingCard() {
        when(cardRepository.findById(card1.getId())).thenReturn(Optional.of(card1));

        cardService.removeCard(card1.getId());

        verify(cardRepository, times(1)).findById(card1.getId());
        verify(cardRepository, times(1)).delete(card1);
    }

    @Test
    void removeCard_ShouldThrowCardNotFoundException_WhenCardNotFound() {
        UUID nonExistentId = UUID.randomUUID();
        when(cardRepository.findById(nonExistentId)).thenReturn(Optional.empty());

        assertThrows(CardNotFoundException.class, () ->
                cardService.removeCard(nonExistentId));
        verify(cardRepository, never()).delete((Card) any());
    }

    @Test
    void transferFundsBetweenOwnCards_ShouldTransferFunds_WithValidRequest() {
        TransferRequest request = TransferRequest.builder()
                .fromCard(card1.getId())
                .toCard(card2.getId())
                .amount(new BigDecimal("100.00"))
                .build();

        when(authService.getCurrentUser()).thenReturn(user);
        when(cardRepository.findById(card1.getId())).thenReturn(Optional.of(card1));
        when(cardRepository.findById(card2.getId())).thenReturn(Optional.of(card2));

        cardService.transferFundsBetweenOwnCards(request);

        assertEquals(new BigDecimal("900.00"), card1.getBalance());
        assertEquals(new BigDecimal("600.00"), card2.getBalance());
        verify(cardRepository, times(1)).saveAll(anyList());
    }

    @Test
    void transferFundsBetweenOwnCards_ShouldThrowNotEnoughFundsException_WhenInsufficientFunds() {
        TransferRequest request = TransferRequest.builder()
                .fromCard(card1.getId())
                .toCard(card2.getId())
                .amount(new BigDecimal("2000.00"))
                .build();

        when(authService.getCurrentUser()).thenReturn(user);
        when(cardRepository.findById(card1.getId())).thenReturn(Optional.of(card1));
        when(cardRepository.findById(card2.getId())).thenReturn(Optional.of(card2));

        assertThrows(NotEnoughFundsException.class, () ->
                cardService.transferFundsBetweenOwnCards(request));
        verify(cardRepository, never()).saveAll(anyList());
    }

    @Test
    void transferFundsBetweenOwnCards_ShouldThrowIllegalTransferException_WhenFromCardBlocked() {
        card1.setStatus(CardStatus.BLOCKED);
        TransferRequest request = TransferRequest.builder()
                .fromCard(card1.getId())
                .toCard(card2.getId())
                .amount(new BigDecimal("100.00"))
                .build();

        when(authService.getCurrentUser()).thenReturn(user);
        when(cardRepository.findById(card1.getId())).thenReturn(Optional.of(card1));
        when(cardRepository.findById(card2.getId())).thenReturn(Optional.of(card2));

        assertThrows(IllegalTransferException.class, () ->
                cardService.transferFundsBetweenOwnCards(request));
        verify(cardRepository, never()).saveAll(anyList());
    }

    @Test
    void transferFundsBetweenOwnCards_ShouldThrowIllegalTransferException_WhenToCardBlocked() {
        card2.setStatus(CardStatus.BLOCKED);
        TransferRequest request = TransferRequest.builder()
                .fromCard(card1.getId())
                .toCard(card2.getId())
                .amount(new BigDecimal("100.00"))
                .build();

        when(authService.getCurrentUser()).thenReturn(user);
        when(cardRepository.findById(card1.getId())).thenReturn(Optional.of(card1));
        when(cardRepository.findById(card2.getId())).thenReturn(Optional.of(card2));

        assertThrows(IllegalTransferException.class, () ->
                cardService.transferFundsBetweenOwnCards(request));
        verify(cardRepository, never()).saveAll(anyList());
    }

    @Test
    void transferFundsBetweenOwnCards_ShouldThrowIllegalTransferException_WhenTransferToSameCard() {
        TransferRequest request = TransferRequest.builder()
                .fromCard(card1.getId())
                .toCard(card1.getId())
                .amount(new BigDecimal("100.00"))
                .build();

        assertThrows(IllegalTransferException.class, () ->
                cardService.transferFundsBetweenOwnCards(request));
        verify(cardRepository, never()).findById(any());
    }

    @Test
    void transferFundsBetweenOwnCards_ShouldThrowAccessDeniedException_WhenUserNotOwnerOfCards() {
        User otherUser = new User(999L, "some@maill.ru", Role.USER);
        TransferRequest request = TransferRequest.builder()
                .fromCard(card1.getId())
                .toCard(card2.getId())
                .amount(new BigDecimal("100.00"))
                .build();

        when(authService.getCurrentUser()).thenReturn(otherUser);
        when(cardRepository.findById(card1.getId())).thenReturn(Optional.of(card1));
        when(cardRepository.findById(card2.getId())).thenReturn(Optional.of(card2));

        assertThrows(AccessDeniedException.class, () ->
                cardService.transferFundsBetweenOwnCards(request));
        verify(cardRepository, never()).saveAll(anyList());
    }
}