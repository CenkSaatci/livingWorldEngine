package com.lwe.core.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class EntityJsonTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void parsesAttributesAndSkills() {
        assertThat(EntityJson.attributes(mapper, "{\"mut\":14,\"kk\":12}"))
            .containsEntry("mut", 14).containsEntry("kk", 12);
        assertThat(EntityJson.skills(mapper, "{\"Athletik\":7}"))
            .containsEntry("Athletik", 7);
    }

    @Test
    void blankAndEmptyObjectYieldEmptyMap() {
        assertThat(EntityJson.attributes(mapper, null)).isEmpty();
        assertThat(EntityJson.attributes(mapper, "  ")).isEmpty();
        assertThat(EntityJson.attributes(mapper, "{}")).isEmpty();
        assertThat(EntityJson.skills(mapper, "{}")).isEmpty();
    }

    @Test
    void corruptJsonFallsBackInsteadOfThrowing() {
        assertThat(EntityJson.attributes(mapper, "{kaputt")).isEmpty();
        assertThat(EntityJson.skills(mapper, "nicht json")).isEmpty();
        assertThat(EntityJson.traits(mapper, "{{")).isEmpty();
    }

    @Test
    void parsesTraitsAndIgnoresNonStrings() {
        assertThat(EntityJson.traits(mapper, "{\"traits\":[\"Zauberer\",\"Glück II\",42]}"))
            .containsExactly("Zauberer", "Glück II");
        assertThat(EntityJson.traits(mapper, "{\"traits\":\"keine Liste\"}")).isEmpty();
        assertThat(EntityJson.traits(mapper, null)).isEmpty();
    }
}
