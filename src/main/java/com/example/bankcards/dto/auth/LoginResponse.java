package com.example.bankcards.dto.auth;

import com.example.bankcards.config.Jwt;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class LoginResponse {
    private Jwt accessToken;
    private Jwt refreshToken;
}