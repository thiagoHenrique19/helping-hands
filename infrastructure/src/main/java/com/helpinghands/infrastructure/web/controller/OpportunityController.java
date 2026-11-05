package com.helpinghands.infrastructure.web.controller;

import com.helpinghands.core.domain.OpportunityStatus;
import com.helpinghands.core.pagination.PageResult;
import com.helpinghands.core.pagination.SortDirection;
import com.helpinghands.core.port.in.ManageOpportunitiesUseCase;
import com.helpinghands.core.port.in.dto.OpportunityRequest;
import com.helpinghands.core.port.in.dto.OpportunityResponse;
import com.helpinghands.core.port.in.dto.OpportunitySearchCriteria;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class OpportunityController {

    private final ManageOpportunitiesUseCase opportunityService;

    public OpportunityController(ManageOpportunitiesUseCase opportunityService) {
        this.opportunityService = opportunityService;
    }

    @PostMapping("/organizations/{organizationId}/opportunities")
    @ResponseStatus(HttpStatus.CREATED)
    public OpportunityResponse create(@PathVariable Long organizationId,
                                      @Valid @RequestBody OpportunityRequest request) {
        return opportunityService.create(organizationId, request);
    }

    @GetMapping("/opportunities")
    public PageResult<OpportunityResponse> search(@RequestParam(required = false) Long organizationId,
                                                  @RequestParam(required = false) OpportunityStatus status,
                                                  @RequestParam(required = false) Boolean remote,
                                                  @RequestParam(required = false) Boolean upcoming,
                                                  @RequestParam(defaultValue = "0") int page,
                                                  @RequestParam(defaultValue = "20") int size,
                                                  @RequestParam(defaultValue = "startsAt") String sortBy,
                                                  @RequestParam(defaultValue = "ASC") SortDirection direction) {
        var criteria = new OpportunitySearchCriteria(organizationId, status, remote, upcoming);
        return opportunityService.search(criteria, PageParams.toQuery(page, size, sortBy, direction));
    }

    @GetMapping("/opportunities/{id}")
    public OpportunityResponse findById(@PathVariable Long id) {
        return opportunityService.findById(id);
    }

    @PostMapping("/opportunities/{id}/close")
    public OpportunityResponse close(@PathVariable Long id) {
        return opportunityService.close(id);
    }

    @PostMapping("/opportunities/{id}/cancel")
    public OpportunityResponse cancel(@PathVariable Long id) {
        return opportunityService.cancel(id);
    }
}
