package com.helpinghands.core.pagination;

import com.helpinghands.core.exception.InvalidRequestException;

public record PageQuery(int page, int size, String sortBy, SortDirection direction) {

    public static final int MAX_SIZE = 100;
    public static final String DEFAULT_SORT = "id";

    public PageQuery {
        if (page < 0) {
            throw new InvalidRequestException("Page index must not be negative");
        }
        if (size < 1 || size > MAX_SIZE) {
            throw new InvalidRequestException("Page size must be between 1 and " + MAX_SIZE);
        }
        if (sortBy == null || sortBy.isBlank()) {
            throw new InvalidRequestException("Sort field must not be blank");
        }
        if (direction == null) {
            direction = SortDirection.ASC;
        }
    }

    public static PageQuery of(int page, int size) {
        return new PageQuery(page, size, DEFAULT_SORT, SortDirection.ASC);
    }
}
