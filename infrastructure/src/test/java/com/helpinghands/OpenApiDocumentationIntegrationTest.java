package com.helpinghands;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class OpenApiDocumentationIntegrationTest extends AbstractPostgresIntegrationTest {


    @Test
    void shouldExposeOpenApiDescriptionWithInfoTagsAndPaths() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.info.title").value("Helping Hands API"))
                .andExpect(jsonPath("$.tags[?(@.name=='Campaigns')]").exists())
                .andExpect(jsonPath("$.tags[?(@.name=='Volunteer opportunities')]").exists())
                .andExpect(jsonPath("$.paths['/api/campaigns/{id}'].get.summary").value("Get a campaign"))
                .andExpect(jsonPath("$.paths['/api/organizations/{organizationId}/analytics/donations-by-month']").exists());
    }

    @Test
    void shouldServeSwaggerUi() throws Exception {
        mockMvc.perform(get("/swagger-ui/index.html")).andExpect(status().isOk());
        mockMvc.perform(get("/swagger-ui.html")).andExpect(status().is3xxRedirection());
    }

    @Test
    void shouldReturnRequestIdAndProblemDetailForUnknownResources() throws Exception {
        mockMvc.perform(get("/api/campaigns/987654").header("X-Request-Id", "trace-1"))
                .andExpect(status().isNotFound())
                .andExpect(header().string("X-Request-Id", "trace-1"))
                .andExpect(jsonPath("$.requestId").value("trace-1"))
                .andExpect(jsonPath("$.type").value("urn:helping-hands:problem:not-found"));
    }
}
