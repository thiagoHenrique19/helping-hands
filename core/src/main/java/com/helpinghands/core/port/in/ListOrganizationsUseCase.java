package com.helpinghands.core.port.in;

import com.helpinghands.core.pagination.PageQuery;
import com.helpinghands.core.pagination.PageResult;
import com.helpinghands.core.port.in.dto.OrganizationResponse;

public interface ListOrganizationsUseCase {

    PageResult<OrganizationResponse> search(String name, PageQuery pageQuery);
}
