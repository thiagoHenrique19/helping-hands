package com.helpinghands;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

class AnalyticsEndpointsIntegrationTest extends AbstractPostgresIntegrationTest {


    private int json(String url, String body, String path) throws Exception {
        String response = mockMvc.perform(post(url).with(admin()).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().is2xxSuccessful()).andReturn().getResponse().getContentAsString();
        return JsonPath.read(response, path);
    }

    @Test
    void shouldReportImpactAfterRealDonations() throws Exception {
        int organizationId = json("/api/organizations",
                "{\"name\":\"Impact\",\"email\":\"" + UUID.randomUUID() + "@org.org\"}", "$.id");
        int campaignId = json("/api/organizations/" + organizationId + "/campaigns",
                "{\"title\":\"Wells\",\"goalAmount\":100}", "$.id");
        json("/api/campaigns/" + campaignId + "/donations",
                "{\"donorName\":\"A\",\"donorEmail\":\"a@mail.com\",\"amount\":40,\"paymentMethod\":\"PIX\"}", "$.id");
        json("/api/campaigns/" + campaignId + "/donations",
                "{\"donorName\":\"B\",\"donorEmail\":\"b@mail.com\",\"amount\":60,\"paymentMethod\":\"BOLETO\"}", "$.id");

        mockMvc.perform(get("/api/organizations/" + organizationId + "/analytics/dashboard").with(admin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalRaised").value(100.0))
                .andExpect(jsonPath("$.donationsCount").value(2))
                .andExpect(jsonPath("$.uniqueDonors").value(2))
                .andExpect(jsonPath("$.completedCampaigns").value(1))
                .andExpect(jsonPath("$.activeCampaigns").value(0));

        String today = java.time.LocalDate.now(java.time.ZoneOffset.UTC).toString();
        mockMvc.perform(get("/api/organizations/" + organizationId + "/analytics/donations-by-month?from=" + today
                        + "&to=" + today).with(admin()))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].donationsCount").value(2))
                .andExpect(jsonPath("$[0].totalAmount").value(100.0));

        mockMvc.perform(get("/api/organizations/" + organizationId + "/analytics/campaign-progress").with(admin()))
                .andExpect(jsonPath("$[0].rank").value(1))
                .andExpect(jsonPath("$[0].progressPercentage").value(100.0));
        mockMvc.perform(get("/api/organizations/" + organizationId + "/analytics/payment-methods").with(admin()))
                .andExpect(jsonPath("$[0].paymentMethod").value("BOLETO"));
        mockMvc.perform(get("/api/analytics/summary").with(admin())).andExpect(status().isOk());
    }

    @Test
    void shouldValidateAnalyticsParameters() throws Exception {
        mockMvc.perform(get("/api/organizations/999999/analytics/dashboard").with(admin())).andExpect(status().isNotFound());
        mockMvc.perform(get("/api/analytics/top-campaigns?limit=0").with(admin())).andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/organizations/1/analytics/donations-by-month?from=2026-05-01&to=2026-01-01").with(admin()))
                .andExpect(status().isBadRequest());
    }
}
