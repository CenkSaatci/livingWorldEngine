package com.lwe.api.dto;

public record BooleanResponse(boolean success) {

    public static BooleanResponse ok() { return new BooleanResponse(true); }

    public static BooleanResponse valid(boolean v) { return new BooleanResponse(v); }
}
