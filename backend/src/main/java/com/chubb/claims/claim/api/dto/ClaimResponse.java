package com.chubb.claims.claim.api.dto;

import com.chubb.claims.claim.domain.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record ClaimResponse(
    Long id, String claimNumber, String claimantId, String claimantName,
    ClaimType claimType, Market market, String currency,
    LocalDate incidentDate, String description, BigDecimal claimedAmount,
    BigDecimal reserveAmount, BigDecimal approvedAmount, ClaimStatus status,
    String assignedOfficerId, String informationRequest, String decisionReason,
    Instant createdAt, Instant updatedAt
) {
    public static ClaimResponse from(Claim c) {
        return new ClaimResponse(c.id, c.claimNumber, c.claimantId, c.claimantName,
            c.claimType, c.market, c.currency, c.incidentDate, c.description,
            c.claimedAmount, c.reserveAmount, c.approvedAmount, c.status,
            c.assignedOfficerId, c.informationRequest, c.decisionReason,
            c.createdAt, c.updatedAt);
    }
}
