package com.example.bankcards.dto.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RegisterUserRequest {

    @NotBlank(message = "Name must be provided")
    @Size(max = 255, message = "Name must be less than 255 characters")
    private String name;

    @NotBlank(message = "Email must be provided")
    @Email(message = "Email must be valid")
    private String email;

    @NotBlank(message = "Password must be provided")
    @Size(min = 6, max = 25, message = "Password must be between 6 to 25 characters long.")
    private String password;
}
