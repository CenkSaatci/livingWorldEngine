package com.lwe.api;

import com.lwe.api.dto.ErrorResponse;
import com.lwe.api.dto.ModeResponse;
import com.lwe.api.dto.PausedResponse;
import com.lwe.api.dto.TimeGetResponse;
import com.lwe.core.domain.User;
import com.lwe.core.domain.World;
import com.lwe.core.repository.WorldRepository;
import com.lwe.core.service.WorldEventService;
import com.lwe.time.WorldTimeService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static com.lwe.core.service.WorldEventService.EventType.*;

@RestController
@RequestMapping("/api/v1/worlds/{worldId}/time")
public class TimeController {

    private static final Logger log = LoggerFactory.getLogger(TimeController.class);

    private final WorldRepository worldRepo;
    private final WorldTimeService timeService;
    private final WorldEventService eventService;

    public TimeController(WorldRepository worldRepo, WorldTimeService timeService,
                          WorldEventService eventService) {
        this.worldRepo = worldRepo;
        this.timeService = timeService;
        this.eventService = eventService;
    }

    @GetMapping
    public ResponseEntity<TimeGetResponse> getTime(@PathVariable UUID worldId,
                                                    @AuthenticationPrincipal User user) {
        var world = requireOwner(worldId, user.getId());
        var cfg = timeService.readTimeConfig(world);
        return ResponseEntity.ok(new TimeGetResponse(
            world.getCurrentGameTime() != null ? world.getCurrentGameTime().toString() : "",
            cfg != null ? cfg.mode() : "hybrid",
            cfg != null && cfg.paused(),
            timeService.dayPhase(world).name().toLowerCase(),
            timeService.dayPhase(world) == WorldTimeService.DayPhase.DAY
                || timeService.dayPhase(world) == WorldTimeService.DayPhase.DAWN
        ));
    }

    @PostMapping("/advance")
    public ResponseEntity<TimeResponse> advance(@PathVariable UUID worldId,
                                                 @Valid @RequestBody AdvanceRequest req,
                                                 @AuthenticationPrincipal User user) {
        var world = requireOwner(worldId, user.getId());
        var minutes = parseDuration(req.by());
        var result = timeService.advanceTime(world, Duration.ofMinutes(minutes));
        return ResponseEntity.ok(timeResponse(result));
    }

    @PostMapping("/set")
    public ResponseEntity<TimeResponse> set(@PathVariable UUID worldId,
                                             @Valid @RequestBody SetRequest req,
                                             @AuthenticationPrincipal User user) {
        var world = requireOwner(worldId, user.getId());
        var target = Instant.parse(req.to());
        var result = timeService.setTime(world, target);
        return ResponseEntity.ok(timeResponse(result));
    }

    @PostMapping("/pause")
    public ResponseEntity<PausedResponse> pause(@PathVariable UUID worldId,
                                                 @AuthenticationPrincipal User user) {
        var world = updateTimeConfig(worldId, user.getId(), cfg -> cfg.withPaused(true));
        eventService.publish(worldId, TIME_PAUSED, null, null, Map.of("at", Instant.now().toString()));
        return ResponseEntity.ok(new PausedResponse(true));
    }

    @PostMapping("/resume")
    public ResponseEntity<PausedResponse> resume(@PathVariable UUID worldId,
                                                  @AuthenticationPrincipal User user) {
        var world = updateTimeConfig(worldId, user.getId(), cfg -> cfg.withPaused(false));
        eventService.publish(worldId, TIME_RESUMED, null, null, Map.of("at", Instant.now().toString()));
        return ResponseEntity.ok(new PausedResponse(false));
    }

    @PatchMapping("/mode")
    public ResponseEntity<?> setMode(@PathVariable UUID worldId,
                                      @Valid @RequestBody ModeRequest req,
                                      @AuthenticationPrincipal User user) {
        if (!req.mode().matches("^(automatic|manual|hybrid)$")) {
            return ResponseEntity.badRequest().body(new ErrorResponse("TIME_MODE_INVALID"));
        }
        var world = updateTimeConfig(worldId, user.getId(), cfg -> cfg.withMode(req.mode()));
        eventService.publish(worldId, TIME_MODE_CHANGED, null, null, Map.of("mode", req.mode()));
        return ResponseEntity.ok(new ModeResponse(req.mode()));
    }

