package com.lwe.core.service;

import com.lwe.core.domain.NpcIntent;
import com.lwe.core.domain.World;
import com.lwe.core.repository.NpcIntentRepository;
import com.lwe.core.repository.WorldRepository;
import com.lwe.rules.IntentExecutor;
import com.lwe.rules.IntentValidator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NpcIntentServiceTest {

    @Mock private NpcIntentRepository repo;
    @Mock private IntentValidator validator;
    @Mock private WorldEventService eventService;
    @Mock private WorldRepository worldRepo;
    @Mock private IntentExecutor executor;
    @Mock private com.lwe.core.util.WorldAccess worldAccess;

    @InjectMocks private NpcIntentService npcIntentService;
    private final UUID userId = UUID.randomUUID();

    private final UUID worldId = UUID.randomUUID();
    private final UUID npcId = UUID.randomUUID();

    @Test
    void shouldApproveAndExecuteWhenAutonom() {
        when(validator.validate(any())).thenReturn(new IntentValidator.ValidationResult(true, null));
        var world = new World("W", UUID.randomUUID(), "{\"ai_mode\":\"autonom\"}");
        when(worldRepo.findById(worldId)).thenReturn(Optional.of(world));
        when(repo.save(any())).thenAnswer(inv -> {
            var intent = inv.<NpcIntent>getArgument(0);
            setId(intent, UUID.randomUUID());
            return intent;
        });
        when(eventService.publish(any(), any(WorldEventService.EventType.class), any(), any(), any())).thenReturn(1L);

        var result = npcIntentService.create(worldId, npcId, "MOVE", "{\"x\":5,\"y\":5}", "moving north");

        assertThat(result.getStatus()).isEqualTo("approved");
        verify(repo).save(any());
        verify(executor).execute(result);
        verify(eventService).publish(eq(worldId), eq(WorldEventService.EventType.NPC_INTENT_PROPOSED), eq(npcId), isNull(), any());
    }

    @Test
    void shouldSetPendingWhenSuggest() {
        when(validator.validate(any())).thenReturn(new IntentValidator.ValidationResult(true, null));
        var world = new World("W", UUID.randomUUID(), "{\"ai_mode\":\"suggest\"}");
        when(worldRepo.findById(worldId)).thenReturn(Optional.of(world));
        when(repo.save(any())).thenAnswer(inv -> {
            var intent = inv.<NpcIntent>getArgument(0);
            setId(intent, UUID.randomUUID());
            return intent;
        });
        when(eventService.publish(any(), any(WorldEventService.EventType.class), any(), any(), any())).thenReturn(1L);

        var result = npcIntentService.create(worldId, npcId, "SPEAK", "{}", "hello");

        assertThat(result.getStatus()).isEqualTo("pending");
        verify(executor, never()).execute(any());
    }

    @Test
    void shouldRejectWhenAiModeOff() {
        when(validator.validate(any())).thenReturn(new IntentValidator.ValidationResult(true, null));
        var world = new World("W", UUID.randomUUID(), "{\"ai_mode\":\"off\"}");
        when(worldRepo.findById(worldId)).thenReturn(Optional.of(world));
        when(repo.save(any())).thenAnswer(inv -> {
            var intent = inv.<NpcIntent>getArgument(0);
            setId(intent, UUID.randomUUID());
            return intent;
        });

        var result = npcIntentService.create(worldId, npcId, "MOVE", "{}", "nope");

        assertThat(result.getStatus()).isEqualTo("rejected");
        assertThat(result.getRejectionReason()).isEqualTo("AI mode is off");
        verify(executor, never()).execute(any());
        verify(eventService, never()).publish(any(), any(), any(), any(), any());
    }

    @Test
    void shouldRejectWhenValidationFails() {
        when(validator.validate(any())).thenReturn(new IntentValidator.ValidationResult(false, "NPC not found"));
        when(repo.save(any())).thenAnswer(inv -> {
            var intent = inv.<NpcIntent>getArgument(0);
            setId(intent, UUID.randomUUID());
            return intent;
        });
        when(eventService.publish(any(), any(WorldEventService.EventType.class), any(), any(), any())).thenReturn(1L);

        var result = npcIntentService.create(worldId, npcId, "ATTACK", "{}", "attack");

        assertThat(result.getStatus()).isEqualTo("rejected");
        assertThat(result.getRejectionReason()).isEqualTo("NPC not found");
        verify(validator).validate(any());
        verify(executor, never()).execute(any());
    }

    @Test
    void shouldApproveIntent() {
        var intent = intentWithId(worldId, npcId, "MOVE", "pending");
        var intentId = intent.getId();

        when(repo.findById(intentId)).thenReturn(Optional.of(intent));
        when(repo.save(any())).thenAnswer(inv -> inv.<NpcIntent>getArgument(0));
        when(eventService.publish(any(), any(WorldEventService.EventType.class), any(), any(), any())).thenReturn(1L);

        var result = npcIntentService.approve(intentId, userId);

        assertThat(result.getStatus()).isEqualTo("approved");
        assertThat(result.getValidatedAt()).isNotNull();
        verify(repo).save(any());
        verify(executor).execute(intent);
        verify(eventService).publish(eq(worldId), eq(WorldEventService.EventType.NPC_INTENT_APPROVED), eq(npcId), isNull(), any());
    }

    @Test
    void shouldRejectIntent() {
        var intent = intentWithId(worldId, npcId, "MOVE", "pending");
        var intentId = intent.getId();

        when(repo.findById(intentId)).thenReturn(Optional.of(intent));
        when(repo.save(any())).thenAnswer(inv -> inv.<NpcIntent>getArgument(0));
        when(eventService.publish(any(), any(WorldEventService.EventType.class), any(), any(), any())).thenReturn(1L);

        var result = npcIntentService.reject(intentId, "not appropriate", userId);

        assertThat(result.getStatus()).isEqualTo("rejected");
        assertThat(result.getRejectionReason()).isEqualTo("not appropriate");
        verify(repo).save(any());
        verify(executor, never()).execute(any());
        verify(eventService).publish(eq(worldId), eq(WorldEventService.EventType.NPC_INTENT_REJECTED), eq(npcId), isNull(), any());
    }

    @Test
    void shouldListPendingIntents() {
        var intent = intentWithId(worldId, npcId, "SPEAK", "pending");
        when(repo.findByWorldIdAndStatusOrderByCreatedAtDesc(worldId, "pending"))
            .thenReturn(List.of(intent));

        var result = npcIntentService.listPending(worldId, userId);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getStatus()).isEqualTo("pending");
    }

    private NpcIntent intentWithId(UUID worldId, UUID npcId, String type, String status) {
        var intent = new NpcIntent(worldId, npcId, type, "{}", null);
        intent.setStatus(status);
        setId(intent, UUID.randomUUID());
        return intent;
    }

    private void setId(NpcIntent intent, UUID id) {
        try {
            var field = NpcIntent.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(intent, id);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
