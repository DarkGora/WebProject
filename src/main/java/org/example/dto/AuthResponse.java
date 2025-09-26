package org.example.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class AuthResponse{

@JsonProperty("access_token")
private String accessToken;

@JsonProperty("token_type")
private String tokenType;

@JsonProperty("expires_in")
private Integer expiresIn;

@JsonProperty("refresh_token")
private String refreshToken;

@JsonProperty("scope")
private String scope;

@JsonProperty("refresh_expires_in")
private Integer refreshExpiresIn;

@JsonProperty("not-before-policy")
private Integer notBeforePolicy;

@JsonProperty("session_state")
private String sessionState;
}