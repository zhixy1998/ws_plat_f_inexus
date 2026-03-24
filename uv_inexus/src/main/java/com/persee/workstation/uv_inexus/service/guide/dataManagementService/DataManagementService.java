/*
 * *
 *  * 版权所有 (C) 2025 北京普析通用仪器有限责任公司。保留所有权利。
 *  * 本软件受商业机密法和版权法保护。
 *  * 本代码仅限与北京普析通用仪器有限责任公司签订有效协议的授权用户内部使用。
 *  * 任何未经明确许可的复制、使用、修改、分发或公开均被严格禁止。
 *
 */

package com.persee.workstation.uv_inexus.service.guide.dataManagementService;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.persee.workstation.uv_inexus.dto.concise.common.Box;
import com.persee.workstation.uv_inexus.dto.guide.ProjectExportRequestDto;
import com.persee.workstation.uv_inexus.dto.guide.ProjectImportDto;
import com.persee.workstation.uv_inexus.dto.guide.ProjectImportResultDto;
import com.persee.workstation.uv_inexus.dto.measurementDto.FormulaCheckParam;
import com.persee.workstation.uv_inexus.dto.measurementDto.ProjectManagementDto;
import com.persee.workstation.uv_inexus.dto.measurementDto.UnFinishTasksAndNumParam;
import com.persee.workstation.uv_inexus.model.Result;
import com.persee.workstation.uv_inexus.po.concise.measurementPo.MethodAndTasks;
import com.persee.workstation.uv_inexus.po.guide.measurementPo.TMeasureMethod;
import com.persee.workstation.uv_inexus.po.guide.measurementPo.TaskQueryParam;
import com.persee.workstation.uv_inexus.po.guide.measurementPo.UnFinishTasksAndNum;
import com.persee.workstation.uv_inexus.po.guide.measurementPo.model.ProjectQueryParam;
import com.persee.workstation.uv_inexus.vo.concise.common.MethodStatisticVo;
import com.persee.workstation.uv_inexus.vo.concise.common.ProjectStatisticVo;
import com.persee.workstation.uv_inexus.vo.concise.common.TaskStatisticVo;
import com.persee.workstation.uv_inexus.vo.measurementVo.TaskDetailVO;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.util.Map;

public interface DataManagementService {
    // 查询未完成任务和已完成任务
    Result<UnFinishTasksAndNum> getTasks(UnFinishTasksAndNumParam unFinishTasksAndNumParam);

    // 查询项目统计信息
    Result queryUserProjectStatistics(ProjectStatisticVo vo);

    //查询项目详情
    Result queryProjectDetail(String projectId);

    /**
     * 导出项目为xlsx
     */
    void exportProjects(ProjectExportRequestDto requestDto, HttpServletResponse response);

    /**
     * 从xlsx导入项目
     */
    Result<ProjectImportResultDto> importProjects(MultipartFile file, ProjectImportDto importDto);

    /**
     * 下载导入模板
     */
    void downloadTemplate(HttpServletResponse response);

    // 查询任务统计信息
    Result queryUserTaskStatistics(TaskStatisticVo vo);

    //查询任务详情
    Result<TaskDetailVO> queryTaskDetail(String taskId);

    // 查询方法统计信息
    Result queryUserMethodStatistics(MethodStatisticVo vo);

    //查询方法详情
    Result<TMeasureMethod> queryMethodDetail(String methodId);

    Result<Map<String,Object>> getTasksManagementList(TaskQueryParam taskQueryParam);



    Result<MethodAndTasks> getMethodsAndTasksNum(String userKey);

    Result<Page<ProjectManagementDto>> getProjectsManagementList(ProjectQueryParam projectQueryParam);




    Result<String> formulaCheck(FormulaCheckParam formulaCheckParam);

    Result<String> deleteTask(Box deleteTaskIdList);

    Result<String> deleteMethod(Box deleteMethodIdList);

    Result<String> deleteProject(Box deleteProjectIdList);
}
