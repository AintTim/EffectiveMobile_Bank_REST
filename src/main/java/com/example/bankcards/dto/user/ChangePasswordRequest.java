package com.example.bankcards.dto.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ChangePasswordRequest {

    @NotBlank(message = "Old password must be provided")
    @Size(min = 6, max = 25, message = "Password must be between 6 to 25 characters long.")
    private String oldPassword;

    @NotBlank(message = "New password must be provided")
    @Size(min = 6, max = 25, message = "Password must be between 6 to 25 characters long.")
    private String newPassword;
}