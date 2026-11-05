package com.helpinghands.infrastructure.web.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.helpinghands.core.domain.ApplicationStatus;
import com.helpinghands.core.exception.BusinessRuleException;
import com.helpinghands.core.exception.ConflictException;
import com.helpinghands.core.pagination.PageQuery;
import com.helpinghands.core.pagination.PageResult;
import com.helpinghands.core.port.in.ManageApplicationsUseCase;
import com.helpinghands.core.port.in.dto.ApplicationResponse;
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

@WebMvcTest(ApplicationController.class)
@Import(WebMvcSecurityTestConfig.class)
@WithMockUser(roles = "ADMIN")
class ApplicationControllerTest {

    private static final String BODY = """
            {"volunteerName":"Vera","volunteerEmail":"vera@mail.com","volunteerPhone":"1199","motivation":"I care"}
            """;

    @Autowired private MockMvc mockMvc;
    @MockBean private ManageApplicationsUseCase applicationService;

    private ApplicationResponse response(ApplicationStatus status) {
        return new ApplicationResponse(5L, 1L, "Vera", "vera@mail.com", "1199", "I care", status, Instant.now());
    }

    @Test
    void shouldApply() throws Exception {
        when(applicationService.apply(eq(1L), any(), any())).thenReturn(response(ApplicationStatus.PENDING));

        mockMvc.perform(post("/api/opportunities/1/applications").contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    void shouldPassTheIdempotencyKeyHeaderToTheUseCase() throws Exception {
        when(applicationService.apply(eq(1L), any(), eq("retry-7"))).thenReturn(response(ApplicationStatus.PENDING));

        mockMvc.perform(post("/api/opportunities/1/applications").header("Idempotency-Key", "retry-7")
                        .contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isCreated());

        verify(applicationService).apply(eq(1L), any(), eq("retry-7"));
    }

    @Test
    void shouldRejectInvalidApplicationPayload() throws Exception {
        mockMvc.perform(post("/api/opportunities/1/applications").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"volunteerName\":\"\",\"volunteerEmail\":\"bad\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturnConflictForDuplicateApplication() throws Exception {
        when(applicationService.apply(eq(1L), any(), any())).thenThrow(new ConflictException("already applied"));

        mockMvc.perform(post("/api/opportunities/1/applications").contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isConflict());
    }

    @Test
    void shouldListApplicationsFilteredByStatus() throws Exception {
        when(applicationService.listByOpportunity(eq(1L), eq(ApplicationStatus.PENDING), any(PageQuery.class)))
                .thenReturn(new PageResult<>(List.of(response(ApplicationStatus.PENDING)), 0, 20, 1, 1));

        mockMvc.perform(get("/api/opportunities/1/applications?status=PENDING"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].volunteerName").value("Vera"));
    }

    @Test
    void shouldApproveRejectAndWithdraw() throws Exception {
        when(applicationService.approve(5L)).thenReturn(response(ApplicationStatus.APPROVED));
        when(applicationService.reject(5L)).thenReturn(response(ApplicationStatus.REJECTED));
        when(applicationService.withdraw(5L)).thenReturn(response(ApplicationStatus.WITHDRAWN));

        mockMvc.perform(post("/api/applications/5/approve")).andExpect(jsonPath("$.status").value("APPROVED"));
        mockMvc.perform(post("/api/applications/5/reject")).andExpect(jsonPath("$.status").value("REJECTED"));
        mockMvc.perform(post("/api/applications/5/withdraw")).andExpect(jsonPath("$.status").value("WITHDRAWN"));
    }

    @Test
    void shouldReturnUnprocessableEntityWhenNoSlotsAvailable() throws Exception {
        when(applicationService.approve(5L)).thenThrow(new BusinessRuleException("No slots available"));

        mockMvc.perform(post("/api/applications/5/approve"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.detail").value("No slots available"));
    }
}
