package com.chubb.claims.claim.api.dto;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
public record AmountRequest(@NotBlank String officerId,
                            @NotNull @DecimalMin("0.01") BigDecimal amount) { }
