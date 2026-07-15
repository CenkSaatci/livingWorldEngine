package com.lwe.api.dto;

import java.util.Map;

public record InventoryResponse(java.util.List<Object> items,
                                 java.util.Map<String, Integer> computedBonuses) {}
