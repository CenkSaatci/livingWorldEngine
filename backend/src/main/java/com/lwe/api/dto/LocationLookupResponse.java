package com.lwe.api.dto;

import java.util.UUID;

public record LocationLookupResponse(UUID id, String name, String type, UUID regionId) {}
