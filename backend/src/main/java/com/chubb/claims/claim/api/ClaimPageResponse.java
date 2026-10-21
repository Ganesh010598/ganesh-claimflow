package com.chubb.claims.claim.api;

import com.chubb.claims.claim.api.dto.ClaimResponse;
import org.springframework.data.domain.Page;
import java.util.List;

// Avoid serializing Spring Data's PageImpl directly: this is our stable API contract.
public record ClaimPageResponse(
    List<ClaimResponse> content,
    long totalElements,
    int totalPages,
    int page,
    int size,
    boolean first,
    boolean last
) {
    public static ClaimPageResponse from(Page<ClaimResponse> result) {
        return new ClaimPageResponse(result.getContent(), result.getTotalElements(),
            result.getTotalPages(), result.getNumber(), result.getSize(),
            result.isFirst(), result.isLast());
    }
}
