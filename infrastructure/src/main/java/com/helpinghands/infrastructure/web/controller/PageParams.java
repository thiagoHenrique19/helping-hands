package com.helpinghands.infrastructure.web.controller;

import com.helpinghands.core.pagination.PageQuery;
import com.helpinghands.core.pagination.SortDirection;

final class PageParams {

    private PageParams() {
    }

    static PageQuery toQuery(int page, int size, String sortBy, SortDirection direction) {
        return new PageQuery(page, size, sortBy, direction);
    }
}
