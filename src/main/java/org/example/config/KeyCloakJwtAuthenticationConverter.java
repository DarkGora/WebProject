package org.example.config;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class KeyCloakJwtAuthenticationConverter implements Converter<Jwt, Collection<GrantedAuthority>> {

    @Override
    public Collection<GrantedAuthority> convert(Jwt jwt) {
        System.out.println("=== JWT TOKEN DEBUG ===");
        System.out.println("JWT Claims: " + jwt.getClaims());

        Map<String, Object> claims = jwt.getClaims();
        List<String> roles = extractRoles(claims);

        System.out.println("Extracted roles: " + roles);

        if (roles.isEmpty()) {
            return Collections.emptyList();
        }

        List<GrantedAuthority> authorities = roles.stream()
                .map(role -> {
                    // префикс ROLE_ для Spring Security
                    String authority = role.startsWith("ROLE_") ? role : "ROLE_" + role;
                    System.out.println("Converting role: " + role + " -> " + authority);
                    return authority;
                })
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toList());

        System.out.println("Final authorities: " + authorities);
        System.out.println("=== END DEBUG ===");

        return authorities;
    }

    private List<String> extractRoles(Map<String, Object> claims) {
        List<String> roles = new ArrayList<>();

        System.out.println("=== EXTRACTING ROLES FROM CLAIMS ===");
        System.out.println("All claims keys: " + claims.keySet());

        // 1. Прямое извлечение ролей из claims
        if (claims.containsKey("roles")) {
            Object rolesObj = claims.get("roles");
            System.out.println("Direct 'roles' claim found: " + rolesObj);

            if (rolesObj instanceof List) {
                List<?> rolesList = (List<?>) rolesObj;
                for (Object role : rolesList) {
                    if (role instanceof String) {
                        String roleStr = (String) role;
                        System.out.println("Found role: " + roleStr);
                        // Фильтруем только нужные роли resume.*
                        if (roleStr.contains("resume.")) {
                            roles.add(roleStr);
                        }
                    }
                }
            }
        }

        // 2. Проверяем resource_access (роли клиента)
        if (claims.containsKey("resource_access")) {
            System.out.println("Found resource_access in claims");
            Map<String, Object> resourceAccess = (Map<String, Object>) claims.get("resource_access");
            System.out.println("Resource access keys: " + resourceAccess.keySet());

            // Перебираем всех клиентов
            for (Map.Entry<String, Object> entry : resourceAccess.entrySet()) {
                String clientName = entry.getKey();
                System.out.println("Processing client: " + clientName);

                if (entry.getValue() instanceof Map) {
                    Map<String, Object> client = (Map<String, Object>) entry.getValue();
                    System.out.println("Client " + clientName + " data: " + client.keySet());

                    if (client.containsKey("roles")) {
                        Object rolesObj = client.get("roles");
                        System.out.println("Client " + clientName + " roles object: " + rolesObj);

                        if (rolesObj instanceof List) {
                            List<?> rolesList = (List<?>) rolesObj;
                            for (Object role : rolesList) {
                                if (role instanceof String) {
                                    String roleStr = (String) role;
                                    System.out.println("Found client role: " + roleStr);
                                    roles.add(roleStr);
                                }
                            }
                        }
                    }
                }
            }
        }

        // 3. Проверяем realm_access (роли realm)
        if (claims.containsKey("realm_access")) {
            System.out.println("Found realm_access in claims");
            Map<String, Object> realmAccess = (Map<String, Object>) claims.get("realm_access");
            System.out.println("Realm access keys: " + realmAccess.keySet());

            if (realmAccess.containsKey("roles")) {
                Object rolesObj = realmAccess.get("roles");
                System.out.println("Realm roles object: " + rolesObj);

                if (rolesObj instanceof List) {
                    List<?> rolesList = (List<?>) rolesObj;
                    for (Object role : rolesList) {
                        if (role instanceof String) {
                            String roleStr = (String) role;
                            System.out.println("Found realm role: " + roleStr);
                            // Для realm ролей добавляем префикс
                            if (roleStr.contains("resume.")) {
                                roles.add("ROLE_" + roleStr);
                            }
                        }
                    }
                }
            }
        }

        // 4. проверка - ищем любые поля с ролями
        for (Map.Entry<String, Object> entry : claims.entrySet()) {
            if (entry.getKey().toLowerCase().contains("role")) {
                System.out.println("Found potential role field: " + entry.getKey() + " = " + entry.getValue());
            }
        }

        // 5. Фильтруем только нужные роли и убираем дубликаты
        List<String> filteredRoles = roles.stream()
                .filter(role -> role.contains("resume."))
                .distinct()
                .collect(Collectors.toList());

        System.out.println("Filtered roles (resume.* only): " + filteredRoles);
        System.out.println("=== END EXTRACTING ROLES ===");

        return filteredRoles;
    }
}