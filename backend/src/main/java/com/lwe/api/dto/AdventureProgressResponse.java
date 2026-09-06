package com.lwe.api.dto;

import java.util.UUID;

public record AdventureProgressResponse(String status, UUID currentNodeId,
                                          String visitedNodes, String nodeText, boolean isEnd) {}
