package com.helpinghands.infrastructure.web.controller;

import com.helpinghands.core.port.in.dto.DonationRequest;
import com.helpinghands.core.port.in.dto.DonationResponse;
import com.helpinghands.core.port.in.MakeDonationUseCase;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/campaigns/{campaignId}/donations")
public class DonationController {

    private final MakeDonationUseCase donationService;

    public DonationController(MakeDonationUseCase donationService) {
        this.donationService = donationService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public DonationResponse donate(@PathVariable Long campaignId, @Valid @RequestBody DonationRequest request) {
        return donationService.donate(campaignId, request);
    }
}
