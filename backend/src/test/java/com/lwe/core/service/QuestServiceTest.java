package com.lwe.core.service;

import com.lwe.core.domain.Quest;
import com.lwe.core.repository.QuestRepository;
import com.lwe.core.util.WorldAccess;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class QuestServiceTest {

    @Mock private QuestRepository repo;
    @Mock private WorldAccess worldAccess;
    @Mock private EntityEventService eventService;

    private QuestService service;
    private final UUID worldId = UUID.randomUUID();
    private final UUID userId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new QuestService(repo, worldAccess, eventService);
    }

    @Test
    void shouldCreateQuest() {
        lenient().doNothing().when(worldAccess).requireAccess(worldId, userId);
        when(repo.save(any())).thenAnswer(inv -> {
            var q = inv.<Quest>getArgument(0);
            setId(q, UUID.randomUUID());
            return q;
        });
        when(eventService.publish(any(), any(), any(), any(), any(), anyInt(), any())).thenReturn(null);

        var quest = service.create(worldId, userId, "Find the Gem", "Retrieve the ancient gem from the dragon",
            "fetch", null, null, "[{\"task\":\"defeat_dragon\"}]", "{\"xp\":500}", false);

        assertThat(quest.getTitle()).isEqualTo("Find the Gem");
        assertThat(quest.getType()).isEqualTo("fetch");
        assertThat(quest.getObjectives()).isEqualTo("[{\"task\":\"defeat_dragon\"}]");
        verify(repo).save(any());
        verify(eventService).publish(any(), any(), any(), any(), any(), anyInt(), any());
    }

    @Test
    void createDefaultsObjectivesAndRewards() {
        lenient().doNothing().when(worldAccess).requireAccess(worldId, userId);
        when(repo.save(any())).thenAnswer(inv -> {
            var q = inv.<Quest>getArgument(0);
            setId(q, UUID.randomUUID());
            return q;
        });
        when(eventService.publish(any(), any(), any(), any(), any(), anyInt(), any())).thenReturn(null);

        var quest = service.create(worldId, userId, "Titel", null,
            "fetch", null, null, null, null, false);

        assertThat(quest.getObjectives()).isEqualTo("[]");
        assertThat(quest.getRewards()).isEqualTo("{}");
    }

    @Test
    void questAuthoringRequiresDm() {
        doThrow(new WorldAccess.WorldAccessException("WORLD_ACCESS_DENIED", "denied"))
            .when(worldAccess).requireDm(worldId, userId);

        assertThatThrownBy(() -> service.create(worldId, userId, "T", null, "fetch",
                null, null, null, null, false))
            .isInstanceOf(WorldAccess.WorldAccessException.class);
        var quest = new Quest(worldId, "Q", "fetch", "[]", "{}");
        setId(quest, UUID.randomUUID());
        when(repo.findById(quest.getId())).thenReturn(Optional.of(quest));
        assertThatThrownBy(() -> service.delete(quest.getId(), userId))
            .isInstanceOf(WorldAccess.WorldAccessException.class);
    }

    @Test
    void createRejectsUnknownType() {
        assertThatThrownBy(() -> service.create(worldId, userId, "T", null, "SIDE",
                null, null, "[]", "{}", false))
            .isInstanceOf(QuestService.QuestException.class)
            .matches(e -> ((QuestService.QuestException) e).getErrorCode().equals("QUEST_TYPE_INVALID"));
    }

    @Test
    void shouldListQuestsByWorld() {
        lenient().doNothing().when(worldAccess).requireAccess(worldId, userId);
        when(repo.findByWorldIdOrderByCreatedAtDesc(worldId)).thenReturn(List.of());

        var list = service.list(worldId, userId, null);

        assertThat(list).isEmpty();
        verify(repo).findByWorldIdOrderByCreatedAtDesc(worldId);
    }

    @Test
    void shouldFilterQuestsByStatus() {
        lenient().doNothing().when(worldAccess).requireAccess(worldId, userId);
        when(repo.findByWorldIdAndStatusOrderByCreatedAtDesc(worldId, "ACTIVE")).thenReturn(List.of());

        var list = service.list(worldId, userId, "ACTIVE");

        assertThat(list).isEmpty();
        verify(repo).findByWorldIdAndStatusOrderByCreatedAtDesc(worldId, "ACTIVE");
    }

    @Test
    void shouldGetQuestById() {
        var quest = new Quest(worldId, "Test", "SIDE", "[]", "{}");
        setId(quest, UUID.randomUUID());
        when(repo.findById(quest.getId())).thenReturn(Optional.of(quest));
        lenient().doNothing().when(worldAccess).requireAccess(worldId, userId);

        var result = service.getById(quest.getId(), userId);

        assertThat(result.getTitle()).isEqualTo("Test");
    }

    @Test
    void shouldUpdateQuestStatus() {
        var quest = new Quest(worldId, "Test", "SIDE", "[]", "{}");
        setId(quest, UUID.randomUUID());
        when(repo.findById(quest.getId())).thenReturn(Optional.of(quest));
        lenient().doNothing().when(worldAccess).requireAccess(worldId, userId);
        when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(eventService.publish(any(), any(), any(), any(), any(), anyInt(), any())).thenReturn(null);

        var result = service.updateStatus(quest.getId(), userId, "COMPLETED");

        assertThat(result.getStatus()).isEqualTo("COMPLETED");
        verify(eventService).publish(eq("quest"), eq(quest.getId()), eq("QUEST_COMPLETED"), any(), any(), anyInt(), any());
    }

    @Test
    void shouldDeleteQuest() {
        var quest = new Quest(worldId, "Test", "SIDE", "[]", "{}");
        setId(quest, UUID.randomUUID());
        when(repo.findById(quest.getId())).thenReturn(Optional.of(quest));
        lenient().doNothing().when(worldAccess).requireAccess(worldId, userId);

        service.delete(quest.getId(), userId);

        verify(repo).delete(quest);
    }

    @Test
    void shouldThrowOnNonExistentQuest() {
        when(repo.findById(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getById(UUID.randomUUID(), userId))
            .isInstanceOf(QuestService.QuestException.class)
            .matches(e -> ((QuestService.QuestException) e).getErrorCode().equals("QUEST_NOT_FOUND"));
    }

    private void setId(Object obj, UUID id) {
        try { var f = obj.getClass().getDeclaredField("id"); f.setAccessible(true); f.set(obj, id); }
        catch (Exception e) { throw new RuntimeException(e); }
    }
}
