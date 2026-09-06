package com.lwe.api.dto;

import java.util.UUID;

public record UserInfoResponse(UUID id, String email, String username, String role, String locale) {}
