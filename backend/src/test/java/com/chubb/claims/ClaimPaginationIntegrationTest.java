package com.chubb.claims;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class ClaimPaginationIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;

    private long createClaim(String claimantId) throws Exception {
        String body = """
            {"claimantId":"%s","claimantName":"Pagination Customer",
             "claimType":"MOTOR","market":"MY","incidentDate":"2026-01-01",
             "description":"Claim for pagination test","claimedAmount":250.00}
            """.formatted(claimantId);
        String response = mvc.perform(post("/api/claims")
                .contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isCreated())
            .andReturn().getResponse().getContentAsString();
        return json.readTree(response).get("id").asLong();
    }

    @Test void paginatesFilteredClaimsAndReturnsUsefulMetadata() throws Exception {
        String claimant = "pagination-" + UUID.randomUUID();
        long first = createClaim(claimant);
        long second = createClaim(claimant);
        long third = createClaim(claimant);

        mvc.perform(get("/api/claims").param("claimantId", claimant)
                .param("page", "0").param("size", "2"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalElements").value(3))
            .andExpect(jsonPath("$.totalPages").value(2))
            .andExpect(jsonPath("$.page").value(0))
            .andExpect(jsonPath("$.size").value(2))
            .andExpect(jsonPath("$.first").value(true))
            .andExpect(jsonPath("$.last").value(false))
            .andExpect(jsonPath("$.content.length()").value(2))
            .andExpect(jsonPath("$.content[0].id").value((int) third))
            .andExpect(jsonPath("$.content[1].id").value((int) second));

        mvc.perform(get("/api/claims").param("claimantId", claimant)
                .param("page", "1").param("size", "2"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.page").value(1))
            .andExpect(jsonPath("$.last").value(true))
            .andExpect(jsonPath("$.content.length()").value(1))
            .andExpect(jsonPath("$.content[0].id").value((int) first));
    }

    @Test void filtersByStatusAndClaimantTogether() throws Exception {
        String claimant = "status-filter-" + UUID.randomUUID();
        long inReview = createClaim(claimant);
        createClaim(claimant); // Still submitted
        mvc.perform(post("/api/claims/" + inReview + "/assign")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"officerId\":\"officer-001\"}"))
            .andExpect(status().isOk());

        mvc.perform(get("/api/claims").param("claimantId", claimant)
                .param("status", "IN_REVIEW").param("page", "0"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalElements").value(1))
            .andExpect(jsonPath("$.content[0].id").value((int) inReview))
            .andExpect(jsonPath("$.content[0].status").value("IN_REVIEW"));
    }

    @Test void returnsAnEmptyPageWhenTheRequestedPageIsBeyondTheLastPage() throws Exception {
        String claimant = "empty-page-" + UUID.randomUUID();
        createClaim(claimant);
        mvc.perform(get("/api/claims").param("claimantId", claimant)
                .param("page", "4").param("size", "2"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content.length()").value(0))
            .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test void keepsTheOriginalArrayResponseForNonPaginatedClients() throws Exception {
        String claimant = "legacy-" + UUID.randomUUID();
        createClaim(claimant);
        createClaim(claimant);
        String body = mvc.perform(get("/api/claims").param("claimantId", claimant))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].claimantId").value(claimant))
            .andReturn().getResponse().getContentAsString();
        JsonNode result = json.readTree(body);
        assertThat(result.isArray()).isTrue();
        assertThat(result.size()).isEqualTo(2);
    }

    @Test void rejectsInvalidPageAndPageSize() throws Exception {
        mvc.perform(get("/api/claims").param("page", "-1"))
            .andExpect(status().isBadRequest());
        mvc.perform(get("/api/claims").param("page", "0").param("size", "0"))
            .andExpect(status().isBadRequest());
        mvc.perform(get("/api/claims").param("page", "0").param("size", "101"))
            .andExpect(status().isBadRequest());
    }
}
