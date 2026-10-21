package com.chubb.claims.claim.api.dto;

import com.chubb.claims.claim.domain.*;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;

public record CreateClaimRequest(
    @NotBlank String claimantId,
    @NotBlank String claimantName,
    @NotNull ClaimType claimType,
    @NotNull Market market,
    @NotNull @PastOrPresent LocalDate incidentDate,
    @NotBlank @Size(max=2000) String description,
    @NotNull @DecimalMin("0.01") @Digits(integer=16,fraction=2) BigDecimal claimedAmount
) { }
