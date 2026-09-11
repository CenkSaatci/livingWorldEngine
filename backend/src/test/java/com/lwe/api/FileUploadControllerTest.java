package com.lwe.api;

import com.lwe.core.domain.User;
import com.lwe.core.domain.WorldMap;
import com.lwe.core.repository.WorldMapRepository;
import com.lwe.core.repository.WorldRepository;
import com.lwe.core.util.WorldAccess;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;
import java.util.UUID;

class FileUploadControllerTest {

    @TempDir
    Path tempDir;

    @Test
    void shouldUploadMapImage() throws IOException {
        var worldMapRepo = mock(WorldMapRepository.class);
        var worldRepo = mock(WorldRepository.class);
        var worldAccess = mock(WorldAccess.class);
        var controller = new FileUploadController(worldMapRepo, worldRepo, worldAccess, tempDir.toString());

        var worldId = UUID.randomUUID();
        var user = new User("t@t.com", "t", "hash", "USER", "de");
        setId(user, UUID.randomUUID());

        var map = new WorldMap(worldId);
        map.setImageUrl("/uploads/" + worldId + "/map.png");
        when(worldMapRepo.findByWorldId(worldId)).thenReturn(Optional.of(map));
        when(worldMapRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        byte[] pngBytes = new byte[] {(byte) 0x89, 'P', 'N', 'G', (byte) 0x0D, (byte) 0x0A, (byte) 0x1A, (byte) 0x0A, 0, 0, 0, 0};
        var file = new MockMultipartFile("file", "map.png", MediaType.IMAGE_PNG_VALUE, pngBytes);
        var response = controller.uploadMap(worldId, file, user);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
    }

    private void setId(Object obj, UUID id) {
        try { var f = obj.getClass().getDeclaredField("id"); f.setAccessible(true); f.set(obj, id); }
        catch (Exception e) { throw new RuntimeException(e); }
    }
}
