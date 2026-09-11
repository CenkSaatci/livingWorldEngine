package com.lwe.api;

import com.lwe.api.dto.ApiResponse;
import com.lwe.core.domain.PasswordResetToken;
import com.lwe.core.domain.User;
import com.lwe.core.repository.PasswordResetTokenRepository;
import com.lwe.core.repository.UserRepository;
import com.lwe.core.service.AuthService;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

class UserControllerForgotPasswordTest {

    private UserController controller(UserRepository userRepo, PasswordResetTokenRepository tokenRepo) {
        return new UserController(mock(AuthService.class), userRepo, tokenRepo, mock(PasswordEncoder.class));
    }

    private User user(UUID id) {
        var u = new User("t@t.com", "t", "hash", "USER", "de");
        try { var f = User.class.getDeclaredField("id"); f.setAccessible(true); f.set(u, id); }
        catch (Exception e) { throw new RuntimeException(e); }
        return u;
    }

    @Test
    void shouldNotLeakResetTokenInResponse() {
        var userRepo = mock(UserRepository.class);
        var tokenRepo = mock(PasswordResetTokenRepository.class);
        var user = user(UUID.randomUUID());
        when(userRepo.findByEmail("t@t.com")).thenReturn(Optional.of(user));
        var saved = new AtomicReference<PasswordResetToken>();
        when(tokenRepo.save(any())).thenAnswer(inv -> {
            saved.set(inv.getArgument(0));
            return saved.get();
        });

        var response = controller(userRepo, tokenRepo)
            .forgotPassword(Map.of("email", "t@t.com"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isInstanceOf(ApiResponse.class);
        var body = ((ApiResponse) response.getBody()).message();
        assertThat(saved.get()).isNotNull();
        assertThat(body).doesNotContain(saved.get().getToken());
    }

    @Test
    void shouldReturnSameMessageForUnknownEmail() {
        var userRepo = mock(UserRepository.class);
        var tokenRepo = mock(PasswordResetTokenRepository.class);
        when(userRepo.findByEmail("nobody@t.com")).thenReturn(Optional.empty());

        var known = controller(userRepo, tokenRepo).forgotPassword(Map.of("email", "t@t.com"));
        var unknown = controller(userRepo, tokenRepo).forgotPassword(Map.of("email", "nobody@t.com"));

        assertThat(((ApiResponse) known.getBody()).message())
            .isEqualTo(((ApiResponse) unknown.getBody()).message());
        verify(tokenRepo, never()).save(any());
    }
}
