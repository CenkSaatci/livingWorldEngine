package com.lwe.core.service;

import com.lwe.core.domain.Quest;
import com.lwe.core.repository.QuestRepository;
import com.lwe.core.repository.WorldRepository;
import com.lwe.core.util.WorldAccess;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class QuestService {

    private final QuestRepository repo;
    private final WorldAccess worldAccess;
    private final EntityEventService eventService;

    public QuestService(QuestRepository repo, WorldAccess worldAccess,
                        EntityEventService eventService) {
        this.repo = repo;
        this.worldAccess = worldAccess;
        this.eventService = eventService;
    }

    private static final java.util.Set<String> TYPES =
        java.util.Set.of("kill", "fetch", "escort", "deliver", "explore", "talk");

    @Transactional
    public Quest create(UUID worldId, UUID userId, String title, String description,
                        String type, UUID giverId, UUID locationId,
                        String objectives, String rewards, boolean aiGenerated) {
        requireDm(worldId, userId);
        if (type == null || !TYPES.contains(type)) {
            throw new QuestException("QUEST_TYPE_INVALID",
                "type must be one of " + TYPES);
        }

        var quest = new Quest(worldId, title, type,
            objectives == null || objectives.isBlank() ? "[]" : objectives,
            rewards == null || rewards.isBlank() ? "{}" : rewards);
        if (description != null) quest.setDescription(description);
        quest.setGiverId(giverId);
        quest.setLocationId(locationId);
        quest.setAiGenerated(aiGenerated);

        quest = repo.save(quest);
        eventService.publish("quest", quest.getId(), "QUEST_CREATED",
            "Quest \"" + title + "\" erstellt", null, 3, userId);
        return quest;
    }

    public List<Quest> list(UUID worldId, UUID userId, String status) {
        worldAccess.requireRead(worldId, userId); // T33-02
        if (status != null) {
            return repo.findByWorldIdAndStatusOrderByCreatedAtDesc(worldId, status);
        }
        return repo.findByWorldIdOrderByCreatedAtDesc(worldId);
    }

    public Quest getById(UUID questId, UUID userId) {
        var quest = repo.findById(questId)
            .orElseThrow(() -> new QuestException("QUEST_NOT_FOUND", "Quest not found"));
        worldAccess.requireRead(quest.getWorldId(), userId); // T33-02
        return quest;
    }

    /** R2: erlaubte Statuswerte (Runde 2). */
    private static final java.util.Set<String> STATUSES =
        java.util.Set.of("pending", "active", "completed", "cancelled");

    @Transactional
    public Quest updateStatus(UUID questId, UUID userId, String status) {
        var quest = getById(questId, userId);
        requireOwner(quest.getWorldId(), userId); // F1: Write-Guard
        var normalized = status == null ? null : status.trim().toLowerCase();
        if (normalized == null || !STATUSES.contains(normalized)) {
            throw new QuestException("QUEST_STATUS_INVALID",
                "status must be one of " + STATUSES);
        }
        quest.setStatus(normalized);
        quest = repo.save(quest);

        eventService.publish("quest", quest.getId(), "QUEST_" + normalized.toUpperCase(),
            "Quest \"" + quest.getTitle() + "\": " + normalized, null, 2, userId);
        return quest;
    }

    @Transactional
    public void delete(UUID questId, UUID userId) {
        var quest = getById(questId, userId);
        requireDm(quest.getWorldId(), userId); // Playtest: Authoring ist DM-only
        repo.delete(quest);
    }

    /** Playtest: Quest-Authoring (create/delete) ist DM-only;
     *  Statuswechsel bleiben member-level (Quest spielen). */
    private void requireDm(UUID worldId, UUID userId) {
        worldAccess.requireDm(worldId, userId);
    }

    private void requireOwner(UUID worldId, UUID userId) {
        worldAccess.requireAccess(worldId, userId);
    }

    public static class QuestException extends RuntimeException {
        private final String errorCode;
        public QuestException(String errorCode, String message) { super(message); this.errorCode = errorCode; }
        public String getErrorCode() { return errorCode; }
    }
}
