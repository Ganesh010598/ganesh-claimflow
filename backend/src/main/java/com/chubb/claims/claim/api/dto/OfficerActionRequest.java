package com.chubb.claims.claim.api.dto;
import jakarta.validation.constraints.NotBlank;
public record OfficerActionRequest(@NotBlank String officerId) { }
