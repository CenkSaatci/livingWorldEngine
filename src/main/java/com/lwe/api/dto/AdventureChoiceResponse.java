package com.lwe.api.dto;

import com.lwe.core.domain.NodeChoice;

import java.util.UUID;

public record AdventureChoiceResponse(UUID id, String label, String skillCheck) {

    public static AdventureChoiceResponse from(NodeChoice c) {
        return new AdventureChoiceResponse(c.getId(), c.getLabel(),
            c.getSkillCheck() != null ? c.getSkillCheck() : "");
    }

    public static AdventureChoiceResponse created(NodeChoice c) {
        return new AdventureChoiceResponse(c.getId(), c.getLabel(), null);
    }
}
