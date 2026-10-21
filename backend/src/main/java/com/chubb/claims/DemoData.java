package com.chubb.claims;

import com.chubb.claims.claim.api.dto.*;
import com.chubb.claims.claim.domain.*;
import com.chubb.claims.claim.repository.ClaimRepository;
import com.chubb.claims.claim.service.ClaimService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;
import java.time.LocalDate;

@Component
public class DemoData implements CommandLineRunner {
    private final ClaimService service;
    private final ClaimRepository repository;
    public DemoData(ClaimService service, ClaimRepository repository) {
        this.service = service;
        this.repository = repository;
    }
    @Override public void run(String... args) {
        if (repository.count() > 0) return;
        service.create(new CreateClaimRequest("demo-customer-001", "Alex Tan", ClaimType.MOTOR,
            Market.MY, LocalDate.now().minusDays(3), "Rear bumper damaged in a parking collision",
            new BigDecimal("3500.00")));
        ClaimResponse property = service.create(new CreateClaimRequest("demo-customer-002", "Jamie Lim",
            ClaimType.PROPERTY, Market.SG, LocalDate.now().minusDays(5),
            "Water damage to kitchen ceiling", new BigDecimal("8000.00")));
        service.assign(property.id(), new AssignRequest("officer-002"));
        service.requestInformation(property.id(), new OfficerMessageRequest("officer-002",
            "Please attach a description of the repair estimate."));
    }
}
