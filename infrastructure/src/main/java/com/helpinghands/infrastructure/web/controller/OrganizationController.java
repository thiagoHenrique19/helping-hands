package com.helpinghands.infrastructure.web.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.helpinghands.core.pagination.PageResult;
import com.helpinghands.core.pagination.SortDirection;
import com.helpinghands.core.port.in.ListOrganizationsUseCase;
import com.helpinghands.core.port.in.RegisterOrganizationUseCase;
import com.helpinghands.core.port.in.UpdateOrganizationUseCase;
import com.helpinghands.core.port.in.VerifyOrganizationUseCase;
import com.helpinghands.core.port.in.dto.OrganizationRequest;
import com.helpinghands.core.port.in.dto.OrganizationResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
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
@Tag(name = "Organizations", description = "Register, update, verify and search NGOs")
@RequestMapping("/api/organizations")
public class OrganizationController {

    private final RegisterOrganizationUseCase registerOrganization;
    private final UpdateOrganizationUseCase updateOrganization;
    private final VerifyOrganizationUseCase verifyOrganization;
    private final ListOrganizationsUseCase listOrganizations;

    public OrganizationController(RegisterOrganizationUseCase registerOrganization,
                                  UpdateOrganizationUseCase updateOrganization,
                                  VerifyOrganizationUseCase verifyOrganization,
                                  ListOrganizationsUseCase listOrganizations) {
        this.registerOrganization = registerOrganization;
        this.updateOrganization = updateOrganization;
        this.verifyOrganization = verifyOrganization;
        this.listOrganizations = listOrganizations;
    }

    @Operation(summary = "Register an organization")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OrganizationResponse register(@Valid @RequestBody OrganizationRequest request) {
        return registerOrganization.register(request);
    }

    @Operation(summary = "List organizations (paginated, optional name search)")
    @GetMapping
    public PageResult<OrganizationResponse> list(@RequestParam(required = false) String name,
                                                 @RequestParam(defaultValue = "0") int page,
                                                 @RequestParam(defaultValue = "20") int size,
                                                 @RequestParam(defaultValue = "id") String sortBy,
                                                 @RequestParam(defaultValue = "ASC") SortDirection direction) {
        return listOrganizations.search(name, PageParams.toQuery(page, size, sortBy, direction));
    }

    @Operation(summary = "Get an organization")
    @GetMapping("/{id}")
    public OrganizationResponse findById(@PathVariable Long id) {
        return registerOrganization.findById(id);
    }

    @Operation(summary = "Update an organization")
    @PutMapping("/{id}")
    public OrganizationResponse update(@PathVariable Long id, @Valid @RequestBody OrganizationRequest request) {
        return updateOrganization.update(id, request);
    }

    @Operation(summary = "Mark an organization as verified")
    @PostMapping("/{id}/verify")
    public OrganizationResponse verify(@PathVariable Long id) {
        return verifyOrganization.verify(id);
    }
}
