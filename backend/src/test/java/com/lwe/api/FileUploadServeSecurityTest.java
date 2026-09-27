package com.lwe.api;

import com.lwe.core.repository.WorldMapRepository;
import com.lwe.core.repository.WorldRepository;
import com.lwe.core.util.WorldAccess;
import static org.assertj.core.api.Assertions.assertThat;
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
 * Serve-Endpunkt für Karten-Assets: Path-Traversal und Fremd-Dateinamen werden
 * abgewehrt (404, kein Leak); gültige Dateien werden <b>ohne Principal</b> ausgeliefert
 * (der Browser-{@code <img>}/PIXI-Loader kann keinen Authorization-Header senden;
 * der Security-Layer gibt GET auf {@code /api/v1/uploads/**} frei).
 */
class FileUploadServeSecurityTest {

    @TempDir
    Path tempDir;

    private FileUploadController controller() {
        return new FileUploadController(
            mock(WorldMapRepository.class), mock(WorldRepository.class),
            mock(WorldAccess.class), tempDir.toString());
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "../../application.yml", "..%2f..%2fsecret", "map.png/../../x",
        "..\\..\\secret", "map.png%00.png", "/etc/passwd", "other.png"
    })
    void shouldRejectTraversalAndForeignFilenames(String filename) {
        var response = controller().serveFile(UUID.randomUUID(), filename);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNull();
    }

    @Test
    void shouldServeValidMapFileWithoutPrincipal() throws Exception {
        var worldId = UUID.randomUUID();
        var dir = tempDir.resolve(worldId.toString());
        Files.createDirectories(dir);
        byte[] png = {(byte) 0x89, 'P', 'N', 'G', 1, 2, 3};
        Files.write(dir.resolve("map.png"), png);

        var response = controller().serveFile(worldId, "map.png");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(png);
    }

    @Test
    void shouldReturn404ForMissingFile() {
        var response = controller().serveFile(UUID.randomUUID(), "map.jpg");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }
}
