package com.lwe.api;

import com.lwe.core.domain.ChatMessage;
import com.lwe.core.domain.User;
import com.lwe.core.domain.World;
import com.lwe.core.repository.ChatMessageRepository;
import com.lwe.core.repository.WorldRepository;
import com.lwe.core.util.WorldAccess;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ChatControllerTest {

    private final SimpMessagingTemplate messaging = mock();
    private final ChatMessageRepository chatRepo = mock();
    private final WorldRepository worldRepo = mock();
    private final WorldAccess worldAccess = mock();
    private final ChatController controller =
        new ChatController(messaging, chatRepo, worldRepo, worldAccess);

    private final UUID worldId = UUID.randomUUID();
    private final UUID userId = UUID.randomUUID();

    private User user() {
        var u = new User("t@t.com", "t", "hash", "USER", "de");
        try {
            var f = User.class.getDeclaredField("id");
            f.setAccessible(true);
            f.set(u, userId);
        } catch (Exception e) { throw new RuntimeException(e); }
        return u;
    }

    private void stubWorld() {
        var world = new World("W", userId, "{}");
        try {
            var f = World.class.getDeclaredField("id");
            f.setAccessible(true);
            f.set(world, worldId);
        } catch (Exception e) { throw new RuntimeException(e); }
        when(worldRepo.findById(worldId)).thenReturn(Optional.of(world));
    }

    @Test
    void postPersistsAndBroadcasts() {
        stubWorld();
        when(chatRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var res = controller.postChat(worldId.toString(), Map.of("sender", "Lysander", "text", "Hallo"), user());

        assertThat(res.getStatusCode().is2xxSuccessful()).isTrue();
        verify(chatRepo).save(argThat((ChatMessage m) ->
            m.getWorldId().equals(worldId) && m.getText().equals("Hallo")));
        verify(messaging).convertAndSend(eq("/topic/world/" + worldId), org.mockito.ArgumentMatchers.<Object>any());
    }

    @Test
    void historyReturnsChronological() {
        stubWorld();
        var older = new ChatMessage(worldId, "A", "eins");
        var newer = new ChatMessage(worldId, "B", "zwei");
        when(chatRepo.findTop50ByWorldIdOrderByCreatedAtDesc(worldId))
            .thenReturn(List.of(newer, older));

        var res = controller.history(worldId.toString(), user());

        assertThat(res.getBody()).extracting(m -> m.get("text")).containsExactly("eins", "zwei");
    }

    @Test
    void strangerDenied() {
        stubWorld();
        doThrow(new WorldAccess.WorldAccessException("WORLD_ACCESS_DENIED", "denied"))
            .when(worldAccess).requireRead(worldId, userId);

        assertThatThrownBy(() -> controller.history(worldId.toString(), user()))
            .isInstanceOf(WorldAccess.WorldAccessException.class);
        assertThatThrownBy(() -> controller.postChat(worldId.toString(), Map.of("text", "x"), user()))
            .isInstanceOf(WorldAccess.WorldAccessException.class);
    }
}
