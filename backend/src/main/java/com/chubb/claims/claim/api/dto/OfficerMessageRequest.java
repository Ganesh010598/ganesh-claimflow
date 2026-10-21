package com.chubb.claims.claim.api.dto;
import jakarta.validation.constraints.*;
public record OfficerMessageRequest(@NotBlank String officerId,
                                    @NotBlank @Size(max=2000) String message) { }
