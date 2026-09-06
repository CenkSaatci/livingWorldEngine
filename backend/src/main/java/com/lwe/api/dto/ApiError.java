package com.lwe.api.dto;

import com.lwe.rules.RuleSchemaValidator;

import java.util.List;
import java.util.Map;

public record ApiError(Map<String, Object> error) {

    public static ApiError of(String code, String message) {
        return new ApiError(Map.of("code", code, "message", message));
    }

    public static ApiError schemaValidation(List<RuleSchemaValidator.ValidationError> errors) {
        return new ApiError(Map.of(
            "code", "GAME_SYSTEM_SCHEMA_INVALID",
            "message", "Schema validation failed",
            "details", errors
        ));
    }

    public static ApiError validationFailed(List<Map<String, String>> details) {
        return new ApiError(Map.of(
            "code", "USER_PROFILE_INVALID",
            "message", "Validation failed",
            "details", details
        ));
    }
}
