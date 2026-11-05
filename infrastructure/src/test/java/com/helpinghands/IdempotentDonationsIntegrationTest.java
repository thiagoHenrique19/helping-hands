package com.helpinghands;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.ResultActions;

class IdempotentDonationsIntegrationTest extends AbstractPostgresIntegrationTest {

    @Autowired private JdbcTemplate jdbc;

    private int campaignId;
    private int otherCampaignId;

    @BeforeEach
    void setUp() throws Exception {
        String organization = mockMvc.perform(post("/api/organizations").with(admin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Org\",\"email\":\"" + UUID.randomUUID() + "@org.org\"}"))
                .andReturn().getResponse().getContentAsString();
        int organizationId = JsonPath.read(organization, "$.id");
        campaignId = createCampaign(organizationId);
        otherCampaignId = createCampaign(organizationId);
    }

    private int createCampaign(int organizationId) throws Exception {
        String body = mockMvc.perform(post("/api/organizations/" + organizationId + "/campaigns").with(admin())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"title\":\"Goal\",\"goalAmount\":1000}"))
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.id");
    }

    private ResultActions donate(int campaign, String key, String amount) throws Exception {
        var request = post("/api/campaigns/" + campaign + "/donations").contentType(MediaType.APPLICATION_JSON)
                .content("{\"donorName\":\"D\",\"donorEmail\":\"d@mail.com\",\"amount\":" + amount
                        + ",\"paymentMethod\":\"PIX\"}");
        if (key != null) {
            request.header("Idempotency-Key", key);
        }
        return mockMvc.perform(request);
    }

    private int donations(int campaign) {
        return jdbc.queryForObject("SELECT count(*) FROM donation WHERE campaign_id = ?", Integer.class, campaign);
    }

    @Test
    void shouldReturnTheOriginalResponseForTheSameKeyAndPayload() throws Exception {
        String key = UUID.randomUUID().toString();
        String first = donate(campaignId, key, "25").andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        String second = donate(campaignId, key, "25.00").andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        assertThat(JsonPath.<String>read(second, "$.receiptCode")).isEqualTo(JsonPath.read(first, "$.receiptCode"));
        assertThat(JsonPath.<Integer>read(second, "$.id")).isEqualTo(JsonPath.<Integer>read(first, "$.id"));
        assertThat(donations(campaignId)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT raised_amount FROM campaign WHERE id = ?", java.math.BigDecimal.class,
                campaignId)).isEqualByComparingTo("25");
    }

    @Test
    void shouldRejectTheSameKeyWithADifferentPayload() throws Exception {
        String key = UUID.randomUUID().toString();
        donate(campaignId, key, "25").andExpect(status().isCreated());

        donate(campaignId, key, "26").andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.type").value("urn:helping-hands:problem:idempotency-key-reuse"));

        assertThat(donations(campaignId)).isEqualTo(1);
    }

    @Test
    void shouldCreateTwoDonationsWhenNoKeyIsSent() throws Exception {
        donate(campaignId, null, "25").andExpect(status().isCreated());
        donate(campaignId, null, "25").andExpect(status().isCreated());

        assertThat(donations(campaignId)).isEqualTo(2);
    }

    @Test
    void shouldTreatTheSameKeyOnAnotherCampaignAsANewRequest() throws Exception {
        String key = UUID.randomUUID().toString();
        donate(campaignId, key, "25").andExpect(status().isCreated());
        donate(otherCampaignId, key, "25").andExpect(status().isCreated());

        assertThat(donations(campaignId)).isEqualTo(1);
        assertThat(donations(otherCampaignId)).isEqualTo(1);
    }

    @Test
    void shouldRefuseMalformedKeys() throws Exception {
        donate(campaignId, "not valid", "25").andExpect(status().isBadRequest());
        donate(campaignId, "k".repeat(256), "25").andExpect(status().isBadRequest());
        assertThat(donations(campaignId)).isZero();
    }

    @Test
    void shouldLetAFailedRequestRetryWithTheSameKey() throws Exception {
        String key = UUID.randomUUID().toString();
        donate(campaignId, key, "0.50").andExpect(status().isUnprocessableEntity());

        donate(campaignId, key, "25").andExpect(status().isCreated());

        assertThat(donations(campaignId)).isEqualTo(1);
    }
}
