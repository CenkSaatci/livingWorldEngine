package com.lwe.api;

import com.lwe.api.dto.ApiError;
import com.lwe.core.service.*;
import com.lwe.core.util.WorldAccess;
import com.lwe.rules.RuleSchemaValidator;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(AuthService.AuthException.class)
    public ResponseEntity<ApiError> handleAuthException(AuthService.AuthException ex) {
        return status(errorCode(ex)).body(ApiError.of(ex.getErrorCode(), ex.getMessage()));
    }

    @ExceptionHandler(CombatService.CombatException.class)
    public ResponseEntity<ApiError> handleCombatException(CombatService.CombatException ex) {
        return status(errorCode(ex)).body(ApiError.of(ex.getErrorCode(), ex.getMessage()));
    }

    @ExceptionHandler(EntityEventService.EntityEventException.class)
    public ResponseEntity<ApiError> handleEntityEventException(EntityEventService.EntityEventException ex) {
        return status(errorCode(ex)).body(ApiError.of(ex.getErrorCode(), ex.getMessage()));
    }

    @ExceptionHandler(AdventureService.AdventureException.class)
    public ResponseEntity<ApiError> handleAdventureException(AdventureService.AdventureException ex) {
        return status(errorCode(ex)).body(ApiError.of(ex.getErrorCode(), ex.getMessage()));
    }

    @ExceptionHandler(LocationService.LocationException.class)
    public ResponseEntity<ApiError> handleLocationException(LocationService.LocationException ex) {
        return status(errorCode(ex)).body(ApiError.of(ex.getErrorCode(), ex.getMessage()));
    }

    @ExceptionHandler(QuestService.QuestException.class)
    public ResponseEntity<ApiError> handleQuestException(QuestService.QuestException ex) {
        return status(errorCode(ex)).body(ApiError.of(ex.getErrorCode(), ex.getMessage()));
    }

    @ExceptionHandler(RegionService.RegionException.class)
    public ResponseEntity<ApiError> handleRegionException(RegionService.RegionException ex) {
        return status(errorCode(ex)).body(ApiError.of(ex.getErrorCode(), ex.getMessage()));
    }

    @ExceptionHandler(FactionService.FactionException.class)
    public ResponseEntity<ApiError> handleFactionException(FactionService.FactionException ex) {
        return status(errorCode(ex)).body(ApiError.of(ex.getErrorCode(), ex.getMessage()));
    }

    @ExceptionHandler(GameSessionService.SessionException.class)
    public ResponseEntity<ApiError> handleSessionException(GameSessionService.SessionException ex) {
        return status(errorCode(ex)).body(ApiError.of(ex.getErrorCode(), ex.getMessage()));
    }

    @ExceptionHandler(QuotaService.QuotaException.class)
    public ResponseEntity<ApiError> handleQuotaException(QuotaService.QuotaException ex) {
        return status(errorCode(ex)).body(ApiError.of(ex.getErrorCode(), ex.getMessage()));
    }

    @ExceptionHandler(NpcIntentService.IntentException.class)
    public ResponseEntity<ApiError> handleIntentException(NpcIntentService.IntentException ex) {
        return status(errorCode(ex)).body(ApiError.of(ex.getErrorCode(), ex.getMessage()));
    }

    @ExceptionHandler(InventoryService.InventoryException.class)
    public ResponseEntity<ApiError> handleInventoryException(InventoryService.InventoryException ex) {
        return status(errorCode(ex)).body(ApiError.of(ex.getErrorCode(), ex.getMessage()));
    }

    @ExceptionHandler(EntityService.EntityException.class)
    public ResponseEntity<ApiError> handleEntityException(EntityService.EntityException ex) {
        return status(errorCode(ex)).body(ApiError.of(ex.getErrorCode(), ex.getMessage()));
    }

    @ExceptionHandler(AbilityService.AbilityException.class)
    public ResponseEntity<ApiError> handleAbilityException(AbilityService.AbilityException ex) {
        return status(errorCode(ex)).body(ApiError.of(ex.getErrorCode(), ex.getMessage()));
    }

    @ExceptionHandler(EntityAbilityService.EntityAbilityException.class)
    public ResponseEntity<ApiError> handleEntityAbilityException(EntityAbilityService.EntityAbilityException ex) {
        return status(errorCode(ex)).body(ApiError.of(ex.getErrorCode(), ex.getMessage()));
    }

    @ExceptionHandler(WorldService.WorldException.class)
    public ResponseEntity<ApiError> handleWorldException(WorldService.WorldException ex) {
        return status(errorCode(ex)).body(ApiError.of(ex.getErrorCode(), ex.getMessage()));
    }

    @ExceptionHandler(GameSystemService.GameSystemException.class)
    public ResponseEntity<ApiError> handleGameSystemException(GameSystemService.GameSystemException ex) {
        return status(errorCode(ex)).body(ApiError.of(ex.getErrorCode(), ex.getMessage()));
    }

    @ExceptionHandler(RuleSchemaValidator.SchemaValidationException.class)
    public ResponseEntity<ApiError> handleSchemaValidation(RuleSchemaValidator.SchemaValidationException ex) {
        return ResponseEntity.badRequest().body(ApiError.schemaValidation(ex.getErrors()));
    }

    @ExceptionHandler(TimeController.TimeControllerException.class)
    public ResponseEntity<ApiError> handleTimeControllerException(TimeController.TimeControllerException ex) {
        return ResponseEntity.status(ex.getStatus()).body(ApiError.of(ex.getErrorCode(), ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex) {
        var details = ex.getBindingResult().getFieldErrors().stream()
            .map(fe -> Map.of("field", fe.getField(), "issue", fe.getDefaultMessage()))
            .toList();
        return ResponseEntity.badRequest().body(ApiError.validationFailed(details));
    }

    @ExceptionHandler(WorldAccess.WorldAccessException.class)
    public ResponseEntity<ApiError> handleWorldAccessException(WorldAccess.WorldAccessException ex) {
        return status(errorCode(ex)).body(ApiError.of(ex.getErrorCode(), ex.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleGeneric(Exception ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(ApiError.of("SYSTEM_INTERNAL_ERROR", "An unexpected error occurred"));
    }

    private static HttpStatus errorCode(Object ex) {
        var code = switch (ex) {
            case AuthService.AuthException e -> switch (e.getErrorCode()) {
                case "AUTH_EMAIL_TAKEN", "AUTH_USERNAME_TAKEN" -> HttpStatus.CONFLICT;
                case "AUTH_INVALID_CREDENTIALS", "AUTH_REFRESH_EXPIRED" -> HttpStatus.UNAUTHORIZED;
                case "AUTH_RATE_LIMITED" -> HttpStatus.TOO_MANY_REQUESTS;
                default -> HttpStatus.BAD_REQUEST;
            };
            case CombatService.CombatException e -> switch (e.getErrorCode()) {
                case "COMBAT_NOT_FOUND" -> HttpStatus.NOT_FOUND;
                case "COMBAT_NOT_ACTIVE", "COMBAT_NOT_YOUR_TURN", "COMBAT_AP_INSUFFICIENT",
                     "COMBAT_RANGE_INVALID", "COMBAT_TARGET_INVALID" -> HttpStatus.UNPROCESSABLE_ENTITY;
                case "COMBAT_INSUFFICIENT_PARTICIPANTS" -> HttpStatus.BAD_REQUEST;
                default -> HttpStatus.BAD_REQUEST;
            };
            case WorldService.WorldException e -> switch (e.getErrorCode()) {
                case "WORLD_NOT_FOUND", "ENTITY_NOT_FOUND" -> HttpStatus.NOT_FOUND;
                case "WORLD_ACCESS_DENIED", "WORLD_OWNER_REQUIRED" -> HttpStatus.FORBIDDEN;
                case "WORLD_MEMBER_ALREADY" -> HttpStatus.CONFLICT;
                case "WORLD_GAME_SYSTEM_INACTIVE" -> HttpStatus.UNPROCESSABLE_ENTITY;
                default -> HttpStatus.BAD_REQUEST;
            };
            case QuotaService.QuotaException e -> switch (e.getErrorCode()) {
                case "WORLD_LIMIT_REACHED", "WORLD_MEMBER_LIMIT" -> HttpStatus.FORBIDDEN;
                default -> HttpStatus.BAD_REQUEST;
            };
            case InventoryService.InventoryException e -> switch (e.getErrorCode()) {
                case "ENTITY_NOT_FOUND", "INVENTORY_ITEM_NOT_FOUND" -> HttpStatus.NOT_FOUND;
                case "INVENTORY_SLOT_OCCUPIED" -> HttpStatus.CONFLICT;
                default -> HttpStatus.BAD_REQUEST;
            };
            case EntityService.EntityException e -> switch (e.getErrorCode()) {
                case "ENTITY_NOT_FOUND" -> HttpStatus.NOT_FOUND;
                case "WORLD_ACCESS_DENIED" -> HttpStatus.FORBIDDEN;
                default -> HttpStatus.BAD_REQUEST;
            };
            case EntityAbilityService.EntityAbilityException e -> switch (e.getErrorCode()) {
                case "ENTITY_NOT_FOUND", "ABILITY_NOT_FOUND" -> HttpStatus.NOT_FOUND;
                case "ALREADY_ASSIGNED" -> HttpStatus.CONFLICT;
                case "WORLD_MISMATCH" -> HttpStatus.UNPROCESSABLE_ENTITY;
                default -> HttpStatus.BAD_REQUEST;
            };
            case WorldInviteService.InviteException e -> switch (e.getErrorCode()) {
                case "INVITE_NOT_FOUND", "WORLD_NOT_FOUND" -> HttpStatus.NOT_FOUND;
                case "INVITE_EXPIRED" -> HttpStatus.GONE;
                case "INVITE_EXHAUSTED" -> HttpStatus.CONFLICT;
                case "ALREADY_MEMBER" -> HttpStatus.CONFLICT;
                default -> HttpStatus.BAD_REQUEST;
            };
            default -> HttpStatus.BAD_REQUEST;
        };
        return code;
    }

    private static ResponseEntity.BodyBuilder status(HttpStatus status) {
        return ResponseEntity.status(status);
    }
}
