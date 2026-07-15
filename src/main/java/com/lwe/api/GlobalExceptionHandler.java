package com.lwe.api;

import com.lwe.core.service.AdventureService;
import com.lwe.core.service.AuthService;
import com.lwe.core.service.EntityEventService;
import com.lwe.core.service.CombatService;
import com.lwe.core.service.EntityService;
import com.lwe.core.service.GameSystemService;
import com.lwe.core.service.InventoryService;
import com.lwe.core.service.LocationService;
import com.lwe.core.service.QuestService;
import com.lwe.core.service.RegionService;
import com.lwe.core.service.NpcIntentService;
import com.lwe.core.service.WorldService;
import com.lwe.core.service.FactionService;
import com.lwe.core.service.GameSessionService;
import com.lwe.core.service.QuotaService;
import com.lwe.core.util.WorldAccess;
import com.lwe.rules.RuleSchemaValidator;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

/**
 * Globaler Exception-Handler — gibt einheitliches Fehler-JSON zurück.
 *
 * @see <a href="../../../docs/ERROR-CODES.md">docs/ERROR-CODES.md</a>
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(AuthService.AuthException.class)
    public ResponseEntity<?> handleAuthException(AuthService.AuthException ex) {
        var status = switch (ex.getErrorCode()) {
            case "AUTH_EMAIL_TAKEN", "AUTH_USERNAME_TAKEN" -> HttpStatus.CONFLICT;
            case "AUTH_INVALID_CREDENTIALS" -> HttpStatus.UNAUTHORIZED;
            case "AUTH_RATE_LIMITED" -> HttpStatus.TOO_MANY_REQUESTS;
            case "AUTH_REFRESH_EXPIRED" -> HttpStatus.UNAUTHORIZED;
            default -> HttpStatus.BAD_REQUEST;
        };
        return ResponseEntity.status(status).body(errorBody(ex.getErrorCode(), ex.getMessage()));
    }

    @ExceptionHandler(CombatService.CombatException.class)
    public ResponseEntity<?> handleCombatException(CombatService.CombatException ex) {
        var status = switch (ex.getErrorCode()) {
            case "COMBAT_NOT_FOUND" -> HttpStatus.NOT_FOUND;
            case "COMBAT_NOT_ACTIVE", "COMBAT_NOT_YOUR_TURN", "COMBAT_AP_INSUFFICIENT",
                 "COMBAT_RANGE_INVALID", "COMBAT_TARGET_INVALID" -> HttpStatus.UNPROCESSABLE_ENTITY;
            case "COMBAT_INSUFFICIENT_PARTICIPANTS" -> HttpStatus.BAD_REQUEST;
            default -> HttpStatus.BAD_REQUEST;
        };
        return ResponseEntity.status(status).body(errorBody(ex.getErrorCode(), ex.getMessage()));
    }

    @ExceptionHandler(EntityEventService.EntityEventException.class)
    public ResponseEntity<?> handleEntityEventException(EntityEventService.EntityEventException ex) {
        var status = switch (ex.getErrorCode()) {
            case "EVENT_NOT_FOUND" -> HttpStatus.NOT_FOUND;
            default -> HttpStatus.BAD_REQUEST;
        };
        return ResponseEntity.status(status).body(errorBody(ex.getErrorCode(), ex.getMessage()));
    }

    @ExceptionHandler(AdventureService.AdventureException.class)
    public ResponseEntity<?> handleAdventureException(AdventureService.AdventureException ex) {
        var status = switch (ex.getErrorCode()) {
            case "ADVENTURE_NOT_FOUND", "ADVENTURE_NODE_NOT_FOUND",
                 "ADVENTURE_CHOICE_NOT_FOUND", "ADVENTURE_PROGRESS_NOT_FOUND" -> HttpStatus.NOT_FOUND;
            case "ADVENTURE_ALREADY_COMPLETED" -> HttpStatus.CONFLICT;
            default -> HttpStatus.BAD_REQUEST;
        };
        return ResponseEntity.status(status).body(errorBody(ex.getErrorCode(), ex.getMessage()));
    }

    @ExceptionHandler(LocationService.LocationException.class)
    public ResponseEntity<?> handleLocationException(LocationService.LocationException ex) {
        var status = switch (ex.getErrorCode()) {
            case "LOCATION_NOT_FOUND", "REGION_NOT_FOUND" -> HttpStatus.NOT_FOUND;
            default -> HttpStatus.BAD_REQUEST;
        };
        return ResponseEntity.status(status).body(errorBody(ex.getErrorCode(), ex.getMessage()));
    }

    @ExceptionHandler(QuestService.QuestException.class)
    public ResponseEntity<?> handleQuestException(QuestService.QuestException ex) {
        var status = switch (ex.getErrorCode()) {
            case "QUEST_NOT_FOUND" -> HttpStatus.NOT_FOUND;
            default -> HttpStatus.BAD_REQUEST;
        };
        return ResponseEntity.status(status).body(errorBody(ex.getErrorCode(), ex.getMessage()));
    }

    @ExceptionHandler(RegionService.RegionException.class)
    public ResponseEntity<?> handleRegionException(RegionService.RegionException ex) {
        var status = switch (ex.getErrorCode()) {
            case "REGION_NOT_FOUND" -> HttpStatus.NOT_FOUND;
            default -> HttpStatus.BAD_REQUEST;
        };
        return ResponseEntity.status(status).body(errorBody(ex.getErrorCode(), ex.getMessage()));
    }

    @ExceptionHandler(FactionService.FactionException.class)
    public ResponseEntity<?> handleFactionException(FactionService.FactionException ex) {
        var status = switch (ex.getErrorCode()) {
            case "FACTION_NOT_FOUND" -> HttpStatus.NOT_FOUND;
            default -> HttpStatus.BAD_REQUEST;
        };
        return ResponseEntity.status(status).body(errorBody(ex.getErrorCode(), ex.getMessage()));
    }

    @ExceptionHandler(GameSessionService.SessionException.class)
    public ResponseEntity<?> handleSessionException(GameSessionService.SessionException ex) {
        var status = switch (ex.getErrorCode()) {
            case "SESSION_NOT_FOUND" -> HttpStatus.NOT_FOUND;
            default -> HttpStatus.BAD_REQUEST;
        };
        return ResponseEntity.status(status).body(errorBody(ex.getErrorCode(), ex.getMessage()));
    }

    @ExceptionHandler(QuotaService.QuotaException.class)
    public ResponseEntity<?> handleQuotaException(QuotaService.QuotaException ex) {
        var status = switch (ex.getErrorCode()) {
            case "WORLD_LIMIT_REACHED", "WORLD_MEMBER_LIMIT" -> HttpStatus.FORBIDDEN;
            default -> HttpStatus.BAD_REQUEST;
        };
        return ResponseEntity.status(status).body(errorBody(ex.getErrorCode(), ex.getMessage()));
    }

    @ExceptionHandler(NpcIntentService.IntentException.class)
    public ResponseEntity<?> handleIntentException(NpcIntentService.IntentException ex) {
        var status = switch (ex.getErrorCode()) {
            case "INTENT_NOT_FOUND" -> HttpStatus.NOT_FOUND;
            default -> HttpStatus.BAD_REQUEST;
        };
        return ResponseEntity.status(status).body(errorBody(ex.getErrorCode(), ex.getMessage()));
    }

    @ExceptionHandler(InventoryService.InventoryException.class)
    public ResponseEntity<?> handleInventoryException(InventoryService.InventoryException ex) {
        var status = switch (ex.getErrorCode()) {
            case "ENTITY_NOT_FOUND", "INVENTORY_ITEM_NOT_FOUND" -> HttpStatus.NOT_FOUND;
            case "INVENTORY_SLOT_OCCUPIED" -> HttpStatus.CONFLICT;
            default -> HttpStatus.BAD_REQUEST;
        };
        return ResponseEntity.status(status).body(errorBody(ex.getErrorCode(), ex.getMessage()));
    }

    @ExceptionHandler(EntityService.EntityException.class)
    public ResponseEntity<?> handleEntityException(EntityService.EntityException ex) {
        var status = switch (ex.getErrorCode()) {
            case "ENTITY_NOT_FOUND" -> HttpStatus.NOT_FOUND;
            case "WORLD_ACCESS_DENIED" -> HttpStatus.FORBIDDEN;
            default -> HttpStatus.BAD_REQUEST;
        };
        return ResponseEntity.status(status).body(errorBody(ex.getErrorCode(), ex.getMessage()));
    }

    @ExceptionHandler(WorldService.WorldException.class)
    public ResponseEntity<?> handleWorldException(WorldService.WorldException ex) {
        var status = switch (ex.getErrorCode()) {
            case "WORLD_NOT_FOUND", "ENTITY_NOT_FOUND" -> HttpStatus.NOT_FOUND;
            case "WORLD_ACCESS_DENIED", "WORLD_OWNER_REQUIRED" -> HttpStatus.FORBIDDEN;
            case "WORLD_MEMBER_ALREADY" -> HttpStatus.CONFLICT;
            case "WORLD_GAME_SYSTEM_INACTIVE" -> HttpStatus.UNPROCESSABLE_ENTITY;
            default -> HttpStatus.BAD_REQUEST;
        };
        return ResponseEntity.status(status).body(errorBody(ex.getErrorCode(), ex.getMessage()));
    }

    @ExceptionHandler(GameSystemService.GameSystemException.class)
    public ResponseEntity<?> handleGameSystemException(GameSystemService.GameSystemException ex) {
        var status = switch (ex.getErrorCode()) {
            case "GAME_SYSTEM_NOT_FOUND" -> HttpStatus.NOT_FOUND;
            case "GAME_SYSTEM_VERSION_CONFLICT" -> HttpStatus.CONFLICT;
            default -> HttpStatus.BAD_REQUEST;
        };
        return ResponseEntity.status(status).body(errorBody(ex.getErrorCode(), ex.getMessage()));
    }

    @ExceptionHandler(RuleSchemaValidator.SchemaValidationException.class)
    public ResponseEntity<?> handleSchemaValidation(RuleSchemaValidator.SchemaValidationException ex) {
        return ResponseEntity.badRequest().body(Map.of(
            "error", Map.of(
                "code", "GAME_SYSTEM_SCHEMA_INVALID",
                "message", "Schema validation failed",
                "details", ex.getErrors()
            )
        ));
    }

    @ExceptionHandler(TimeController.TimeControllerException.class)
    public ResponseEntity<?> handleTimeControllerException(TimeController.TimeControllerException ex) {
        return ResponseEntity.status(ex.getStatus()).body(errorBody(ex.getErrorCode(), ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<?> handleValidation(MethodArgumentNotValidException ex) {
        var details = ex.getBindingResult().getFieldErrors().stream()
            .map(fe -> Map.of("field", fe.getField(), "issue", fe.getDefaultMessage()))
            .toList();
        return ResponseEntity.badRequest().body(Map.of(
            "error", Map.of(
                "code", "USER_PROFILE_INVALID",
                "message", "Validation failed",
                "details", details
            )
        ));
    }

    @ExceptionHandler(WorldAccess.WorldAccessException.class)
    public ResponseEntity<?> handleWorldAccessException(WorldAccess.WorldAccessException ex) {
        var status = switch (ex.getErrorCode()) {
            case "WORLD_NOT_FOUND" -> HttpStatus.NOT_FOUND;
            case "WORLD_ACCESS_DENIED" -> HttpStatus.FORBIDDEN;
            default -> HttpStatus.BAD_REQUEST;
        };
        return ResponseEntity.status(status).body(errorBody(ex.getErrorCode(), ex.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> handleGeneric(Exception ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorBody(
            "SYSTEM_INTERNAL_ERROR", "An unexpected error occurred"));
    }

    private static Map<String, Object> errorBody(String code, String message) {
        return Map.of("error", Map.of("code", code, "message", message));
    }
}