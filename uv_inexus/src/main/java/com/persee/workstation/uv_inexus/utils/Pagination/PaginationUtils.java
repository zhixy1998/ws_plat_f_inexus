/*
 * *****************************************************************************************************
 * Module name:    **.**
 *
 * Copyright (C) 2025 北京普析通用仪器有限责任公司 All Rights Reserved.
 *                此源代码仅限在公司内部使用。未经公司事先书面许可，任何单位和个人不得以任何方式复制、传播本代码的任何部分。
 * Description:
 *
 *
 * *************************************************************************************
 */

package com.persee.workstation.uv_inexus.utils.Pagination;

import java.util.ArrayList;
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
        // 确保 sortFields 和 sortDirections 长度相同
        if (sortFields.length != sortDirections.length) {
            throw new IllegalArgumentException("Sort fields and directions must have the same length");
        }
        
        // 创建 Sort 对象
        List<Sort.Order> orders = new ArrayList<>();
        for (int i = 0; i < sortFields.length; i++) {
            orders.add(new Sort.Order(sortDirections[i], sortFields[i]));
        }
        
        return PageRequest.of(pageNo, pageSize, Sort.by(orders));
    }
}
