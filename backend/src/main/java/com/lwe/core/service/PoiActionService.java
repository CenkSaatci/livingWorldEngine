package com.lwe.core.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lwe.core.domain.GameEntity;
import com.lwe.core.domain.Location;
import com.lwe.core.repository.CampaignRepository;
import com.lwe.core.repository.GameEntityRepository;
import com.lwe.core.repository.LocationRepository;
import com.lwe.core.repository.RegionRepository;
import com.lwe.core.util.EntityAccess;
import com.lwe.core.util.EntityJson;
import com.lwe.core.util.RuleNames;
import com.lwe.core.util.WorldAccess;
import com.lwe.rules.DiceExpression;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * POI-Aktionen (ADR-015): deklarativer Katalog aus {@code rules.poi_actions[]},
 * gebunden an Ort ({@code location.services}) oder NPC ({@code services_offered}).
 *
 * <p>Ablauf strikt „erst validieren, dann anwenden" (H-2): alle negativen Posten
 * werden geprüft, bevor irgendein Effekt wirkt. Item- und Chat-Zugriffe liegen in
 * {@link PoiItems} bzw. {@link PoiChat}, die Bindung in {@link PoiBindings}.
 */
@Service
public class PoiActionService {

    private final LocationRepository locationRepo;
    private final RegionRepository regionRepo;
    private final CampaignRepository campaignRepo;
    private final GameEntityRepository entityRepo;
    private final RulesLoader rulesLoader;
    private final WorldAccess worldAccess;
    private final EntityAccess entityAccess;
    private final CurrencyService currencyService;
    private final ConditionService conditionService;
    private final ProbeService probeService;
    private final RestService restService;
    private final CampaignMemberService campaignMemberService;
    private final PoiBindings bindings;
    private final PoiItems items;
    private final PoiChat chat;
    private final ObjectMapper mapper;

    public PoiActionService(LocationRepository locationRepo, RegionRepository regionRepo,
                            CampaignRepository campaignRepo, GameEntityRepository entityRepo,
                            RulesLoader rulesLoader, WorldAccess worldAccess, EntityAccess entityAccess,
                            CurrencyService currencyService, ConditionService conditionService,
                            ProbeService probeService, RestService restService,
                            CampaignMemberService campaignMemberService, PoiBindings bindings,
                            PoiItems items, PoiChat chat, ObjectMapper mapper) {
        this.locationRepo = locationRepo;
        this.regionRepo = regionRepo;
        this.campaignRepo = campaignRepo;
        this.entityRepo = entityRepo;
        this.rulesLoader = rulesLoader;
        this.worldAccess = worldAccess;
        this.entityAccess = entityAccess;
        this.currencyService = currencyService;
        this.conditionService = conditionService;
        this.probeService = probeService;
        this.restService = restService;
        this.campaignMemberService = campaignMemberService;
        this.bindings = bindings;
        this.items = items;
        this.chat = chat;
        this.mapper = mapper;
    }

    // ---------------------------------------------------------------- Listing

    public record ActionInfo(String name, String description, String chat, boolean trade,
                             boolean dmOnly, String requiresTrait, boolean available,
                             String reason, List<Map<String, Object>> costs) {}

