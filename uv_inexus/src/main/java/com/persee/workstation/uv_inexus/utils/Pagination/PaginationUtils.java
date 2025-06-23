package com.persee.workstation.uv_inexus.utils.Pagination;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

public class PaginationUtils {

    public static <T> PaginationResult<T> createPaginationResult(Page<T> page) {
        return new PaginationResult<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.hasNext()
        );
    }

    public static Pageable createPageable(int pageNo, int pageSize) {
        return PageRequest.of(pageNo, pageSize);
    }

    public static Pageable createPageableWithSort(int pageNo, int pageSize, String sortField, Sort.Direction sortDirection) {
        return PageRequest.of(pageNo, pageSize, Sort.by(sortDirection, sortField));
    }

    public static Pageable createPageableWithMultiSort(int pageNo, int pageSize, String[] sortFields, Sort.Direction[] sortDirections) {
        Sort sort = Sort.by(java.util.Arrays.stream(sortDirections)
                .map(direction -> java.util.Arrays.stream(sortFields)
                        .map(field -> new Sort.Order(direction, field))
                        .toList())
                .flatMap(List::stream)
                .toList());
        return PageRequest.of(pageNo, pageSize, sort);
    }
}
