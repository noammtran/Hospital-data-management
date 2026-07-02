package com.usth.hospital.dto.response;

import java.util.List;
import org.springframework.data.domain.Page;

public record PageResponse<T>(
        List<T> items,
        PaginationMeta pagination
) {

    public record PaginationMeta(
            int page,
            int pageSize,
            long totalItems,
            int totalPages,
            boolean hasNext,
            boolean hasPrev
    ) {
    }

    public static <T> PageResponse<T> from(Page<T> page) {
        return new PageResponse<>(
                page.getContent(),
                new PaginationMeta(
                        page.getNumber() + 1,
                        page.getSize(),
                        page.getTotalElements(),
                        page.getTotalPages(),
                        page.hasNext(),
                        page.hasPrevious()
                )
        );
    }
}
