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
                .requestMatchers(HttpMethod.GET, "/users/**").permitAll()
                .requestMatchers(HttpMethod.POST, "/cards/**").hasRole(Role.ADMIN.name())
                .requestMatchers(HttpMethod.PATCH, "/cards/**").hasRole(Role.ADMIN.name())
                .requestMatchers(HttpMethod.DELETE, "/cards/**").hasRole(Role.ADMIN.name());
    }
}
