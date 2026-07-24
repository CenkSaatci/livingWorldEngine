package com.lwe.api.dto;

import java.util.List;

public record ProbeResponse(
    String probeType,
    int[] dice,
    int modifier,
    int total,
    boolean success,
    List<DieDetail> details,
    List<ConditionalResult> activeConditionals
) {
    public record DieDetail(int die, String attribute, int attrValue, boolean success) {}
    public record ConditionalResult(String name, String bonus, String target) {}
}
