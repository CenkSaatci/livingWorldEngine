package com.lwe.api.dto;

import com.lwe.core.service.InventoryService.InventoryEntry;

import java.util.List;
import java.util.Map;

public record InventoryResponse(List<InventoryEntry> items,
                                 Map<String, Integer> computedBonuses) {}
