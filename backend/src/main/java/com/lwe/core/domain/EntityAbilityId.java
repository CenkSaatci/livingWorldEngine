package com.lwe.core.domain;

import java.io.Serializable;
import java.util.UUID;

public record EntityAbilityId(UUID entityId, UUID abilityId) implements Serializable {}
