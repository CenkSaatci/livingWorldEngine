package com.lwe.core.service;

import com.lwe.core.domain.GameSystem;
import com.lwe.core.repository.GameSystemRepository;
import com.lwe.rules.RuleSchemaValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GameSystemServiceTest {

    @Mock private GameSystemRepository repo;
    @Mock private RuleSchemaValidator validator;

    private GameSystemService service;

    private final String validRules = """
        {"version":1,"attributes":[{"name":"s","type":"INT","default":10}],"dice_mechanics":{"probe":"1d20"}}
        """;
    private final String schema = "{\"type\":\"object\",\"properties\":{}}";

    @BeforeEach
    void setUp() {
        service = new GameSystemService(repo, validator);
    }

    @Test
    void shouldCreateValidGameSystem() {
        when(repo.existsByName("D20Lite")).thenReturn(false);
        doNothing().when(validator).validateOrThrow(validRules, schema);
        when(repo.save(any())).thenAnswer(inv -> {
            var gs = inv.<GameSystem>getArgument(0);
            var f = GameSystem.class.getDeclaredField("id");
            f.setAccessible(true);
            f.set(gs, UUID.randomUUID());
            return gs;
        });

        var result = service.create("D20Lite", 1, validRules, schema);

        assertThat(result.getName()).isEqualTo("D20Lite");
        assertThat(result.getVersion()).isEqualTo(1);
        assertThat(result.getId()).isNotNull();
        verify(validator).validateOrThrow(validRules, schema);
    }

    @Test
    void shouldRejectDuplicateName() {
        when(repo.existsByName("D20Lite")).thenReturn(true);

        assertThatThrownBy(() -> service.create("D20Lite", 1, validRules, schema))
            .isInstanceOf(GameSystemService.GameSystemException.class)
            .matches(e -> ((GameSystemService.GameSystemException) e).getErrorCode()
                .equals("GAME_SYSTEM_VERSION_CONFLICT"));
    }

    @Test
    void shouldListActive() {
        when(repo.findByActiveTrue()).thenReturn(List.of());
        assertThat(service.listActive()).isEmpty();
    }

    @Test
    void shouldThrowOnMissingId() {
        when(repo.findById(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getById(UUID.randomUUID()))
            .isInstanceOf(GameSystemService.GameSystemException.class)
            .matches(e -> ((GameSystemService.GameSystemException) e).getErrorCode()
                .equals("GAME_SYSTEM_NOT_FOUND"));
    }
}