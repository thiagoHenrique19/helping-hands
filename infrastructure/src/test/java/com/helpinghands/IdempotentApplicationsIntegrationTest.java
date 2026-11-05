package com.helpinghands;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.helpinghands.core.domain.Organization;
import com.helpinghands.core.domain.VolunteerOpportunity;
import com.helpinghands.core.port.in.ManageApplicationsUseCase;
import com.helpinghands.core.port.in.dto.ApplicationRequest;
import com.helpinghands.core.port.in.dto.ApplicationResponse;
import com.helpinghands.core.port.out.OrganizationRepository;
import com.helpinghands.core.port.out.VolunteerOpportunityRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.ResultActions;

class IdempotentApplicationsIntegrationTest extends AbstractPostgresIntegrationTest {

    @Autowired private ManageApplicationsUseCase applications;
    @Autowired private VolunteerOpportunityRepository opportunities;
    @Autowired private OrganizationRepository organizations;
    @Autowired private JdbcTemplate jdbc;

    private Long opportunityId;

    @BeforeEach
    void setUp() {
        var organization = organizations.save(Organization.register("Org", UUID.randomUUID() + "@org.org", "d"));
        Instant startsAt = Instant.now().plus(3, ChronoUnit.DAYS);
        opportunityId = opportunities.save(VolunteerOpportunity.create(organization, "Task", "d", "Somewhere", false,
                startsAt, startsAt.plus(2, ChronoUnit.HOURS), 5)).getId();
    }

    private ResultActions apply(String key, String motivation) throws Exception {
        var request = post("/api/opportunities/" + opportunityId + "/applications")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"volunteerName\":\"Vera\",\"volunteerEmail\":\"vera@mail.com\",\"motivation\":\""
                        + motivation + "\"}");
        if (key != null) {
            request.header("Idempotency-Key", key);
        }
        return mockMvc.perform(request);
    }

    private int applicationRows() {
        return jdbc.queryForObject("SELECT count(*) FROM volunteer_application WHERE opportunity_id = ?",
                Integer.class, opportunityId);
    }

    @Test
    void shouldAnswerTheRetryWithTheOriginalApplicationInsteadOfAConflict() throws Exception {
        String key = UUID.randomUUID().toString();
        apply(key, "I care").andExpect(status().isCreated());

        apply(key, "I care").andExpect(status().isCreated()).andExpect(jsonPath("$.status").value("PENDING"));

        assertThat(applicationRows()).isEqualTo(1);
    }

    @Test
    void shouldStillReportConflictForTheSameVolunteerWithoutAKey() throws Exception {
        apply(null, "I care").andExpect(status().isCreated());
        apply(null, "I care").andExpect(status().isConflict());
    }

    @Test
    void shouldRejectTheSameKeyWithAnotherPayload() throws Exception {
        String key = UUID.randomUUID().toString();
        apply(key, "I care").andExpect(status().isCreated());

        apply(key, "Something else").andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.type").value("urn:helping-hands:problem:idempotency-key-reuse"));
    }

    @Test
    void shouldCreateASingleApplicationForParallelRetries() throws Exception {
        String key = UUID.randomUUID().toString();
        var request = new ApplicationRequest("Vera", "vera@mail.com", null, "I care");
        List<Callable<ApplicationResponse>> tasks = new ArrayList<>();
        for (int i = 0; i < 12; i++) {
            tasks.add(() -> applications.apply(opportunityId, request, key));
        }

        var outcomes = ConcurrentRunner.runAll(tasks);

        assertThat(outcomes).allMatch(ConcurrentRunner.Outcome::succeeded);
        assertThat(outcomes.stream().map(o -> o.value().id()).distinct()).hasSize(1);
        assertThat(applicationRows()).isEqualTo(1);
    }
}
