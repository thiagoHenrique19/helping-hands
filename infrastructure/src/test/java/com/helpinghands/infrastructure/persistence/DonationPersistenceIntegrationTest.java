package com.helpinghands.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.helpinghands.AbstractPostgresIntegrationTest;
import com.helpinghands.core.domain.Campaign;
import com.helpinghands.core.domain.Donation;
import com.helpinghands.core.domain.Organization;
import com.helpinghands.core.domain.PaymentMethod;
import com.helpinghands.core.pagination.PageQuery;
import com.helpinghands.core.pagination.PageResult;
import com.helpinghands.core.pagination.SortDirection;
import com.helpinghands.core.port.in.dto.TopDonorResponse;
import com.helpinghands.core.port.out.CampaignRepository;
import com.helpinghands.core.port.out.DonationRepository;
import com.helpinghands.core.port.out.OrganizationRepository;
import com.helpinghands.core.port.out.ReceiptCodeGenerator;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class DonationPersistenceIntegrationTest extends AbstractPostgresIntegrationTest {

    @Autowired private DonationRepository donationRepository;
    @Autowired private CampaignRepository campaignRepository;
    @Autowired private OrganizationRepository organizationRepository;
    @Autowired private ReceiptCodeGenerator receiptCodeGenerator;

    private Campaign campaign;

    @BeforeEach
    void setUp() {
        Organization organization = organizationRepository.save(
                Organization.register("Org", UUID.randomUUID() + "@org.org", "d"));
        campaign = campaignRepository.save(Campaign.create(organization, "Title", "d", new BigDecimal("100000")));
    }

    private void donate(String name, String email, String amount, boolean anonymous) {
        donationRepository.save(Donation.make(campaign, name, email, new BigDecimal(amount), anonymous, null,
                PaymentMethod.PIX, receiptCodeGenerator.next()));
    }

    @Test
    void shouldGenerateSequentialReceiptCodes() {
        String first = receiptCodeGenerator.next();
        String second = receiptCodeGenerator.next();

        assertThat(first).matches("DON-\\d{8}");
        assertThat(Long.parseLong(second.substring(4))).isEqualTo(Long.parseLong(first.substring(4)) + 1);
    }

    @Test
    void shouldRankTopDonorsByTotalExcludingAnonymous() {
        donate("Alice", "alice@mail.com", "50.00", false);
        donate("Alice B.", "ALICE@mail.com", "30.00", false);
        donate("Bob", "bob@mail.com", "70.00", false);
        donate("Whale", "whale@mail.com", "9999.00", true);
        donate("Carol", "carol@mail.com", "10.00", false);

        List<TopDonorResponse> top = donationRepository.findTopDonors(campaign.getId(), 2);

        assertThat(top).hasSize(2);
        assertThat(top.get(0).donorName()).isEqualTo("Alice B.");
        assertThat(top.get(0).totalAmount()).isEqualByComparingTo("80.00");
        assertThat(top.get(0).donationsCount()).isEqualTo(2);
        assertThat(top.get(1).donorName()).isEqualTo("Bob");
    }

    @Test
    void shouldReturnEmptyTopDonorsWhenOnlyAnonymousDonations() {
        donate("Ghost", "ghost@mail.com", "20.00", true);

        assertThat(donationRepository.findTopDonors(campaign.getId(), 10)).isEmpty();
    }

    @Test
    void shouldPaginateDonationsOfCampaign() {
        donate("A", "a@mail.com", "10.00", false);
        donate("B", "b@mail.com", "30.00", true);
        donate("C", "c@mail.com", "20.00", false);

        PageResult<Donation> page = donationRepository.findByCampaignId(campaign.getId(),
                new PageQuery(0, 2, "amount", SortDirection.DESC));

        assertThat(page.content()).extracting(Donation::getDonorName).containsExactly("B", "C");
        assertThat(page.totalElements()).isEqualTo(3);
        assertThat(page.totalPages()).isEqualTo(2);
    }
}
