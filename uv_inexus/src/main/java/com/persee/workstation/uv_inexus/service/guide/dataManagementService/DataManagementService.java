package com.persee.workstation.uv_inexus.service.guide.dataManagementService;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.persee.workstation.uv_inexus.dto.measurementDto.MethodManagementDto;
import com.persee.workstation.uv_inexus.dto.measurementDto.ProjectManagementDto;
import com.persee.workstation.uv_inexus.dto.measurementDto.SelectMethodsByTypeResult;
import com.persee.workstation.uv_inexus.dto.measurementDto.TaskManagementDto;
import com.persee.workstation.uv_inexus.model.R;
import com.persee.workstation.uv_inexus.po.concise.measurementPo.MethodAndTasks;
import com.persee.workstation.uv_inexus.po.guide.measurementPo.TaskQueryParam;
import com.persee.workstation.uv_inexus.po.guide.measurementPo.model.ProjectQueryParam;


import java.util.List;

public interface DataManagementService {


    R<Page<TaskManagementDto>> getTasksManagementList(TaskQueryParam taskQueryParam);



    R<MethodAndTasks> getMethodsAndTasksNum(String userKey);

    R<Page<ProjectManagementDto>> getProjectsManagementList(ProjectQueryParam projectQueryParam);
}
