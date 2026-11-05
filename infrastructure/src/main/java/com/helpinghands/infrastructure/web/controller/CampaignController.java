package com.helpinghands.infrastructure.web.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.helpinghands.core.domain.CampaignCategory;
import com.helpinghands.core.domain.CampaignStatus;
import com.helpinghands.core.pagination.PageResult;
import com.helpinghands.core.pagination.SortDirection;
import com.helpinghands.core.port.in.ManageCampaignsUseCase;
import com.helpinghands.core.port.in.dto.CampaignRequest;
import com.helpinghands.core.port.in.dto.CampaignResponse;
import com.helpinghands.core.port.in.dto.CampaignSearchCriteria;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "Campaigns", description = "Fundraising campaigns run by organizations")
@RequestMapping("/api")
public class CampaignController {

    private final ManageCampaignsUseCase campaignService;

    public CampaignController(ManageCampaignsUseCase campaignService) {
        this.campaignService = campaignService;
    }

    @Operation(summary = "Create a campaign for an organization")
    @PostMapping("/organizations/{organizationId}/campaigns")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN') or @access.ownsOrganization(authentication, #organizationId)")
    public CampaignResponse create(@PathVariable Long organizationId, @Valid @RequestBody CampaignRequest request) {
        return campaignService.create(organizationId, request);
    }

    @Operation(summary = "Get a campaign")
    @GetMapping("/campaigns/{id}")
    public CampaignResponse findById(@PathVariable Long id) {
        return campaignService.findById(id);
    }

    @Operation(summary = "Search campaigns by organization, status, category and title (paginated, sortable)")
    @GetMapping("/campaigns")
    public PageResult<CampaignResponse> search(@RequestParam(required = false) Long organizationId,
                                               @RequestParam(required = false) CampaignStatus status,
                                               @RequestParam(required = false) CampaignCategory category,
                                               @RequestParam(required = false) String title,
                                               @RequestParam(defaultValue = "0") int page,
                                               @RequestParam(defaultValue = "20") int size,
                                               @RequestParam(defaultValue = "id") String sortBy,
                                               @RequestParam(defaultValue = "ASC") SortDirection direction) {
        var criteria = new CampaignSearchCriteria(organizationId, status, category, title);
        return campaignService.search(criteria, PageParams.toQuery(page, size, sortBy, direction));
    }

    @Operation(summary = "Update an active campaign")
    @PutMapping("/campaigns/{id}")
    @PreAuthorize("hasRole('ADMIN') or @access.ownsCampaign(authentication, #id)")
    public CampaignResponse update(@PathVariable Long id, @Valid @RequestBody CampaignRequest request) {
        return campaignService.update(id, request);
    }

    @Operation(summary = "Cancel an active campaign")
    @PostMapping("/campaigns/{id}/cancel")
    @PreAuthorize("hasRole('ADMIN') or @access.ownsCampaign(authentication, #id)")
    public CampaignResponse cancel(@PathVariable Long id) {
        return campaignService.cancel(id);
    }
}
