package com.example.bankcards.security;

import com.example.bankcards.entity.Role;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer;
import org.springframework.stereotype.Component;

@Component
public class CardSecurityRules implements SecurityRules {

    @Override
    public void configure(AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry registry) {
        registry
                .requestMatchers(HttpMethod.GET, "api/cards").hasRole(Role.ADMIN.name())
                .requestMatchers(HttpMethod.GET, "/api/cards/**").hasAnyRole(Role.ADMIN.name(), Role.USER.name())
                .requestMatchers(HttpMethod.POST, "/api/cards").hasRole(Role.ADMIN.name())
                .requestMatchers(HttpMethod.POST, "/api/cards/block/").hasRole(Role.USER.name())
                .requestMatchers(HttpMethod.POST, "/api/cards/transfer").hasRole(Role.USER.name())
                .requestMatchers(HttpMethod.PATCH, "/api/cards/**").hasRole(Role.ADMIN.name())
                .requestMatchers(HttpMethod.DELETE, "/api/cards/**").hasRole(Role.ADMIN.name());
    }
}
