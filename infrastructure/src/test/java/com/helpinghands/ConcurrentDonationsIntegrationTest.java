package com.helpinghands;

import static org.assertj.core.api.Assertions.assertThat;

import com.helpinghands.core.domain.Campaign;
import com.helpinghands.core.domain.CampaignStatus;
import com.helpinghands.core.domain.Organization;
import com.helpinghands.core.domain.PaymentMethod;
import com.helpinghands.core.event.CampaignCompleted;
import com.helpinghands.core.exception.BusinessRuleException;
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
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

@Import(RecordedEvents.class)
class ConcurrentDonationsIntegrationTest extends AbstractPostgresIntegrationTest {

    @Autowired private MakeDonationUseCase donations;
    @Autowired private CampaignRepository campaigns;
    @Autowired private OrganizationRepository organizations;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private RecordedEvents events;

    private Long newCampaign(String goal) {
        var organization = organizations.save(Organization.register("Org", UUID.randomUUID() + "@org.org", "d"));
        return campaigns.save(Campaign.create(organization, "Concurrent", "d", new BigDecimal(goal))).getId();
    }

    private Callable<Object> donation(Long campaignId, int amount) {
        return () -> donations.donate(campaignId, new DonationRequest("Donor " + amount,
                "donor" + amount + "@mail.com", new BigDecimal(amount), false, null, PaymentMethod.PIX), null);
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

    @Test
    void shouldCompleteTheCampaignExactlyOnceWhenManyDonationsCrossTheGoal() throws Exception {
        Long campaignId = newCampaign("100");
        List<Callable<Object>> tasks = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            tasks.add(donation(campaignId, 30 + i));
        }

        var outcomes = ConcurrentRunner.runAll(tasks);

        long accepted = outcomes.stream().filter(ConcurrentRunner.Outcome::succeeded).count();
        assertThat(outcomes).filteredOn(o -> !o.succeeded())
                .allMatch(o -> o.error() instanceof BusinessRuleException);
        assertThat(accepted).isBetween(2L, 4L);
        Integer donationRows = jdbc.queryForObject("SELECT count(*) FROM donation WHERE campaign_id = ?",
                Integer.class, campaignId);
        assertThat(donationRows).isEqualTo((int) accepted);
        assertThat(events.of(CampaignCompleted.class)).filteredOn(e -> e.campaignId().equals(campaignId))
                .hasSize(1);
    }

    @Test
    void shouldCreateOneDonationWhenTheSameRequestIsRetriedInParallel() throws Exception {
        Long campaignId = newCampaign("1000");
        var request = new DonationRequest("Retry", "retry@mail.com", new BigDecimal("40"), false, null,
                PaymentMethod.PIX);
        String key = UUID.randomUUID().toString();
        List<Callable<Object>> tasks = new ArrayList<>();
        for (int i = 0; i < 15; i++) {
            tasks.add(() -> donations.donate(campaignId, request, key));
        }

        var outcomes = ConcurrentRunner.runAll(tasks);

        assertThat(outcomes).allMatch(ConcurrentRunner.Outcome::succeeded);
        assertThat(outcomes.stream().map(o -> ((com.helpinghands.core.port.in.dto.DonationResponse) o.value()).id())
                .distinct()).hasSize(1);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM donation WHERE campaign_id = ?", Integer.class,
                campaignId)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT raised_amount FROM campaign WHERE id = ?", BigDecimal.class,
                campaignId)).isEqualByComparingTo("40");
    }
}
