package com.persee.workstation.uv_inexus.service.measurementService;

import com.persee.workstation.uv_inexus.dto.measurementDto.CreateTaskParamDto;
import com.persee.workstation.uv_inexus.dto.measurementDto.ProjectParamDto;
import com.persee.workstation.uv_inexus.model.Result;

public interface ExperimentTestService {
    Result newProject(ProjectParamDto paramDto);

    Result createTask(CreateTaskParamDto paramDto);

    Result queryAllTask(String userName, Integer quantity);
}
