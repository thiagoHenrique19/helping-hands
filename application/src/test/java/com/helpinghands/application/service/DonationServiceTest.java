package com.helpinghands.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.helpinghands.application.idempotency.IdempotentExecution;
import com.helpinghands.application.mapper.DonationMapper;
import com.helpinghands.core.domain.Campaign;
import com.helpinghands.core.domain.CampaignStatus;
import com.helpinghands.core.domain.Donation;
import com.helpinghands.core.domain.Organization;
import com.helpinghands.core.domain.PaymentMethod;
import com.helpinghands.core.exception.BusinessRuleException;
import com.helpinghands.core.exception.ResourceNotFoundException;
import com.helpinghands.core.port.in.dto.DonationRequest;
import com.helpinghands.core.port.in.dto.DonationResponse;
import com.helpinghands.core.port.out.CampaignRepository;
import com.helpinghands.core.event.CampaignCompleted;
import com.helpinghands.core.port.out.DomainEventPublisher;
import com.helpinghands.core.idempotency.StoredIdempotency;
import com.helpinghands.core.port.out.DonationRepository;
import com.helpinghands.core.port.out.IdempotencyPayloads;
import com.helpinghands.core.port.out.IdempotencyStore;
import com.helpinghands.core.port.out.ReceiptCodeGenerator;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DonationServiceTest {

    @Mock private CampaignRepository campaignRepository;
    @Mock private DonationRepository donationRepository;
    @Mock private ReceiptCodeGenerator receiptCodeGenerator;
    @Mock private DomainEventPublisher eventPublisher;
    @Mock private IdempotencyStore idempotencyStore;
    @Mock private IdempotencyPayloads idempotencyPayloads;

    private DonationService donationService;
    private Campaign campaign;

    @BeforeEach
    void setUp() {
        donationService = new DonationService(campaignRepository, donationRepository, receiptCodeGenerator,
                new DonationMapper(), eventPublisher, new IdempotentExecution(idempotencyStore, idempotencyPayloads));
        Organization organization = Organization.register("Green Future", "hi@green.org", "Reforestation");
        campaign = Campaign.create(organization, "Plant 1000 trees", "Atlantic forest", new BigDecimal("100"));
    }

    private DonationRequest request(String name, String amount) {
        return new DonationRequest(name, name.toLowerCase() + "@mail.com", new BigDecimal(amount), false, null,
                PaymentMethod.PIX);
    }

    @Test
    void shouldRegisterDonationAndUpdateCampaign() {
        when(campaignRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(campaign));
        when(receiptCodeGenerator.next()).thenReturn("DON-00000007");
        when(donationRepository.save(any(Donation.class))).thenAnswer(invocation -> invocation.getArgument(0));

        DonationResponse response = donationService.donate(1L, new DonationRequest("Alice", "alice@mail.com",
                new BigDecimal("40"), true, "Keep going", PaymentMethod.CREDIT_CARD), null);

        assertThat(response.amount()).isEqualByComparingTo("40");
        assertThat(response.receiptCode()).isEqualTo("DON-00000007");
        assertThat(response.anonymous()).isTrue();
        assertThat(response.message()).isEqualTo("Keep going");
        assertThat(response.paymentMethod()).isEqualTo(PaymentMethod.CREDIT_CARD);
        assertThat(campaign.getRaisedAmount()).isEqualByComparingTo("40");
    }

    @Test
    void shouldLockCampaignRowInsteadOfReadingItPlainly() {
        when(campaignRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(campaign));
        when(receiptCodeGenerator.next()).thenReturn("DON-1");
        when(donationRepository.save(any(Donation.class))).thenAnswer(invocation -> invocation.getArgument(0));

        donationService.donate(1L, request("Gus", "10"), null);

        verify(campaignRepository, never()).findById(any());
    }

    @Test
    void shouldCompleteCampaignWhenGoalIsReached() {
        when(campaignRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(campaign));
        when(receiptCodeGenerator.next()).thenReturn("DON-1");
        when(donationRepository.save(any(Donation.class))).thenAnswer(invocation -> invocation.getArgument(0));

        donationService.donate(1L, request("Bob", "100"), null);

        assertThat(campaign.getStatus()).isEqualTo(CampaignStatus.COMPLETED);
        verify(eventPublisher).publish(any(CampaignCompleted.class));
    }

    @Test
    void shouldNotPublishEventWhileCampaignIsStillActive() {
        when(campaignRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(campaign));
        when(receiptCodeGenerator.next()).thenReturn("DON-1");
        when(donationRepository.save(any(Donation.class))).thenAnswer(invocation -> invocation.getArgument(0));

        donationService.donate(1L, request("Eve", "10"), null);

        verify(eventPublisher, never()).publish(any());
    }

    @Test
    void shouldRejectDonationBelowMinimumAmount() {
        when(campaignRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(campaign));
        when(receiptCodeGenerator.next()).thenReturn("DON-1");

        assertThatThrownBy(() -> donationService.donate(1L, request("Dan", "0.50"), null))
                .isInstanceOf(BusinessRuleException.class);

        assertThat(campaign.getRaisedAmount()).isEqualByComparingTo("0");
        verify(donationRepository, never()).save(any());
    }

    @Test
    void shouldReturnTheFirstResponseAndSkipTheSecondDonationForARepeatedKey() {
        var request = request("Hana", "40");
        var original = new DonationResponse(5L, 1L, "Hana", new BigDecimal("40"), java.time.Instant.now(),
                "DON-5", false, null, PaymentMethod.PIX);
        when(idempotencyPayloads.fingerprint(request)).thenReturn("hash");
        when(idempotencyStore.reserve("donation:1", "key-1", "hash")).thenReturn(false);
        when(idempotencyStore.find("donation:1", "key-1")).thenReturn(Optional.of(new StoredIdempotency("hash", "{}")));
        when(idempotencyPayloads.deserialize("{}", DonationResponse.class)).thenReturn(original);

        DonationResponse response = donationService.donate(1L, request, "key-1");

        assertThat(response).isSameAs(original);
        verify(campaignRepository, never()).findByIdForUpdate(any());
        verify(donationRepository, never()).save(any());
        verify(receiptCodeGenerator, never()).next();
    }

    @Test
    void shouldStoreTheResponseOfAFirstRequestWithKey() {
        var request = request("Ivo", "40");
        when(campaignRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(campaign));
        when(receiptCodeGenerator.next()).thenReturn("DON-9");
        when(donationRepository.save(any(Donation.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(idempotencyPayloads.fingerprint(request)).thenReturn("hash");
        when(idempotencyStore.reserve("donation:1", "key-2", "hash")).thenReturn(true);
        when(idempotencyPayloads.serialize(any())).thenReturn("{\"snapshot\":true}");

        donationService.donate(1L, request, "key-2");

        verify(idempotencyStore).complete("donation:1", "key-2", "{\"snapshot\":true}");
    }

    @Test
    void shouldFailWhenCampaignDoesNotExist() {
        when(campaignRepository.findByIdForUpdate(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> donationService.donate(99L, request("Carol", "10"), null))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(donationRepository, never()).save(any());
    }
}
