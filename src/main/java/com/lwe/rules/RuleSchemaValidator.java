package com.lwe.rules;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Validiert ein {@code rules_json} (Game System) gegen ein JSON Schema aus
 * {@code schema_json}. Nutzt die Networknt-JSON-Schema-Validator-Bibliothek.
 *
 * @see <a href="../../../docs/RULES-SCHEMA.md">docs/RULES-SCHEMA.md</a>
 */
@Component
public class RuleSchemaValidator {

    private final ObjectMapper objectMapper;
    private final JsonSchemaFactory factory;

    public RuleSchemaValidator(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012);
    }

    /**
     * Validiert {@code rulesJson} gegen {@code schemaJson}.
     *
     * @return Liste von Validierungsfehlern (leer = gültig)
     */
    public List<ValidationError> validate(String rulesJson, String schemaJson) {
        try {
            var schemaNode = objectMapper.readTree(schemaJson);
            var schema = factory.getSchema(schemaNode);

            var rulesNode = objectMapper.readTree(rulesJson);
            var errors = schema.validate(rulesNode);

            return errors.stream()
                .map(e -> new ValidationError(e.getInstanceLocation().toString(), e.getMessage()))
                .toList();
        } catch (Exception e) {
            return List.of(new ValidationError("(root)", "Parsing error: " + e.getMessage()));
        }
    }

    /**
     * Validiert und wirft bei Fehlern eine Exception.
     */
    public void validateOrThrow(String rulesJson, String schemaJson) {
        var errors = validate(rulesJson, schemaJson);
        if (!errors.isEmpty()) {
            throw new SchemaValidationException(errors);
        }
    }

    public record ValidationError(String path, String message) {}

    public static class SchemaValidationException extends RuntimeException {
        private final List<ValidationError> errors;
        public SchemaValidationException(List<ValidationError> errors) {
            super("Schema validation failed: " + errors.size() + " error(s)");
            this.errors = errors;
        }
        public List<ValidationError> getErrors() { return errors; }
    }
}