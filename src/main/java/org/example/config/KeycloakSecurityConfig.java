package org.example.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.authentication.logout.LogoutHandler;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Configuration
public class KeycloakSecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.GET, "/promo-cod").hasRole("resume.admin")
                        .requestMatchers(HttpMethod.GET, "/projects").hasRole("resume.user")
                        .requestMatchers(HttpMethod.GET, "/employees").hasAnyRole("resume.user", "resume.admin", "resume.client")
                        .requestMatchers("/", "/login", "/error", "/access-denied", "/static/**",
                                "/images/**", "/webjars/**", "/css/**", "/js/**").permitAll()
                        .anyRequest().authenticated()
                )
                .oauth2Login(oauth2 -> oauth2
                        .loginPage("/login")
                        .defaultSuccessUrl("/", true)
                        .failureUrl("/login?error=true")
                )
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint(new LoginUrlAuthenticationEntryPoint("/login"))
                )
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter()))
                )
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/login?logout=true")
                        .addLogoutHandler(keycloakLogoutHandler())
                        .invalidateHttpSession(true)
                        .clearAuthentication(true)
                        .deleteCookies("JSESSIONID")
                        .permitAll()
                )
                .csrf(AbstractHttpConfigurer::disable)
                .build();
    }

    private LogoutHandler keycloakLogoutHandler() {
        return (request, response, authentication) -> {
            try {
                if (authentication != null && authentication.isAuthenticated()) {
                    String idToken = getIdTokenFromAuthentication(authentication);
                    String logoutUrl = "http://localhost:8081/realms/resume/protocol/openid-connect/logout";

                    if (idToken != null) {
                        String redirectUri = "http://localhost:8080/login?logout=true";
                        String fullLogoutUrl = logoutUrl +
                                "?id_token_hint=" + idToken +
                                "&post_logout_redirect_uri=" + URLEncoder.encode(redirectUri, StandardCharsets.UTF_8);

                        response.sendRedirect(fullLogoutUrl);
                    } else {
                        response.sendRedirect("/login?logout=true");
                    }
                }
            } catch (IOException e) {
                try {
                    response.sendRedirect("/login?logout=true");
                } catch (IOException ex) {
                }
            }
        };
    }

    private String getIdTokenFromAuthentication(Authentication authentication) {
        if (authentication instanceof OAuth2AuthenticationToken) {
            OAuth2AuthenticationToken oauthToken = (OAuth2AuthenticationToken) authentication;
            Object principal = oauthToken.getPrincipal();

            if (principal instanceof OidcUser) {
                OidcUser oidcUser = (OidcUser) principal;
                return oidcUser.getIdToken().getTokenValue();
            }
        }
        return null;
    }

    @Bean
    public Converter<Jwt, ? extends AbstractAuthenticationToken> jwtAuthenticationConverter() {
        var converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(new KeyCloakJwtAuthenticationConverter());
        return converter;
    }
}