package com.lwe.api.dto;

public record RollResponse(String expression, int[] dice, int total, int modifier,
                            int sides, int count) {}
