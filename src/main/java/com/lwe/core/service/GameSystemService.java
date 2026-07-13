package com.lwe.core.service;

import com.lwe.core.domain.GameSystem;
import com.lwe.core.repository.GameSystemRepository;
import com.lwe.rules.RuleSchemaValidator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class GameSystemService {

    private final GameSystemRepository repo;
    private final RuleSchemaValidator validator;

    public GameSystemService(GameSystemRepository repo, RuleSchemaValidator validator) {
        this.repo = repo;
        this.validator = validator;
    }

    /**
     * Neues Regelwerk anlegen. {@code rulesJson} wird vor Persistenz gegen
     * {@code schemaJson} validiert.
     */
    @Transactional
    public GameSystem create(String name, int version, String rulesJson, String schemaJson) {
        if (repo.existsByName(name)) {
            throw new GameSystemException("GAME_SYSTEM_VERSION_CONFLICT",
                "A game system with name '" + name + "' already exists");
        }
        validator.validateOrThrow(rulesJson, schemaJson);
        var gs = new GameSystem(name, version, rulesJson, schemaJson);
        return repo.save(gs);
    }

    /**
     * Alle aktiven Regelwerke abrufen.
     */
    public List<GameSystem> listActive() {
        return repo.findByActiveTrue();
    }

    /**
     * Regelwerk per ID laden.
     */
    public GameSystem getById(UUID id) {
        return repo.findById(id)
            .orElseThrow(() -> new GameSystemException("GAME_SYSTEM_NOT_FOUND",
                "Game system not found: " + id));
    }

    /**
     * Regelwerk erneut validieren (ohne Persistenzänderung).
     */
    public List<RuleSchemaValidator.ValidationError> revalidate(UUID id) {
        var gs = getById(id);
        return validator.validate(gs.getRulesJson(), gs.getSchemaJson());
    }

    public static class GameSystemException extends RuntimeException {
        private final String errorCode;
        public GameSystemException(String errorCode, String message) {
            super(message);
            this.errorCode = errorCode;
        }
        public String getErrorCode() { return errorCode; }
    }
}