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
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

@RestController
@RequestMapping("/api/v1")
public class FileUploadController {

    private final WorldMapRepository worldMapRepo;
    private final WorldRepository worldRepo;
    private final WorldAccess worldAccess;
    private final Path uploadDir;

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(".png", ".jpg", ".jpeg", ".webp");
    private static final Pattern SERVE_FILENAME = Pattern.compile("^map\\.(png|jpg|jpeg|webp)$");
    private static final Map<String, MediaType> MEDIA_TYPES = Map.of(
        ".png", MediaType.IMAGE_PNG,
        ".jpg", MediaType.IMAGE_JPEG,
        ".jpeg", MediaType.IMAGE_JPEG,
        ".webp", MediaType.valueOf("image/webp")
    );
    private static final long MAX_BYTES = 10 * 1024 * 1024;

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
        if (file.getSize() > MAX_BYTES) {
            return ResponseEntity.badRequest().body(new ErrorResponse("File too large (max 10MB)"));
        }

        try {
            var ext = extractExtension(file.getOriginalFilename()).toLowerCase(java.util.Locale.ROOT);
            if (!ALLOWED_EXTENSIONS.contains(ext)) {
                return ResponseEntity.badRequest().body(new ErrorResponse("Only PNG, JPG or WEBP images allowed"));
            }
            var contentType = file.getContentType();
            if (contentType == null || !contentType.toLowerCase(java.util.Locale.ROOT).startsWith("image/")) {
                return ResponseEntity.badRequest().body(new ErrorResponse("Only image uploads allowed"));
            }
            var bytes = file.getBytes();
            if (!hasImageMagicBytes(bytes, ext)) {
                return ResponseEntity.badRequest().body(new ErrorResponse("File content is not a valid image"));
            }
            var targetDir = uploadDir.resolve(worldId.toString());
            Files.createDirectories(targetDir);

            var filename = "map" + ext;
            var targetPath = targetDir.resolve(filename);
            Files.write(targetPath, bytes);

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

    /** Prüft Magic Bytes statt nur Endung/Content-Type (Polyglot-/Rename-Angriffe). */
    private boolean hasImageMagicBytes(byte[] bytes, String ext) {
        if (bytes == null || bytes.length < 4) return false;
        return switch (ext) {
            case ".png" -> bytes.length >= 8
                && (bytes[0] & 0xFF) == 0x89 && bytes[1] == 0x50 && bytes[2] == 0x4E && bytes[3] == 0x47
                && bytes[4] == 0x0D && bytes[5] == 0x0A && bytes[6] == 0x1A && bytes[7] == 0x0A;
            case ".jpg", ".jpeg" -> (bytes[0] & 0xFF) == 0xFF && (bytes[1] & 0xFF) == 0xD8
                && (bytes[2] & 0xFF) == 0xFF;
            case ".webp" -> bytes.length >= 12
                && bytes[0] == 0x52 && bytes[1] == 0x49 && bytes[2] == 0x46 && bytes[3] == 0x46
                && bytes[8] == 0x57 && bytes[9] == 0x45 && bytes[10] == 0x42 && bytes[11] == 0x50;
            default -> false;
        };
    }

    @GetMapping("/uploads/{worldId}/{filename}")
    public ResponseEntity<?> serveFile(@PathVariable UUID worldId,
                                       @PathVariable String filename,
                                       @AuthenticationPrincipal User user) {
        if (user == null) {
            return ResponseEntity.status(401).body(new ErrorResponse("Authentication required"));
        }
        if (!SERVE_FILENAME.matcher(filename).matches()) {
            return ResponseEntity.notFound().build();
        }
        try {
            worldAccess.requireAccess(worldId, user.getId());
            var base = uploadDir.resolve(worldId.toString()).normalize();
            var path = base.resolve(filename).normalize();
            // Path-Traversal: aufgelöster Pfad muss unterhalb von uploadDir/worldId bleiben.
            if (!path.startsWith(base)) {
                return ResponseEntity.notFound().build();
            }
            if (!Files.isRegularFile(path)) {
                return ResponseEntity.notFound().build();
            }
            var bytes = Files.readAllBytes(path);
            var ext = filename.substring(filename.lastIndexOf('.')).toLowerCase(java.util.Locale.ROOT);
            return ResponseEntity.ok().contentType(MEDIA_TYPES.get(ext)).body(bytes);
        } catch (com.lwe.core.util.WorldAccess.WorldAccessException e) {
            if ("WORLD_NOT_FOUND".equals(e.getErrorCode())) return ResponseEntity.notFound().build();
            return ResponseEntity.status(403).body(new ErrorResponse(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }
}
