package com.chubb.claims.claim.repository;

import com.chubb.claims.claim.domain.Claim;
import com.chubb.claims.claim.domain.ClaimStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface ClaimRepository extends JpaRepository<Claim, Long> {
    // Both queries filter in the database, unlike fetching every claim and filtering in Java.
    @Query("""
        SELECT c FROM Claim c
        WHERE (:claimantId IS NULL OR c.claimantId = :claimantId)
          AND (:status IS NULL OR c.status = :status)
        """)
    List<Claim> findMatching(@Param("claimantId") String claimantId,
                             @Param("status") ClaimStatus status, Sort sort);

    @Query("""
        SELECT c FROM Claim c
        WHERE (:claimantId IS NULL OR c.claimantId = :claimantId)
          AND (:status IS NULL OR c.status = :status)
        """)
    Page<Claim> findMatching(@Param("claimantId") String claimantId,
                             @Param("status") ClaimStatus status, Pageable pageable);
}
