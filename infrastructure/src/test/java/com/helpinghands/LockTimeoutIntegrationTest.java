package com.helpinghands;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.helpinghands.core.domain.Campaign;
import com.helpinghands.core.domain.Organization;
import com.helpinghands.core.port.out.CampaignRepository;
import com.helpinghands.core.port.out.OrganizationRepository;
import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@TestPropertySource(properties = "app.database.lock-timeout-ms=300")
class LockTimeoutIntegrationTest extends AbstractPostgresIntegrationTest {

    @Autowired private CampaignRepository campaigns;
    @Autowired private OrganizationRepository organizations;
    @Autowired private PlatformTransactionManager transactionManager;

    @Test
    void shouldAnswer409InsteadOfHangingWhenTheCampaignRowStaysLocked() throws Exception {
        var organization = organizations.save(Organization.register("Org", UUID.randomUUID() + "@org.org", "d"));
        Long id = campaigns.save(Campaign.create(organization, "Busy", "d", new BigDecimal("100"))).getId();

        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            campaigns.findByIdForUpdate(id).orElseThrow();
            // another thread, otherwise the request would join this transaction
            CompletableFuture.runAsync(() -> {
                try {
                    mockMvc.perform(post("/api/campaigns/" + id + "/donations")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content("{\"donorName\":\"D\",\"donorEmail\":\"d@mail.com\",\"amount\":10,"
                                            + "\"paymentMethod\":\"PIX\"}"))
                            .andExpect(status().isConflict())
                            .andExpect(jsonPath("$.type").value("urn:helping-hands:problem:conflict"));
                } catch (Exception e) {
                    throw new IllegalStateException(e);
                }
            }).join();
        });
    }
}
