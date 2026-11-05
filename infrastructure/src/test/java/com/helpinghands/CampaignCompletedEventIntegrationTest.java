package com.helpinghands;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.http.MediaType;

@ExtendWith(OutputCaptureExtension.class)
class CampaignCompletedEventIntegrationTest extends AbstractPostgresIntegrationTest {


    @Test
    void shouldPublishCampaignCompletedWhenGoalIsReached(CapturedOutput output) throws Exception {
        String organization = mockMvc.perform(post("/api/organizations").with(admin()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Org\",\"email\":\"" + UUID.randomUUID() + "@org.org\"}"))
                .andReturn().getResponse().getContentAsString();
        int organizationId = JsonPath.read(organization, "$.id");
        String campaign = mockMvc.perform(post("/api/organizations/" + organizationId + "/campaigns").with(admin())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"title\":\"Small goal\",\"goalAmount\":20}"))
                .andReturn().getResponse().getContentAsString();
        int campaignId = JsonPath.read(campaign, "$.id");

        mockMvc.perform(post("/api/campaigns/" + campaignId + "/donations").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"donorName\":\"D\",\"donorEmail\":\"d@mail.com\",\"amount\":20,\"paymentMethod\":\"PIX\"}"))
                .andExpect(status().isCreated());

        assertThat(output.getOut()).contains("Campaign " + campaignId + " of organization " + organizationId + " completed");
    }
}
