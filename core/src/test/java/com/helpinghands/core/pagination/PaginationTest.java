package com.helpinghands.core.pagination;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.helpinghands.core.exception.InvalidRequestException;
import java.util.List;
import org.junit.jupiter.api.Test;

class PaginationTest {

    @Test
    void shouldCreatePageQueryWithDefaultSorting() {
        PageQuery query = PageQuery.of(0, 20);

        assertThat(query.sortBy()).isEqualTo("id");
        assertThat(query.direction()).isEqualTo(SortDirection.ASC);
    }

    @Test
    void shouldRejectNegativePage() {
        assertThatThrownBy(() -> PageQuery.of(-1, 10)).isInstanceOf(InvalidRequestException.class);
    }

    @Test
    void shouldRejectSizeOutOfRange() {
        assertThatThrownBy(() -> PageQuery.of(0, 0)).isInstanceOf(InvalidRequestException.class);
        assertThatThrownBy(() -> PageQuery.of(0, PageQuery.MAX_SIZE + 1)).isInstanceOf(InvalidRequestException.class);
    }

    @Test
    void shouldRejectBlankSortField() {
        assertThatThrownBy(() -> new PageQuery(0, 10, " ", SortDirection.ASC))
                .isInstanceOf(InvalidRequestException.class);
    }

    @Test
    void shouldMapPageContentKeepingMetadata() {
        PageResult<Integer> page = new PageResult<>(List.of(1, 2), 0, 2, 5, 3);

        PageResult<String> mapped = page.map(number -> "n" + number);

        assertThat(mapped.content()).containsExactly("n1", "n2");
        assertThat(mapped.totalElements()).isEqualTo(5);
        assertThat(mapped.totalPages()).isEqualTo(3);
    }
}