    /**
     * Verfügbare Aktionen eines Ortes für einen Charakter (inkl. Sperrgrund).
     * Standardmäßig nur die an diesem Ort gebundenen Aktionen; {@code includeAll}
     * (nur Leiter) liefert den ganzen Katalog für die Autoren-Vorschau.
     */
    public List<ActionInfo> list(UUID locationId, UUID actorId, UUID userId, UUID campaignId,
                                 boolean includeAll) {
        var ctx = context(locationId, userId, campaignId);
        var actor = actorId == null ? null : loadActor(actorId, userId, ctx.worldId());
        var bound = bindings.boundNames(ctx.location(), ctx.worldId());
        boolean dm = isDm(ctx.worldId(), ctx.campaignId(), userId);
        var out = new ArrayList<ActionInfo>();
        for (var action : catalog(ctx.rules())) {
            var name = text(action.get("name"));
            if (name == null) continue;
            // M-1: dmOnly-Aktionen (inkl. Beschreibung) nicht an Nicht-Leiter ausliefern.
            boolean dmOnly = Boolean.TRUE.equals(action.get("dmOnly"));
            if (dmOnly && !dm) continue;
            boolean isBound = PoiBindings.isBound(name, bound);
            // M-5: ungebundene Aktionen nur in der Leiter-Vorschau.
            if (!isBound && !(includeAll && dm)) continue;
            var reasons = availabilityReasons(action, bound, actor, ctx, userId);
            out.add(new ActionInfo(name, text(action.get("description")),
                chatMode(action, hasEffects(action)), action.get("trade") instanceof Map,
                dmOnly, text(action.get("requiresTrait")),
                reasons.isEmpty(), reasons.isEmpty() ? null : reasons.get(0),
                costs(action, ctx.rules())));
        }
        return out;
    }

    // -------------------------------------------------------------- Execution

    public record ActionResult(String action, boolean success, boolean trade, String text,
                               int moneyBefore, int moneyAfter,
                               String moneyBeforeText, String moneyAfterText,
                               List<Map<String, Object>> effects, ProbeInfo probe) {}
    public record ProbeInfo(String skill, boolean success, int total, int[] dice, int modifier) {}

    @Transactional
    public ActionResult execute(UUID locationId, String actionName, UUID actorId, UUID userId,
                                UUID campaignId) {
        var ctx = context(locationId, userId, campaignId);
        var action = findAction(ctx.rules(), actionName);
        if (action == null) throw new PoiException("POI_ACTION_UNKNOWN", "Unknown action: " + actionName);
        var bound = bindings.boundNames(ctx.location(), ctx.worldId());
        if (!PoiBindings.isBound(actionName, bound)) {
            throw new PoiException("POI_ACTION_NOT_AVAILABLE", "Action not available here");
        }
        if (Boolean.TRUE.equals(action.get("dmOnly")) && !isDm(ctx.worldId(), ctx.campaignId(), userId)) {
            throw new PoiException("POI_ACTION_FORBIDDEN", "DM only action");
        }
        var actor = loadActor(actorId, userId, ctx.worldId());
        var requiredTrait = text(action.get("requiresTrait"));
        if (requiredTrait != null && !RuleNames.hasTrait(EntityJson.traits(mapper, actor.getMetadataJson()), requiredTrait)) {
            throw new PoiException("POI_ACTION_TRAIT_REQUIRED", "Requires trait: " + requiredTrait);
        }
        // Schreib-Lock: Geld/HP/Inventar in einer Einheit.
        actor = entityRepo.findByIdForUpdate(actor.getId())
            .orElseThrow(() -> new PoiException("ENTITY_NOT_FOUND", "Actor not found"));

        int moneyBefore = currencyService.money(actor);
        var moneyBeforeText = currencyService.format(moneyBefore, ctx.rules());

        // Händler-Aktion: keine Effekte, die UI öffnet die Händlerliste.
        if (action.get("trade") instanceof Map) {
            return new ActionResult(actionName, true, true, text(action.get("description")),
                moneyBefore, moneyBefore, moneyBeforeText, moneyBeforeText, List.of(), null);
        }

        // 1) Probe (falls konfiguriert) — Ergebnis wählt den Effekt-Zweig.
        ProbeInfo probe = null;
        List<Map<String, Object>> effects = effects(action);
        if (action.get("probe") instanceof Map<?, ?> probeDef) {
            var skill = text(probeDef.get("skill"));
            if (skill == null) throw new PoiException("POI_ACTION_INVALID", "Probe without skill");
            // M-3: fail-closed bei unbekanntem Skill (d20/d100 würfelten sonst still mit Mod 0).
            requireKnownSkill(skill, ctx.rules());
            int target = probeDef.get("target") instanceof Number n ? n.intValue() : 10;
            int difficulty = probeDef.get("difficulty") instanceof Number n ? n.intValue() : 0;
            var response = probeService.executeProbe(actor.getId(), userId, skill, target, false,
                ctx.campaignId(), difficulty);
            probe = new ProbeInfo(skill, response.success(), response.total(),
                response.dice(), response.modifier());
            effects = effectList(probeDef.get(response.success() ? "onSuccess" : "onFailure"));
        }

        // 2) Vorvalidierung ohne Wirkung.
        validate(actor, effects, ctx);

        // 3) Anwenden (transaktional).
        var applied = new ArrayList<Map<String, Object>>();
        var texts = new ArrayList<String>();
        for (var effect : effects) {
            applyEffect(actor, effect, ctx, applied, texts);
        }
        // Rast zuletzt: RestService lädt dieselbe (gemanagte) Entity und speichert.
        for (var effect : effects) {
            if ("rest".equals(text(effect.get("type")))) {
                var mode = text(effect.get("mode"));
                if ("short".equalsIgnoreCase(mode)) restService.shortRest(actor.getId(), userId, ctx.campaignId());
                else restService.longRest(actor.getId(), userId, ctx.campaignId());
            }
        }
        entityRepo.save(actor);

        int moneyAfter = currencyService.money(actor);
        var text = String.join(" ", texts).strip();
        if (text.isEmpty()) text = text(action.get("description"));

        // 4) Chat/Event gemäß Sichtbarkeit.
        deliverChat(action, actor, ctx, text, applied);

        return new ActionResult(actionName, true, false, text,
            moneyBefore, moneyAfter, moneyBeforeText,
            currencyService.format(moneyAfter, ctx.rules()), applied, probe);
    }

