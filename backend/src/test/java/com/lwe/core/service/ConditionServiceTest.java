package com.lwe.core.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lwe.core.domain.GameEntity;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ConditionServiceTest {

    private final ObjectMapper mapper = new ObjectMapper();
    private final ConditionService service = new ConditionService(mapper);

    private GameEntity entityWithMetadata(String metadataJson) {
        var e = new GameEntity(java.util.UUID.randomUUID(), "PC", "Held");
        e.setMetadataJson(metadataJson);
        return e;
    }

    private static final Map<String, Object> RULES = Map.of(
        "conditions", List.of(
            Map.of("name", "Wunde", "effects", List.of(
                Map.of("target", "probe", "op", "add", "value", -4),
                Map.of("target", "damage", "op", "add", "value", -2))),
            Map.of("name", "Betaeubt", "effects", List.of(
                Map.of("target", "probe", "op", "add", "value", -8)))));

    @Test
    void activeParsesStringsAndObjects() {
        var e = entityWithMetadata("{\"conditions\":[\"Wunde\",{\"name\":\"Betaeubt\",\"rounds\":2}]}");

        var active = service.active(e);

        assertThat(active).extracting(ConditionService.ConditionInstance::name)
            .containsExactly("Wunde", "Betaeubt");
        assertThat(active.get(1).rounds()).isEqualTo(2);
        assertThat(active.get(0).rounds()).isNull();
    }

    @Test
    void activeHandlesMissingMetadata() {
        assertThat(service.active(entityWithMetadata(null))).isEmpty();
        assertThat(service.active(entityWithMetadata("{}"))).isEmpty();
    }

    @Test
    void modifierSumsMatchingTargetEffects() {
        var e = entityWithMetadata("{\"conditions\":[\"Wunde\",\"Betaeubt\"]}");

        assertThat(service.modifier(e, RULES, "probe")).isEqualTo(-12);
        assertThat(service.modifier(e, RULES, "damage")).isEqualTo(-2);
        assertThat(service.modifier(e, RULES, "initiative")).isZero();
    }

    @Test
    void modifierIgnoresUnknownCatalogEntries() {
        var e = entityWithMetadata("{\"conditions\":[\"Unbekannt\"]}");
        assertThat(service.modifier(e, RULES, "probe")).isZero();
    }

    @Test
    void tickDecrementsAndExpires() {
        var e = entityWithMetadata("{\"conditions\":[\"Wunde\",{\"name\":\"Betaeubt\",\"rounds\":2}]}");

        var afterFirst = service.tick(e);
        assertThat(afterFirst).extracting(ConditionService.ConditionInstance::name)
            .containsExactly("Wunde", "Betaeubt");
        assertThat(afterFirst.get(1).rounds()).isEqualTo(1);

        var afterSecond = service.tick(e);
        assertThat(afterSecond).extracting(ConditionService.ConditionInstance::name)
            .containsExactly("Wunde");
    }

    @Test
    void addReplacesSameNameAndRemoveWorks() {
        var e = entityWithMetadata(null);

        service.add(e, new ConditionService.ConditionInstance("Wunde", null));
        service.add(e, new ConditionService.ConditionInstance("Wunde", 3));
        assertThat(service.active(e)).hasSize(1);
        assertThat(service.active(e).get(0).rounds()).isEqualTo(3);

        service.remove(e, "Wunde");
        assertThat(service.active(e)).isEmpty();
    }
}
