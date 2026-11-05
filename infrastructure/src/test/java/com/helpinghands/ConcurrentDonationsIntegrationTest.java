package com.helpinghands;

import static org.assertj.core.api.Assertions.assertThat;

import com.helpinghands.core.domain.Campaign;
import com.helpinghands.core.domain.CampaignStatus;
import com.helpinghands.core.domain.Organization;
import com.helpinghands.core.domain.PaymentMethod;
import com.helpinghands.core.port.in.MakeDonationUseCase;
import com.helpinghands.core.port.in.dto.DonationRequest;
import com.helpinghands.core.port.out.CampaignRepository;
import com.helpinghands.core.port.out.OrganizationRepository;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

class ConcurrentDonationsIntegrationTest extends AbstractPostgresIntegrationTest {

    @Autowired private MakeDonationUseCase donations;
    @Autowired private CampaignRepository campaigns;
    @Autowired private OrganizationRepository organizations;
    @Autowired private JdbcTemplate jdbc;
    
    private Long newCampaign(String goal) {
        var organization = organizations.save(Organization.register("Org", UUID.randomUUID() + "@org.org", "d"));
        return campaigns.save(Campaign.create(organization, "Concurrent", "d", new BigDecimal(goal))).getId();
    }

    private Callable<Object> donation(Long campaignId, int amount) {
        return () -> donations.donate(campaignId, new DonationRequest("Donor " + amount,
                "donor" + amount + "@mail.com", new BigDecimal(amount), false, null, PaymentMethod.PIX));
    }

    @Test
    void shouldNotLoseAnyUpdateWhenFiftyDonationsArriveTogether() throws Exception {
        Long campaignId = newCampaign("1000000");
        List<Callable<Object>> tasks = new ArrayList<>();
        for (int amount = 1; amount <= 50; amount++) {
            tasks.add(donation(campaignId, amount));
        }

        var outcomes = ConcurrentRunner.runAll(tasks);

        assertThat(outcomes).allMatch(ConcurrentRunner.Outcome::succeeded);
        BigDecimal raised = jdbc.queryForObject("SELECT raised_amount FROM campaign WHERE id = ?",
                BigDecimal.class, campaignId);
        BigDecimal donated = jdbc.queryForObject("SELECT sum(amount) FROM donation WHERE campaign_id = ?",
                BigDecimal.class, campaignId);
        assertThat(raised).isEqualByComparingTo("1275");
        assertThat(donated).isEqualByComparingTo(raised);
        assertThat(campaigns.findById(campaignId).orElseThrow().getStatus()).isEqualTo(CampaignStatus.ACTIVE);
    }
}
