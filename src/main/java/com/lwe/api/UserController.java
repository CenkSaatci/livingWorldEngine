package com.lwe.api;

import com.lwe.core.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
public class UserController {

    private final AuthService authService;

    public UserController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest req,
                                      HttpServletRequest httpReq) {
        var locale = extractLocale(httpReq);
        var result = authService.register(req.email(), req.username(), req.password(), locale);
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest req,
                                   HttpServletRequest httpReq) {
        var ip = httpReq.getRemoteAddr();
        var result = authService.login(req.email(), req.password(), ip);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(@RequestBody RefreshRequest req) {
        var result = authService.refresh(req.refreshToken());
        return ResponseEntity.ok(result);
    }

    @PostMapping("/service-login")
    public ResponseEntity<?> serviceLogin(@RequestBody ServiceLoginRequest req) {
        var result = authService.serviceLogin(req.serviceUser(), req.servicePassword());
        return ResponseEntity.ok(result);
    }

    private String extractLocale(HttpServletRequest req) {
        var header = req.getHeader("Accept-Language");
        if (header == null || header.isBlank() || header.length() < 2) return null;
        var locale = header.split(",")[0].trim().substring(0, 2).toLowerCase(java.util.Locale.ROOT);
        return locale.matches("^(de|en|fr|es|pl|it)$") ? locale : null;
    }

    public record RegisterRequest(
        @NotBlank @Email String email,
        @NotBlank @Size(min = 3, max = 100) String username,
        @NotBlank @Size(min = 6) String password
    ) {}

    public record LoginRequest(
        @NotBlank String email,
        @NotBlank String password
    ) {}

    public record RefreshRequest(
        @NotBlank String refreshToken
    ) {}

    public record ServiceLoginRequest(
        @NotBlank String serviceUser,
        @NotBlank String servicePassword
    ) {}
}