package com.helpinghands;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@AutoConfigureMockMvc
@ExtendWith(OutputCaptureExtension.class)
class VolunteeringIntegrationTest extends AbstractPostgresIntegrationTest {

    @Autowired private MockMvc mockMvc;

    private int organizationId;

    @BeforeEach
    void setUp() throws Exception {
        String body = mockMvc.perform(post("/api/organizations").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Org\",\"email\":\"" + UUID.randomUUID() + "@org.org\"}"))
                .andReturn().getResponse().getContentAsString();
        organizationId = JsonPath.read(body, "$.id");
    }

    private int createOpportunity(int slots, boolean remote) throws Exception {
        Instant startsAt = Instant.now().plus(5, ChronoUnit.DAYS);
        String body = mockMvc.perform(post("/api/organizations/" + organizationId + "/opportunities")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Beach clean-up\",\"description\":\"d\",\"location\":\"Santos\","
                                + "\"remote\":" + remote + ",\"startsAt\":\"" + startsAt + "\",\"endsAt\":\""
                                + startsAt.plus(3, ChronoUnit.HOURS) + "\",\"slots\":" + slots + "}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.id");
    }

    private int apply(int opportunityId, String email) throws Exception {
        String body = mockMvc.perform(post("/api/opportunities/" + opportunityId + "/applications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"volunteerName\":\"Vol\",\"volunteerEmail\":\"" + email + "\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.id");
    }

    @Test
    void shouldFillOpportunityAndReopenOnWithdrawal(CapturedOutput output) throws Exception {
        int opportunityId = createOpportunity(1, false);
        int first = apply(opportunityId, "first@mail.com");
        int second = apply(opportunityId, "second@mail.com");

        mockMvc.perform(post("/api/applications/" + first + "/approve"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"));
        mockMvc.perform(get("/api/opportunities/" + opportunityId))
                .andExpect(jsonPath("$.status").value("FULL"))
                .andExpect(jsonPath("$.availableSlots").value(0));
        mockMvc.perform(post("/api/applications/" + second + "/approve")).andExpect(status().isUnprocessableEntity());

        assertThat(output.getOut()).contains("Volunteer application " + first + " approved")
                .contains("Opportunity " + opportunityId + " of organization " + organizationId + " is now filled");

        mockMvc.perform(post("/api/applications/" + first + "/withdraw"))
                .andExpect(jsonPath("$.status").value("WITHDRAWN"));
        mockMvc.perform(get("/api/opportunities/" + opportunityId))
                .andExpect(jsonPath("$.status").value("OPEN"))
                .andExpect(jsonPath("$.availableSlots").value(1));
        mockMvc.perform(post("/api/applications/" + second + "/approve")).andExpect(status().isOk());
    }

    @Test
    void shouldRejectDuplicateApplicationAndApplicationsToClosedOpportunity() throws Exception {
        int opportunityId = createOpportunity(2, true);
        apply(opportunityId, "dup@mail.com");

        mockMvc.perform(post("/api/opportunities/" + opportunityId + "/applications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"volunteerName\":\"Vol\",\"volunteerEmail\":\"DUP@mail.com\"}"))
                .andExpect(status().isConflict());

        mockMvc.perform(post("/api/opportunities/" + opportunityId + "/close"))
                .andExpect(jsonPath("$.status").value("CLOSED"));
        mockMvc.perform(post("/api/opportunities/" + opportunityId + "/applications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"volunteerName\":\"Late\",\"volunteerEmail\":\"late@mail.com\"}"))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void shouldListAndFilterOpportunitiesAndApplications() throws Exception {
        int remoteOne = createOpportunity(2, true);
        createOpportunity(2, false);
        int pending = apply(remoteOne, "p@mail.com");
        int rejected = apply(remoteOne, "r@mail.com");
        mockMvc.perform(post("/api/applications/" + rejected + "/reject")).andExpect(status().isOk());

        mockMvc.perform(get("/api/opportunities?organizationId=" + organizationId + "&remote=true&upcoming=true"))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].id").value(remoteOne));
        mockMvc.perform(get("/api/opportunities?organizationId=" + organizationId))
                .andExpect(jsonPath("$.totalElements").value(2));

        mockMvc.perform(get("/api/opportunities/" + remoteOne + "/applications?status=PENDING"))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].id").value(pending));
        mockMvc.perform(get("/api/opportunities/" + remoteOne + "/applications?size=1"))
                .andExpect(jsonPath("$.totalPages").value(2));
    }

    @Test
    void shouldCancelOpportunity() throws Exception {
        int opportunityId = createOpportunity(2, true);

        mockMvc.perform(post("/api/opportunities/" + opportunityId + "/cancel"))
                .andExpect(jsonPath("$.status").value("CANCELLED"));
        mockMvc.perform(post("/api/opportunities/" + opportunityId + "/cancel"))
                .andExpect(status().isUnprocessableEntity());
        mockMvc.perform(get("/api/opportunities/999999")).andExpect(status().isNotFound());
    }
}
