package com.helpinghands.infrastructure.web.controller;

import com.helpinghands.core.port.in.dto.CampaignRequest;
import com.helpinghands.core.port.in.dto.CampaignResponse;
import com.helpinghands.core.port.in.ManageCampaignsUseCase;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class CampaignController {

    private final ManageCampaignsUseCase campaignService;

    public CampaignController(ManageCampaignsUseCase campaignService) {
        this.campaignService = campaignService;
    }

    @PostMapping("/organizations/{organizationId}/campaigns")
    @ResponseStatus(HttpStatus.CREATED)
    public CampaignResponse create(@PathVariable Long organizationId, @Valid @RequestBody CampaignRequest request) {
        return campaignService.create(organizationId, request);
    }

    @GetMapping("/campaigns/{id}")
    public CampaignResponse findById(@PathVariable Long id) {
        return campaignService.findById(id);
    }

    @GetMapping("/campaigns")
    public List<CampaignResponse> listActive() {
        return campaignService.listActive();
    }
}
