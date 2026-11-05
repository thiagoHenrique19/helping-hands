package com.helpinghands.infrastructure.web.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.helpinghands.core.pagination.PageResult;
import com.helpinghands.core.pagination.SortDirection;
import com.helpinghands.core.port.in.ListDonationsUseCase;
import com.helpinghands.core.port.in.MakeDonationUseCase;
import com.helpinghands.core.port.in.ViewTopDonorsUseCase;
import com.helpinghands.core.port.in.dto.DonationRequest;
import com.helpinghands.core.port.in.dto.DonationResponse;
import com.helpinghands.core.port.in.dto.PublicDonationResponse;
import com.helpinghands.core.port.in.dto.TopDonorResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "Donations", description = "Donations, public donation feed and top donors")
@RequestMapping("/api/campaigns/{campaignId}")
public class DonationController {

    private final MakeDonationUseCase donationService;
    private final ListDonationsUseCase listDonations;
    private final ViewTopDonorsUseCase viewTopDonors;

    public DonationController(MakeDonationUseCase donationService,
                              ListDonationsUseCase listDonations,
                              ViewTopDonorsUseCase viewTopDonors) {
        this.donationService = donationService;
        this.listDonations = listDonations;
        this.viewTopDonors = viewTopDonors;
    }

    @Operation(summary = "Donate to a campaign")
    @PostMapping("/donations")
    @ResponseStatus(HttpStatus.CREATED)
    public DonationResponse donate(@PathVariable Long campaignId,
                                   @Parameter(description = "Optional. Repeating it with the same body returns the first response.")
                                   @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
                                   @Valid @RequestBody DonationRequest request) {
        return donationService.donate(campaignId, request, idempotencyKey);
    }

    @Operation(summary = "List the public donations of a campaign (anonymous donors are masked)")
    @GetMapping("/donations")
    public PageResult<PublicDonationResponse> list(@PathVariable Long campaignId,
                                                   @RequestParam(defaultValue = "0") int page,
                                                   @RequestParam(defaultValue = "20") int size,
                                                   @RequestParam(defaultValue = "createdAt") String sortBy,
                                                   @RequestParam(defaultValue = "DESC") SortDirection direction) {
        return listDonations.listByCampaign(campaignId, PageParams.toQuery(page, size, sortBy, direction));
    }

    @Operation(summary = "Top identified donors of a campaign")
    @GetMapping("/top-donors")
    public List<TopDonorResponse> topDonors(@PathVariable Long campaignId,
                                            @RequestParam(defaultValue = "10") int limit) {
        return viewTopDonors.topDonors(campaignId, limit);
    }
}
