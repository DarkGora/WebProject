package org.example.controller.testapi;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/test")
public class TestController {

    @PreAuthorize("hasRole('resume.admin')")
    @GetMapping("/admin-only")
    public String adminOnly() {
        return "This is accessible only to ADMIN";
    }

    @PreAuthorize("hasRole('resume.user')")
    @GetMapping("/user-only")
    public String userOnly() {
        return "This is accessible only to USER";
    }

    @GetMapping("/current-user-roles")
    public Map<String, Object> getCurrentUserRoles(Authentication authentication) {
        Map<String, Object> result = new HashMap<>();
        if (authentication != null) {
            result.put("username", authentication.getName());
            result.put("roles", authentication.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .collect(Collectors.toList()));
            result.put("isAdmin", authentication.getAuthorities().stream()
                    .anyMatch(auth -> auth.getAuthority().equals("resume.admin")));
        }
        return result;
    }
}