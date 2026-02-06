package com.example.bankcards.dto.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateUserRequest {

    @NotBlank(message = "Name must be provided")
    @Size(max = 255, message = "Name must be less than 255 characters")
    public String name;

    @NotBlank(message = "Email must be provided")
    @Email(message = "Email must be valid")
    public String email;
}
