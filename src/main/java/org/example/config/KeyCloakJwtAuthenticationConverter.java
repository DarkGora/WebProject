package org.example.config;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.*;
import java.util.stream.Collectors;

public class KeyCloakJwtAuthenticationConverter implements Converter<Jwt, Collection<GrantedAuthority>> {

    @Override
    public Collection<GrantedAuthority> convert(Jwt jwt) {
        List<String> roles = jwt.getClaimAsStringList("roles");

        if (roles == null || roles.isEmpty()) {
            return Collections.emptyList();
        }

        System.out.println("=== JWT Конвертор ролей ===");
        System.out.println("Роли в JWT: " + roles);

        Set<GrantedAuthority> authorities = new HashSet<>();

        for (String role : roles) {
            authorities.add(new SimpleGrantedAuthority(role));

            if (role.startsWith("ROLE_")) {
                authorities.add(new SimpleGrantedAuthority(role.substring(5)));
            }
        }

        System.out.println("Конец JWT конвектора авторизации: " + authorities.stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList()));

        return new ArrayList<>(authorities);
    }
}