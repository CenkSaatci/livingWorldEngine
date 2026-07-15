package com.lwe.api.dto;

import java.util.UUID;

public record AdventureAdvanceResponse(UUID nextNodeId, boolean completed,
                                         boolean skillCheckSuccess, String nodeText, boolean isEnd) {}
