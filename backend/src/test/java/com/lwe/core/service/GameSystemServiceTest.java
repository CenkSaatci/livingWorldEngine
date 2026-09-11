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
    void createWithOwnerIsPrivateAndOwned() {
        when(repo.existsByName("D20Lite")).thenReturn(false);
        doNothing().when(validator).validateOrThrow(validRules, schema);
        when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        var owner = UUID.randomUUID();

        var result = service.create("D20Lite", 1, validRules, schema, owner);

        assertThat(result.getOwnerId()).isEqualTo(owner);
        assertThat(result.getVisibility()).isEqualTo("PRIVATE");
    }

    @Test
    void updateRequiresOwnerOrAdmin() {
        var owner = UUID.randomUUID();
        var stranger = UUID.randomUUID();
        var gs = new GameSystem("Owned", 1, validRules, schema, owner);
        when(repo.findById(any())).thenReturn(Optional.of(gs));
        when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // Fremder -> 403-Code
        assertThatThrownBy(() -> service.update(gs.getId(), "Neu", null, null, stranger, false))
            .isInstanceOf(GameSystemService.GameSystemException.class)
            .matches(e -> ((GameSystemService.GameSystemException) e).getErrorCode()
                .equals("GAME_SYSTEM_ACCESS_DENIED"));

        // Owner + Admin duerfen
        service.update(gs.getId(), "Neu", null, null, owner, false);
        service.update(gs.getId(), "Neu2", null, null, stranger, true);
        assertThat(gs.getName()).isEqualTo("Neu2");
    }

    @Test
    void legacySystemWithoutOwnerIsAdminOnly() {
        var gs = new GameSystem("Legacy", 1, validRules, schema); // owner null, PUBLIC
        when(repo.findById(any())).thenReturn(Optional.of(gs));

        assertThatThrownBy(() -> service.delete(gs.getId(), UUID.randomUUID(), false))
            .isInstanceOf(GameSystemService.GameSystemException.class)
            .matches(e -> ((GameSystemService.GameSystemException) e).getErrorCode()
                .equals("GAME_SYSTEM_ACCESS_DENIED"));

        when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        service.delete(gs.getId(), UUID.randomUUID(), true);
        assertThat(gs.isActive()).isFalse();
    }

    @Test
    void cloneRequiresOwnershipAndAssignsCloner() {
        var owner = UUID.randomUUID();
        var stranger = UUID.randomUUID();
        var gs = new GameSystem("Owned", 1, validRules, schema, owner);
        when(repo.findById(any())).thenReturn(Optional.of(gs));

        assertThatThrownBy(() -> service.clone(gs.getId(), stranger, false))
            .isInstanceOf(GameSystemService.GameSystemException.class);

        when(repo.existsByName(any())).thenReturn(false);
        when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        var copy = service.clone(gs.getId(), owner, false);
        assertThat(copy.getOwnerId()).isEqualTo(owner);
    }

    @Test
    void listVisibleUsesFilterForNonAdmin() {
        var userId = UUID.randomUUID();
        when(repo.findVisibleForUser(userId)).thenReturn(List.of());
        assertThat(service.listVisible(userId, false)).isEmpty();
        verify(repo).findVisibleForUser(userId);

        when(repo.findByActiveTrue()).thenReturn(List.of());
        assertThat(service.listVisible(userId, true)).isEmpty();
        verify(repo).findByActiveTrue();
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