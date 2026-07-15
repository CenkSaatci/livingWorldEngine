package com.lwe.core.service;

import com.lwe.core.domain.GameEntity;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class AttributeUtilsTest {

    private final UUID worldId = UUID.randomUUID();

    @Test
    void shouldReturnValueFromAttributes() {
        var entity = new GameEntity(worldId, "PC", "Hero");
        entity.setAttributesJson("{\"strength\":16,\"dexterity\":14}");

        var result = AttributeUtils.extractAttribute(entity, "strength");

        assertThat(result).isPresent().contains(16);
    }

    @Test
    void shouldReturnEmptyForUnknownAttribute() {
        var entity = new GameEntity(worldId, "PC", "Hero");
        entity.setAttributesJson("{\"strength\":16}");

        var result = AttributeUtils.extractAttribute(entity, "charisma");

        assertThat(result).isEmpty();
    }

    @Test
    void shouldReturnEmptyForMalformedJson() {
        var entity = new GameEntity(worldId, "PC", "Hero");
        entity.setAttributesJson("not-json");

        var result = AttributeUtils.extractAttribute(entity, "strength");

        assertThat(result).isEmpty();
    }

    @Test
    void shouldComputeManhattanDistance() {
        var a = new GameEntity(worldId, "PC", "Hero");
        a.setPositionJson("{\"x\":3,\"y\":7}");
        var b = new GameEntity(worldId, "NPC", "Goblin");
        b.setPositionJson("{\"x\":8,\"y\":2}");

        // |3-8| + |7-2| = 5 + 5 = 10
        assertThat(AttributeUtils.gridDistance(a, b)).isEqualTo(10);
    }

    @Test
    void shouldReturnZeroForSamePosition() {
        var a = new GameEntity(worldId, "PC", "Hero");
        a.setPositionJson("{\"x\":5,\"y\":5}");
        var b = new GameEntity(worldId, "NPC", "Goblin");
        b.setPositionJson("{\"x\":5,\"y\":5}");

        assertThat(AttributeUtils.gridDistance(a, b)).isZero();
    }
}
