package com.chubb.claims.reporting;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public record DashboardResponse(
    long totalClaims, long openClaims, long awaitingReview, long awaitingCustomer,
    long approvedUnsettled, List<Exposure> exposureByMarket, Map<String,Long> workloadByOfficer
) {
    public record Exposure(String market, String currency, BigDecimal amount) { }
}
