package org.example.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
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
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class KeycloakSecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
                .authorizeHttpRequests(auth -> auth
                        // Статические ресурсы - доступны всем
                        .requestMatchers("/static/**", "/css/**", "/js/**", "/images/**",
                                "/webjars/**", "/error", "/favicon.ico").permitAll()

                        // Публичные страницы - доступны всем
                        .requestMatchers("/", "/login", "/access-denied", "/home").permitAll()

                        // Главная страница со списком сотрудников - требует аутентификации
                        .requestMatchers("/employees").hasAnyRole("resume.user", "resume.admin", "resume.client")

                        // === АДМИНСКИЕ ENDPOINTS ===
                        .requestMatchers("/employee/new", "/employee/add", "/employee/edit/**").hasRole("resume.admin")
                        .requestMatchers(HttpMethod.POST, "/employee/save").hasRole("resume.admin")
                        .requestMatchers(HttpMethod.POST, "/employee/delete/**").hasRole("resume.admin")

                        // === ПРОСМОТР ДАННЫХ СОТРУДНИКОВ ===
                        .requestMatchers(HttpMethod.GET, "/employee/**").hasAnyRole("resume.user", "resume.admin", "resume.client")
                        .requestMatchers(HttpMethod.GET, "/search").hasAnyRole("resume.user", "resume.admin", "resume.client")

                        // === API ENDPOINTS ===
                        .requestMatchers("/api/employees/**").hasAnyRole("resume.user", "resume.admin", "resume.client")
                        .requestMatchers("/api/**").hasAnyRole("resume.user", "resume.admin", "resume.client")

                        // === СПЕЦИАЛЬНЫЕ ПРАВА ===
                        .requestMatchers(HttpMethod.GET, "/promo-cod").hasRole("resume.admin")
                        .requestMatchers(HttpMethod.GET, "/projects").hasRole("resume.user")

                        // ВСЕ остальные запросы требуют аутентификации
                        .anyRequest().authenticated()
                )
                .oauth2Login(oauth2 -> oauth2
                        .loginPage("/login")
                        .defaultSuccessUrl("/", true) // После логина на страницу сотрудников
                        .failureUrl("/login?error=true")
                        .userInfoEndpoint(userInfo -> userInfo
                                .oidcUserService(oidcUserService())
                        )
                )
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter()))
                )
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint(new LoginUrlAuthenticationEntryPoint("/login"))
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

            System.out.println("=== OIDC USER DEBUG ===");
            System.out.println("OIDC User Claims: " + oidcUser.getClaims());

            Map<String, Object> claims = oidcUser.getClaims();
            List<String> roles = extractRolesFromClaims(claims);

            System.out.println("OIDC Extracted roles: " + roles);

            Collection<GrantedAuthority> authorities = roles.stream()
                    .map(role -> {
                        String authority = role.startsWith("ROLE_") ? role : "ROLE_" + role;
                        System.out.println("Converting role: " + role + " -> " + authority);
                        return authority;
                    })
                    .map(SimpleGrantedAuthority::new)
                    .collect(Collectors.toList());

            System.out.println("OIDC Final authorities: " + authorities);
            System.out.println("=== END OIDC DEBUG ===");

            return new DefaultOidcUser(authorities, oidcUser.getIdToken(), oidcUser.getUserInfo());
        };
    }

    private List<String> extractRolesFromClaims(Map<String, Object> claims) {
        List<String> roles = new ArrayList<>();

        System.out.println("=== EXTRACTING ROLES FROM OIDC CLAIMS ===");
        System.out.println("All claims keys: " + claims.keySet());

        if (claims.containsKey("roles")) {
            Object rolesObj = claims.get("roles");
            System.out.println("Direct 'roles' claim found: " + rolesObj);

            if (rolesObj instanceof List) {
                List<?> rolesList = (List<?>) rolesObj;
                for (Object role : rolesList) {
                    if (role instanceof String) {
                        String roleStr = (String) role;
                        System.out.println("Found role: " + roleStr);
                        if (roleStr.contains("resume.")) {
                            roles.add(roleStr);
                        }
                    }
                }
            }
        }

        if (claims.containsKey("resource_access")) {
            System.out.println("Found resource_access in claims");
            Map<String, Object> resourceAccess = (Map<String, Object>) claims.get("resource_access");
            System.out.println("Resource access keys: " + resourceAccess.keySet());

            for (Map.Entry<String, Object> entry : resourceAccess.entrySet()) {
                String clientName = entry.getKey();
                System.out.println("Processing client: " + clientName);

                if (entry.getValue() instanceof Map) {
                    Map<String, Object> client = (Map<String, Object>) entry.getValue();
                    System.out.println("Client " + clientName + " data: " + client.keySet());

                    if (client.containsKey("roles")) {
                        Object rolesObj = client.get("roles");
                        System.out.println("Client " + clientName + " roles: " + rolesObj);

                        if (rolesObj instanceof List) {
                            List<?> rolesList = (List<?>) rolesObj;
                            for (Object role : rolesList) {
                                if (role instanceof String) {
                                    String roleStr = (String) role;
                                    System.out.println("Found client role: " + roleStr);
                                    if (roleStr.contains("resume.")) {
                                        roles.add(roleStr);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        if (claims.containsKey("realm_access")) {
            System.out.println("Found realm_access in claims");
            Map<String, Object> realmAccess = (Map<String, Object>) claims.get("realm_access");
            System.out.println("Realm access keys: " + realmAccess.keySet());

            if (realmAccess.containsKey("roles")) {
                Object rolesObj = realmAccess.get("roles");
                System.out.println("Realm roles: " + rolesObj);

                if (rolesObj instanceof List) {
                    List<?> rolesList = (List<?>) rolesObj;
                    for (Object role : rolesList) {
                        if (role instanceof String) {
                            String roleStr = (String) role;
                            System.out.println("Found realm role: " + roleStr);
                            if (roleStr.contains("resume.")) {
                                roles.add("ROLE_" + roleStr);
                            }
                        }
                    }
                }
            }
        }


        if (roles.isEmpty()) {
            System.out.println("No roles found in standard locations, searching all claims...");
            for (Map.Entry<String, Object> entry : claims.entrySet()) {
                if (entry.getKey().toLowerCase().contains("role")) {
                    System.out.println("Found potential role field: " + entry.getKey() + " = " + entry.getValue());
                }
            }
        }

        System.out.println("Final extracted roles: " + roles);
        System.out.println("=== END EXTRACTING ROLES ===");

        return roles;
    }

    @Bean
    public Converter<Jwt, ? extends AbstractAuthenticationToken> jwtAuthenticationConverter() {
        var converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(new KeyCloakJwtAuthenticationConverter());
        return converter;
    }
}