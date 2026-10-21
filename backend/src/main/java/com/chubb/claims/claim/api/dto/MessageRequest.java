package com.chubb.claims.claim.api.dto;
import jakarta.validation.constraints.*;
public record MessageRequest(@NotBlank @Size(max=2000) String message) { }
