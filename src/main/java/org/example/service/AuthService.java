package org.example.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {
    private final RestTemplate restTemplate;

    @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri}")
    private String jwtIssuerUrl;

    @Value("${spring.security.oauth2.client.registration.keycloak.client-id}")
    private String clientId;

    @Value("${spring.security.oauth2.client.registration.keycloak.client-secret}")
    private String clientSecret;

    @Value("${spring.security.oauth2.client.registration.keycloak.scope:openid}")
    private String scope;

    public ResponseEntity<String> sendTokenRequest(String username, String password) {
        if (username == null || username.trim().isEmpty()) {
            throw new IllegalArgumentException("Username cannot be null or empty");
        }
        if (password == null || password.trim().isEmpty()) {
            throw new IllegalArgumentException("Password cannot be null or empty");
        }

        if (jwtIssuerUrl == null || jwtIssuerUrl.trim().isEmpty()) {
            throw new IllegalStateException("JWT issuer URL is not configured");
        }

        String tokenUrl = jwtIssuerUrl + "/protocol/openid-connect/token";

        log.debug("Sending token request to: {}", tokenUrl);
        log.debug("Username: {}, Client ID: {}, Scope: {}", username, clientId, scope);

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            headers.setAccept(List.of(MediaType.APPLICATION_JSON));

            MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
            body.add("client_id", clientId);
            body.add("username", username);
            body.add("password", password);
            body.add("client_secret", clientSecret);
            body.add("grant_type", "password");

            // Исправление формата scope - используем пробелы вместо запятых
            if (scope != null && !scope.trim().isEmpty()) {
                String correctedScope = scope.trim().replace(",", " ");
                body.add("scope", correctedScope);
                log.debug("Corrected scope format: {}", correctedScope);
            } else {
                body.add("scope", "openid");
            }

            HttpEntity<MultiValueMap<String, String>> requestEntity = new HttpEntity<>(body, headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    tokenUrl,
                    HttpMethod.POST,
                    requestEntity,
                    String.class
            );

            log.debug("Token request completed with status: {}", response.getStatusCode());
            return response;

        } catch (RestClientException e) {
            log.error("Error during token request to {}: {}", tokenUrl, e.getMessage());

            if (e.getMessage().contains("invalid_scope")) {
                log.info("Trying without scope parameter...");
                return retryWithoutScope(username, password, tokenUrl);
            }
            throw new RuntimeException("Failed to authenticate user: " + e.getMessage(), e);
        }
    }

    private ResponseEntity<String> retryWithoutScope(String username, String password, String tokenUrl) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            headers.setAccept(List.of(MediaType.APPLICATION_JSON));

            MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
            body.add("client_id", clientId);
            body.add("username", username);
            body.add("password", password);
            body.add("client_secret", clientSecret);
            body.add("grant_type", "password");

            HttpEntity<MultiValueMap<String, String>> requestEntity = new HttpEntity<>(body, headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    tokenUrl,
                    HttpMethod.POST,
                    requestEntity,
                    String.class
            );

            log.debug("Token request without scope completed with status: {}", response.getStatusCode());
            return response;

        } catch (RestClientException e) {
            log.error("Error during token request without scope: {}", e.getMessage());
            throw new RuntimeException("Failed to authenticate user even without scope: " + e.getMessage(), e);
        }
    }
}