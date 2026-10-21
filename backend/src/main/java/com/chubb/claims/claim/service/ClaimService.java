package com.chubb.claims.claim.service;

import com.chubb.claims.claim.api.dto.*;
import com.chubb.claims.claim.domain.*;
import com.chubb.claims.claim.repository.*;
import com.chubb.claims.common.NotFoundException;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.Year;
import java.util.List;

@Service
@Transactional(readOnly=true)
public class ClaimService {
    private final ClaimRepository claims;
    private final ClaimActivityRepository activities;

    public ClaimService(ClaimRepository claims, ClaimActivityRepository activities) {
        this.claims = claims;
        this.activities = activities;
    }

    private Claim find(Long id) {
        return claims.findById(id).orElseThrow(() -> new NotFoundException("Claim " + id + " not found"));
    }

    private ClaimResponse result(Claim claim) {
        claims.flush(); // ensures updatedAt and optimistic-lock checks have been applied
        return ClaimResponse.from(claim);
    }

    private void history(Claim claim, String actor, String action, String message) {
        ClaimActivity activity = new ClaimActivity();
        activity.claim = claim;
        activity.actorId = actor;
        activity.action = action;
        activity.message = message;
        activity.status = claim.status;
        activities.save(activity);
    }

    private void require(Claim claim, ClaimStatus status) {
        if (claim.status != status) {
            throw new IllegalStateException("Expected " + status + " but claim is " + claim.status);
        }
    }

    private void requireOfficer(Claim claim, String officerId) {
        if (claim.assignedOfficerId == null || !claim.assignedOfficerId.equals(officerId)) {
            throw new IllegalStateException("Action requires the assigned officer");
        }
    }

    @Transactional
    public ClaimResponse create(CreateClaimRequest req) {
        Claim c = new Claim();
        c.claimantId = req.claimantId().trim();
        c.claimantName = req.claimantName().trim();
        c.claimType = req.claimType();
        c.market = req.market();
        c.currency = req.market().currency();
        c.incidentDate = req.incidentDate();
        c.description = req.description().trim();
        c.claimedAmount = req.claimedAmount();
        c.status = ClaimStatus.SUBMITTED;
        claims.saveAndFlush(c);
        c.claimNumber = "CLM-" + Year.now().getValue() + "-" + String.format("%06d", c.id);
        history(c, c.claimantId, "SUBMITTED", "New " + c.claimType + " claim submitted");
        return result(c);
    }

    public ClaimResponse get(Long id) { return ClaimResponse.from(find(id)); }

    public List<ClaimResponse> list(String claimantId, ClaimStatus status) {
        return claims.findAll(Sort.by(Sort.Direction.DESC, "createdAt", "id")).stream()
            .filter(c -> claimantId == null || claimantId.equals(c.claimantId))
            .filter(c -> status == null || status == c.status)
            .map(ClaimResponse::from).toList();
    }

    public List<ActivityResponse> history(Long id) {
        find(id);
        return activities.findByClaim_IdOrderByCreatedAtAscIdAsc(id).stream()
            .map(ActivityResponse::from).toList();
    }

    @Transactional
    public ClaimResponse assign(Long id, AssignRequest req) {
        Claim c = find(id);
        require(c, ClaimStatus.SUBMITTED);
        c.assignedOfficerId = req.officerId().trim();
        c.status = ClaimStatus.IN_REVIEW;
        history(c, c.assignedOfficerId, "ASSIGNED", "Claim assigned for review");
        return result(c);
    }

    @Transactional
    public ClaimResponse setReserve(Long id, AmountRequest req) {
        Claim c = find(id);
        require(c, ClaimStatus.IN_REVIEW);
        requireOfficer(c, req.officerId());
        c.reserveAmount = req.amount();
        history(c, req.officerId(), "RESERVE_UPDATED", "Reserve updated to " + c.currency + " " + req.amount());
        return result(c);
    }

    @Transactional
    public ClaimResponse requestInformation(Long id, OfficerMessageRequest req) {
        Claim c = find(id);
        require(c, ClaimStatus.IN_REVIEW);
        requireOfficer(c, req.officerId());
        c.informationRequest = req.message().trim();
        c.status = ClaimStatus.NEEDS_INFORMATION;
        history(c, req.officerId(), "INFORMATION_REQUESTED", c.informationRequest);
        return result(c);
    }

    @Transactional
    public ClaimResponse provideInformation(Long id, MessageRequest req) {
        Claim c = find(id);
        require(c, ClaimStatus.NEEDS_INFORMATION);
        c.informationRequest = null;
        c.status = ClaimStatus.IN_REVIEW;
        history(c, c.claimantId, "INFORMATION_PROVIDED", req.message().trim());
        return result(c);
    }

    @Transactional
    public ClaimResponse decide(Long id, DecisionRequest req) {
        Claim c = find(id);
        require(c, ClaimStatus.IN_REVIEW);
        requireOfficer(c, req.officerId());
        if (req.approved()) {
            BigDecimal amount = req.approvedAmount();
            if (amount == null || amount.signum() <= 0 || amount.compareTo(c.claimedAmount) > 0) {
                throw new IllegalArgumentException("Approved amount must be > 0 and <= claimed amount");
            }
            c.approvedAmount = amount;
            c.status = ClaimStatus.APPROVED;
        } else {
            c.approvedAmount = null;
            c.status = ClaimStatus.REJECTED;
        }
        c.decisionReason = req.reason().trim();
        history(c, req.officerId(), c.status.toString(), c.decisionReason);
        return result(c);
    }

    @Transactional
    public ClaimResponse settle(Long id, OfficerActionRequest req) {
        Claim c = find(id);
        require(c, ClaimStatus.APPROVED);
        requireOfficer(c, req.officerId());
        c.status = ClaimStatus.SETTLED;
        history(c, req.officerId(), "SETTLED", "Settlement marked complete (demo only: no payment processed)");
        return result(c);
    }
}
