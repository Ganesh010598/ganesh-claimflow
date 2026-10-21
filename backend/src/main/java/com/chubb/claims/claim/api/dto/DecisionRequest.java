package com.chubb.claims.claim.api.dto;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
public record DecisionRequest(@NotBlank String officerId, @NotNull Boolean approved,
                              BigDecimal approvedAmount, @NotBlank String reason) { }
