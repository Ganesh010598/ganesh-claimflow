package com.chubb.claims.claim.api.dto;

import com.chubb.claims.claim.domain.*;
import java.time.Instant;

public record ActivityResponse(Long id, String actorId, String action,
                                String message, ClaimStatus status, Instant createdAt) {
    public static ActivityResponse from(ClaimActivity a) {
        return new ActivityResponse(a.id, a.actorId, a.action, a.message, a.status, a.createdAt);
    }
}
