package com.example.bankcards.security;

import com.example.bankcards.entity.Role;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer;
import org.springframework.stereotype.Component;

@Component
public class UserSecurityRules implements SecurityRules {

    @Override
    public void configure(AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry registry) {
        registry
                .requestMatchers(HttpMethod.GET, "/api/users/**").hasAnyRole(Role.ADMIN.name(), Role.USER.name())
                .requestMatchers(HttpMethod.POST, "/api/users").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/users/{id}/change-password").hasAnyRole(Role.ADMIN.name(), Role.USER.name())
                .requestMatchers(HttpMethod.PUT, "/api/users/**").hasRole(Role.ADMIN.name())
                .requestMatchers(HttpMethod.DELETE, "/api/users/**").hasRole(Role.ADMIN.name());
    }
}
