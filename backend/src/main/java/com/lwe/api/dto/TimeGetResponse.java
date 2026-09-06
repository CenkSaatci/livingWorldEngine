package com.lwe.api.dto;

public record TimeGetResponse(String currentGameTime, String mode, boolean paused,
                               String dayPhase, boolean isDaytime) {}
