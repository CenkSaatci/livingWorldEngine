package com.lwe.core.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lwe.core.domain.*;
import com.lwe.core.repository.*;
import org.springframework.stereotype.Service;
import static com.lwe.core.service.WorldEventService.EventType.*;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class AdventureService {

    private final AdventureRepository adventureRepo;
    private final AdventureNodeRepository nodeRepo;
    private final NodeChoiceRepository choiceRepo;
    private final AdventureProgressRepository progressRepo;
    private final WorldRepository worldRepo;
    private final RollService rollService;
    private final WorldEventService eventService;
    private final com.lwe.core.util.WorldAccess worldAccess;
    private final ObjectMapper objectMapper;

    public AdventureService(AdventureRepository adventureRepo, AdventureNodeRepository nodeRepo,
                            NodeChoiceRepository choiceRepo, AdventureProgressRepository progressRepo,
                            WorldRepository worldRepo, RollService rollService,
                            WorldEventService eventService, com.lwe.core.util.WorldAccess worldAccess,
                        ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.adventureRepo = adventureRepo;
        this.nodeRepo = nodeRepo;
        this.choiceRepo = choiceRepo;
        this.progressRepo = progressRepo;
        this.worldRepo = worldRepo;
        this.rollService = rollService;
        this.eventService = eventService;
        this.worldAccess = worldAccess;
    }

    @Transactional
    public Adventure createAdventure(UUID worldId, UUID userId, String name, String description,
                                      UUID locationId, UUID giverEntityId) {
        verifyWorldAccess(worldId, userId);
        var adv = new Adventure(worldId, name);
        if (description != null) adv.setDescription(description);
        if (locationId != null) adv.setLocationId(locationId);
        if (giverEntityId != null) adv.setGiverEntityId(giverEntityId);
        return adventureRepo.save(adv);
    }

    public List<Adventure> listByWorld(UUID worldId, UUID userId) {
        worldAccess.requireRead(worldId, userId); // P1-Audit
        return adventureRepo.findByWorldId(worldId);
    }

    public List<Adventure> listByLocation(UUID locationId) {
        return adventureRepo.findByLocationId(locationId);
    }

    public List<Adventure> listByGiver(UUID giverEntityId) {
        return adventureRepo.findByGiverEntityId(giverEntityId);
    }

    @Transactional
    public AdventureNode addNode(UUID adventureId, UUID userId, String text, String imageUrl, boolean isEnd) {
        verifyAdventureAccess(adventureId, userId);
        var node = new AdventureNode(adventureId, text, isEnd);
        if (imageUrl != null) node.setImageUrl(imageUrl);
        return nodeRepo.save(node);
    }

    @Transactional
    public AdventureNode updateNode(UUID adventureId, UUID nodeId, UUID userId,
                                    String text, String imageUrl, boolean isEnd) {
        verifyAdventureAccess(adventureId, userId);
        var node = nodeRepo.findById(nodeId)
            .orElseThrow(() -> new AdventureException("NODE_NOT_FOUND", "Node not found"));
        if (!node.getAdventureId().equals(adventureId))
            throw new AdventureException("NODE_NOT_IN_ADVENTURE", "Node does not belong to this adventure");
        if (text != null) node.setText(text);
        if (imageUrl != null) node.setImageUrl(imageUrl);
        node.setEnd(isEnd);
        return nodeRepo.save(node);
    }

    @Transactional
    public void setStartNode(UUID adventureId, UUID userId, UUID nodeId) {
        var adv = verifyAdventureAccess(adventureId, userId);
        adv.setStartNodeId(nodeId);
        adventureRepo.save(adv);
    }

    @Transactional
    public NodeChoice addChoice(UUID adventureId, UUID nodeId, UUID userId, String label,
                                UUID targetNodeId, String skillCheckJson, UUID onSuccess, UUID onFailure) {
        verifyAdventureAccess(adventureId, userId); // F2: Zugriff + Node-Zugehoerigkeit
        verifyNodeInAdventure(adventureId, nodeId);
        // Skill-Check-JSON validieren, falls vorhanden
        if (skillCheckJson != null && !skillCheckJson.isBlank()) {
            try {
                var tree = objectMapper.readTree(skillCheckJson);
                if (!tree.has("skill") || !tree.has("target")) {
                    throw new AdventureException("ADVENTURE_NODE_NOT_FOUND",
                        "skill_check must contain 'skill' and 'target' fields");
                }
            } catch (Exception e) {
                throw new AdventureException("ADVENTURE_NODE_NOT_FOUND",
                    "Invalid skill_check JSON: " + e.getMessage());
            }
        }
        var choice = new NodeChoice(nodeId, label, targetNodeId);
        if (skillCheckJson != null) choice.setSkillCheck(skillCheckJson);
        if (onSuccess != null) choice.setOnSuccessNodeId(onSuccess);
        if (onFailure != null) choice.setOnFailureNodeId(onFailure);
        return choiceRepo.save(choice);
    }

    @Transactional
    public AdventureProgress start(UUID adventureId, UUID entityId, UUID userId) {
        var adv = adventureRepo.findById(adventureId)
            .orElseThrow(() -> new AdventureException("ADVENTURE_NOT_FOUND", "Adventure not found"));
        verifyWorldAccess(adv.getWorldId(), userId);

        if (adv.getStartNodeId() == null)
            throw new AdventureException("ADVENTURE_NODE_NOT_FOUND", "Adventure has no start node");

        var existing = progressRepo.findByAdventureIdAndEntityId(adventureId, entityId);
        if (existing.isPresent()) {
            if ("COMPLETED".equals(existing.get().getStatus()))
                throw new AdventureException("ADVENTURE_ALREADY_COMPLETED", "Adventure already completed");
            return existing.get(); // Resume
        }

        var progress = new AdventureProgress(adventureId, entityId, adv.getStartNodeId());
        progress = progressRepo.save(progress);

        eventService.publish(adv.getWorldId(), ADVENTURE_STARTED, entityId, null,
            Map.of("adventureId", adventureId, "startNodeId", adv.getStartNodeId()));

        return progress;
    }

    @Transactional
    public AdvanceResult advance(UUID adventureId, UUID entityId, UUID choiceId, UUID userId) {
        verifyAdventureAccess(adventureId, userId); // N1-Audit
        var progress = progressRepo.findByAdventureIdAndEntityId(adventureId, entityId)
            .orElseThrow(() -> new AdventureException("ADVENTURE_PROGRESS_NOT_FOUND",
                "Character has not started this adventure"));

        if (!"ACTIVE".equals(progress.getStatus()))
            throw new AdventureException("ADVENTURE_ALREADY_COMPLETED", "Adventure is already completed");

        var choice = choiceRepo.findById(choiceId)
            .orElseThrow(() -> new AdventureException("ADVENTURE_CHOICE_NOT_FOUND",
                "Choice not found"));

        // Determine next node
        UUID nextNodeId;
        boolean skillCheckSuccess = false;

        if (choice.getSkillCheck() != null && !choice.getSkillCheck().isBlank()) {
            // Evaluate skill check via Rule-Engine
            try {
                var tree = objectMapper.readTree(choice.getSkillCheck());
                var skill = tree.path("skill").asText("staerke");
                var modifier = tree.path("modifier").asInt(0);
                var target = tree.path("target").asInt(10);

                // Use RollService to evaluate
                var rollResult = rollService.executeRoll(userId,
                    adventureRepo.findById(adventureId).orElseThrow().getWorldId(),
                    entityId, skill, modifier, target);

                skillCheckSuccess = rollResult != null && rollResult.success();
            } catch (Exception e) {
                skillCheckSuccess = false;
            }

            nextNodeId = skillCheckSuccess
                ? (choice.getOnSuccessNodeId() != null ? choice.getOnSuccessNodeId() : choice.getTargetNodeId())
                : (choice.getOnFailureNodeId() != null ? choice.getOnFailureNodeId() : choice.getTargetNodeId());
        } else {
            nextNodeId = choice.getTargetNodeId();
        }

        if (nextNodeId == null)
            throw new AdventureException("ADVENTURE_NODE_NOT_FOUND", "No valid next node");

        // Update progress
        progress.setCurrentNodeId(nextNodeId);
        progress.addVisitedNode(nextNodeId);

        var nextNode = nodeRepo.findById(nextNodeId)
            .orElseThrow(() -> new AdventureException("ADVENTURE_NODE_NOT_FOUND", "Next node not found"));

        if (nextNode.isEnd()) {
            progress.setStatus("COMPLETED");
        }

        progressRepo.save(progress);

        var adv = adventureRepo.findById(adventureId).orElse(null);
        eventService.publish(adv != null ? adv.getWorldId() : null, ADVENTURE_ADVANCED,
            entityId, null, Map.of(
                "adventureId", adventureId,
                "choiceId", choiceId,
                "nextNodeId", nextNodeId,
                "skillCheckSuccess", skillCheckSuccess));

        return new AdvanceResult(nextNode, nextNode.isEnd(), skillCheckSuccess);
    }

    public AdventureProgress getProgress(UUID adventureId, UUID entityId, UUID userId) {
        verifyAdventureAccess(adventureId, userId);
        return progressRepo.findByAdventureIdAndEntityId(adventureId, entityId)
            .orElseThrow(() -> new AdventureException("ADVENTURE_PROGRESS_NOT_FOUND",
                "Character has not started this adventure"));
    }

    @Transactional
    public void abandon(UUID adventureId, UUID entityId, UUID userId) {
        var progress = getProgress(adventureId, entityId, userId);
        progress.setStatus("ABANDONED");
        progressRepo.save(progress);
    }

    public java.util.List<AdventureNode> getNodes(UUID adventureId, UUID userId) {
        verifyAdventureAccess(adventureId, userId);
        return nodeRepo.findByAdventureId(adventureId);
    }

    // -- Override API (Live DM) --

    @Transactional
    public void overrideNodeText(UUID adventureId, UUID userId, String newText) {
        var adv = verifyAdventureAccess(adventureId, userId);
        if (newText == null || newText.isBlank()) return;
        eventService.publish(adv.getWorldId(), ADVENTURE_NODE_CHANGED, null, null, Map.of(
            "adventureId", adventureId.toString(), "text", newText));
    }

    @Transactional
    public void forceNode(UUID adventureId, UUID userId, UUID nodeId) {
        verifyAdventureAccess(adventureId, userId);
        var node = verifyNodeInAdventure(adventureId, nodeId); // F2
        var progresses = progressRepo.findByAdventureIdAndStatus(adventureId, "ACTIVE");
        for (var p : progresses) {
            p.setCurrentNodeId(nodeId);
            progressRepo.save(p);
        }
        // Audit T33-09: Event gehoert zur WELT (worldId), nicht zur Adventure-ID.
        adventureRepo.findById(adventureId).ifPresent(adv ->
            eventService.publish(adv.getWorldId(), ADVENTURE_NODE_CHANGED, null, null, Map.of(
                "adventureId", adventureId.toString(), "nodeId", nodeId.toString())));
    }

    @Transactional
    public NodeChoice injectChoice(UUID adventureId, UUID userId, UUID nodeId, String label,
                                    UUID targetNodeId, String skillCheckJson) {
        verifyAdventureAccess(adventureId, userId);
        verifyNodeInAdventure(adventureId, nodeId); // F2
        var choice = new NodeChoice(nodeId, label, targetNodeId);
        if (skillCheckJson != null) choice.setSkillCheck(skillCheckJson);
        choice = choiceRepo.save(choice);
        adventureRepo.findById(adventureId).ifPresent(adv ->
            eventService.publish(adv.getWorldId(), ADVENTURE_CHOICES_CHANGED, null, null, Map.of(
                "adventureId", adventureId.toString(), "nodeId", nodeId.toString())));
        return choice;
    }

    public Adventure getById(UUID id) {
        return adventureRepo.findById(id)
            .orElseThrow(() -> new AdventureException("ADVENTURE_NOT_FOUND", "Adventure not found"));
    }

    /** P1-Audit: Lesezugriff auf ein Adventure pruefen (PUBLIC-Welten inklusive). */
    public Adventure getById(UUID id, UUID userId) {
        var adv = getById(id);
        worldAccess.requireRead(adv.getWorldId(), userId);
        return adv;
    }

    public java.util.List<NodeChoice> getChoices(UUID nodeId, UUID userId) {
        var node = getNodeById(nodeId, userId); // inkl. Zugriff
        return choiceRepo.findByNodeId(node.getId());
    }

    public AdventureNode getNodeById(UUID nodeId, UUID userId) {
        var node = nodeRepo.findById(nodeId)
            .orElseThrow(() -> new AdventureException("ADVENTURE_NODE_NOT_FOUND", "Node not found"));
        var adv = getById(node.getAdventureId());
        worldAccess.requireRead(adv.getWorldId(), userId); // P1-Audit
        return node;
    }

    private AdventureNode verifyNodeInAdventure(UUID adventureId, UUID nodeId) {
        var node = nodeRepo.findById(nodeId)
            .orElseThrow(() -> new AdventureException("ADVENTURE_NODE_NOT_FOUND", "Node not found"));
        if (!node.getAdventureId().equals(adventureId)) {
            throw new AdventureException("NODE_NOT_IN_ADVENTURE", "Node does not belong to this adventure");
        }
        return node;
    }

    private Adventure verifyAdventureAccess(UUID adventureId, UUID userId) {
        var adv = adventureRepo.findById(adventureId)
            .orElseThrow(() -> new AdventureException("ADVENTURE_NOT_FOUND", "Adventure not found"));
        verifyWorldAccess(adv.getWorldId(), userId);
        return adv;
    }

    private void verifyWorldAccess(UUID worldId, UUID userId) {
        worldAccess.requireAccess(worldId, userId);
    }

    public record AdvanceResult(AdventureNode nextNode, boolean completed, boolean skillCheckSuccess) {}

    public static class AdventureException extends RuntimeException {
        private final String errorCode;
        public AdventureException(String errorCode, String message) { super(message); this.errorCode = errorCode; }
        public String getErrorCode() { return errorCode; }
    }
}