package com.helpinghands;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@AutoConfigureMockMvc
class DonationEnhancementsIntegrationTest extends AbstractPostgresIntegrationTest {

    @Autowired private MockMvc mockMvc;

    private int campaignId;

    @BeforeEach
    void setUp() throws Exception {
        String organization = mockMvc.perform(post("/api/organizations").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Org\",\"email\":\"" + UUID.randomUUID() + "@org.org\"}"))
                .andReturn().getResponse().getContentAsString();
        int organizationId = JsonPath.read(organization, "$.id");
        String campaign = mockMvc.perform(post("/api/organizations/" + organizationId + "/campaigns")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Big goal\",\"goalAmount\":1000000}"))
                .andReturn().getResponse().getContentAsString();
        campaignId = JsonPath.read(campaign, "$.id");
    }

    private String donate(String name, String email, String amount, boolean anonymous, String method) throws Exception {
        return mockMvc.perform(post("/api/campaigns/" + campaignId + "/donations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"donorName\":\"" + name + "\",\"donorEmail\":\"" + email + "\",\"amount\":" + amount
                                + ",\"anonymous\":" + anonymous + ",\"message\":\"msg " + name
                                + "\",\"paymentMethod\":\"" + method + "\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
    }

    @Test
    void shouldIssueIncreasingReceiptCodes() throws Exception {
        String first = donate("Ann", "ann@mail.com", "10", false, "PIX");
        String second = donate("Ann", "ann@mail.com", "10", false, "BOLETO");

        String firstCode = JsonPath.read(first, "$.receiptCode");
        String secondCode = JsonPath.read(second, "$.receiptCode");
        org.assertj.core.api.Assertions.assertThat(firstCode).matches("DON-\\d{8}");
        org.assertj.core.api.Assertions.assertThat(secondCode).isGreaterThan(firstCode);
    }

    @Test
    void shouldRejectDonationBelowMinimum() throws Exception {
        mockMvc.perform(post("/api/campaigns/" + campaignId + "/donations").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"donorName\":\"Cheap\",\"donorEmail\":\"c@mail.com\",\"amount\":0.50,\"paymentMethod\":\"PIX\"}"))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void shouldMaskAnonymousDonorsInPublicListing() throws Exception {
        donate("Visible", "visible@mail.com", "10", false, "PIX");
        donate("Hidden", "hidden@mail.com", "20", true, "CREDIT_CARD");

        mockMvc.perform(get("/api/campaigns/" + campaignId + "/donations?sortBy=amount&direction=DESC"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.content[0].donorName").value("Anonymous"))
                .andExpect(jsonPath("$.content[0].message").value("msg Hidden"))
                .andExpect(jsonPath("$.content[0].donorEmail").doesNotExist())
                .andExpect(jsonPath("$.content[1].donorName").value("Visible"));
    }

    @Test
    void shouldListTopDonorsExcludingAnonymous() throws Exception {
        donate("Ann", "ann@mail.com", "50", false, "PIX");
        donate("Ann", "ann@mail.com", "25.50", false, "PIX");
        donate("Bob", "bob@mail.com", "60", false, "BOLETO");
        donate("Secret", "secret@mail.com", "5000", true, "PIX");

        mockMvc.perform(get("/api/campaigns/" + campaignId + "/top-donors?limit=2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].donorName").value("Ann"))
                .andExpect(jsonPath("$[0].totalAmount").value(75.5))
                .andExpect(jsonPath("$[0].donationsCount").value(2))
                .andExpect(jsonPath("$[1].donorName").value("Bob"));

        mockMvc.perform(get("/api/campaigns/" + campaignId + "/top-donors?limit=0")).andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/campaigns/999999/top-donors")).andExpect(status().isNotFound());
    }
}
