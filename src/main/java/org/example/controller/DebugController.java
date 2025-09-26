package org.example.controller;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
public class DebugController {

    @GetMapping("/api/debug/auth")
    public Map<String, Object> debugAuth(Authentication authentication) {
        Map<String, Object> debugInfo = new HashMap<>();

        if (authentication == null) {
            debugInfo.put("status", "NOT_AUTHENTICATED");
            return debugInfo;
        }

        debugInfo.put("authenticated", authentication.isAuthenticated());
        debugInfo.put("name", authentication.getName());
        debugInfo.put("authorities", authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList()));
        debugInfo.put("principalType", authentication.getPrincipal().getClass().getName());

        if (authentication.getPrincipal() instanceof Jwt) {
            Jwt jwt = (Jwt) authentication.getPrincipal();
            debugInfo.put("jwtClaims", jwt.getClaims());
        }

        return debugInfo;
    }
    @GetMapping("/api/debug/auth-details")
    public Map<String, Object> getAuthDetails(Authentication authentication) {
        Map<String, Object> result = new HashMap<>();

        if (authentication != null) {
            result.put("name", authentication.getName());
            result.put("authenticated", authentication.isAuthenticated());
            result.put("authorities", authentication.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .collect(Collectors.toList()));
            result.put("principalType", authentication.getPrincipal().getClass().getSimpleName());

            if (authentication.getPrincipal() instanceof OidcUser) {
                OidcUser oidcUser = (OidcUser) authentication.getPrincipal();
                result.put("claims", oidcUser.getClaims());
            }
        }

        return result;
    }
}

