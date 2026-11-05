package com.helpinghands.application.service;

import com.helpinghands.application.idempotency.IdempotentExecution;
import com.helpinghands.application.mapper.DonationMapper;
import com.helpinghands.core.domain.CampaignStatus;
import com.helpinghands.core.domain.Donation;
import com.helpinghands.core.event.CampaignCompleted;
import com.helpinghands.core.exception.ResourceNotFoundException;
import com.helpinghands.core.port.in.MakeDonationUseCase;
import com.helpinghands.core.port.in.dto.DonationRequest;
import com.helpinghands.core.port.in.dto.DonationResponse;
import com.helpinghands.core.port.out.CampaignRepository;
import com.helpinghands.core.port.out.DomainEventPublisher;
import com.helpinghands.core.port.out.DonationRepository;
import com.helpinghands.core.port.out.ReceiptCodeGenerator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DonationService implements MakeDonationUseCase {

    private final CampaignRepository campaignRepository;
    private final DonationRepository donationRepository;
    private final ReceiptCodeGenerator receiptCodeGenerator;
    private final DonationMapper donationMapper;
    private final DomainEventPublisher eventPublisher;
    private final IdempotentExecution idempotentExecution;

    public DonationService(CampaignRepository campaignRepository,
                           DonationRepository donationRepository,
                           ReceiptCodeGenerator receiptCodeGenerator,
                           DonationMapper donationMapper,
                           DomainEventPublisher eventPublisher,
                           IdempotentExecution idempotentExecution) {
        this.campaignRepository = campaignRepository;
        this.donationRepository = donationRepository;
        this.receiptCodeGenerator = receiptCodeGenerator;
        this.donationMapper = donationMapper;
        this.eventPublisher = eventPublisher;
        this.idempotentExecution = idempotentExecution;
    }

    @Override
    @Transactional
    public DonationResponse donate(Long campaignId, DonationRequest request, String idempotencyKey) {
        return idempotentExecution.run("donation:" + campaignId, idempotencyKey, request, DonationResponse.class,
                () -> register(campaignId, request));
    }

    private DonationResponse register(Long campaignId, DonationRequest request) {
        var campaign = campaignRepository.findByIdForUpdate(campaignId)
                .orElseThrow(() -> new ResourceNotFoundException("Campaign", campaignId));
        var donation = Donation.make(campaign, request.donorName(), request.donorEmail(), request.amount(),
                request.anonymous(), request.message(), request.paymentMethod(), receiptCodeGenerator.next());
        campaign.receiveDonation(request.amount());
        campaignRepository.save(campaign);
        if (campaign.getStatus() == CampaignStatus.COMPLETED) {
            eventPublisher.publish(new CampaignCompleted(campaign.getId(), campaign.getOrganization().getId(),
                    campaign.getRaisedAmount()));
        }
        return donationMapper.toResponse(donationRepository.save(donation));
    }
}
