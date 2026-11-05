package com.helpinghands.infrastructure.web.controller;

import com.helpinghands.core.domain.ApplicationStatus;
import com.helpinghands.core.pagination.PageResult;
import com.helpinghands.core.pagination.SortDirection;
import com.helpinghands.core.port.in.ManageApplicationsUseCase;
import com.helpinghands.core.port.in.dto.ApplicationRequest;
import com.helpinghands.core.port.in.dto.ApplicationResponse;
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
public class ApplicationController {

    private final ManageApplicationsUseCase applicationService;

    public ApplicationController(ManageApplicationsUseCase applicationService) {
        this.applicationService = applicationService;
    }

    @PostMapping("/opportunities/{opportunityId}/applications")
    @ResponseStatus(HttpStatus.CREATED)
    public ApplicationResponse apply(@PathVariable Long opportunityId, @Valid @RequestBody ApplicationRequest request) {
        return applicationService.apply(opportunityId, request);
    }

    @GetMapping("/opportunities/{opportunityId}/applications")
    public PageResult<ApplicationResponse> list(@PathVariable Long opportunityId,
                                                @RequestParam(required = false) ApplicationStatus status,
                                                @RequestParam(defaultValue = "0") int page,
                                                @RequestParam(defaultValue = "20") int size,
                                                @RequestParam(defaultValue = "createdAt") String sortBy,
                                                @RequestParam(defaultValue = "ASC") SortDirection direction) {
        return applicationService.listByOpportunity(opportunityId, status,
                PageParams.toQuery(page, size, sortBy, direction));
    }

    @PostMapping("/applications/{id}/approve")
    public ApplicationResponse approve(@PathVariable Long id) {
        return applicationService.approve(id);
    }

    @PostMapping("/applications/{id}/reject")
    public ApplicationResponse reject(@PathVariable Long id) {
        return applicationService.reject(id);
    }

    @PostMapping("/applications/{id}/withdraw")
    public ApplicationResponse withdraw(@PathVariable Long id) {
        return applicationService.withdraw(id);
    }
}
