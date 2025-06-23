package com.persee.workstation.uv_inexus.controller.PaginationController;

import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.persee.workstation.uv_inexus.entity.PaginationEntity;
import com.persee.workstation.uv_inexus.service.PaginationService.PaginationService;
import com.persee.workstation.uv_inexus.utils.Pagination.PaginationResult;

@RestController
public class PaginationController {

    private final PaginationService paginationService;

    public PaginationController(PaginationService paginationService) {
        this.paginationService = paginationService;
    }

    @GetMapping("/api/pagination/basic")
    public PaginationResult<PaginationEntity> getPaginatedEntities(
            @RequestParam(defaultValue = "0") int pageNo,
            @RequestParam(defaultValue = "10") int pageSize) {
        return paginationService.getPaginatedEntities(pageNo, pageSize);
    }

    @GetMapping("/api/pagination/sorted")
    public PaginationResult<PaginationEntity> getPaginatedEntitiesWithSort(
            @RequestParam(defaultValue = "0") int pageNo,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(defaultValue = "name") String sortField,
            @RequestParam(defaultValue = "ASC") Sort.Direction sortDirection) {
        return paginationService.getPaginatedEntitiesWithSort(pageNo, pageSize, sortField, sortDirection);
    }

    @GetMapping("/api/pagination/multi-sorted")
    public PaginationResult<PaginationEntity> getPaginatedEntitiesWithMultiSort(
            @RequestParam(defaultValue = "0") int pageNo,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam String[] sortFields,
            @RequestParam Sort.Direction[] sortDirections) {
        return paginationService.getPaginatedEntitiesWithMultiSort(pageNo, pageSize, sortFields, sortDirections);
    }
}
