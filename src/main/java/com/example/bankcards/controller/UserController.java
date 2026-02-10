package com.example.bankcards.controller;

import com.example.bankcards.dto.user.ChangePasswordRequest;
import com.example.bankcards.dto.user.RegisterUserRequest;
import com.example.bankcards.dto.user.UpdateUserRequest;
import com.example.bankcards.dto.user.UserDto;
import com.example.bankcards.exception.DuplicateUserException;
import com.example.bankcards.exception.ErrorDto;
import com.example.bankcards.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;

@Tag(
        name = "Пользователи",
        description = "API для управления пользователями"
)
@AllArgsConstructor
@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService service;

    @Operation(
            summary = "Получить список всех пользователей",
            description = "Возвращает список пользователей с возможностью сортировки по полям name и email. " +
                    "Доступно всем пользователям без аутентификации."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Успешное получение списка пользователей",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = UserDto.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Некорректный параметр сортировки",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorDto.class)
                    )
            )
    })
    @GetMapping
    @SecurityRequirement(name = "bearerAuth")
    public List<UserDto> getUsers(
            @RequestParam(required = false, defaultValue = "", name = "sort") String sortBy) {
        return service.getAllUsers(sortBy);
    }

    @Operation(
            summary = "Получить информацию о пользователе по ID",
            description = "Возвращает детальную информацию о пользователе."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Пользователь найден",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = UserDto.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Пользователь не найден",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorDto.class)
                    )
            )
    })
    @GetMapping("/{id}")
    @SecurityRequirement(name = "bearerAuth")
    public UserDto getUser(@PathVariable Long id) {
        return service.getUserDto(id);
    }

    @Operation(
            summary = "Регистрация нового пользователя",
            description = "Создает нового пользователя в системе. Email должен быть уникальным."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "201",
                    description = "Пользователь успешно зарегистрирован",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = UserDto.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Некорректные данные или email уже занят",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ErrorDto.class)
                    )
            )
    })
    @PostMapping
    public ResponseEntity<?> registerUser(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Данные для регистрации пользователя",
                    required = true,
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = RegisterUserRequest.class)
                    )
            )
            @Valid @RequestBody RegisterUserRequest request,
            UriComponentsBuilder uriBuilder) {
        var user = service.registerUser(request);
        var uri = uriBuilder.path("/users/{id}").buildAndExpand(user.getId()).toUri();

        return ResponseEntity.created(uri).body(user);
    }

    @Operation(
            summary = "Обновить информацию о пользователе",
            description = "Обновляет данные существующего пользователя. Требуются права администратора."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Данные пользователя успешно обновлены",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = UserDto.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Некорректные данные",
                    content = @Content(schema = @Schema(implementation = ErrorDto.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Требуется аутентификация",
                    content = @Content(schema = @Schema(implementation = ErrorDto.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Пользователь не найден",
                    content = @Content(schema = @Schema(implementation = ErrorDto.class))
            )
    })
    @SecurityRequirement(name = "bearerAuth")
    @PutMapping("/{id}")
    public UserDto updateUser(
            @PathVariable(name = "id") Long id,
            @Valid @RequestBody UpdateUserRequest request) {
        return service.updateUser(id, request);
    }

    @Operation(
            summary = "Удалить пользователя",
            description = "Удаляет пользователя из системы. Требуются права администратора."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "204",
                    description = "Пользователь успешно удален"
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Требуется аутентификация",
                    content = @Content(schema = @Schema(implementation = ErrorDto.class))
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Недостаточно прав",
                    content = @Content(schema = @Schema(implementation = ErrorDto.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Пользователь не найден",
                    content = @Content(schema = @Schema(implementation = ErrorDto.class))
            )
    })
    @SecurityRequirement(name = "bearerAuth")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        service.deleteUser(id);

        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/change-password")
    public void changePassword(
            @PathVariable Long id,
            @Valid @RequestBody ChangePasswordRequest request
    ) {
        service.changePassword(id, request);
    }

    @ExceptionHandler(DuplicateUserException.class)
    public ResponseEntity<ErrorDto> handleDuplicateUserException() {
        return ResponseEntity.badRequest()
                .body(new ErrorDto("Email is already registered."));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Void> handleAccessDenied() {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }
}
