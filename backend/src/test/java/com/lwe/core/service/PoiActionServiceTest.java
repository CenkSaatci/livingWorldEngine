package com.lwe.core.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lwe.api.dto.ProbeResponse;
import com.lwe.core.domain.GameEntity;
import com.lwe.core.domain.GameItem;
import com.lwe.core.domain.GameSystem;
import com.lwe.core.domain.Location;
import com.lwe.core.domain.Region;
import com.lwe.core.repository.*;
import com.lwe.core.util.EntityAccess;
import com.lwe.core.util.WorldAccess;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class PoiActionServiceTest {

    private final LocationRepository locationRepo = mock();
    private final RegionRepository regionRepo = mock();
    private final CampaignRepository campaignRepo = mock();
    private final GameEntityRepository entityRepo = mock();
    private final GameItemRepository itemRepo = mock();
    private final RulesLoader rulesLoader = mock();
    private final WorldAccess worldAccess = mock();
    private final EntityAccess entityAccess = mock();
    private final ProbeService probeService = mock();
    private final RestService restService = mock();
    private final InventoryService inventoryService = mock();
    private final CampaignMemberService campaignMemberService = mock();
    private final ChatMessageRepository chatRepo = mock();
    private final SimpMessagingTemplate messaging = mock();
    private final ObjectMapper mapper = new ObjectMapper();
    private final CurrencyService currencyService = new CurrencyService(mapper);
    private final ConditionService conditionService = new ConditionService(mapper);

    private PoiActionService service;

    private final UUID userId = UUID.randomUUID();
    private final UUID worldId = UUID.randomUUID();
    private final UUID regionId = UUID.randomUUID();
    private final UUID locationId = UUID.randomUUID();
    private final UUID actorId = UUID.randomUUID();

    private Map<String, Object> rules;
    private GameEntity actor;

    @BeforeEach
    void setUp() {
        service = new PoiActionService(locationRepo, regionRepo, campaignRepo, entityRepo,
            rulesLoader, worldAccess, entityAccess, currencyService, conditionService, probeService,
            restService, campaignMemberService,
            new PoiBindings(entityRepo, mapper),
            new PoiItems(itemRepo, inventoryService, mapper),
            new PoiChat(chatRepo, messaging),
            mapper);

        var location = new Location(regionId, "village", "Bree");
        location.setServices("[\"Medicus\",\"Wirtshaus\",\"Handeln\",\"Aussichtspunkt\",\"Geheim\",\"Ruine\",\"Probe\",\"FalscherSkill\"]");
        when(locationRepo.findById(locationId)).thenReturn(Optional.of(location));
        when(regionRepo.findById(regionId)).thenReturn(Optional.of(new Region(worldId, "Mittelreich")));
        when(campaignRepo.findByWorldId(worldId)).thenReturn(List.of());
        // Standard: kein Welt-DM (dmOnly-Aktionen sperren).
        doThrow(new WorldAccess.WorldAccessException("WORLD_ACCESS_DENIED", "kein DM"))
            .when(worldAccess).requireDm(any(), any());

        rules = Map.of(
            "currency", Map.of("denominations", List.of(
                Map.of("name", "Kupfer", "abbr", "K", "factor", 1),
                Map.of("name", "Gold", "abbr", "G", "factor", 100))),
            "conditions", List.of(Map.of("name", "Wunde")),
            "skills", List.of(Map.of("name", "Sinnesschärfe", "attributes", List.of("klugheit"))),
            "poi_actions", List.of(
                Map.of("name", "Medicus", "chat", "actor", "effects", List.of(
                    Map.of("type", "money", "amount", -15),
                    Map.of("type", "heal", "amount", "2d6"),
                    Map.of("type", "condition", "name", "Wunde", "remove", true))),
                Map.of("name", "Wirtshaus", "chat", "public", "effects", List.of(
                    Map.of("type", "money", "amount", -5),
                    Map.of("type", "rest", "mode", "long"))),
                Map.of("name", "Handeln", "trade", Map.of("buy", true, "sell", true)),
                Map.of("name", "Aussichtspunkt", "description", "Weitblick", "effects", List.of()),
                Map.of("name", "Geheim", "dmOnly", true),
                Map.of("name", "Ruine", "requiresTrait", "Mutig", "effects", List.of()),
                Map.of("name", "Fund", "effects", List.of(
                    Map.of("type", "item", "name", "Alter Schlüssel", "qty", 1))),
                Map.of("name", "Probe", "probe", Map.of("skill", "Sinnesschärfe", "target", 15, "difficulty", 0,
                    "onSuccess", List.of(Map.of("type", "text", "text", "Du findest etwas.")),
                    "onFailure", List.of(Map.of("type", "text", "text", "Nur Schutt.")))),
                Map.of("name", "FalscherSkill", "probe", Map.of("skill", "Nix",
                    "onSuccess", List.of(), "onFailure", List.of()))));
        when(rulesLoader.loadRules(any(), eq(worldId))).thenReturn(rules);
        when(rulesLoader.resolveSystem(any(), eq(worldId))).thenReturn(null);

        actor = new GameEntity(worldId, "PC", "Mira");
        setId(actor, actorId);
        actor.setHpMax(20);
        actor.setHpCurrent(10);
        actor.setMetadataJson("{\"money\":100,\"conditions\":[\"Wunde\"],\"traits\":[\"Mutig\"]}");
        when(entityAccess.requireControl(actorId, userId)).thenReturn(actor);
        when(entityRepo.findByIdForUpdate(actorId)).thenReturn(Optional.of(actor));
    }

    /** JPA-IDs sind generiert und haben keinen Setter — für Unit-Tests per Reflection. */
    private static void setId(Object entity, UUID id) {
        try {
            var field = entity.getClass().getDeclaredField("id");
            field.setAccessible(true);
            field.set(entity, id);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private PoiActionService.ActionResult execute(String name) {
        return service.execute(locationId, name, actorId, userId, null);
    }

    @Test
    void unknownActionRejected() {
        assertThatThrownBy(() -> execute("NichtDa"))
            .isInstanceOf(PoiException.class)
            .extracting(e -> ((PoiException) e).getErrorCode())
            .isEqualTo("POI_ACTION_UNKNOWN");
    }

    @Test
    void actionNotBoundToLocationRejected() {
        assertThatThrownBy(() -> execute("Fund"))
            .isInstanceOf(PoiException.class)
            .extracting(e -> ((PoiException) e).getErrorCode())
            .isEqualTo("POI_ACTION_NOT_AVAILABLE");
    }

    @Test
    void medicusAppliesMoneyHealAndConditionRemoval() {
        var result = execute("Medicus");

        assertThat(currencyService.money(actor)).isEqualTo(85);
        assertThat(actor.getHpCurrent()).isBetween(11, 20);
        assertThat(conditionService.active(actor)).isEmpty();
        assertThat(result.moneyAfter()).isEqualTo(85);
        assertThat(result.effects()).extracting(e -> e.get("type"))
            .contains("money", "heal", "condition");
        assertThat(result.moneyAfterText()).isEqualTo("85 K");
    }

    @Test
    void insufficientMoneyAppliesNothing() {
        actor.setMetadataJson("{\"money\":5,\"conditions\":[\"Wunde\"]}");

        assertThatThrownBy(() -> execute("Medicus"))
            .isInstanceOf(CurrencyService.CurrencyException.class);
        assertThat(currencyService.money(actor)).isEqualTo(5);
        assertThat(actor.getHpCurrent()).isEqualTo(10);
        assertThat(conditionService.active(actor)).isNotEmpty();
        verify(inventoryService, never()).addItemInternal(any(), any(), anyInt());
    }

    @Test
    void dmOnlyActionForbiddenForPlayer() {
        assertThatThrownBy(() -> execute("Geheim"))
            .isInstanceOf(PoiException.class)
            .extracting(e -> ((PoiException) e).getErrorCode())
            .isEqualTo("POI_ACTION_FORBIDDEN");
    }

    @Test
    void missingTraitRejected() {
        actor.setMetadataJson("{\"money\":100,\"traits\":[]}");
        assertThatThrownBy(() -> execute("Ruine"))
            .isInstanceOf(PoiException.class)
            .extracting(e -> ((PoiException) e).getErrorCode())
            .isEqualTo("POI_ACTION_TRAIT_REQUIRED");
    }

    @Test
    void restActionDelegatesToRestService() {
        var result = execute("Wirtshaus");
        assertThat(currencyService.money(actor)).isEqualTo(95);
        verify(restService).longRest(actorId, userId, null);
        assertThat(result.effects()).extracting(e -> e.get("type")).contains("rest");
    }

    @Test
    void tradeActionReturnsTradeFlagWithoutEffects() {
        var result = execute("Handeln");
        assertThat(result.trade()).isTrue();
        assertThat(result.effects()).isEmpty();
        assertThat(currencyService.money(actor)).isEqualTo(100);
    }

    @Test
    void narrativeActionNeedsNoMechanics() {
        var result = execute("Aussichtspunkt");
        assertThat(result.success()).isTrue();
        assertThat(result.text()).isEqualTo("Weitblick");
        assertThat(result.effects()).isEmpty();
    }

    @Test
    void probeFailureRunsFailureBranch() {
        when(probeService.executeProbe(eq(actorId), eq(userId), eq("Sinnesschärfe"),
            anyInt(), anyBoolean(), any(), anyInt()))
            .thenReturn(new ProbeResponse("d20_target", new int[]{3}, 0, 3, false, List.of(), List.of()));

        var result = execute("Probe");

        assertThat(result.probe().success()).isFalse();
        assertThat(result.text()).isEqualTo("Nur Schutt.");
        // M-2: konfiguriertes Ziel wird durchgereicht (Default wäre 10).
        verify(probeService).executeProbe(actorId, userId, "Sinnesschärfe", 15, false, null, 0);
    }

    @Test
    void probeWithUnknownSkillRejected() {
        assertThatThrownBy(() -> execute("FalscherSkill"))
            .isInstanceOf(PoiException.class)
            .extracting(e -> ((PoiException) e).getErrorCode())
            .isEqualTo("ROLL_SKILL_NOT_FOUND");
    }

    @Test
    void itemEffectAddsResolvedItem() {
        var system = new GameSystem("Test", 1, "{}", null);
        when(rulesLoader.resolveSystem(any(), eq(worldId))).thenReturn(system);
        var item = new GameItem(system.getId(), "Alter Schlüssel", "MISC", BigDecimal.ONE, 0);
        setId(item, UUID.randomUUID());
        when(itemRepo.findByGameSystemId(any())).thenReturn(List.of(item));
        // Fund an den Ort binden
        var location = locationRepo.findById(locationId).orElseThrow();
        location.setServices("[\"Fund\"]");

        execute("Fund");

        verify(inventoryService).addItemInternal(actor, item.getId(), 1);
    }

    @Test
    void listReturnsBoundActionsAndHidesDmOnlyAndUnbound() {
        var list = service.list(locationId, actorId, userId, null, false);
        var byName = list.stream().collect(java.util.stream.Collectors.toMap(
            PoiActionService.ActionInfo::name, a -> a));

        assertThat(byName.get("Medicus").available()).isTrue();
        assertThat(byName.get("Handeln").trade()).isTrue();
        assertThat(byName.get("Medicus").costs()).isNotEmpty();
        // M-5: ungebundene Aktionen erscheinen nicht mehr.
        assertThat(byName).doesNotContainKey("Fund");
        // M-1: dmOnly-Aktionen (inkl. Beschreibung) nicht an Nicht-Leiter.
        assertThat(byName).doesNotContainKey("Geheim");
    }

    @Test
    void listAllShowsUnboundAndDmOnlyForDm() {
        // Leiter: requireDm wirft nicht → isDm true.
        org.mockito.Mockito.doNothing().when(worldAccess).requireDm(any(), any());

        var list = service.list(locationId, actorId, userId, null, true);
        var byName = list.stream().collect(java.util.stream.Collectors.toMap(
            PoiActionService.ActionInfo::name, a -> a));

        assertThat(byName).containsKey("Fund");
        assertThat(byName.get("Fund").available()).isFalse();
        assertThat(byName.get("Fund").reason()).isEqualTo("NOT_HERE");
        assertThat(byName).containsKey("Geheim");
    }
}
