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

class HelpingHandsFlowIntegrationTest extends AbstractPostgresIntegrationTest {


    @Test
    void shouldRunFullDonationFlow() throws Exception {
        String organizationBody = mockMvc.perform(post("/api/organizations").with(admin()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Hope NGO\",\"email\":\"" + UUID.randomUUID() + "@ngo.org\",\"description\":\"Food aid\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        int organizationId = JsonPath.read(organizationBody, "$.id");

        String campaignBody = mockMvc.perform(post("/api/organizations/" + organizationId + "/campaigns").with(admin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"Food baskets","description":"500 baskets","goalAmount":100}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andReturn().getResponse().getContentAsString();
        int campaignId = JsonPath.read(campaignBody, "$.id");

        mockMvc.perform(post("/api/campaigns/" + campaignId + "/donations").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"donorName":"Dave","donorEmail":"dave@mail.com","amount":100,"paymentMethod":"PIX"}
                                """))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/campaigns/" + campaignId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.progressPercentage").value(100.0));
    }
}
