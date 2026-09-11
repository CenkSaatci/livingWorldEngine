package com.lwe.core.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lwe.core.domain.GameEntity;
import com.lwe.core.domain.World;
import com.lwe.core.repository.GameEntityRepository;
import com.lwe.core.repository.WorldRepository;
import com.lwe.core.util.WorldAccess;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CharacterSheetServiceTest {

    @Mock GameEntityRepository entityRepo;
    @Mock WorldRepository worldRepo;
    @Mock WorldAccess worldAccess;
    @Mock LevelUpService levelUpService;
    @Mock RulesLoader rulesLoader;

    private CharacterSheetService service;
    private ModifierService modifierService;
    private DerivedValueService derivedValueService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private final UUID entityId = UUID.randomUUID();
    private final UUID userId = UUID.randomUUID();
    private final UUID worldId = UUID.randomUUID();
    private final UUID systemId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        modifierService = new ModifierService();
        derivedValueService = new DerivedValueService();
        service = new CharacterSheetService(entityRepo, worldRepo, worldAccess,
            modifierService, derivedValueService, levelUpService, rulesLoader, objectMapper);
        doNothing().when(worldAccess).requireAccess(any(), any());
        lenient().when(levelUpService.getLevel(any(), any())).thenReturn(1);
    }

    private void stubRules(String rulesJson) throws Exception {
        when(rulesLoader.loadRules(isNull(), any())).thenReturn(
            objectMapper.readValue(rulesJson, new TypeReference<Map<String, Object>>() {}));
    }

    private void mockWorld(UUID gsId) {
        var world = mock(World.class);
        when(worldRepo.findById(worldId)).thenReturn(Optional.of(world));
    }

    private GameEntity mockEntity(String attrsJson, String skillsJson) {
        var entity = mock(GameEntity.class);
        when(entity.getId()).thenReturn(entityId);
        when(entity.getName()).thenReturn("Held");
        when(entity.getEntityType()).thenReturn("PC");
        when(entity.getWorldId()).thenReturn(worldId);
        when(entity.getAttributesJson()).thenReturn(attrsJson);
        if (skillsJson != null) when(entity.getSkillsJson()).thenReturn(skillsJson);
        when(entityRepo.findById(entityId)).thenReturn(Optional.of(entity));
        return entity;
    }

    @Test
    void getSheet_returnsBasicInfo() throws Exception {
        mockEntity("{\"staerke\":15,\"geschick\":12}", null);
        mockWorld(systemId);
        stubRules("""
            {
                "attributes": [{"name":"staerke","type":"INT","min":3,"max":20,"default":10}],
                "derived_values": [{"name":"hp","formula":"10+@{staerke}"}],
                "skills": [{"name":"Athletik","attributes":["staerke"],"bonus":2}],
                "conditionals": [{"name":"Stark","attribute":"staerke","operator":"gt","value":14,"bonus":"+2","target":"schaden"}]
            }
            """);

        var sheet = service.getSheet(entityId, userId);

        assertEquals("Held", sheet.entity().name());
        assertEquals("PC", sheet.entity().entityType());

        var staerke = sheet.attributes().stream().filter(a -> a.name().equals("staerke")).findFirst().orElseThrow();
        assertEquals(15, staerke.value());
        assertEquals(0.0, staerke.modifier(), 0.01);

        var hp = sheet.derivedValues().stream().filter(d -> d.name().equals("hp")).findFirst().orElseThrow();
        assertEquals(25.0, hp.value(), 0.01);

        var athletik = sheet.skills().stream().filter(s -> s.name().equals("Athletik")).findFirst().orElseThrow();
        assertEquals(2, athletik.total());

        var stark = sheet.conditionals().stream().filter(c -> c.name().equals("Stark")).findFirst().orElseThrow();
        assertTrue(stark.active());
    }

    @Test
    void getSheet_includesPerCharacterSkills() throws Exception {
        mockEntity("{\"staerke\":15,\"geschick\":12}", "{\"Athletik\":5}");
        mockWorld(systemId);
        stubRules("""
            {
                "attributes": [{"name":"staerke","type":"INT","min":3,"max":20,"default":10}],
                "skills": [{"name":"Athletik","attributes":["staerke"],"bonus":2}],
                "dice_mechanics":{"probe":"1d20+mod"}
            }
            """);

        var sheet = service.getSheet(entityId, userId);

        var athletik = sheet.skills().stream().filter(s -> s.name().equals("Athletik")).findFirst().orElseThrow();
        assertThat(athletik.perCharacterValue()).isEqualTo(5);
        assertThat(athletik.total()).isEqualTo(5);
    }

    @Test
    void getSheet_appliesSelectedTraitEffects() throws Exception {
        var entity = mockEntity("{\"staerke\":15}", null);
        when(entity.getMetadataJson()).thenReturn("{\"traits\":[\"Hohe Lebenskraft III\",\"Zauberer\"]}");
        mockWorld(systemId);
        stubRules("""
            {
                "attributes": [{"name":"staerke","type":"INT","default":10}],
                "derived_values": [
                    {"name":"hp","formula":"10+@{staerke}"},
                    {"name":"asp","formula":"20+@{staerke}"}
                ],
                "traits": [
                    {"name":"Hohe Lebenskraft","kind":"advantage",
                     "effects":[{"target":"derived:hp","op":"add","value":3}]},
                    {"name":"Zauberer","kind":"advantage",
                     "effects":[{"target":"attribute:staerke","op":"add","value":1}]}
                ],
                "dice_mechanics":{"probe":"1d20+mod"}
            }
            """);

        var sheet = service.getSheet(entityId, userId);

        // Attribut-Effekt: 15 + 1 (Trait, Tier-Suffix wird ignoriert)
        var staerke = sheet.attributes().stream().filter(a -> a.name().equals("staerke")).findFirst().orElseThrow();
        assertThat(staerke.value()).isEqualTo(16);
        // hp = 10 + 16 + 3 (Trait-Effekt nach Formel) = 29
        var hp = sheet.derivedValues().stream().filter(d -> d.name().equals("hp")).findFirst().orElseThrow();
        assertThat(hp.value()).isEqualTo(29.0);
        // asp ohne Effekt = 20 + 16
        var asp = sheet.derivedValues().stream().filter(d -> d.name().equals("asp")).findFirst().orElseThrow();
        assertThat(asp.value()).isEqualTo(36.0);
    }

    @Test
    void getSheet_includesAbilities() throws Exception {
        mockEntity("{}", null);
        mockWorld(systemId);
        stubRules("""
            {
                "attributes": [{"name":"staerke","type":"INT","min":3,"max":20,"default":10}],
                "abilities": [
                    {"name":"Angriff","type":"active","costType":"AP","cost":1,"diceExpression":"1d20+staerke","effect":"Nahkampf-Angriff","tags":["attack","melee"]},
                    {"name":"Parade","type":"active","costType":"AP","cost":0,"diceExpression":"1d20+mut","effect":"Reaktionsparade","tags":["defensive"]},
                    {"name":"Extra Attack","type":"passive","costType":"","cost":0,"diceExpression":"","effect":"","bonus":"multiAttack:2"}
                ],
                "dice_mechanics":{"probe":"1d20+mod"}
            }
            """);

        var sheet = service.getSheet(entityId, userId);

        assertThat(sheet.abilities()).hasSize(3);
        var active = sheet.abilities().stream().filter(a -> a.type().equals("active")).toList();
        assertThat(active).hasSize(2);
        assertThat(active.get(0).name()).isEqualTo("Angriff");
        assertThat(active.get(0).apCost()).isEqualTo(1);
        assertThat(active.get(0).diceExpression()).isEqualTo("1d20+staerke");

        var passive = sheet.abilities().stream().filter(a -> a.type().equals("passive")).toList();
        assertThat(passive).hasSize(1);
        assertThat(passive.get(0).name()).isEqualTo("Extra Attack");
    }

    @Test
    void getSheet_handlesEmptyRulesJson() {
        mockWorld(null);
        mockEntity("{\"staerke\":15}", null);
        when(rulesLoader.loadRules(isNull(), any())).thenReturn(Map.of());

        var sheet = service.getSheet(entityId, userId);
        assertThat(sheet).isNotNull();
        assertThat(sheet.attributes()).isNotEmpty();
        assertThat(sheet.skills()).isEmpty();
        assertThat(sheet.abilities()).isEmpty();
    }

    @Test
    void getSheet_handlesMissingGameSystem() {
        mockWorld(systemId);
        mockEntity("{\"staerke\":15}", null);
        when(rulesLoader.loadRules(isNull(), any())).thenReturn(Map.of());

        var sheet = service.getSheet(entityId, userId);
        assertThat(sheet).isNotNull();
        assertThat(sheet.abilities()).isEmpty();
    }

    @Test
    void getSheet_handlesMissingAbilitiesInRules() throws Exception {
        mockEntity("{\"staerke\":15}", null);
        mockWorld(systemId);
        stubRules("""
            {"version":1,"attributes":[{"name":"staerke","type":"INT","min":3,"max":20,"default":10}],"dice_mechanics":{"probe":"1d20+mod"}}
            """);

        var sheet = service.getSheet(entityId, userId);
        assertThat(sheet.abilities()).isEmpty();
    }
}
