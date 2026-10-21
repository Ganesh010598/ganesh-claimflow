package com.chubb.claims.claim.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "claims", indexes = {
    @Index(name = "idx_claimant", columnList = "claimant_id"),
    @Index(name = "idx_status", columnList = "status")
})
public class Claim {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;
    @Column(name="claim_number", unique=true)
    public String claimNumber;
    @Column(name="claimant_id", nullable=false)
    public String claimantId;
    @Column(nullable=false)
    public String claimantName;
    @Enumerated(EnumType.STRING) @Column(nullable=false)
    public ClaimType claimType;
    @Enumerated(EnumType.STRING) @Column(nullable=false)
    public Market market;
    @Column(nullable=false)
    public String currency;
    @Column(nullable=false)
    public LocalDate incidentDate;
    @Column(nullable=false, length=2000)
    public String description;
    @Column(nullable=false, precision=18, scale=2)
    public BigDecimal claimedAmount;
    @Column(precision=18, scale=2)
    public BigDecimal reserveAmount;
    @Column(precision=18, scale=2)
    public BigDecimal approvedAmount;
    @Enumerated(EnumType.STRING) @Column(nullable=false)
    public ClaimStatus status;
    public String assignedOfficerId;
    @Column(length=2000)
    public String informationRequest;
    @Column(length=2000)
    public String decisionReason;
    @Column(nullable=false)
    public Instant createdAt;
    @Column(nullable=false)
    public Instant updatedAt;
    @Version
    public Long version;

    @PrePersist
    void onCreate() { createdAt = Instant.now(); updatedAt = createdAt; }
    @PreUpdate
    void onUpdate() { updatedAt = Instant.now(); }
}
