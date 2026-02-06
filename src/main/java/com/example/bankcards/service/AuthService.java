package com.example.bankcards.service;

import com.example.bankcards.config.Jwt;
import com.example.bankcards.dto.auth.LoginRequest;
import com.example.bankcards.dto.auth.LoginResponse;
import com.example.bankcards.entity.User;
import com.example.bankcards.repository.UserRepository;
import lombok.AllArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserRepository repository;
    private final JwtService jwtService;

    public User getCurrentUser() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        var userId = (Long) authentication.getPrincipal();

        return repository.findById(userId).orElse(null);
    }

    public LoginResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()
                )
        );

        var user = repository.findByEmail(request.getEmail()).orElseThrow();
        var accessToken = jwtService.generateAccessToken(user);
        var refreshToken = jwtService.generateRefreshToken(user);

        return new LoginResponse(accessToken, refreshToken);
    }

    public Jwt refreshAccessToken(String refreshToken) {
        var jwt = jwtService.parseToken(refreshToken);
        if (jwt == null || jwt.isExpired()) {
            throw new BadCredentialsException("Invalid refresh token");
        }

        var user = repository.findById(jwt.getUserId()).orElseThrow();
        return jwtService.generateAccessToken(user);
    }
}
