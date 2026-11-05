package com.helpinghands.infrastructure.web.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.helpinghands.core.domain.OpportunityStatus;
import com.helpinghands.core.exception.BusinessRuleException;
import com.helpinghands.core.exception.ResourceNotFoundException;
import com.helpinghands.core.pagination.PageQuery;
import com.helpinghands.core.pagination.PageResult;
import com.helpinghands.core.pagination.SortDirection;
import com.helpinghands.core.port.in.ManageOpportunitiesUseCase;
import com.helpinghands.core.port.in.dto.OpportunityResponse;
import com.helpinghands.core.port.in.dto.OpportunitySearchCriteria;
import java.time.Instant;
import java.util.List;
import com.helpinghands.infrastructure.security.WebMvcSecurityTestConfig;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(OpportunityController.class)
@Import(WebMvcSecurityTestConfig.class)
@WithMockUser(roles = "ADMIN")
class OpportunityControllerTest {

    private static final String BODY = """
            {"title":"Tutoring","description":"d","location":"School","remote":false,
             "startsAt":"2030-01-01T10:00:00Z","endsAt":"2030-01-01T12:00:00Z","slots":3}
            """;

    @Autowired private MockMvc mockMvc;
    @MockBean private ManageOpportunitiesUseCase opportunityService;

    private OpportunityResponse response(OpportunityStatus status) {
        return new OpportunityResponse(1L, 2L, "Tutoring", "d", "School", false, Instant.parse("2030-01-01T10:00:00Z"),
                Instant.parse("2030-01-01T12:00:00Z"), 3, 0, 3, status);
    }

    @Test
    void shouldCreateOpportunity() throws Exception {
        when(opportunityService.create(eq(2L), any())).thenReturn(response(OpportunityStatus.OPEN));

        mockMvc.perform(post("/api/organizations/2/opportunities").contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("OPEN"))
                .andExpect(jsonPath("$.availableSlots").value(3));
    }

    @Test
    void shouldRejectInvalidPayload() throws Exception {
        mockMvc.perform(post("/api/organizations/2/opportunities").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"\",\"slots\":0}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturnUnprocessableEntityForDomainViolation() throws Exception {
        when(opportunityService.create(eq(2L), any())).thenThrow(new BusinessRuleException("Start date must be in the future"));

        mockMvc.perform(post("/api/organizations/2/opportunities").contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void shouldFindOpportunity() throws Exception {
        when(opportunityService.findById(1L)).thenReturn(response(OpportunityStatus.OPEN));

        mockMvc.perform(get("/api/opportunities/1")).andExpect(status().isOk()).andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void shouldReturnNotFoundForUnknownOpportunity() throws Exception {
        when(opportunityService.findById(9L)).thenThrow(new ResourceNotFoundException("Volunteer opportunity", 9L));

        mockMvc.perform(get("/api/opportunities/9")).andExpect(status().isNotFound());
    }

    @Test
    void shouldSearchWithFilters() throws Exception {
        var criteria = new OpportunitySearchCriteria(2L, OpportunityStatus.OPEN, true, true);
        when(opportunityService.search(criteria, new PageQuery(0, 20, "startsAt", SortDirection.ASC)))
                .thenReturn(new PageResult<>(List.of(response(OpportunityStatus.OPEN)), 0, 20, 1, 1));

        mockMvc.perform(get("/api/opportunities?organizationId=2&status=OPEN&remote=true&upcoming=true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].title").value("Tutoring"));
    }

    @Test
    void shouldCloseAndCancel() throws Exception {
        when(opportunityService.close(1L)).thenReturn(response(OpportunityStatus.CLOSED));
        when(opportunityService.cancel(1L)).thenReturn(response(OpportunityStatus.CANCELLED));

        mockMvc.perform(post("/api/opportunities/1/close")).andExpect(jsonPath("$.status").value("CLOSED"));
        mockMvc.perform(post("/api/opportunities/1/cancel")).andExpect(jsonPath("$.status").value("CANCELLED"));
    }
}