    // ----------------------------------------------------------- Availability

    private List<String> availabilityReasons(Map<String, Object> action, Set<String> bound,
                                             GameEntity actor, Ctx ctx, UUID userId) {
        var reasons = new ArrayList<String>();
        if (!PoiBindings.isBound(text(action.get("name")), bound)) reasons.add("NOT_HERE");
        if (Boolean.TRUE.equals(action.get("dmOnly")) && !isDm(ctx.worldId(), ctx.campaignId(), userId)) {
            reasons.add("DM_ONLY");
        }
        var trait = text(action.get("requiresTrait"));
        if (trait != null && actor != null
            && !RuleNames.hasTrait(EntityJson.traits(mapper, actor.getMetadataJson()), trait)) {
            reasons.add("TRAIT_REQUIRED");
        }
        return reasons;
    }

    // ------------------------------------------------------------- Validation

    private void validate(GameEntity actor, List<Map<String, Object>> effects, Ctx ctx) {
        int moneyCost = 0;
        for (var effect : effects) {
            var type = text(effect.get("type"));
            if (type == null) throw new PoiException("POI_ACTION_INVALID", "Effect without type");
            switch (type) {
                case "money" -> moneyCost += Math.max(0, -(intAmount(effect)));
                case "item" -> {
                    var itemName = text(effect.get("name"));
                    if (itemName == null) throw new PoiException("POI_ACTION_INVALID", "Item without name");
                    var item = items.resolve(ctx.system(), itemName);
                    int qty = itemQty(effect);
                    if (qty == 0) throw new PoiException("POI_ACTION_INVALID", "Item quantity must not be 0");
                    if (qty < 0) {
                        int have = items.quantity(actor, item.getId());
                        if (have < -qty) {
                            throw new PoiException("POI_ITEM_NOT_OWNED",
                                "Needs " + (-qty) + "x " + itemName + ", has " + have);
                        }
                    }
                }
                case "condition" -> {
                    var name = text(effect.get("name"));
                    if (name == null) throw new PoiException("POI_ACTION_INVALID", "Condition without name");
                    validateCondition(name, ctx.rules());
                }
                case "heal" -> healExpression(effect);
                case "fate" -> {
                    if (intAmount(effect) < 0 && fatePoints(actor, ctx.rules()) + intAmount(effect) < 0) {
                        throw new PoiException("FATE_NONE_LEFT", "No fate points left");
                    }
                }
                case "rest", "text" -> { /* keine Vorprüfung */ }
                default -> throw new PoiException("POI_ACTION_INVALID", "Unknown effect type: " + type);
            }
        }
        currencyService.requirePayable(actor, moneyCost);
    }

