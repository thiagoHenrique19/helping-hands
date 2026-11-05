package com.helpinghands.infrastructure.persistence;

import com.helpinghands.core.exception.InvalidRequestException;
import com.helpinghands.core.pagination.PageQuery;
import com.helpinghands.core.pagination.PageResult;
import com.helpinghands.core.pagination.SortDirection;
import java.util.Set;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

final class PageTranslator {

    private PageTranslator() {
    }

    static Pageable toPageable(PageQuery query, Set<String> sortableFields) {
        if (!sortableFields.contains(query.sortBy())) {
            throw new InvalidRequestException(
                    "Cannot sort by '" + query.sortBy() + "'. Allowed fields: " + sortableFields.stream().sorted().toList());
        }
        Sort.Direction direction = query.direction() == SortDirection.DESC ? Sort.Direction.DESC : Sort.Direction.ASC;
        Sort sort = Sort.by(direction, query.sortBy());
        if (!"id".equals(query.sortBy())) {
            sort = sort.and(Sort.by(Sort.Direction.ASC, "id"));
        }
        return PageRequest.of(query.page(), query.size(), sort);
    }

    static <T> PageResult<T> toPageResult(Page<T> page) {
        return new PageResult<>(page.getContent(), page.getNumber(), page.getSize(),
                page.getTotalElements(), page.getTotalPages());
    }
}
