package com.lwe.api;

import com.lwe.api.dto.ErrorResponse;
import com.lwe.api.dto.MapUploadResponse;
import com.lwe.core.domain.User;
import com.lwe.core.repository.WorldMapRepository;
import com.lwe.core.repository.WorldRepository;
import com.lwe.core.util.WorldAccess;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
public class FileUploadController {

    private final WorldMapRepository worldMapRepo;
    private final WorldRepository worldRepo;
    private final WorldAccess worldAccess;
    private final Path uploadDir;

    public FileUploadController(WorldMapRepository worldMapRepo,
                                WorldRepository worldRepo,
                                WorldAccess worldAccess,
                                @Value("${app.upload-dir:uploads}") String uploadDir) {
        this.worldMapRepo = worldMapRepo;
        this.worldRepo = worldRepo;
        this.worldAccess = worldAccess;
        this.uploadDir = Path.of(uploadDir);
    }

    @PostMapping("/worlds/{worldId}/map/upload")
    public ResponseEntity<?> uploadMap(@PathVariable UUID worldId,
                                       @RequestParam("file") MultipartFile file,
                                       @AuthenticationPrincipal User user) {
        worldAccess.requireAccess(worldId, user.getId());

        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(new ErrorResponse("File is empty"));
        }

        try {
            var ext = extractExtension(file.getOriginalFilename());
            var targetDir = uploadDir.resolve(worldId.toString());
            Files.createDirectories(targetDir);

            var filename = "map" + ext;
            var targetPath = targetDir.resolve(filename);
            file.transferTo(targetPath.toFile());

            var map = worldMapRepo.findByWorldId(worldId)
                .orElseGet(() -> {
                    var m = new com.lwe.core.domain.WorldMap(worldId);
                    m.setName("default");
                    return m;
                });

            var url = "/uploads/" + worldId + "/" + filename;
            map.setImageUrl(url);
            worldMapRepo.save(map);

            return ResponseEntity.ok(new MapUploadResponse(url, "Map uploaded successfully"));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(new ErrorResponse(e.getMessage()));
        }
    }

    private String extractExtension(String filename) {
        if (filename == null) return ".png";
        var idx = filename.lastIndexOf('.');
        return idx >= 0 ? filename.substring(idx) : ".png";
    }

    @GetMapping("/uploads/{worldId}/{filename}")
    public ResponseEntity<?> serveFile(@PathVariable UUID worldId,
                                       @PathVariable String filename) {
        try {
            var path = uploadDir.resolve(worldId.toString()).resolve(filename);
            if (!Files.exists(path)) {
                return ResponseEntity.notFound().build();
            }
            var bytes = Files.readAllBytes(path);
            var mediaType = filename.endsWith(".png") ? org.springframework.http.MediaType.IMAGE_PNG
                : filename.endsWith(".jpg") || filename.endsWith(".jpeg")
                    ? org.springframework.http.MediaType.IMAGE_JPEG
                    : org.springframework.http.MediaType.IMAGE_PNG;
            return ResponseEntity.ok().contentType(mediaType).body(bytes);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }
}
