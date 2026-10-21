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
class ClaimsIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;

    private long newClaim() throws Exception {
        String payload = """
          {"claimantId":"%s","claimantName":"Demo Customer","claimType":"MOTOR",
           "market":"MY","incidentDate":"2026-01-01",
           "description":"Rear bumper damage","claimedAmount":1200.00}
          """.formatted(UUID.randomUUID());
        String body = mvc.perform(post("/api/claims").contentType(MediaType.APPLICATION_JSON)
                        .content(payload)).andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("SUBMITTED"))
                .andReturn().getResponse().getContentAsString();
        return json.readTree(body).get("id").asLong();
    }

    @Test void createsAndRetrievesClaim() throws Exception {
        long id = newClaim();
        mvc.perform(get("/api/claims/" + id)).andExpect(status().isOk())
                .andExpect(jsonPath("$.currency").value("MYR"));
        mvc.perform(get("/api/claims/" + id + "/history")).andExpect(status().isOk())
                .andExpect(jsonPath("$[0].action").value("SUBMITTED"));
    }

    @Test void rejectsInvalidAmount() throws Exception {
        mvc.perform(post("/api/claims").contentType(MediaType.APPLICATION_JSON)
                .content("""
              {"claimantId":"x","claimantName":"Demo","claimType":"MOTOR","market":"MY",
               "incidentDate":"2026-01-01","description":"Incident","claimedAmount":0}
              """)).andExpect(status().isBadRequest());
    }

    @Test void enforcesWorkflowAndOfficer() throws Exception {
        long id = newClaim();
        mvc.perform(post("/api/claims/" + id + "/decision").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"officerId\":\"officer-001\",\"approved\":true,\"approvedAmount\":500,\"reason\":\"Ok\"}"))
                .andExpect(status().isConflict());
        mvc.perform(post("/api/claims/" + id + "/assign").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"officerId\":\"officer-001\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("IN_REVIEW"));
        mvc.perform(post("/api/claims/" + id + "/decision").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"officerId\":\"officer-999\",\"approved\":false,\"reason\":\"No\"}"))
                .andExpect(status().isConflict());
        mvc.perform(post("/api/claims/" + id + "/decision").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"officerId\":\"officer-001\",\"approved\":true,\"approvedAmount\":500,\"reason\":\"Valid\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("APPROVED"));
        mvc.perform(post("/api/claims/" + id + "/settle").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"officerId\":\"officer-001\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("SETTLED"));
        mvc.perform(post("/api/claims/" + id + "/settle").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"officerId\":\"officer-001\"}"))
                .andExpect(status().isConflict());
    }

    @Test void informationRoundTrip() throws Exception {
        long id = newClaim();
        mvc.perform(post("/api/claims/" + id + "/assign").contentType(MediaType.APPLICATION_JSON)
                .content("{\"officerId\":\"officer-001\"}")).andExpect(status().isOk());
        mvc.perform(post("/api/claims/" + id + "/request-info").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"officerId\":\"officer-001\",\"message\":\"Provide photo\"}"))
                .andExpect(jsonPath("$.status").value("NEEDS_INFORMATION"));
        mvc.perform(post("/api/claims/" + id + "/information").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"Photo reference supplied\"}"))
                .andExpect(jsonPath("$.status").value("IN_REVIEW"));
    }

    @Test void dashboardDoesNotMixCurrencies() throws Exception {
        JsonNode result = json.readTree(mvc.perform(get("/api/dashboard"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
        assertThat(result.get("exposureByMarket").isArray()).isTrue();
        assertThat(result.get("totalClaims").asInt()).isGreaterThan(0);
    }
}
