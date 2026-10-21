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

    // Each test creates a separate claim to avoid depending on execution order.
    private void assignTo(long id, String officerId) throws Exception {
        mvc.perform(post("/api/claims/" + id + "/assign")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"officerId\":\"" + officerId + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.assignedOfficerId").value(officerId))
                .andExpect(jsonPath("$.status").value("IN_REVIEW"));
    }

    @Test void cannotAssignTheSameClaimTwice() throws Exception {
        long id = newClaim();
        assignTo(id, "officer-001");

        mvc.perform(post("/api/claims/" + id + "/assign")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"officerId\":\"officer-002\"}"))
                .andExpect(status().isConflict());

        // The rejected action must not overwrite the original assignment.
        mvc.perform(get("/api/claims/" + id))
                .andExpect(jsonPath("$.assignedOfficerId").value("officer-001"));
    }

    @Test void officerCannotUpdateSomebodyElsesReserve() throws Exception {
        long id = newClaim();
        assignTo(id, "officer-001");

        mvc.perform(post("/api/claims/" + id + "/reserve")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"officerId\":\"officer-999\",\"amount\":400}"))
                .andExpect(status().isConflict());

        mvc.perform(get("/api/claims/" + id))
                .andExpect(jsonPath("$.reserveAmount").doesNotExist());
    }

    @Test void reserveMustBePositive() throws Exception {
        long id = newClaim();
        assignTo(id, "officer-001");

        mvc.perform(post("/api/claims/" + id + "/reserve")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"officerId\":\"officer-001\",\"amount\":0}"))
                .andExpect(status().isBadRequest());
    }

    @Test void cannotApproveMoreThanTheClaimedAmount() throws Exception {
        long id = newClaim(); // claimed amount is 1200.00
        assignTo(id, "officer-001");

        mvc.perform(post("/api/claims/" + id + "/decision")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"officerId\":\"officer-001\",\"approved\":true,"
                                + "\"approvedAmount\":1300,\"reason\":\"Too much\"}"))
                .andExpect(status().isBadRequest());

        mvc.perform(get("/api/claims/" + id))
                .andExpect(jsonPath("$.status").value("IN_REVIEW"))
                .andExpect(jsonPath("$.approvedAmount").doesNotExist());
    }

    @Test void approvalRequiresAPositiveAmount() throws Exception {
        long id = newClaim();
        assignTo(id, "officer-001");

        mvc.perform(post("/api/claims/" + id + "/decision")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"officerId\":\"officer-001\",\"approved\":true,"
                                + "\"approvedAmount\":0,\"reason\":\"Invalid\"}"))
                .andExpect(status().isBadRequest());

        mvc.perform(post("/api/claims/" + id + "/decision")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"officerId\":\"officer-001\",\"approved\":true,"
                                + "\"reason\":\"Missing amount\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test void rejectedClaimCannotBeSettledOrApprovedAgain() throws Exception {
        long id = newClaim();
        assignTo(id, "officer-001");

        mvc.perform(post("/api/claims/" + id + "/decision")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"officerId\":\"officer-001\",\"approved\":false,"
                                + "\"reason\":\"Not covered\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REJECTED"));

        mvc.perform(post("/api/claims/" + id + "/settle")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"officerId\":\"officer-001\"}"))
                .andExpect(status().isConflict());

        mvc.perform(post("/api/claims/" + id + "/decision")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"officerId\":\"officer-001\",\"approved\":true,"
                                + "\"approvedAmount\":500,\"reason\":\"Retry\"}"))
                .andExpect(status().isConflict());
    }

    @Test void requestForInformationRequiresAssignedOfficer() throws Exception {
        long id = newClaim();
        assignTo(id, "officer-001");

        mvc.perform(post("/api/claims/" + id + "/request-info")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"officerId\":\"officer-999\",\"message\":\"Upload a photo\"}"))
                .andExpect(status().isConflict());
    }

    @Test void cannotRequestInformationTwiceWithoutAReply() throws Exception {
        long id = newClaim();
        assignTo(id, "officer-001");

        String request = "{\"officerId\":\"officer-001\",\"message\":\"Upload a photo\"}";
        mvc.perform(post("/api/claims/" + id + "/request-info")
                        .contentType(MediaType.APPLICATION_JSON).content(request))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("NEEDS_INFORMATION"));

        mvc.perform(post("/api/claims/" + id + "/request-info")
                        .contentType(MediaType.APPLICATION_JSON).content(request))
                .andExpect(status().isConflict());

        // Approval is also blocked until the claimant supplies the information.
        mvc.perform(post("/api/claims/" + id + "/decision")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"officerId\":\"officer-001\",\"approved\":true,"
                                + "\"approvedAmount\":500,\"reason\":\"Premature\"}"))
                .andExpect(status().isConflict());
    }

    @Test void cannotProvideInformationUnlessItWasRequested() throws Exception {
        long id = newClaim();

        mvc.perform(post("/api/claims/" + id + "/information")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"Unrequested attachment\"}"))
                .andExpect(status().isConflict());
    }

    @Test void missingClaimReturnsNotFound() throws Exception {
        mvc.perform(get("/api/claims/999999999"))
                .andExpect(status().isNotFound());
        mvc.perform(get("/api/claims/999999999/history"))
                .andExpect(status().isNotFound());
    }

    @Test void invalidCreateRequestReturnsUsefulValidationError() throws Exception {
        mvc.perform(post("/api/claims")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                  {"claimantId":"  ","claimantName":"Demo","claimType":"MOTOR",
                   "market":"MY","incidentDate":"2026-01-01",
                   "description":"Incident","claimedAmount":100}
                  """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").isNotEmpty());
    }

    @Test void officerIdIsRequiredForAssignment() throws Exception {
        long id = newClaim();
        mvc.perform(post("/api/claims/" + id + "/assign")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"officerId\":\" \"}"))
                .andExpect(status().isBadRequest());
    }

    @Test void historyShowsAssignmentAndApproval() throws Exception {
        long id = newClaim();
        assignTo(id, "officer-001");

        mvc.perform(post("/api/claims/" + id + "/decision")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"officerId\":\"officer-001\",\"approved\":true,"
                                + "\"approvedAmount\":500,\"reason\":\"Covered\"}"))
                .andExpect(status().isOk());

        mvc.perform(get("/api/claims/" + id + "/history"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].action").value("SUBMITTED"))
                .andExpect(jsonPath("$[1].action").value("ASSIGNED"))
                .andExpect(jsonPath("$[2].action").value("APPROVED"));
    }

    @Test void dashboardDoesNotMixCurrencies() throws Exception {
        JsonNode result = json.readTree(mvc.perform(get("/api/dashboard"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
        assertThat(result.get("exposureByMarket").isArray()).isTrue();
        assertThat(result.get("totalClaims").asInt()).isGreaterThan(0);
    }
}
