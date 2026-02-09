package com.example.bankcards.dto.user;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "Запрос на регистрацию пользователя")
public class RegisterUserRequest {

    @NotBlank(message = "Name must be provided")
    @Size(max = 255, message = "Name must be less than 255 characters")
    @Schema(
            description = "Имя пользователя",
            example = "Иван Иванов",
            maxLength = 255
    )
    private String name;

    @NotBlank(message = "Email must be provided")
    @Email(message = "Email must be valid")
    @Schema(
            description = "Email пользователя",
            example = "user@example.com",
            format = "email"
    )
    private String email;

    @NotBlank(message = "Password must be provided")
    @Size(min = 6, max = 25, message = "Password must be between 6 to 25 characters long.")
    @Schema(
            description = "Пароль пользователя",
            example = "SecurePass123",
            minLength = 6,
            maxLength = 25
    )
    private String password;
}
