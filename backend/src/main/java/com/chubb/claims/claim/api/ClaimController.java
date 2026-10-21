package com.chubb.claims.claim.api;

import com.chubb.claims.claim.api.dto.*;
import com.chubb.claims.claim.domain.ClaimStatus;
import com.chubb.claims.claim.service.ClaimService;
import jakarta.validation.Valid;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/claims")
public class ClaimController {
    private final ClaimService service;
    public ClaimController(ClaimService service) { this.service = service; }

    @PostMapping
    public ResponseEntity<ClaimResponse> create(@Valid @RequestBody CreateClaimRequest request) {
        ClaimResponse claim = service.create(request);
        return ResponseEntity.created(URI.create("/api/claims/" + claim.id())).body(claim);
    }

    @GetMapping("/{id}")
    public ClaimResponse one(@PathVariable Long id) { return service.get(id); }

    @GetMapping
    public List<ClaimResponse> all(@RequestParam(required=false) String claimantId,
                                    @RequestParam(required=false) ClaimStatus status) {
        return service.list(claimantId, status);
    }

    @GetMapping("/{id}/history")
    public List<ActivityResponse> history(@PathVariable Long id) { return service.history(id); }

    @PostMapping("/{id}/assign")
    public ClaimResponse assign(@PathVariable Long id, @Valid @RequestBody AssignRequest request) {
        return service.assign(id, request);
    }

    @PostMapping("/{id}/reserve")
    public ClaimResponse reserve(@PathVariable Long id, @Valid @RequestBody AmountRequest request) {
        return service.setReserve(id, request);
    }

    @PostMapping("/{id}/request-info")
    public ClaimResponse requestInfo(@PathVariable Long id, @Valid @RequestBody OfficerMessageRequest request) {
        return service.requestInformation(id, request);
    }

    @PostMapping("/{id}/information")
    public ClaimResponse information(@PathVariable Long id, @Valid @RequestBody MessageRequest request) {
        return service.provideInformation(id, request);
    }

    @PostMapping("/{id}/decision")
    public ClaimResponse decision(@PathVariable Long id, @Valid @RequestBody DecisionRequest request) {
        return service.decide(id, request);
    }

    @PostMapping("/{id}/settle")
    public ClaimResponse settle(@PathVariable Long id, @Valid @RequestBody OfficerActionRequest request) {
        return service.settle(id, request);
    }
}