    // -------------------------------------------------------------- Application

    private void applyEffect(GameEntity actor, Map<String, Object> effect, Ctx ctx,
                             List<Map<String, Object>> applied, List<String> texts) {
        var type = text(effect.get("type"));
        var row = new LinkedHashMap<String, Object>();
        switch (type) {
            case "money" -> {
                int amount = intAmount(effect);
                if (amount >= 0) currencyService.credit(actor, amount);
                else currencyService.pay(actor, -amount);
                row.put("type", "money");
                row.put("amount", amount);
                row.put("text", currencyService.format(Math.abs(amount), ctx.rules()));
            }
            case "item" -> {
                var item = items.resolve(ctx.system(), text(effect.get("name")));
                int qty = itemQty(effect);
                if (qty == 0) throw new PoiException("POI_ACTION_INVALID", "Item quantity must not be 0");
                if (qty > 0) items.add(actor, item.getId(), qty);
                else items.remove(actor, item.getId(), -qty);
                row.put("type", "item");
                row.put("name", item.getName());
                row.put("qty", qty);
            }
            case "heal" -> {
                int healed = applyHeal(actor, effect);
                row.put("type", "heal");
                row.put("amount", healed);
            }
            case "condition" -> {
                var name = text(effect.get("name"));
                boolean remove = Boolean.TRUE.equals(effect.get("remove"));
                Integer rounds = effect.get("rounds") instanceof Number n ? n.intValue() : null;
                if (remove) conditionService.remove(actor, name);
                else conditionService.add(actor, new ConditionService.ConditionInstance(name, rounds));
                row.put("type", "condition");
                row.put("name", name);
                row.put("remove", remove);
                if (rounds != null) row.put("rounds", rounds);
            }
            case "fate" -> {
                int next = writeFatePoints(actor, ctx.rules(), intAmount(effect));
                row.put("type", "fate");
                row.put("amount", intAmount(effect));
                row.put("current", next);
            }
            case "rest" -> {
                row.put("type", "rest");
                row.put("mode", text(effect.get("mode")));
            }
            case "text" -> {
                var value = text(effect.get("text"));
                if (value != null) {
                    texts.add(value);
                    row.put("type", "text");
                    row.put("text", value);
                }
            }
            default -> throw new PoiException("POI_ACTION_INVALID", "Unknown effect type: " + type);
        }
        applied.add(row);
    }

    private int applyHeal(GameEntity actor, Map<String, Object> effect) {
        var expr = healExpression(effect);
        int healed;
        if ("full".equalsIgnoreCase(expr)) {
            healed = Math.max(0, actor.getHpMax() - actor.getHpCurrent());
            actor.setHpCurrent(actor.getHpMax());
        } else if (expr.matches("\\d+")) {
            healed = Math.min(Integer.parseInt(expr), Math.max(0, actor.getHpMax() - actor.getHpCurrent()));
            actor.setHpCurrent(actor.getHpCurrent() + healed);
        } else {
            var roll = new DiceExpression(expr);
            healed = Math.min(roll.getTotal(), Math.max(0, actor.getHpMax() - actor.getHpCurrent()));
            actor.setHpCurrent(actor.getHpCurrent() + healed);
        }
        return healed;
    }

