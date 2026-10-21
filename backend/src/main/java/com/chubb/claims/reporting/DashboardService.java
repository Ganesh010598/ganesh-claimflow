package com.chubb.claims.reporting;

import com.chubb.claims.claim.domain.*;
import com.chubb.claims.claim.repository.ClaimRepository;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.util.*;

@Service
public class DashboardService {
    private final ClaimRepository claims;
    public DashboardService(ClaimRepository claims) { this.claims = claims; }

    public DashboardResponse getDashboard() {
        List<Claim> all = claims.findAll();
        long open = 0, review = 0, customer = 0, approved = 0;
        Map<String, BigDecimal> exposure = new TreeMap<>();
        Map<String, Long> workload = new TreeMap<>();
        for (Claim c : all) {
            if (c.status == ClaimStatus.REJECTED || c.status == ClaimStatus.SETTLED) continue;
            open++;
            if (c.status == ClaimStatus.SUBMITTED || c.status == ClaimStatus.IN_REVIEW) review++;
            if (c.status == ClaimStatus.NEEDS_INFORMATION) customer++;
            if (c.status == ClaimStatus.APPROVED) approved++;
            if (c.assignedOfficerId != null) workload.merge(c.assignedOfficerId, 1L, Long::sum);
            // Demonstration estimate: approved amount, then reserve, then claimed amount.
            BigDecimal estimate = c.status == ClaimStatus.APPROVED && c.approvedAmount != null
                ? c.approvedAmount : c.reserveAmount != null ? c.reserveAmount : c.claimedAmount;
            exposure.merge(c.market.name() + ":" + c.currency, estimate, BigDecimal::add);
        }
        List<DashboardResponse.Exposure> buckets = exposure.entrySet().stream().map(e -> {
            String[] key = e.getKey().split(":");
            return new DashboardResponse.Exposure(key[0], key[1], e.getValue());
        }).toList();
        return new DashboardResponse(all.size(), open, review, customer, approved, buckets, workload);
    }
}
