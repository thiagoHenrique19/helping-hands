package com.helpinghands.infrastructure.web.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.helpinghands.core.exception.ConflictException;
import com.helpinghands.core.exception.ResourceNotFoundException;
import com.helpinghands.core.pagination.PageQuery;
import com.helpinghands.core.pagination.PageResult;
import com.helpinghands.core.pagination.SortDirection;
import com.helpinghands.core.port.in.ListOrganizationsUseCase;
import com.helpinghands.core.port.in.RegisterOrganizationUseCase;
import com.helpinghands.core.port.in.UpdateOrganizationUseCase;
import com.helpinghands.core.port.in.VerifyOrganizationUseCase;
import com.helpinghands.core.port.in.dto.OrganizationResponse;
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

@WebMvcTest(OrganizationController.class)
@Import(WebMvcSecurityTestConfig.class)
@WithMockUser(roles = "ADMIN")
class OrganizationControllerTest {

    private static final String BODY = """
            {"name":"Hope NGO","email":"hope@ngo.org","description":"Food","phone":"1199","website":"https://hope.org"}
            """;

    @Autowired private MockMvc mockMvc;
    @MockBean private RegisterOrganizationUseCase registerOrganization;
    @MockBean private UpdateOrganizationUseCase updateOrganization;
    @MockBean private VerifyOrganizationUseCase verifyOrganization;
    @MockBean private ListOrganizationsUseCase listOrganizations;

    private OrganizationResponse response(boolean verified) {
        return new OrganizationResponse(1L, "Hope NGO", "hope@ngo.org", "Food", "1199", "https://hope.org", verified);
    }

    @Test
    void shouldRegisterOrganization() throws Exception {
        when(registerOrganization.register(any(), any())).thenReturn(response(false));

        mockMvc.perform(post("/api/organizations").contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.verified").value(false));
    }

    @Test
    void shouldReturnConflictForDuplicateEmail() throws Exception {
        when(registerOrganization.register(any(), any())).thenThrow(new ConflictException("already exists"));

        mockMvc.perform(post("/api/organizations").contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isConflict());
    }

    @Test
    void shouldRejectInvalidWebsite() throws Exception {
        mockMvc.perform(post("/api/organizations").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"A\",\"email\":\"a@a.org\",\"website\":\"ftp://nope\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldUpdateOrganization() throws Exception {
        when(updateOrganization.update(eq(1L), any())).thenReturn(response(false));

        mockMvc.perform(put("/api/organizations/1").contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Hope NGO"));
    }

    @Test
    void shouldVerifyOrganization() throws Exception {
        when(verifyOrganization.verify(1L)).thenReturn(response(true));

        mockMvc.perform(post("/api/organizations/1/verify"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verified").value(true));
    }

    @Test
    void shouldReturnNotFoundWhenVerifyingUnknownOrganization() throws Exception {
        when(verifyOrganization.verify(9L)).thenThrow(new ResourceNotFoundException("Organization", 9L));

        mockMvc.perform(post("/api/organizations/9/verify")).andExpect(status().isNotFound());
    }

    @Test
    void shouldListOrganizationsPaginated() throws Exception {
        when(listOrganizations.search(eq("hope"), eq(new PageQuery(1, 5, "name", SortDirection.DESC))))
                .thenReturn(new PageResult<>(List.of(response(false)), 1, 5, 6, 2));

        mockMvc.perform(get("/api/organizations?name=hope&page=1&size=5&sortBy=name&direction=DESC"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].name").value("Hope NGO"))
                .andExpect(jsonPath("$.totalElements").value(6))
                .andExpect(jsonPath("$.totalPages").value(2));
    }

    @Test
    void shouldReturnBadRequestForInvalidPageSize() throws Exception {
        mockMvc.perform(get("/api/organizations?size=1000")).andExpect(status().isBadRequest());
    }
}