    private int fatePoints(GameEntity actor, Map<String, Object> rules) {
        int max = fateMax(rules);
        if (actor.getMetadataJson() == null || actor.getMetadataJson().isBlank()) return max;
        try {
            var node = mapper.readTree(actor.getMetadataJson()).path("fate_points");
            return node.isInt() || node.isLong() ? node.asInt() : max;
        } catch (Exception e) {
            return max;
        }
    }

    private int fateMax(Map<String, Object> rules) {
        if (rules.get("creationBudget") instanceof Map<?, ?> b && b.get("fatePoints") instanceof Number n) {
            return n.intValue();
        }
        return 0;
    }

    private int writeFatePoints(GameEntity actor, Map<String, Object> rules, int delta) {
        int current = fatePoints(actor, rules);
        int max = fateMax(rules);
        int next = Math.max(0, current + delta);
        if (max > 0) next = Math.min(next, max);
        try {
            var parsed = actor.getMetadataJson() == null || actor.getMetadataJson().isBlank()
                ? mapper.createObjectNode() : mapper.readTree(actor.getMetadataJson());
            var meta = parsed.isObject()
                ? (com.fasterxml.jackson.databind.node.ObjectNode) parsed : mapper.createObjectNode();
            meta.put("fate_points", next);
            actor.setMetadataJson(mapper.writeValueAsString(meta));
        } catch (Exception e) {
            throw new RuntimeException("Failed to update fate points", e);
        }
        return next;
    }

    private String healExpression(Map<String, Object> effect) {
        var amount = effect.get("amount");
        if (amount instanceof Number n) return String.valueOf(n.intValue());
        var expr = text(amount);
        if (expr == null) throw new PoiException("POI_ACTION_INVALID", "heal without amount");
        if ("full".equalsIgnoreCase(expr) || expr.matches("\\d+") || expr.matches("^\\d+d\\d+([+-]\\d+)?$")) {
            return expr;
        }
        throw new PoiException("POI_ACTION_INVALID", "Invalid heal expression: " + expr);
    }

    // ------------------------------------------------------------------- Chat

    private void deliverChat(Map<String, Object> action, GameEntity actor, Ctx ctx, String text,
                             List<Map<String, Object>> applied) {
        chat.deliver(chatMode(action, hasEffects(action)), ctx.worldId(), actor,
            text(action.get("name")), text, applied);
    }

    private String chatMode(Map<String, Object> action, boolean hasEffects) {
        var configured = text(action.get("chat"));
        if (configured != null) return configured;
        return hasEffects ? "actor" : "public";
    }

    // -------------------------------------------------------------- Helpers

    private record Ctx(Location location, UUID worldId, UUID campaignId, Map<String, Object> rules,
                       com.lwe.core.domain.GameSystem system) {}

    private Ctx context(UUID locationId, UUID userId, UUID campaignId) {
        var location = locationRepo.findById(locationId)
            .orElseThrow(() -> new PoiException("LOCATION_NOT_FOUND", "Location not found"));
        var worldId = regionRepo.findById(location.getRegionId())
            .orElseThrow(() -> new PoiException("REGION_NOT_FOUND", "Region not found")).getWorldId();
        worldAccess.requireRead(worldId, userId);
        var resolvedCampaign = resolveCampaign(worldId, campaignId);
        var rules = rulesLoader.loadRules(resolvedCampaign, worldId);
        return new Ctx(location, worldId, resolvedCampaign, rules,
            rulesLoader.resolveSystem(resolvedCampaign, worldId));
    }

    private UUID resolveCampaign(UUID worldId, UUID campaignId) {
        if (campaignId != null && rulesLoader.campaignBelongsToWorld(campaignId, worldId)) return campaignId;
        return campaignRepo.findByWorldId(worldId).stream().findFirst().map(c -> c.getId()).orElse(null);
    }

