package com.persee.workstation.uv_inexus.service.guide.dataManagementService;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.persee.workstation.uv_inexus.dto.measurementDto.MethodManagementDto;
import com.persee.workstation.uv_inexus.dto.measurementDto.ProjectManagementDto;
import com.persee.workstation.uv_inexus.dto.measurementDto.SelectMethodsByTypeResult;
import com.persee.workstation.uv_inexus.dto.measurementDto.TaskManagementDto;
import com.persee.workstation.uv_inexus.model.R;


import java.util.List;

public interface DataManagementService {
    R<List<MethodManagementDto>> getMethodManagement();


    R<Page<MethodManagementDto>> selectMethods(String selectParm, Integer pageNum, Integer pageSize);

    R<List<SelectMethodsByTypeResult>> selectMethodsByType();

    R<List<ProjectManagementDto>> getProjectsManagementList();

    R<List<TaskManagementDto>> getTasksManagementList();

    R<Page<ProjectManagementDto>> selectProjects(String selectParm, Integer pageNum, Integer pageSize);

    R<List<Integer>> getMethodsAndTasks(String userKey);

    R<List<Integer>> getMethodsAndSuccessful(String userKey);

    R<List<Integer>> getTasksAndSuccessful(String userKey);

}
