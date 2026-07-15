package com.lwe.api.dto;

import java.util.List;

public record PaginatedWorldResponse(List<WorldInfoResponse> items, int total, int page, boolean hasMore) {}
