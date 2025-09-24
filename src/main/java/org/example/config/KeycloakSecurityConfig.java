package org.example.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;

import java.util.*;
import java.util.stream.Collectors;

@Configuration
public class KeycloakSecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.GET, "/promo-cod").hasAuthority("ROLE_resume.admin")
                        .requestMatchers(HttpMethod.GET, "/projects").hasAuthority("ROLE_resume.user")
                        .requestMatchers(HttpMethod.GET, "/employees").hasAnyAuthority("ROLE_resume.user", "ROLE_resume.admin", "ROLE_resume.client")
                        .requestMatchers("/api/employees/**").hasAnyAuthority("ROLE_resume.user", "ROLE_resume.admin", "ROLE_resume.client")
                        .requestMatchers("/", "/login", "/error", "/access-denied", "/static/**",
                                "/images/**", "/webjars/**", "/css/**", "/js/**").permitAll()
                        .anyRequest().authenticated()
                )
                .oauth2Login(oauth2 -> oauth2
                        .loginPage("/login")
                        .defaultSuccessUrl("/", true)
                        .failureUrl("/login?error=true")
                        .userInfoEndpoint(userInfo -> userInfo
                                .oidcUserService(oidcUserService())
                        )
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
                        .invalidateHttpSession(true)
                        .clearAuthentication(true)
                        .deleteCookies("JSESSIONID")
                        .permitAll()
                )
                .csrf(AbstractHttpConfigurer::disable)
                .build();
    }

    @Bean
    public OAuth2UserService<OidcUserRequest, OidcUser> oidcUserService() {
        final OidcUserService delegate = new OidcUserService();

        return (userRequest) -> {
            OidcUser oidcUser = delegate.loadUser(userRequest);

            Map<String, Object> claims = oidcUser.getClaims();
            List<String> roleClaims = (List<String>) claims.get("roles");

            System.out.println("=== OIDC User Service ===");
            System.out.println("Raw roles from claims: " + roleClaims);

            Set<GrantedAuthority> authorities = new HashSet<>();

            authorities.addAll(oidcUser.getAuthorities());

            if (roleClaims != null) {
                for (String role : roleClaims) {
                    authorities.add(new SimpleGrantedAuthority(role));
                    if (role.startsWith("ROLE_")) {
                        authorities.add(new SimpleGrantedAuthority(role.substring(5)));
                    }
                }
            }

            System.out.println("Final authorities: " + authorities.stream()
                    .map(GrantedAuthority::getAuthority)
                    .collect(Collectors.toList()));

            return new DefaultOidcUser(authorities, oidcUser.getIdToken(), oidcUser.getUserInfo());
        };
    }

    @Bean
    public Converter<Jwt, ? extends AbstractAuthenticationToken> jwtAuthenticationConverter() {
        var converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(new KeyCloakJwtAuthenticationConverter());
        return converter;
    }
}