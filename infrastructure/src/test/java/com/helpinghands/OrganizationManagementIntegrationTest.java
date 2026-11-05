package com.helpinghands;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.helpinghands.core.port.in.ExpireCampaignsUseCase;
import com.jayway.jsonpath.JsonPath;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

@AutoConfigureMockMvc
class OrganizationManagementIntegrationTest extends AbstractPostgresIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private ExpireCampaignsUseCase expireCampaignsUseCase;

    private int createOrganization(String name, String email) throws Exception {
        String body = mockMvc.perform(post("/api/organizations").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"" + name + "\",\"email\":\"" + email + "\",\"phone\":\"1199\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.id");
    }

    private int createCampaign(int organizationId, String json) throws Exception {
        String body = mockMvc.perform(post("/api/organizations/" + organizationId + "/campaigns")
                        .contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.id");
    }

    @Test
    void shouldRejectDuplicateEmailIgnoringCase() throws Exception {
        String email = UUID.randomUUID() + "@dup.org";
        createOrganization("First", email);

        mockMvc.perform(post("/api/organizations").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Second\",\"email\":\"" + email.toUpperCase() + "\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    void shouldUpdateAndVerifyOrganization() throws Exception {
        int id = createOrganization("Before", UUID.randomUUID() + "@org.org");

        mockMvc.perform(put("/api/organizations/" + id).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"After\",\"email\":\"" + UUID.randomUUID() + "@org.org\",\"website\":\"https://after.org\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("After"))
                .andExpect(jsonPath("$.website").value("https://after.org"));

        mockMvc.perform(post("/api/organizations/" + id + "/verify"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verified").value(true));
        mockMvc.perform(post("/api/organizations/" + id + "/verify")).andExpect(status().isUnprocessableEntity());
    }

    @Test
    void shouldListOrganizationsFilteringByName() throws Exception {
        String token = "srch" + UUID.randomUUID().toString().substring(0, 8);
        createOrganization("Alpha " + token, UUID.randomUUID() + "@a.org");
        createOrganization("Beta " + token, UUID.randomUUID() + "@b.org");
        createOrganization("Unrelated", UUID.randomUUID() + "@c.org");

        mockMvc.perform(get("/api/organizations?name=" + token.toUpperCase() + "&size=1&sortBy=name"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].name").value("Alpha " + token))
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.totalPages").value(2));
    }

    @Test
    void shouldManageCampaignLifecycle() throws Exception {
        int organizationId = createOrganization("Lifecycle", UUID.randomUUID() + "@l.org");
        int campaignId = createCampaign(organizationId,
                "{\"title\":\"Clean river\",\"goalAmount\":1000,\"category\":\"ENVIRONMENT\"}");

        mockMvc.perform(post("/api/campaigns/" + campaignId + "/donations").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"donorName\":\"Ann\",\"donorEmail\":\"ann@mail.com\",\"amount\":600}"))
                .andExpect(status().isCreated());

        mockMvc.perform(put("/api/campaigns/" + campaignId).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Clean river\",\"goalAmount\":500}"))
                .andExpect(status().isUnprocessableEntity());

        mockMvc.perform(put("/api/campaigns/" + campaignId).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Clean the river\",\"goalAmount\":2000,\"category\":\"ENVIRONMENT\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Clean the river"))
                .andExpect(jsonPath("$.goalAmount").value(2000));

        mockMvc.perform(post("/api/campaigns/" + campaignId + "/cancel"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
        mockMvc.perform(put("/api/campaigns/" + campaignId).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"x\",\"goalAmount\":5000}"))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void shouldFilterAndSortCampaigns() throws Exception {
        int organizationId = createOrganization("Filters", UUID.randomUUID() + "@f.org");
        createCampaign(organizationId, "{\"title\":\"Beta animals\",\"goalAmount\":300,\"category\":\"ANIMALS\"}");
        createCampaign(organizationId, "{\"title\":\"Alpha animals\",\"goalAmount\":200,\"category\":\"ANIMALS\"}");
        createCampaign(organizationId, "{\"title\":\"Food drive\",\"goalAmount\":100,\"category\":\"FOOD\"}");

        mockMvc.perform(get("/api/campaigns?organizationId=" + organizationId
                        + "&category=ANIMALS&status=ACTIVE&sortBy=title&direction=ASC"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.content[0].title").value("Alpha animals"))
                .andExpect(jsonPath("$.content[1].title").value("Beta animals"));

        mockMvc.perform(get("/api/campaigns?organizationId=" + organizationId + "&title=food"))
                .andExpect(jsonPath("$.content.length()").value(1));

        mockMvc.perform(get("/api/campaigns?sortBy=unknownField")).andExpect(status().isBadRequest());
    }

    @Test
    void shouldExpireOverdueCampaignsAndRejectDonations() throws Exception {
        int organizationId = createOrganization("Deadline", UUID.randomUUID() + "@d.org");
        String endsAt = Instant.now().plus(1, ChronoUnit.DAYS).toString();
        int campaignId = createCampaign(organizationId,
                "{\"title\":\"Winter coats\",\"goalAmount\":100,\"endsAt\":\"" + endsAt + "\"}");
        jdbcTemplate.update("UPDATE campaign SET ends_at = now() - interval '1 minute' WHERE id = ?", campaignId);

        int expired = expireCampaignsUseCase.expireOverdueCampaigns();

        Assertions.assertThat(expired).isGreaterThanOrEqualTo(1);
        mockMvc.perform(get("/api/campaigns/" + campaignId))
                .andExpect(jsonPath("$.status").value("EXPIRED"));
        mockMvc.perform(post("/api/campaigns/" + campaignId + "/donations").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"donorName\":\"Late\",\"donorEmail\":\"late@mail.com\",\"amount\":10}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.message").value("Campaign has expired"));
    }
}
