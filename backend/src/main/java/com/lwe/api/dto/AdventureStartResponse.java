package com.lwe.api.dto;

import com.lwe.core.domain.AdventureNode;

import java.util.UUID;

public record AdventureStartResponse(UUID progressId, UUID currentNodeId, String status,
                                      AdventureNodeResponse node) {}