    // -- Helpers --

    private World requireOwner(UUID worldId, UUID userId) {
        var world = worldRepo.findById(worldId)
            .orElseThrow(() -> new TimeControllerException("TIME_NOT_FOUND", HttpStatus.NOT_FOUND));
        if (!world.getOwnerId().equals(userId))
            throw new TimeControllerException("TIME_ACCESS_DENIED", HttpStatus.FORBIDDEN);
        return world;
    }

    private World updateTimeConfig(UUID worldId, UUID userId,
                                   java.util.function.UnaryOperator<WorldTimeService.TimeConfig> updater) {
        var world = requireOwner(worldId, userId);
        var cfg = timeService.readTimeConfig(world);
        if (cfg == null) throw new TimeControllerException("TIME_CONFIG_MISSING", HttpStatus.NOT_FOUND);
        var newCfg = updater.apply(cfg);
        try {
            var tree = new com.fasterxml.jackson.databind.ObjectMapper().readTree(world.getSettingsJson());
            var timeNode = tree.path("time");
            if (timeNode.isObject()) {
                ((com.fasterxml.jackson.databind.node.ObjectNode) tree).replace("time",
                    new com.fasterxml.jackson.databind.ObjectMapper().valueToTree(Map.of(
                        "mode", newCfg.mode(),
                        "tick_interval_real_seconds", newCfg.tickIntervalRealSeconds(),
                        "tick_advance_game_minutes", newCfg.tickAdvanceGameMinutes(),
                        "paused", newCfg.paused(),
                        "day_starts_at_hour", newCfg.dayStartsAtHour()
                    )));
            }
            world.setSettingsJson(tree.toString());
            worldRepo.save(world);
        } catch (Exception e) {
            log.error("Failed to update time config for world {}", worldId, e);
            throw new TimeControllerException("TIME_CONFIG_INVALID", HttpStatus.INTERNAL_SERVER_ERROR);
        }
        return world;
    }

    private long parseDuration(String by) {
        if (by == null) return 60;
        var input = by.trim().toLowerCase();
        long result;
        switch (input) {
            case "dawn" -> result = 0;
            case "noon" -> result = 360;
            case "dusk" -> result = 720;
            case "midnight" -> result = 1080;
            default -> {
                try {
                    var dur = java.time.Duration.parse(input);
                    return dur.toMinutes();
                } catch (Exception e) {
                    var parts = input.split(" ");
                    if (parts.length == 2) {
                        try {
                            long num = Long.parseLong(parts[0]);
                            result = switch (parts[1]) {
                                case "day", "days" -> num * 24 * 60;
                                case "hour", "hours" -> num * 60;
                                case "minute", "minutes" -> num;
                                default -> 60;
                            };
                        } catch (NumberFormatException nfe) {
                            return 60;
                        }
                    } else {
                        return 60;
                    }
                }
            }
        }
        return result;
    }

    private TimeResponse timeResponse(World w) {
        return new TimeResponse(
            w.getCurrentGameTime() != null ? w.getCurrentGameTime().toString() : "",
            timeService.readTimeConfig(w) != null ? timeService.readTimeConfig(w).mode() : "hybrid");
    }

    public record AdvanceRequest(String by) {}
    public record SetRequest(@NotBlank String to) {}
    public record ModeRequest(@NotBlank String mode) {}
    public record TimeResponse(String currentGameTime, String mode) {}

    public static class TimeControllerException extends RuntimeException {
        private final String errorCode;
        private final HttpStatus status;
        public TimeControllerException(String errorCode, HttpStatus status) {
            super(errorCode);
            this.errorCode = errorCode;
            this.status = status;
        }
        public String getErrorCode() { return errorCode; }
        public HttpStatus getStatus() { return status; }
    }
}
