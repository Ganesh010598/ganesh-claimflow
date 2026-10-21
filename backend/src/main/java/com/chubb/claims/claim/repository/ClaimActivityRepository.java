package com.chubb.claims.claim.repository;

import com.chubb.claims.claim.domain.ClaimActivity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ClaimActivityRepository extends JpaRepository<ClaimActivity, Long> {
    List<ClaimActivity> findByClaim_IdOrderByCreatedAtAscIdAsc(Long claimId);
}
