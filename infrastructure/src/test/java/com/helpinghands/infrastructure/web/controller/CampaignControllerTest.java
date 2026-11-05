package com.helpinghands.infrastructure.web.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.helpinghands.core.domain.CampaignCategory;
import com.helpinghands.core.domain.CampaignStatus;
import com.helpinghands.core.exception.BusinessRuleException;
import com.helpinghands.core.pagination.PageQuery;
import com.helpinghands.core.pagination.PageResult;
import com.helpinghands.core.port.in.ManageCampaignsUseCase;
import com.helpinghands.core.port.in.dto.CampaignResponse;
import com.helpinghands.core.port.in.dto.CampaignSearchCriteria;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(CampaignController.class)
class CampaignControllerTest {

    private static final String BODY = """
            {"title":"Books","description":"d","goalAmount":100,"category":"EDUCATION"}
            """;

    @Autowired private MockMvc mockMvc;
    @MockBean private ManageCampaignsUseCase campaignService;

    private CampaignResponse response(CampaignStatus status) {
        return new CampaignResponse(1L, 2L, "Books", "d", new BigDecimal("100"), BigDecimal.ZERO,
                new BigDecimal("0.00"), status, CampaignCategory.EDUCATION, null);
    }

    @Test
    void shouldCreateCampaign() throws Exception {
        when(campaignService.create(eq(2L), any())).thenReturn(response(CampaignStatus.ACTIVE));

        mockMvc.perform(post("/api/organizations/2/campaigns").contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.category").value("EDUCATION"));
    }

    @Test
    void shouldUpdateCampaign() throws Exception {
        when(campaignService.update(eq(1L), any())).thenReturn(response(CampaignStatus.ACTIVE));

        mockMvc.perform(put("/api/campaigns/1").contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Books"));
    }

    @Test
    void shouldReturnUnprocessableEntityWhenUpdateViolatesRules() throws Exception {
        when(campaignService.update(eq(1L), any())).thenThrow(new BusinessRuleException("Only active campaigns can be updated"));

        mockMvc.perform(put("/api/campaigns/1").contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void shouldCancelCampaign() throws Exception {
        when(campaignService.cancel(1L)).thenReturn(response(CampaignStatus.CANCELLED));

        mockMvc.perform(post("/api/campaigns/1/cancel"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }

    @Test
    void shouldSearchCampaignsWithFiltersAndPagination() throws Exception {
        var criteria = new CampaignSearchCriteria(2L, CampaignStatus.ACTIVE, CampaignCategory.EDUCATION, "book");
        when(campaignService.search(eq(criteria), any(PageQuery.class)))
                .thenReturn(new PageResult<>(List.of(response(CampaignStatus.ACTIVE)), 0, 20, 1, 1));

        mockMvc.perform(get("/api/campaigns?organizationId=2&status=ACTIVE&category=EDUCATION&title=book"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(1))
                .andExpect(jsonPath("$.size").value(20));
    }

    @Test
    void shouldReturnBadRequestForUnknownStatus() throws Exception {
        mockMvc.perform(get("/api/campaigns?status=NOPE")).andExpect(status().isBadRequest());
    }
}
