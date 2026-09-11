package com.lwe.api;

import com.lwe.api.dto.ApiError;
import com.lwe.core.service.*;
import com.lwe.core.util.WorldAccess;
import com.lwe.rules.RuleSchemaValidator;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex) {
        var details = ex.getBindingResult().getFieldErrors().stream()
            .map(fe -> Map.of("field", fe.getField(), "issue", fe.getDefaultMessage()))
            .toList();
        return ResponseEntity.badRequest().body(ApiError.validationFailed(details));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiError> handleConstraintViolation(ConstraintViolationException ex) {
        var details = ex.getConstraintViolations().stream()
            .map(v -> Map.of("field", v.getPropertyPath().toString(), "issue", v.getMessage()))
            .toList();
        return ResponseEntity.badRequest().body(ApiError.validationFailed(details));
    }

    @ExceptionHandler(RuleSchemaValidator.SchemaValidationException.class)
    public ResponseEntity<ApiError> handleSchemaValidation(RuleSchemaValidator.SchemaValidationException ex) {
        return ResponseEntity.badRequest().body(ApiError.schemaValidation(ex.getErrors()));
    }

    @ExceptionHandler(TimeController.TimeControllerException.class)
    public ResponseEntity<ApiError> handleTimeControllerException(TimeController.TimeControllerException ex) {
        return ResponseEntity.status(ex.getStatus()).body(ApiError.of(ex.getErrorCode(), ex.getMessage()));
    }

    @ExceptionHandler(org.springframework.web.servlet.resource.NoResourceFoundException.class)
    public ResponseEntity<ApiError> handleNoResource(
            org.springframework.web.servlet.resource.NoResourceFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(ApiError.of("ROUTE_NOT_FOUND", "No such endpoint: " + ex.getResourcePath()));
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ApiError> handleServiceException(RuntimeException ex) {
        var errorCode = extractErrorCode(ex);
        if (errorCode != null) {
            return status(errorCode(ex)).body(ApiError.of(errorCode, ex.getMessage()));
        }
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(ApiError.of("SYSTEM_INTERNAL_ERROR", "An unexpected error occurred"));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleGeneric(Exception ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(ApiError.of("SYSTEM_INTERNAL_ERROR", "An unexpected error occurred"));
    }

    private static String extractErrorCode(RuntimeException ex) {
        return switch (ex) {
            case AuthService.AuthException e -> e.getErrorCode();
            case CombatService.CombatException e -> e.getErrorCode();
            case EntityEventService.EntityEventException e -> e.getErrorCode();
            case AdventureService.AdventureException e -> e.getErrorCode();
            case LocationService.LocationException e -> e.getErrorCode();
            case QuestService.QuestException e -> e.getErrorCode();
            case RegionService.RegionException e -> e.getErrorCode();
            case FactionService.FactionException e -> e.getErrorCode();
            case GameSessionService.SessionException e -> e.getErrorCode();
            case QuotaService.QuotaException e -> e.getErrorCode();
            case NpcIntentService.IntentException e -> e.getErrorCode();
            case InventoryService.InventoryException e -> e.getErrorCode();
            case EntityService.EntityException e -> e.getErrorCode();
            case AbilityService.AbilityException e -> e.getErrorCode();
            case EntityAbilityService.EntityAbilityException e -> e.getErrorCode();
            case RestService.RestException e -> e.getErrorCode();
            case CampaignService.CampaignException e -> e.getErrorCode();
            case CampaignMemberService.CampaignMemberException e -> e.getErrorCode();
            case GameSystemService.GameSystemException e -> e.getErrorCode();
            case ItemService.ItemException e -> e.getErrorCode();
            case WorldService.WorldException e -> e.getErrorCode();
            case WorldInviteService.InviteException e -> e.getErrorCode();
            case WorldAccess.WorldAccessException e -> e.getErrorCode();
            case LevelUpService.LevelException e -> e.getErrorCode();
            case FormulaEvaluator.EvaluationException e -> "FORMULA_EXPRESSION_INVALID";
            case IllegalArgumentException e -> "INVALID_INPUT";
            case null -> null;
            default -> null;
        };
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
                case "WORLD_ACCESS_DENIED" -> HttpStatus.FORBIDDEN;
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
            case WorldAccess.WorldAccessException e -> switch (e.getErrorCode()) {
                case "WORLD_ACCESS_DENIED" -> HttpStatus.FORBIDDEN;
                case "WORLD_NOT_FOUND", "MAP_NOT_FOUND" -> HttpStatus.NOT_FOUND;
                default -> HttpStatus.BAD_REQUEST;
            };
            case LevelUpService.LevelException e -> switch (e.getErrorCode()) {
                case "ENTITY_NOT_FOUND" -> HttpStatus.NOT_FOUND;
                default -> HttpStatus.BAD_REQUEST;
            };
            case FormulaEvaluator.EvaluationException e -> HttpStatus.BAD_REQUEST;
            case IllegalArgumentException e -> HttpStatus.BAD_REQUEST;
            case CampaignService.CampaignException e -> switch (e.getErrorCode()) {
                case "CAMPAIGN_NOT_FOUND", "WORLD_NOT_FOUND", "GAME_SYSTEM_NOT_FOUND" -> HttpStatus.NOT_FOUND;
                case "WORLD_ACCESS_DENIED" -> HttpStatus.FORBIDDEN;
                default -> HttpStatus.BAD_REQUEST;
            };
            case CampaignMemberService.CampaignMemberException e -> switch (e.getErrorCode()) {
                case "CAMPAIGN_NOT_FOUND", "USER_NOT_FOUND", "MEMBER_NOT_FOUND" -> HttpStatus.NOT_FOUND;
                case "DM_REMOVAL_DENIED", "DM_REQUIRED", "WORLD_ACCESS_DENIED" -> HttpStatus.FORBIDDEN;
                case "MEMBER_ALREADY" -> HttpStatus.CONFLICT;
                default -> HttpStatus.BAD_REQUEST;
            };
            case RestService.RestException e -> switch (e.getErrorCode()) {
                case "ENTITY_NOT_FOUND" -> HttpStatus.NOT_FOUND;
                default -> HttpStatus.BAD_REQUEST;
            };
            case GameSystemService.GameSystemException e -> switch (e.getErrorCode()) {
                case "GAME_SYSTEM_NOT_FOUND" -> HttpStatus.NOT_FOUND;
                case "GAME_SYSTEM_VERSION_CONFLICT" -> HttpStatus.CONFLICT;
                default -> HttpStatus.BAD_REQUEST;
            };
            case ItemService.ItemException e -> switch (e.getErrorCode()) {
                case "ITEM_NOT_FOUND", "GAME_SYSTEM_NOT_FOUND" -> HttpStatus.NOT_FOUND;
                default -> HttpStatus.BAD_REQUEST;
            };
            case AbilityService.AbilityException e -> switch (e.getErrorCode()) {
                case "ABILITY_NOT_FOUND", "GAME_SYSTEM_NOT_FOUND" -> HttpStatus.NOT_FOUND;
                default -> HttpStatus.BAD_REQUEST;
            };
            case GameSessionService.SessionException e -> switch (e.getErrorCode()) {
                case "SESSION_NOT_FOUND", "WORLD_NOT_FOUND" -> HttpStatus.NOT_FOUND;
                case "WORLD_ACCESS_DENIED" -> HttpStatus.FORBIDDEN;
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
