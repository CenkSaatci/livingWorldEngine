package com.lwe.api.dto;

public record SkillCheckResponse(String skillId, String expression, int[] dice, int total,
                                  int target, boolean success, String error) {}
