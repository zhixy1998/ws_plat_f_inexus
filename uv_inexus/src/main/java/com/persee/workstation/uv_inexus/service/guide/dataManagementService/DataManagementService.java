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

package com.persee.workstation.uv_inexus.service.guide.dataManagementService;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.persee.workstation.uv_inexus.dto.concise.common.Box;
import com.persee.workstation.uv_inexus.dto.measurementDto.FormulaCheckParam;
import com.persee.workstation.uv_inexus.dto.measurementDto.ProjectManagementDto;
import com.persee.workstation.uv_inexus.dto.measurementDto.TaskManagementDto;
import com.persee.workstation.uv_inexus.dto.measurementDto.UnFinishTasksAndNumParam;
import com.persee.workstation.uv_inexus.model.Result;
import com.persee.workstation.uv_inexus.po.concise.measurementPo.MethodAndTasks;
import com.persee.workstation.uv_inexus.po.guide.measurementPo.TaskQueryParam;
import com.persee.workstation.uv_inexus.po.guide.measurementPo.UnFinishTasksAndNum;
import com.persee.workstation.uv_inexus.po.guide.measurementPo.model.ProjectQueryParam;

public interface DataManagementService {


    Result<Page<TaskManagementDto>> getTasksManagementList(TaskQueryParam taskQueryParam);



    Result<MethodAndTasks> getMethodsAndTasksNum(String userKey);

    Result<Page<ProjectManagementDto>> getProjectsManagementList(ProjectQueryParam projectQueryParam);

    Result<UnFinishTasksAndNum> getTasks(UnFinishTasksAndNumParam unFinishTasksAndNumParam);


    Result<String> formulaCheck(FormulaCheckParam formulaCheckParam);

    Result<String> deleteTask(Box deleteTaskIdList);

    Result<String> deleteMethod(Box deleteMethodIdList);

    Result<String> deleteProject(Box deleteProjectIdList);
}