    private GameEntity loadActor(UUID actorId, UUID userId, UUID worldId) {
        var actor = entityAccess.requireControl(actorId, userId);
        if (!actor.getWorldId().equals(worldId)) {
            throw new PoiException("WORLD_ACCESS_DENIED", "Actor is not in this world");
        }
        return actor;
    }

    private boolean isDm(UUID worldId, UUID campaignId, UUID userId) {
        try {
            worldAccess.requireDm(worldId, userId);
            return true;
        } catch (WorldAccess.WorldAccessException e) {
            // kein Welt-DM — Kampagnen-Leiter prüfen
        }
        if (campaignId == null) return false;
        var campaign = campaignRepo.findById(campaignId).orElse(null);
        if (campaign == null || !worldId.equals(campaign.getWorldId())) return false;
        return userId.equals(campaign.getCreatorId()) || campaignMemberService.isDm(campaignId, userId);
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> catalog(Map<String, Object> rules) {
        if (!(rules.get("poi_actions") instanceof List<?> list)) return List.of();
        return list.stream().filter(Map.class::isInstance)
            .map(m -> (Map<String, Object>) m).toList();
    }

    private Map<String, Object> findAction(Map<String, Object> rules, String name) {
        if (name == null) return null;
        return catalog(rules).stream()
            .filter(a -> RuleNames.eq(text(a.get("name")), name))
            .findFirst().orElse(null);
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> effects(Map<String, Object> action) {
        return effectList(action.get("effects"));
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> effectList(Object raw) {
        if (!(raw instanceof List<?> list)) return List.of();
        return list.stream().filter(Map.class::isInstance)
            .map(m -> (Map<String, Object>) m).toList();
    }

    private boolean hasEffects(Map<String, Object> action) {
        return !effects(action).isEmpty();
    }

    /** Negative Geld-/Item-Posten als Anzeige-Kosten. */
    private List<Map<String, Object>> costs(Map<String, Object> action, Map<String, Object> rules) {
        var out = new ArrayList<Map<String, Object>>();
        for (var effect : effects(action)) {
            var type = text(effect.get("type"));
            if ("money".equals(type) && intAmount(effect) < 0) {
                out.add(Map.of("type", "money", "amount", intAmount(effect),
                    "text", currencyService.format(-intAmount(effect), rules)));
            } else if ("item".equals(type) && itemQty(effect) < 0) {
                out.add(Map.of("type", "item", "name", String.valueOf(effect.get("name")),
                    "qty", -itemQty(effect)));
            }
        }
        return out;
    }

    private void validateCondition(String name, Map<String, Object> rules) {
        if (!(rules.get("conditions") instanceof List<?> catalog) || catalog.isEmpty()) return;
        boolean known = catalog.stream().anyMatch(c -> c instanceof Map<?, ?> m
            && m.get("name") instanceof String n && RuleNames.eq(name, n));
        if (!known) throw new PoiException("UNKNOWN_CONDITION", "Unknown condition: " + name);
    }

    /** M-3: konfigurierter, aber unbekannter Probe-Skill ist ein Fehler (fail-closed). */
    private void requireKnownSkill(String skill, Map<String, Object> rules) {
        if (!(rules.get("skills") instanceof List<?> catalog) || catalog.isEmpty()) return;
        boolean known = catalog.stream().anyMatch(s -> s instanceof Map<?, ?> m
            && m.get("name") instanceof String n && RuleNames.eq(skill, n));
        if (!known) throw new PoiException("ROLL_SKILL_NOT_FOUND", "Unknown skill: " + skill);
    }

    private static int intAmount(Map<String, Object> effect) {
        var amount = effect.get("amount");
        if (amount instanceof Number n) return n.intValue();
        return 0;
    }

    private static int itemQty(Map<String, Object> effect) {
        var qty = effect.get("qty");
        if (qty instanceof Number n) return n.intValue();
        return 0;
    }

    private static String text(Object value) {
        if (value == null) return null;
        var s = String.valueOf(value);
        return s.isBlank() ? null : s;
    }
}
