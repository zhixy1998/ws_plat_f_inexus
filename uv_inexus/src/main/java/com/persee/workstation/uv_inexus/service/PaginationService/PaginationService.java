package com.persee.workstation.uv_inexus.service.PaginationService;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import com.persee.workstation.uv_inexus.entity.PaginationEntity;
import com.persee.workstation.uv_inexus.repository.PaginationEntityRepository;
import com.persee.workstation.uv_inexus.utils.Pagination.PaginationResult;
import com.persee.workstation.uv_inexus.utils.Pagination.PaginationUtils;

@Service
public class PaginationService {

    private final PaginationEntityRepository paginationEntityRepository;

    public PaginationService(PaginationEntityRepository paginationEntityRepository) {
        this.paginationEntityRepository = paginationEntityRepository;
    }

    public PaginationResult<PaginationEntity> getPaginatedEntities(int pageNo, int pageSize) {
        Pageable pageable = PaginationUtils.createPageable(pageNo, pageSize);
        Page<PaginationEntity> page = paginationEntityRepository.customFindAll(pageable);
        return PaginationUtils.createPaginationResult(page);
    }

    public PaginationResult<PaginationEntity> getPaginatedEntitiesWithSort(int pageNo, int pageSize, String sortField, Sort.Direction sortDirection) {
        Pageable pageable = PaginationUtils.createPageableWithSort(pageNo, pageSize, sortField, sortDirection);
        Page<PaginationEntity> page = paginationEntityRepository.customFindAll(pageable);
        return PaginationUtils.createPaginationResult(page);
    }

    public PaginationResult<PaginationEntity> getPaginatedEntitiesWithMultiSort(int pageNo, int pageSize, String[] sortFields, Sort.Direction[] sortDirections) {
        Pageable pageable = PaginationUtils.createPageableWithMultiSort(pageNo, pageSize, sortFields, sortDirections);
        Page<PaginationEntity> page = paginationEntityRepository.customFindAll(pageable);
        return PaginationUtils.createPaginationResult(page);
    }
}