package com.helpinghands;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.helpinghands.core.port.in.BootstrapAdminUseCase;
import com.jayway.jsonpath.JsonPath;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

class AuthenticationIntegrationTest extends AbstractPostgresIntegrationTest {

    private static final String PASSWORD = "very-secret-1";

    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private BootstrapAdminUseCase bootstrapAdmin;

    private String unique(String prefix) {
        return prefix + UUID.randomUUID().toString().substring(0, 8) + "@mail.com";
    }

    private void register(String email, String role) throws Exception {
        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"" + PASSWORD + "\",\"role\":\"" + role + "\"}"))
                .andExpect(status().isCreated());
    }

    private RequestPostProcessor login(String email) throws Exception {
        String body = mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"" + PASSWORD + "\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return bearer(JsonPath.read(body, "$.accessToken"));
    }

    private RequestPostProcessor organizationUser() throws Exception {
        String email = unique("ngo");
        register(email, "ORGANIZATION");
        return login(email);
    }

    private int createOrganization(RequestPostProcessor auth) throws Exception {
        String body = mockMvc.perform(post("/api/organizations").with(auth).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"NGO\",\"email\":\"" + unique("org") + "\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.id");
    }

    @Test
    void shouldRegisterAndLoginWithoutLeakingSecrets() throws Exception {
        String email = unique("jane");

        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email.toUpperCase() + "\",\"password\":\"" + PASSWORD + "\",\"role\":\"VOLUNTEER\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.role").value("VOLUNTEER"))
                .andExpect(jsonPath("$.passwordHash").doesNotExist());

        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"" + PASSWORD + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.accessToken").isNotEmpty());

        assertThat(jdbcTemplate.queryForObject("SELECT password_hash FROM app_user WHERE email = ?", String.class, email))
                .startsWith("$2").doesNotContain(PASSWORD);
    }

    @Test
    void shouldRejectDuplicateAdminAndWeakRegistrations() throws Exception {
        String email = unique("dup");
        register(email, "VOLUNTEER");

        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"" + PASSWORD + "\",\"role\":\"VOLUNTEER\"}"))
                .andExpect(status().isConflict());
        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + unique("adm") + "\",\"password\":\"" + PASSWORD + "\",\"role\":\"ADMIN\"}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + unique("weak") + "\",\"password\":\"short\",\"role\":\"VOLUNTEER\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldRejectInvalidCredentials() throws Exception {
        String email = unique("bob");
        register(email, "VOLUNTEER");

        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"wrong-password\"}"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"nobody@mail.com\",\"password\":\"" + PASSWORD + "\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldRejectInactiveUsersAndInvalidTokens() throws Exception {
        String email = unique("gone");
        register(email, "VOLUNTEER");
        jdbcTemplate.update("UPDATE app_user SET active = FALSE WHERE email = ?", email);

        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"" + PASSWORD + "\"}"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/campaigns/1/cancel").with(bearer("not.a.jwt")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldLetOrganizationOwnersManageOnlyTheirOwnResources() throws Exception {
        RequestPostProcessor ownerA = organizationUser();
        RequestPostProcessor ownerB = organizationUser();
        int organizationA = createOrganization(ownerA);
        int organizationB = createOrganization(ownerB);

        mockMvc.perform(post("/api/organizations").with(ownerA).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Second\",\"email\":\"" + unique("second") + "\"}"))
                .andExpect(status().isUnprocessableEntity());

        String campaign = mockMvc.perform(post("/api/organizations/" + organizationA + "/campaigns").with(ownerA)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"title\":\"Wells\",\"goalAmount\":100}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        int campaignId = JsonPath.read(campaign, "$.id");

        mockMvc.perform(post("/api/organizations/" + organizationA + "/campaigns").with(ownerB)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"title\":\"Hack\",\"goalAmount\":100}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(put("/api/campaigns/" + campaignId).with(ownerB).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Hacked\",\"goalAmount\":100}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(put("/api/campaigns/" + campaignId).with(ownerA).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Wells v2\",\"goalAmount\":200}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Wells v2"));

        mockMvc.perform(put("/api/organizations/" + organizationA).with(ownerB).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"x\",\"email\":\"" + unique("x") + "\"}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/organizations/" + organizationA + "/verify").with(ownerA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verified").value(true));

        mockMvc.perform(get("/api/organizations/" + organizationA + "/analytics/dashboard").with(ownerA))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/organizations/" + organizationA + "/analytics/dashboard").with(ownerB))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/organizations/" + organizationA + "/analytics/dashboard").with(admin()))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/analytics/summary").with(ownerA)).andExpect(status().isForbidden());
        mockMvc.perform(get("/api/analytics/summary").with(admin())).andExpect(status().isOk());

        mockMvc.perform(post("/api/campaigns/" + campaignId + "/cancel").with(ownerB)).andExpect(status().isForbidden());
        mockMvc.perform(post("/api/campaigns/" + campaignId + "/cancel").with(ownerA)).andExpect(status().isOk());
        assertThat(organizationB).isNotEqualTo(organizationA);
    }

    @Test
    void shouldKeepPublicEndpointsOpenAndProtectWrites() throws Exception {
        RequestPostProcessor owner = organizationUser();
        int organizationId = createOrganization(owner);
        String campaign = mockMvc.perform(post("/api/organizations/" + organizationId + "/campaigns").with(owner)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"title\":\"Open\",\"goalAmount\":100}"))
                .andReturn().getResponse().getContentAsString();
        int campaignId = JsonPath.read(campaign, "$.id");

        mockMvc.perform(get("/api/campaigns/" + campaignId)).andExpect(status().isOk());
        mockMvc.perform(get("/api/campaigns")).andExpect(status().isOk());
        mockMvc.perform(get("/api/organizations/" + organizationId)).andExpect(status().isOk());
        mockMvc.perform(get("/api/opportunities")).andExpect(status().isOk());
        mockMvc.perform(get("/actuator/health")).andExpect(status().isOk());
        mockMvc.perform(post("/api/campaigns/" + campaignId + "/donations").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"donorName\":\"Anon\",\"donorEmail\":\"a@mail.com\",\"amount\":10,\"paymentMethod\":\"PIX\"}"))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/organizations").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"N\",\"email\":\"" + unique("n") + "\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.type").value("urn:helping-hands:problem:unauthorized"));
        mockMvc.perform(post("/api/campaigns/" + campaignId + "/cancel")).andExpect(status().isUnauthorized());
    }

    @Test
    void shouldForbidVolunteerAccountsFromCreatingOrganizations() throws Exception {
        String email = unique("vol");
        register(email, "VOLUNTEER");

        mockMvc.perform(post("/api/organizations").with(login(email)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"N\",\"email\":\"" + unique("n") + "\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldLetOnlyTheOwnerHandleVolunteerApplications() throws Exception {
        RequestPostProcessor owner = organizationUser();
        RequestPostProcessor stranger = organizationUser();
        int organizationId = createOrganization(owner);
        createOrganization(stranger);
        Instant startsAt = Instant.now().plus(4, ChronoUnit.DAYS);
        String opportunity = mockMvc.perform(post("/api/organizations/" + organizationId + "/opportunities").with(owner)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Clean-up\",\"location\":\"Beach\",\"remote\":false,\"startsAt\":\"" + startsAt
                                + "\",\"endsAt\":\"" + startsAt.plus(2, ChronoUnit.HOURS) + "\",\"slots\":1}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        int opportunityId = JsonPath.read(opportunity, "$.id");

        String application = mockMvc.perform(post("/api/opportunities/" + opportunityId + "/applications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"volunteerName\":\"Vera\",\"volunteerEmail\":\"" + unique("vera") + "\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        int applicationId = JsonPath.read(application, "$.id");

        mockMvc.perform(get("/api/opportunities/" + opportunityId + "/applications")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/opportunities/" + opportunityId + "/applications").with(stranger))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/opportunities/" + opportunityId + "/applications").with(owner))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1));

        mockMvc.perform(post("/api/applications/" + applicationId + "/approve").with(stranger))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/applications/" + applicationId + "/approve")).andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/applications/" + applicationId + "/approve").with(owner))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"));
        mockMvc.perform(post("/api/applications/" + applicationId + "/withdraw")).andExpect(status().isOk());
        mockMvc.perform(post("/api/opportunities/" + opportunityId + "/close").with(stranger))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/opportunities/" + opportunityId + "/close").with(owner)).andExpect(status().isOk());
    }

    @Test
    void shouldBootstrapAdministratorOnlyWhenNoUsersExist() throws Exception {
        jdbcTemplate.execute("TRUNCATE app_user CASCADE");
        String email = unique("root");

        assertThat(bootstrapAdmin.bootstrapAdmin(email, "bootstrap-pass-1")).isTrue();
        assertThat(bootstrapAdmin.bootstrapAdmin(unique("second"), "bootstrap-pass-1")).isFalse();

        String body = mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"bootstrap-pass-1\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        mockMvc.perform(get("/api/analytics/summary").with(bearer(JsonPath.read(body, "$.accessToken"))))
                .andExpect(status().isOk());
    }
}
