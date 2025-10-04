package org.example.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import org.example.model.dto.AuthResponse;
import org.example.model.dto.UserRequest;
import org.example.service.AuthService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@AllArgsConstructor
public class AuthController {
    private final ObjectMapper objectMapper;
    private final AuthService service;

    @PostMapping("/auth")
    public ResponseEntity<AuthResponse> auth(@RequestBody UserRequest user) {
        ResponseEntity<String> response = service.sendTokenRequest(user.getLogin(), user.getPassword());
        if (response.getStatusCode().is2xxSuccessful()) {
            try {
                AuthResponse authResponse = objectMapper.readValue(response.getBody(), AuthResponse.class);
                return ResponseEntity.ok(authResponse);
            } catch (JsonProcessingException e) {
                throw new RuntimeException("Failed to parse token response", e);
            }
        } else {
            throw new RuntimeException("Token exchange failed: " + response.getStatusCode());
        }
    }
}