package com.helpinghands.infrastructure.web.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.helpinghands.core.domain.ApplicationStatus;
import com.helpinghands.core.pagination.PageResult;
import com.helpinghands.core.pagination.SortDirection;
import com.helpinghands.core.port.in.ManageApplicationsUseCase;
import com.helpinghands.core.port.in.dto.ApplicationRequest;
import com.helpinghands.core.port.in.dto.ApplicationResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
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
@Tag(name = "Volunteer applications", description = "Applications of volunteers to opportunities")
@RequestMapping("/api")
public class ApplicationController {

    private final ManageApplicationsUseCase applicationService;

    public ApplicationController(ManageApplicationsUseCase applicationService) {
        this.applicationService = applicationService;
    }

    @Operation(summary = "Apply to an opportunity")
    @PostMapping("/opportunities/{opportunityId}/applications")
    @ResponseStatus(HttpStatus.CREATED)
    public ApplicationResponse apply(@PathVariable Long opportunityId,
                                     @Parameter(description = "Optional. Repeating it with the same body returns the first response.")
                                     @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
                                     @Valid @RequestBody ApplicationRequest request) {
        return applicationService.apply(opportunityId, request, idempotencyKey);
    }

    @Operation(summary = "List the applications of an opportunity")
    @GetMapping("/opportunities/{opportunityId}/applications")
    @PreAuthorize("hasRole('ADMIN') or @access.ownsOpportunity(authentication, #opportunityId)")
    public PageResult<ApplicationResponse> list(@PathVariable Long opportunityId,
                                                @RequestParam(required = false) ApplicationStatus status,
                                                @RequestParam(defaultValue = "0") int page,
                                                @RequestParam(defaultValue = "20") int size,
                                                @RequestParam(defaultValue = "createdAt") String sortBy,
                                                @RequestParam(defaultValue = "ASC") SortDirection direction) {
        return applicationService.listByOpportunity(opportunityId, status,
                PageParams.toQuery(page, size, sortBy, direction));
    }

    @Operation(summary = "Approve a pending application (fills a slot)")
    @PostMapping("/applications/{id}/approve")
    @PreAuthorize("hasRole('ADMIN') or @access.ownsApplication(authentication, #id)")
    public ApplicationResponse approve(@PathVariable Long id) {
        return applicationService.approve(id);
    }

    @Operation(summary = "Reject a pending application")
    @PostMapping("/applications/{id}/reject")
    @PreAuthorize("hasRole('ADMIN') or @access.ownsApplication(authentication, #id)")
    public ApplicationResponse reject(@PathVariable Long id) {
        return applicationService.reject(id);
    }

    @Operation(summary = "Withdraw an application (frees the slot when it was approved)")
    @PostMapping("/applications/{id}/withdraw")
    public ApplicationResponse withdraw(@PathVariable Long id) {
        return applicationService.withdraw(id);
    }
}
