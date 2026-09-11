package com.lwe.api;

import com.lwe.core.domain.User;
import com.lwe.core.repository.WorldMapRepository;
import com.lwe.core.repository.WorldRepository;
import com.lwe.core.util.WorldAccess;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpStatus;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

/**
 * TDD: Serve-Endpunkt muss Path-Traversal abwehren (404, kein Leak),
 * gültige Dateien ausliefern (200) und ohne Principal 401 liefern.
 */
class FileUploadServeSecurityTest {

    @TempDir
    Path tempDir;

    private FileUploadController controller(WorldAccess worldAccess) {
        return new FileUploadController(
            mock(WorldMapRepository.class), mock(WorldRepository.class),
            worldAccess, tempDir.toString());
    }

    private User user() {
        var u = new User("t@t.com", "t", "hash", "USER", "de");
        try { var f = User.class.getDeclaredField("id"); f.setAccessible(true); f.set(u, UUID.randomUUID()); }
        catch (Exception e) { throw new RuntimeException(e); }
        return u;
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "../../application.yml", "..%2f..%2fsecret", "map.png/../../x",
        "..\\..\\secret", "map.png%00.png", "/etc/passwd", "other.png"
    })
    void shouldRejectTraversalAndForeignFilenames(String filename) {
        var worldAccess = mock(WorldAccess.class);
        var worldId = UUID.randomUUID();

        var response = controller(worldAccess).serveFile(worldId, filename, user());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNull();
        verify(worldAccess, never()).requireAccess(any(), any());
    }

    @Test
    void shouldServeValidMapFile() throws Exception {
        var worldAccess = mock(WorldAccess.class);
        var worldId = UUID.randomUUID();
        var dir = tempDir.resolve(worldId.toString());
        Files.createDirectories(dir);
        byte[] png = {(byte) 0x89, 'P', 'N', 'G', 1, 2, 3};
        Files.write(dir.resolve("map.png"), png);
        var u = user();

        var response = controller(worldAccess).serveFile(worldId, "map.png", u);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(png);
        verify(worldAccess).requireAccess(worldId, u.getId());
    }

    @Test
    void shouldRequireAuthentication() {
        var worldAccess = mock(WorldAccess.class);

        var response = controller(worldAccess).serveFile(UUID.randomUUID(), "map.png", null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }
}
