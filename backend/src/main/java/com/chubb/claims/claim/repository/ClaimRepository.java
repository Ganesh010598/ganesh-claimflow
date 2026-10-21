package com.chubb.claims.claim.repository;

import com.chubb.claims.claim.domain.Claim;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClaimRepository extends JpaRepository<Claim, Long> { }
