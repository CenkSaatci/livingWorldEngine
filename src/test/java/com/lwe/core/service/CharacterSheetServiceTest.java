package com.lwe.core.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lwe.core.domain.GameEntity;
import com.lwe.core.domain.GameSystem;
import com.lwe.core.domain.World;
import com.lwe.core.repository.GameEntityRepository;
import com.lwe.core.repository.GameSystemRepository;
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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CharacterSheetServiceTest {

    @Mock GameEntityRepository entityRepo;
    @Mock WorldRepository worldRepo;
    @Mock GameSystemRepository systemRepo;
    @Mock WorldAccess worldAccess;
    @Mock LevelUpService levelUpService;

    private CharacterSheetService service;
    private ModifierService modifierService;
    private DerivedValueService derivedValueService;

    private final UUID entityId = UUID.randomUUID();
    private final UUID userId = UUID.randomUUID();
    private final UUID worldId = UUID.randomUUID();
    private final UUID systemId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        modifierService = new ModifierService();
        derivedValueService = new DerivedValueService();
        service = new CharacterSheetService(entityRepo, worldRepo, systemRepo, worldAccess,
            modifierService, derivedValueService, levelUpService, new ObjectMapper());
    }

    @Test
    void getSheet_returnsBasicInfo() {
        var entity = mock(GameEntity.class);
        when(entity.getId()).thenReturn(entityId);
        when(entity.getName()).thenReturn("Held");
        when(entity.getEntityType()).thenReturn("PC");
        when(entity.getWorldId()).thenReturn(worldId);
        when(entity.getAttributesJson()).thenReturn("{\"staerke\":15,\"geschick\":12}");
        when(entityRepo.findById(entityId)).thenReturn(Optional.of(entity));

        var world = mock(World.class);
        when(world.getGameSystemId()).thenReturn(systemId);
        when(worldRepo.findById(worldId)).thenReturn(Optional.of(world));
        doNothing().when(worldAccess).requireAccess(any(), any());

        var system = mock(GameSystem.class);
        when(system.getRulesJson()).thenReturn("""
            {
                "attributes": [{"name":"staerke","type":"INT","min":3,"max":20,"default":10}],
                "derived_values": [{"name":"hp","formula":"10+@{staerke}"}],
                "skills": [{"name":"Athletik","attributes":["staerke"],"bonus":2}],
                "conditionals": [{"name":"Stark","attribute":"staerke","operator":"gt","value":14,"bonus":"+2","target":"schaden"}]
            }
            """);
        when(systemRepo.findById(systemId)).thenReturn(Optional.of(system));

        var sheet = service.getSheet(entityId, userId);

        assertEquals("Held", sheet.entity().name());
        assertEquals("PC", sheet.entity().entityType());

        // Attribute
        var staerke = sheet.attributes().stream().filter(a -> a.name().equals("staerke")).findFirst().orElseThrow();
        assertEquals(15, staerke.value());
        assertEquals(0.0, staerke.modifier(), 0.01); // kein modifierFormula

        // Derived Values
        var hp = sheet.derivedValues().stream().filter(d -> d.name().equals("hp")).findFirst().orElseThrow();
        assertEquals(25.0, hp.value(), 0.01);

        // Skills
        var athletik = sheet.skills().stream().filter(s -> s.name().equals("Athletik")).findFirst().orElseThrow();
        assertEquals(2, athletik.total()); // bonus 2 + mod 0

        // Conditionals
        var stark = sheet.conditionals().stream().filter(c -> c.name().equals("Stark")).findFirst().orElseThrow();
        assertTrue(stark.active());
    }

    @Test
    void getSheet_includesPerCharacterSkills() {
        var entity = mock(GameEntity.class);
        when(entity.getId()).thenReturn(entityId);
        when(entity.getName()).thenReturn("Held");
        when(entity.getEntityType()).thenReturn("PC");
        when(entity.getWorldId()).thenReturn(worldId);
        when(entity.getAttributesJson()).thenReturn("{\"staerke\":15,\"geschick\":12}");
        when(entity.getSkillsJson()).thenReturn("{\"Athletik\":5}");
        when(entityRepo.findById(entityId)).thenReturn(Optional.of(entity));

        var world = mock(World.class);
        when(world.getGameSystemId()).thenReturn(systemId);
        when(worldRepo.findById(worldId)).thenReturn(Optional.of(world));
        doNothing().when(worldAccess).requireAccess(any(), any());

        var system = mock(GameSystem.class);
        when(system.getRulesJson()).thenReturn("""
            {
                "attributes": [{"name":"staerke","type":"INT","min":3,"max":20,"default":10}],
                "skills": [{"name":"Athletik","attributes":["staerke"],"bonus":2}],
                "dice_mechanics":{"probe":"1d20+mod"}
            }
            """);
        when(systemRepo.findById(systemId)).thenReturn(Optional.of(system));

        var sheet = service.getSheet(entityId, userId);

        var athletik = sheet.skills().stream().filter(s -> s.name().equals("Athletik")).findFirst().orElseThrow();
        assertThat(athletik.perCharacterValue()).isEqualTo(5);
        // total = perCharacter (5) + attrMod (0) = 5
        assertThat(athletik.total()).isEqualTo(5);
    }

    @Test
    void getSheet_includesAbilities() {
        var entity = mock(GameEntity.class);
        when(entity.getId()).thenReturn(entityId);
        when(entity.getName()).thenReturn("Held");
        when(entity.getEntityType()).thenReturn("PC");
        when(entity.getWorldId()).thenReturn(worldId);
        when(entity.getAttributesJson()).thenReturn("{}");
        when(entityRepo.findById(entityId)).thenReturn(Optional.of(entity));

        var world = mock(World.class);
        when(world.getGameSystemId()).thenReturn(systemId);
        when(worldRepo.findById(worldId)).thenReturn(Optional.of(world));
        doNothing().when(worldAccess).requireAccess(any(), any());

        var system = mock(GameSystem.class);
        when(system.getRulesJson()).thenReturn("""
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
        when(systemRepo.findById(systemId)).thenReturn(Optional.of(system));

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
        var entity = mock(GameEntity.class);
        when(entity.getId()).thenReturn(entityId);
        when(entity.getName()).thenReturn("Held");
        when(entity.getEntityType()).thenReturn("PC");
        when(entity.getWorldId()).thenReturn(worldId);
        when(entity.getAttributesJson()).thenReturn("{\"staerke\":15}");
        when(entityRepo.findById(entityId)).thenReturn(Optional.of(entity));

        var world = mock(World.class);
        when(world.getGameSystemId()).thenReturn(null);
        when(worldRepo.findById(worldId)).thenReturn(Optional.of(world));
        doNothing().when(worldAccess).requireAccess(any(), any());

        var sheet = service.getSheet(entityId, userId);
        assertThat(sheet).isNotNull();
        // Attributes from entity JSON are still shown (min/max default to 1/99)
        assertThat(sheet.attributes()).isNotEmpty();
        assertThat(sheet.skills()).isEmpty();
        assertThat(sheet.abilities()).isEmpty();
    }

    @Test
    void getSheet_handlesMissingGameSystem() {
        var entity = mock(GameEntity.class);
        when(entity.getId()).thenReturn(entityId);
        when(entity.getName()).thenReturn("Held");
        when(entity.getEntityType()).thenReturn("PC");
        when(entity.getWorldId()).thenReturn(worldId);
        when(entity.getAttributesJson()).thenReturn("{\"staerke\":15}");
        when(entityRepo.findById(entityId)).thenReturn(Optional.of(entity));

        var world = mock(World.class);
        when(world.getGameSystemId()).thenReturn(systemId);
        when(worldRepo.findById(worldId)).thenReturn(Optional.of(world));
        doNothing().when(worldAccess).requireAccess(any(), any());

        when(systemRepo.findById(systemId)).thenReturn(Optional.empty());

        var sheet = service.getSheet(entityId, userId);
        assertThat(sheet).isNotNull();
        assertThat(sheet.abilities()).isEmpty();
    }

    @Test
    void getSheet_handlesMissingAbilitiesInRules() {
        var entity = mock(GameEntity.class);
        when(entity.getId()).thenReturn(entityId);
        when(entity.getName()).thenReturn("Held");
        when(entity.getEntityType()).thenReturn("PC");
        when(entity.getWorldId()).thenReturn(worldId);
        when(entity.getAttributesJson()).thenReturn("{\"staerke\":15}");
        when(entityRepo.findById(entityId)).thenReturn(Optional.of(entity));

        var world = mock(World.class);
        when(world.getGameSystemId()).thenReturn(systemId);
        when(worldRepo.findById(worldId)).thenReturn(Optional.of(world));
        doNothing().when(worldAccess).requireAccess(any(), any());

        var system = mock(GameSystem.class);
        when(system.getRulesJson()).thenReturn("""
            {"version":1,"attributes":[{"name":"staerke","type":"INT","min":3,"max":20,"default":10}],"dice_mechanics":{"probe":"1d20+mod"}}
            """);
        when(systemRepo.findById(systemId)).thenReturn(Optional.of(system));

        var sheet = service.getSheet(entityId, userId);
        assertThat(sheet.abilities()).isEmpty();
    }
}
