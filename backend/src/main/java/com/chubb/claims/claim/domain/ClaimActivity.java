package com.chubb.claims.claim.domain;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name="claim_activities", indexes = @Index(name="idx_activity_claim", columnList="claim_id"))
public class ClaimActivity {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY)
    public Long id;
    @ManyToOne(fetch=FetchType.LAZY, optional=false)
    @JoinColumn(name="claim_id", nullable=false)
    public Claim claim;
    @Column(nullable=false)
    public String actorId;
    @Column(nullable=false)
    public String action;
    @Column(length=2000)
    public String message;
    @Enumerated(EnumType.STRING) @Column(nullable=false)
    public ClaimStatus status;
    @Column(nullable=false)
    public Instant createdAt = Instant.now();
}
