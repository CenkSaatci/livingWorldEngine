package com.lwe.api.dto;

import com.lwe.core.domain.AdventureNode;

import java.util.UUID;

public record AdventureNodeResponse(UUID id, UUID adventureId, String text,
                                     String imageUrl, boolean isEnd) {

    public static AdventureNodeResponse from(AdventureNode n) {
        return new AdventureNodeResponse(n.getId(), n.getAdventureId(), n.getText(),
            n.getImageUrl() != null ? n.getImageUrl() : "", n.isEnd());
    }

    public static AdventureNodeResponse minimal(AdventureNode n) {
        return new AdventureNodeResponse(n.getId(), null, n.getText(), null, n.isEnd());
    }
}
