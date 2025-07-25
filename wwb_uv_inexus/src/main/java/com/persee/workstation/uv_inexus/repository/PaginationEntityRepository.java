package com.persee.workstation.uv_inexus.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.persee.workstation.uv_inexus.entity.PaginationEntity;

public interface PaginationEntityRepository extends JpaRepository<PaginationEntity, Long> {
    @Query("SELECT p FROM PaginationEntity p")
    Page<PaginationEntity> customFindAll(@Param("pageable") Pageable pageable);
}