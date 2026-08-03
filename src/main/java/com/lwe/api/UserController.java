package com.lwe.api;

import com.lwe.api.dto.ApiResponse;
import com.lwe.api.dto.ErrorResponse;
import com.lwe.api.dto.MessageWithTokenResponse;
import com.lwe.core.domain.PasswordResetToken;
import com.lwe.core.domain.User;
import com.lwe.core.repository.PasswordResetTokenRepository;
import com.lwe.core.repository.UserRepository;
import com.lwe.core.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Map;
import java.security.SecureRandom;

@RestController
@RequestMapping("/api/v1/auth")
public class UserController {

    private final AuthService authService;
    private final UserRepository userRepo;
    private final PasswordResetTokenRepository tokenRepo;
    private final PasswordEncoder passwordEncoder;

    public UserController(AuthService authService, UserRepository userRepo,
                          PasswordResetTokenRepository tokenRepo, PasswordEncoder passwordEncoder) {
        this.authService = authService;
        this.userRepo = userRepo;
        this.tokenRepo = tokenRepo;
        this.passwordEncoder = passwordEncoder;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest req,
                                       HttpServletRequest httpReq) {
        var locale = extractLocale(httpReq);
        var result = authService.register(req.email(), req.username(), req.password(), locale);
        return ResponseEntity.status(201).body(result);
    }

    @PostMapping("/verify-email")
    public ResponseEntity<?> verifyEmail(@RequestBody Map<String, String> body) {
        var token = body.get("token");
        if (token == null)
            return ResponseEntity.badRequest().body(new ErrorResponse("Token required"));
        try {
            authService.verifyEmail(token);
            return ResponseEntity.ok(new ApiResponse("Email verified successfully"));
        } catch (AuthService.AuthException e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        }
    }

    @PostMapping("/resend-verification")
    public ResponseEntity<?> resendVerification(@RequestBody Map<String, String> body) {
        var email = body.get("email");
        if (email == null)
            return ResponseEntity.badRequest().body(new ErrorResponse("Email required"));
        try {
            var token = authService.resendVerification(email);
            return ResponseEntity.ok(new MessageWithTokenResponse("Verification email resent", token));
        } catch (AuthService.AuthException e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        }
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

    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestBody Map<String, String> body) {
        var email = body.get("email");
        if (email == null)
            return ResponseEntity.badRequest().body(new ErrorResponse("Email required"));

        var user = userRepo.findByEmail(email).orElse(null);
        var msg = "If the email exists, a reset link has been generated";

        if (user != null) {
            var token = HexFormat.of().formatHex(new SecureRandom().generateSeed(32));
            var expiresAt = Instant.now().plus(Duration.ofHours(1));
            tokenRepo.deleteByUserId(user.getId());
            tokenRepo.save(new PasswordResetToken(user.getId(), token, expiresAt));
            return ResponseEntity.ok(new MessageWithTokenResponse(msg, token));
        }
        return ResponseEntity.ok(new ApiResponse(msg));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestBody Map<String, String> body) {
        var tokenStr = body.get("token");
        var newPassword = body.get("password");
        if (tokenStr == null || newPassword == null)
            return ResponseEntity.badRequest().body(new ErrorResponse("Token and password required"));
        if (newPassword.length() < 6)
            return ResponseEntity.badRequest().body(new ErrorResponse("Password must be at least 6 characters"));

        var token = tokenRepo.findByTokenAndUsedFalse(tokenStr).orElse(null);
        if (token == null)
            return ResponseEntity.badRequest().body(new ErrorResponse("Invalid or expired token"));
        if (token.getExpiresAt().isBefore(Instant.now()))
            return ResponseEntity.badRequest().body(new ErrorResponse("Token expired"));

        var user = userRepo.findById(token.getUserId()).orElse(null);
        if (user == null)
            return ResponseEntity.badRequest().body(new ErrorResponse("User not found"));

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepo.save(user);
        token.setUsed(true);
        tokenRepo.save(token);

        return ResponseEntity.ok(new ApiResponse("Password reset successful"));
    }

    @PostMapping("/change-password")
    public ResponseEntity<?> changePassword(@AuthenticationPrincipal User user,
                                             @RequestBody Map<String, String> body) {
        var currentPassword = body.get("currentPassword");
        var newPassword = body.get("newPassword");
        if (currentPassword == null || newPassword == null)
            return ResponseEntity.badRequest().body(new ErrorResponse("currentPassword and newPassword required"));
        if (newPassword.length() < 6)
            return ResponseEntity.badRequest().body(new ErrorResponse("Password must be at least 6 characters"));

        var dbUser = userRepo.findById(user.getId()).orElse(null);
        if (dbUser == null)
            return ResponseEntity.badRequest().body(new ErrorResponse("User not found"));
        if (!passwordEncoder.matches(currentPassword, dbUser.getPasswordHash()))
            return ResponseEntity.badRequest().body(new ErrorResponse("Current password is incorrect"));

        dbUser.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepo.save(dbUser);

        return ResponseEntity.ok(new ApiResponse("Password changed successfully"));
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
    public record LoginRequest(@NotBlank String email, @NotBlank String password) {}
    public record RefreshRequest(@NotBlank String refreshToken) {}
    public record ServiceLoginRequest(@NotBlank String serviceUser, @NotBlank String servicePassword) {}
}
