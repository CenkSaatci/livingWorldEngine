package com.lwe.api.dto;

public record TimeResponse(String currentGameTime, String mode, boolean paused,
                           String dayPhase, boolean isDaytime) {}
