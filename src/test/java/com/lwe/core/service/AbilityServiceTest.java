package com.lwe.core.service;

import com.lwe.core.domain.Ability;
import com.lwe.core.domain.Ability.AbilityType;
import com.lwe.core.domain.GameSystem;
import com.lwe.core.repository.AbilityRepository;
import com.lwe.core.repository.GameSystemRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AbilityServiceTest {

    @Mock private AbilityRepository repo;
    @Mock private GameSystemRepository systemRepo;

    private AbilityService service;
    private final UUID userId = UUID.randomUUID();
    private final UUID gameSystemId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new AbilityService(repo, systemRepo);
    }

    private void stubSystemExists() {
        var system = new GameSystem("Test-System", 1, "{}", "{}");
        setId(system, gameSystemId);
        when(systemRepo.findById(gameSystemId)).thenReturn(Optional.of(system));
    }

    @Test
    void shouldCreateActiveAbility() {
        stubSystemExists();
        when(repo.save(any())).thenAnswer(inv -> {
            var a = inv.<Ability>getArgument(0);
            setId(a, UUID.randomUUID());
            return a;
        });

        var ability = service.create(gameSystemId, userId, "Feuerball", AbilityType.ACTIVE,
            "Ein mächtiger Feuerzauber", "{\"damage\":\"3d6\"}", null, 2, 0, "enemy");

        assertThat(ability.getName()).isEqualTo("Feuerball");
        assertThat(ability.getType()).isEqualTo(AbilityType.ACTIVE);
        assertThat(ability.getApCost()).isEqualTo(2);
        verify(repo).save(any());
    }

    @Test
    void shouldCreatePassiveAbility() {
        stubSystemExists();
        when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var ability = service.create(gameSystemId, userId, "Zäher Hund", AbilityType.PASSIVE,
            "Erhöht max HP um 10", null, "{\"hp_max\":10,\"regeneration\":2}", 0, 0, null);

        assertThat(ability.getName()).isEqualTo("Zäher Hund");
        assertThat(ability.getType()).isEqualTo(AbilityType.PASSIVE);
        assertThat(ability.getStatBonusesJson()).isEqualTo("{\"hp_max\":10,\"regeneration\":2}");
    }

    @Test
    void shouldRejectUnknownSystem() {
        when(systemRepo.findById(gameSystemId)).thenReturn(Optional.empty());

        assertThatThrownBy(() ->
            service.create(gameSystemId, userId, "X", AbilityType.ACTIVE, null, "{}", null, 1, 0, null))
            .isInstanceOf(AbilityService.AbilityException.class)
            .matches(e -> ((AbilityService.AbilityException) e).getErrorCode().equals("GAME_SYSTEM_NOT_FOUND"));
    }

    private void setId(Object obj, UUID id) {
        try { var f = obj.getClass().getDeclaredField("id"); f.setAccessible(true); f.set(obj, id); }
        catch (Exception e) { throw new RuntimeException(e); }
    }
}
