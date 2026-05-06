/*
 * *
 *  * 版权所有 (C) 2025 北京普析通用仪器有限责任公司。保留所有权利。
 *  * 本软件受商业机密法和版权法保护。
 *  * 本代码仅限与北京普析通用仪器有限责任公司签订有效协议的授权用户内部使用。
 *  * 任何未经明确许可的复制、使用、修改、分发或公开均被严格禁止。
 *
 */

package com.persee.workstation.uv_inexus.service.guide.dataManagementService.impl;


import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.persee.workstation.uv_inexus.constant.Constants;
import com.persee.workstation.uv_inexus.dao.guide.measurementDao.*;
import com.persee.workstation.uv_inexus.dao.sysUserPermissionDao.SysUserGroupMapper;
import com.persee.workstation.uv_inexus.dao.sysUserPermissionDao.SystemUserManageDAO;
import com.persee.workstation.uv_inexus.dto.concise.common.*;
import com.persee.workstation.uv_inexus.dto.guide.ProjectExportRequestDto;
import com.persee.workstation.uv_inexus.dto.guide.ProjectImportDto;
import com.persee.workstation.uv_inexus.dto.guide.ProjectImportResultDto;
import com.persee.workstation.uv_inexus.dto.measurementDto.*;
import com.persee.workstation.uv_inexus.enums.measurementEnums.MeasureTypeEnum;
import com.persee.workstation.uv_inexus.enums.measurementEnums.TaskEnum;
import com.persee.workstation.uv_inexus.enums.measurementEnums.TaskStatusEnum;
import com.persee.workstation.uv_inexus.i18n.YamlMessageSource;
import com.persee.workstation.uv_inexus.model.R;
import com.persee.workstation.uv_inexus.model.Result;
import com.persee.workstation.uv_inexus.po.concise.measurementPo.MethodAndTasks;
import com.persee.workstation.uv_inexus.po.concise.measurementPo.TaskCount;
import com.persee.workstation.uv_inexus.po.concise.measurementPo.TaskCountByTime;
import com.persee.workstation.uv_inexus.po.guide.measurementPo.*;
import com.persee.workstation.uv_inexus.po.guide.measurementPo.model.ProjectQueryParam;
import com.persee.workstation.uv_inexus.po.guide.measurementPo.model.TMeasureProjectCollaboratorGroup;
import com.persee.workstation.uv_inexus.po.guide.measurementPo.model.TaskSpecimen;
import com.persee.workstation.uv_inexus.po.systemUserPermissionPo.SysUserGroupPO;
import com.persee.workstation.uv_inexus.po.systemUserPermissionPo.SysUserPO;
import com.persee.workstation.uv_inexus.service.guide.dataManagementService.DataManagementService;
import com.persee.workstation.uv_inexus.service.guide.measurementService.TMeasureProjectCollaboratorGroupService;
import com.persee.workstation.uv_inexus.service.guide.parseReport.LaTeXConverter;
import com.persee.workstation.uv_inexus.service.sysUserPermissionService.SysUserService;
import com.persee.workstation.uv_inexus.vo.concise.common.MethodStatisticVo;
import com.persee.workstation.uv_inexus.vo.concise.common.ProjectStatisticVo;
import com.persee.workstation.uv_inexus.vo.concise.common.TaskStatisticVo;
import com.persee.workstation.uv_inexus.vo.measurementVo.SampleInfoVO;
import com.persee.workstation.uv_inexus.vo.measurementVo.TaskDetailVO;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;


@Service
@Slf4j
public class DataManagementServiceImpl implements DataManagementService {

    @Autowired
    private TMeasureMethodMapper methodManagementMapper;
    @Autowired
    private TMeasureProjectMapper projectManagementMapper;
    @Autowired
    private TMeasureTaskMapper taskManagementMapper;
    @Autowired
    private SystemUserManageDAO systemUserManageDAO;
    @Autowired
    private TMeasureSampleMapper sampleManagementMapper;
    @Autowired
    private TMeasureDataMapper dataManagementMapper;
    @Autowired
    private TMeasureAnalysisDataMapper analysisDataMapper;
    @Autowired
    private TMeasureAnalysisImageMapper analysisImageMapper;
    @Autowired
    private SysUserService userService;
    @Autowired
    private SysUserGroupMapper userGroupMapper ;
    @Autowired
    private TMeasureProjectCollaboratorGroupService collaboratorGroupService;

    // Excel列索引
    private static final int COL_PROJECT_NAME = 0;
    private static final int COL_REMARK = 1;
    private static final int COL_CREATOR = 2;
    private static final int COL_CREATE_TIME = 3;
    private static final int COL_OWNER_GROUP = 4;
    private static final int COL_FIELD5 = 5;

    /**
     * 查询未完成任务和已完成任务
     * @param unFinishTasksAndNumParam
     * @return
     */
    @Override
    public Result<UnFinishTasksAndNum> getTasks(UnFinishTasksAndNumParam unFinishTasksAndNumParam) {
        if (unFinishTasksAndNumParam == null) {
            //参数不能为空
            return Result.fail(YamlMessageSource.getMessageStatic("00306003"));
        }
        if (unFinishTasksAndNumParam.getNeedSelectNum() == null || unFinishTasksAndNumParam.getNeedSelectNum() == 0) {
            //数量不能为0
            return Result.fail(YamlMessageSource.getMessageStatic("00306004"));
        }
        if (unFinishTasksAndNumParam.getNeedSelectNum() > 50) {
            //数量不能大于50
            return Result.fail(YamlMessageSource.getMessageStatic("00306005"));
        }
        if (unFinishTasksAndNumParam.getTaskType().isEmpty()) {
            //类型不能为空
            return Result.fail(YamlMessageSource.getMessageStatic("0000004B"));
        }
        if (unFinishTasksAndNumParam.getSystemIdentityId() == null){
            return Result.fail("用户未登录");
        }
        String status = unFinishTasksAndNumParam.getStatus();

        String taskType = unFinishTasksAndNumParam.getTaskType();
        int needSelectNum = unFinishTasksAndNumParam.getNeedSelectNum();
        String selectKey = unFinishTasksAndNumParam.getSelectKey();
        LambdaQueryWrapper<TMeasureTask> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(TMeasureTask::getMeasureType, taskType);
        // 如果status等于2的情况下则查询已完成的任务,如果小于2则查询未完成的任务
        if (status.equals(Constants.PARAM_ONE)){
            // 查询taskStatus为2（结束）的记录
            queryWrapper.eq(TMeasureTask::getTaskStatus, TaskStatusEnum.COMPLETED.getCode());
        } else {
            // 查询taskStatus为0（未测）或1（已测）的记录
            queryWrapper.in(TMeasureTask::getTaskStatus, Arrays.asList(TaskStatusEnum.UNTESTED.getCode(), TaskStatusEnum.TESTED.getCode()));
        }
        queryWrapper.eq(TMeasureTask::getUserId, unFinishTasksAndNumParam.getSystemIdentityId());
        if (selectKey!=null&&!selectKey.isEmpty()){
            queryWrapper.like(TMeasureTask::getTaskName, selectKey);
        }
        List<TMeasureTask> tMeasureTasks = taskManagementMapper.selectList(queryWrapper);

        UnFinishTasksAndNum unFinishTasksAndNum = new UnFinishTasksAndNum();
        int size = Math.min(tMeasureTasks.size(), needSelectNum);

        List<TMeasureTaskDto> newDtolist = new ArrayList<>();
        for (int i = 0; i < size; i++) {

            String taskString = TaskEnum.getTaskStr(tMeasureTasks.get(i).getIsDelete());
            TMeasureTaskDto tMeasureTaskDto = new TMeasureTaskDto();

            BeanUtils.copyProperties(tMeasureTasks.get(i), tMeasureTaskDto);
            tMeasureTaskDto.setStatus(taskString);

            String methodKey = tMeasureTasks.get(i).getMethodKey();
            LambdaQueryWrapper<TMeasureMethod> queryWrapper1 = new LambdaQueryWrapper<>();
            queryWrapper1.eq(TMeasureMethod::getId, methodKey);
            TMeasureMethod tMeasureMethod = methodManagementMapper.selectOne(queryWrapper1);
            String methodName = tMeasureMethod.getMethodName();
            tMeasureTaskDto.setMethodId(methodKey);
            tMeasureTaskDto.setMethodName(methodName);

            String projectItemKey = tMeasureTasks.get(i).getProjectItemKey();
            LambdaQueryWrapper<TMeasureProject> queryWrapper2 = new LambdaQueryWrapper<>();
            queryWrapper2.eq(TMeasureProject::getId, projectItemKey);
            TMeasureProject tMeasureProject = projectManagementMapper.selectOne(queryWrapper2);
            String projectName = tMeasureProject.getProjectName();
            tMeasureTaskDto.setProjectName(projectName);
            tMeasureTaskDto.setProjectId(projectItemKey);

            String sampleKey = tMeasureTasks.get(i).getSampleKey();
            List<TaskSpecimen> taskSpecimens = JSON.parseArray(sampleKey, TaskSpecimen.class);
            int count = 0;
            if (taskSpecimens!=null&& taskSpecimens.size() > 0){
                for (int j = 0; j < taskSpecimens.size(); j++) {
                    String deleteType = taskSpecimens.get(j).getDeleteType();
                    if (deleteType.equals("0")) {
                        count++;
                    }
                }
            }
            tMeasureTaskDto.setSampleNumber(count);


            tMeasureTaskDto.setCreateTime(tMeasureTasks.get(i).getCreateTime());
            tMeasureTaskDto.setStartTime(tMeasureTasks.get(i).getStartTime());
            tMeasureTaskDto.setEndTime(tMeasureTasks.get(i).getEndTime());
            newDtolist.add(tMeasureTaskDto);
        }

        unFinishTasksAndNum.setTasks(newDtolist);

        return Result.ok(unFinishTasksAndNum);


    }

    /**
     * 查询用户项目统计
     * @param vo
     * @return
     */
    @Override
    public Result queryUserProjectStatistics(ProjectStatisticVo vo) {
        if (vo.getUpdateBy() == null){
            return Result.fail("用户未登录");
        }
        // 获取分页参数，设置默认值
        Integer pageNum = vo.getPageNum() != null ? vo.getPageNum() : 1;
        Integer pageSize = vo.getPageSize() != null ? vo.getPageSize() : 10;

        // 创建分页对象
        Page<TMeasureProject> page = new Page<>(pageNum, pageSize);

        SysUserPO user = userService.getOne(new QueryWrapper<SysUserPO>().eq("username", vo.getUpdateBy()).last("LIMIT 1"));
        if (user == null){
            return Result.fail("用户不存在");
        }
        // 根据用户查询用户所在的组
        SysUserGroupPO userGroupPO = userGroupMapper.selectOne(new QueryWrapper<SysUserGroupPO>().eq("user_id", user.getId()).last("LIMIT 1"));
        if (userGroupPO == null){
            return Result.fail("用户未加入任何组");
        }
        // 根据查询到的组id条件查询协作组中所有的组内的协作项目id
        List<TMeasureProjectCollaboratorGroup> projectList = collaboratorGroupService.list(new QueryWrapper<TMeasureProjectCollaboratorGroup>().eq("group_id", userGroupPO.getGroupId()));
        QueryWrapper<TMeasureProject> wrapper = new QueryWrapper<>();
        List<String> collects;
        if (projectList != null && projectList.size() > 0){
            collects=projectList.stream().map(TMeasureProjectCollaboratorGroup::getProjectId).collect(Collectors.toList());
        } else {
            collects = new ArrayList<>();
        }

        wrapper.and(qw -> {
            // 第一个条件：创建者匹配
            qw.eq("creator_key", vo.getUpdateBy());

            // 第二个条件：协作者项目列表（动态判断）
            if (CollectionUtils.isNotEmpty(collects)) {
                qw.or().in("id", collects);
            }

            // 第三个条件：用户组匹配
            qw.or().eq("owner_group_id", userGroupPO.getGroupId());
        });

        List<TMeasureProject> projects = projectManagementMapper.selectList(wrapper);

        ProjectStatisticDto result = new ProjectStatisticDto();
        if (projects == null || projects.size() == 0){
            result.setProjectNum(0L);
            result.setTaskNum(0L);
            result.setMethodNum(0L);
            result.setProjectDataNum(new ArrayList<>());
            result.setTotal(0L);
            result.setPageNum(pageNum);
            result.setPageSize(pageSize);
            result.setPages(0L);
            result.setProjectList(new ArrayList<>());
            return Result.ok(result);
        }
        // 根据projects中的项目id查询所有包含这个项目的任务和方法
        List<String> collect = projects.stream().map(TMeasureProject::getId).collect(Collectors.toList());
        Long taskNums = taskManagementMapper.selectCount(
                new LambdaQueryWrapper<TMeasureTask>()
                        .in(TMeasureTask::getProjectItemKey, collect)
                        .eq(TMeasureTask::getTaskStatus, Constants.PARAM_TWO)
        );
        Long methodNums = methodManagementMapper.selectCount(new QueryWrapper<TMeasureMethod>().in("project_item_key", collect));
        ArrayList<ProjectDataNumDto> dataNumList = new ArrayList<>();
        ArrayList<ProjectSumListDto> dataSumList = new ArrayList<>();
        if (vo.getPageNum() != null){
            wrapper.like("project_name", vo.getProjectName());
        }
        List<TMeasureProject> projects1 = projectManagementMapper.selectList(wrapper);
        // 根据projects1中的项目id查询所有包含这个项目的任务和方法
        for (TMeasureProject allProject : projects1) {
            ProjectDataNumDto numDto = new ProjectDataNumDto();
            // 根据项目id查询所有包含这个项目的任务
            Long projectItemKey = taskManagementMapper.selectCount(new QueryWrapper<TMeasureTask>().eq("project_item_key", allProject.getId()));
            // 根据项目id查询所有包含这个项目的方法
            Long methodNum = methodManagementMapper.selectCount(new QueryWrapper<TMeasureMethod>().eq("project_item_key", allProject.getId()));
            numDto.setProjectName(allProject.getProjectName());
            numDto.setTaskNum(projectItemKey);
            numDto.setMethodNum(methodNum);
            dataNumList.add(numDto);
        }

        // 执行分页查询，projects1 现在是分页后的结果
        Page<TMeasureProject> projectPage = projectManagementMapper.selectPage(page, wrapper);
        List<TMeasureProject> pageProjects = projectPage.getRecords();
        for (TMeasureProject pageProject : pageProjects) {
            ProjectSumListDto sumListDto = new ProjectSumListDto();
            // 根据项目id查询所有包含这个项目的任务
            Long projectItemKey = taskManagementMapper.selectCount(new QueryWrapper<TMeasureTask>().eq("project_item_key", pageProject.getId()));
            // 根据项目id查询所有包含这个项目的方法
            Long methodNum = methodManagementMapper.selectCount(new QueryWrapper<TMeasureMethod>().eq("project_item_key", pageProject.getId()));
            sumListDto.setProjectId(pageProject.getId());
            sumListDto.setProjectName(pageProject.getProjectName());
            sumListDto.setCreateBy(pageProject.getCreatorKey());
            sumListDto.setCreateTime(pageProject.getCreateTime());
            sumListDto.setMethodNum(methodNum);
            sumListDto.setTaskNum(projectItemKey);
            sumListDto.setUpdateTime(pageProject.getLastUpdatedTime());
            dataSumList.add(sumListDto);
        }
        result.setProjectNum((long) projects.size());
        result.setTaskNum(taskNums);
        result.setMethodNum(methodNums);
        result.setProjectDataNum(dataNumList);
        result.setTotal(projectPage.getTotal()); // 总记录数
        result.setPageNum(pageNum);
        result.setPageSize(pageSize);
        result.setPages(projectPage.getPages()); // 总页数
        result.setProjectList(dataSumList);
        return Result.ok(result);
    }

    /**
     * 项目详情
     * @param projectId
     * @return
     */
    @Override
    public Result queryProjectDetail(String projectId) {
        // 校验入参
        if (StringUtils.isBlank(projectId)) {
            return Result.fail(YamlMessageSource.getMessageStatic("00000045"));
        }

        // 查询项目
        TMeasureProject project = projectManagementMapper.selectById(projectId);
        if (project == null || !"0".equals(project.getIsDelete())) {
            return Result.fail(YamlMessageSource.getMessageStatic("00301010"));
        }
        // 组装返回数据
        Map<String, String> map = new HashMap<>();
        map.put("projectName", project.getProjectName());
        map.put("remark", project.getRemark() != null ? project.getRemark() : "");
        map.put("projectId",projectId);

        return Result.ok(map);
    }

    @Override
    public void exportProjects(ProjectExportRequestDto requestDto, HttpServletResponse response) {
        try {
            List<String> projectIds = requestDto.getProjectIds();

            // 1. 参数校验
            if (projectIds == null || projectIds.isEmpty()) {
                writeErrorResponse(response, "请选择要导出的项目");
                return;
            }

            // 2. 查询项目数据
            List<TMeasureProject> projects = projectManagementMapper.selectList(
                    new QueryWrapper<TMeasureProject>().in("id", projectIds)
            );

            if (projects.isEmpty()) {
                writeErrorResponse(response, "未找到要导出的项目");
                return;
            }

            // 3. 创建Excel文件
            String fileName = "projects_export_" +
                    LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")) + ".xlsx";

            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setHeader("Content-Disposition",
                    "attachment; filename=\"" + URLEncoder.encode(fileName, "UTF-8") + "\"");

            try (Workbook workbook = new XSSFWorkbook();
                 OutputStream os = response.getOutputStream()) {

                Sheet sheet = workbook.createSheet("项目数据");

                // 创建标题行
                createHeaderRow(sheet);

                // 填充数据行
                int rowNum = 1;
                for (TMeasureProject project : projects) {
                    Row row = sheet.createRow(rowNum++);
                    row.createCell(COL_PROJECT_NAME).setCellValue(project.getProjectName());
                    row.createCell(COL_REMARK).setCellValue(project.getRemark() != null ? project.getRemark() : "");
                    row.createCell(COL_CREATOR).setCellValue(project.getCreatorKey() != null ? project.getCreatorKey() : "");
                    row.createCell(COL_CREATE_TIME).setCellValue(
                            project.getCreateTime() != null ?
                                    project.getCreateTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) : "");
                    row.createCell(COL_OWNER_GROUP).setCellValue(project.getOwnerGroupId() != null ? project.getOwnerGroupId() : "");
                    row.createCell(COL_FIELD5).setCellValue(project.getField5() != null ? project.getField5() : "");
                }

                // 自动调整列宽
                for (int i = 0; i <= COL_FIELD5; i++) {
                    sheet.autoSizeColumn(i);
                }

                workbook.write(os);
                os.flush();
            }

        } catch (Exception e) {
            log.error("导出项目失败", e);
            writeErrorResponse(response, "导出失败：" + e.getMessage());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<ProjectImportResultDto> importProjects(MultipartFile file, ProjectImportDto importDto) {
        ProjectImportResultDto result = new ProjectImportResultDto();
        List<String> successIds = new ArrayList<>();
        List<String> failedNames = new ArrayList<>();
        List<String> skippedNames = new ArrayList<>();

        int successCount = 0;
        int failCount = 0;
        int skipCount = 0;

        try {
            // 1. 参数校验
            if (file.isEmpty()) {
                return Result.fail("导入文件不能为空");
            }

            if (!StringUtils.isNotBlank(importDto.getUpdateBy())) {
                return Result.fail("导入负责人不能为空");
            }

            // 2. 获取当前用户信息
            SysUserPO currentUser = systemUserManageDAO.selectOne(
                    new QueryWrapper<SysUserPO>().eq("username", importDto.getUpdateBy())
            );

            if (currentUser == null) {
                return Result.fail("用户不存在: " + importDto.getUpdateBy());
            }

            // 3. 获取用户组信息
            SysUserGroupPO userGroup = userGroupMapper.selectOne(
                    new QueryWrapper<SysUserGroupPO>().eq("user_id", currentUser.getId())
            );

            if (userGroup == null) {
                return Result.fail("用户未加入任何组: " + importDto.getUpdateBy());
            }

            // 4. 解析Excel文件
            try (InputStream is = file.getInputStream();
                 Workbook workbook = new XSSFWorkbook(is)) {

                Sheet sheet = workbook.getSheetAt(0);
                if (sheet == null) {
                    return Result.fail("Excel文件中没有数据");
                }

                // 从第2行开始读取（跳过标题行）
                int rowCount = sheet.getPhysicalNumberOfRows();
                for (int i = 1; i < rowCount; i++) {
                    Row row = sheet.getRow(i);
                    if (row == null) continue;

                    try {
                        // 读取数据
                        String projectName = getCellValue(row.getCell(COL_PROJECT_NAME));
                        if (!StringUtils.isNotBlank(projectName)) {
                            continue; // 项目名称为空，跳过
                        }

                        String remark = getCellValue(row.getCell(COL_REMARK));
                        String field5 = getCellValue(row.getCell(COL_FIELD5));

                        // 检查项目是否已存在
                        TMeasureProject existingProject = projectManagementMapper.selectOne(
                                new QueryWrapper<TMeasureProject>()
                                        .eq("project_name", projectName)
                                        .eq("is_delete", "0")
                        );

                        if (existingProject != null && !importDto.getOverwrite()) {
                            // 项目存在且不覆盖，跳过
                            skippedNames.add(projectName);
                            skipCount++;
                            continue;
                        }

                        // 创建或更新项目
                        TMeasureProject project;
                        if (existingProject != null && importDto.getOverwrite()) {
                            project = existingProject;
                        } else {
                            project = new TMeasureProject();
                            project.setId(UUID.randomUUID().toString());
                            project.setCreateTime(LocalDateTime.now());
                        }

                        project.setProjectName(projectName);
                        project.setRemark(remark);
                        project.setField5(field5);
                        project.setLastUpdatedTime(LocalDateTime.now());
                        project.setCreatorKey(importDto.getUpdateBy());
                        project.setLastUpdatedBy(importDto.getUpdateBy());
                        project.setOwnerGroupId(userGroup.getGroupId());
                        project.setIsDelete("0");

                        // 保存
                        if (existingProject != null && importDto.getOverwrite()) {
                            projectManagementMapper.updateById(project);
                        } else {
                            projectManagementMapper.insert(project);
                        }

                        successIds.add(project.getId());
                        successCount++;

                    } catch (Exception e) {
                        log.error("导入第{}行失败", i+1, e);
                        failedNames.add("第" + (i+1) + "行");
                        failCount++;
                    }
                }
            }

            // 5. 构建返回结果
            result.setSuccessCount(successCount);
            result.setFailCount(failCount);
            result.setSkipCount(skipCount);
            result.setSuccessIds(successIds);
            result.setFailedNames(failedNames);
            result.setSkippedNames(skippedNames);

            return Result.ok(result);

        } catch (Exception e) {
            log.error("导入项目失败", e);
            return Result.fail("导入失败：" + e.getMessage());
        }
    }

    @Override
    public void downloadTemplate(HttpServletResponse response) {
        try {
            String fileName = "project_import_template.xlsx";

            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setHeader("Content-Disposition",
                    "attachment; filename=\"" + URLEncoder.encode(fileName, "UTF-8") + "\"");

            try (Workbook workbook = new XSSFWorkbook();
                 OutputStream os = response.getOutputStream()) {

                Sheet sheet = workbook.createSheet("项目导入模板");

                // 创建标题行
                createHeaderRow(sheet);

                // 创建示例行
                Row exampleRow = sheet.createRow(1);
                exampleRow.createCell(COL_PROJECT_NAME).setCellValue("示例项目名称");
                exampleRow.createCell(COL_REMARK).setCellValue("项目备注");
                exampleRow.createCell(COL_CREATOR).setCellValue("创建人");
                exampleRow.createCell(COL_CREATE_TIME).setCellValue("2024-01-01 00:00:00");
                exampleRow.createCell(COL_OWNER_GROUP).setCellValue("group123");
                exampleRow.createCell(COL_FIELD5).setCellValue("预留字段");

                // 设置列宽
                for (int i = 0; i <= COL_FIELD5; i++) {
                    sheet.autoSizeColumn(i);
                }

                workbook.write(os);
                os.flush();
            }

        } catch (Exception e) {
            log.error("下载模板失败", e);
            writeErrorResponse(response, "下载模板失败：" + e.getMessage());
        }
    }

    /**
     * 创建标题行
     */
    private void createHeaderRow(Sheet sheet) {
        Row headerRow = sheet.createRow(0);
        headerRow.createCell(COL_PROJECT_NAME).setCellValue("项目名称");
        headerRow.createCell(COL_REMARK).setCellValue("备注");
        headerRow.createCell(COL_CREATOR).setCellValue("创建人");
        headerRow.createCell(COL_CREATE_TIME).setCellValue("创建时间");
        headerRow.createCell(COL_OWNER_GROUP).setCellValue("归属组ID");
        headerRow.createCell(COL_FIELD5).setCellValue("预留字段5");

        // 设置标题行样式
        CellStyle style = sheet.getWorkbook().createCellStyle();
        Font font = sheet.getWorkbook().createFont();
        font.setBold(true);
        style.setFont(font);

        for (int i = 0; i <= COL_FIELD5; i++) {
            headerRow.getCell(i).setCellStyle(style);
        }
    }

    /**
     * 获取单元格值
     */
    private String getCellValue(Cell cell) {
        if (cell == null) return "";

        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return cell.getDateCellValue().toString();
                } else {
                    return String.valueOf(cell.getNumericCellValue());
                }
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            default:
                return "";
        }
    }

    /**
     * 写入错误响应
     */
    private void writeErrorResponse(HttpServletResponse response, String message) {
        try {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write(JSON.toJSONString(Result.fail(message)));
        } catch (Exception e) {
            log.error("写入错误响应失败", e);
        }
    }

    /**
     * 用户任务统计
     * @param vo
     * @return
     */
    @Override
    public Result queryUserTaskStatistics(TaskStatisticVo vo) {
        if (vo.getUpdateBy() == null){
            return Result.fail("用户未登录");
        }
        // 获取分页参数，设置默认值
        Integer pageNum = vo.getPageNum() != null ? vo.getPageNum() : 1;
        Integer pageSize = vo.getPageSize() != null ? vo.getPageSize() : 10;

        // 创建分页对象
        Page<TMeasureTask> page = new Page<>(pageNum, pageSize);
        SysUserPO user = userService.getOne(new QueryWrapper<SysUserPO>().eq("username", vo.getUpdateBy()));
        if (user == null){
            return Result.fail("用户不存在");
        }
        // 根据用户id查询所有任务
        List<TMeasureTask> tasks = taskManagementMapper.selectList(new QueryWrapper<TMeasureTask>().eq("user_id", user.getId()).eq("is_delete","0"));
        TaskStatisticDto result = new TaskStatisticDto();
        if (tasks == null || tasks.size() == 0){
            result.setTaskTotal(0L);
            result.setTaskCompleteNum(0L);
            result.setTaskDataNum(new ArrayList<>());
            result.setTotal(0L);
            result.setPageNum(pageNum);
            result.setPageSize(pageSize);
            result.setPages(0L);
            result.setTaskList(new ArrayList<>());
            return Result.ok(result);
        }

        // 构建查询条件（分页查询用）
        QueryWrapper<TMeasureTask> pageWrapper = new QueryWrapper<>();
        pageWrapper.eq("user_id", user.getId());

        // 根据测量模式筛选
        if (StringUtils.isNotBlank(vo.getMeasureType())) {
            pageWrapper.eq("measure_type", vo.getMeasureType());
        }

        // 根据任务名称模糊查询
        if (StringUtils.isNotBlank(vo.getTaskName())) {
            pageWrapper.like("task_name", vo.getTaskName());
        }
        pageWrapper.eq("is_delete", "0");

        // 执行分页查询
        Page<TMeasureTask> taskPage = taskManagementMapper.selectPage(page, pageWrapper);
        List<TMeasureTask> taskList = taskPage.getRecords();
        List<TaskSumListDto> taskSumList = new ArrayList<>();

        for (TMeasureTask task : taskList){
            TaskSumListDto taskSumListDto = new TaskSumListDto();
            // 根据任务中的方法id查询方法
            TMeasureMethod method = methodManagementMapper.selectOne(new QueryWrapper<TMeasureMethod>().eq("id", task.getMethodKey()));
            // 根据任务中的项目id查询项目
            TMeasureProject project = projectManagementMapper.selectOne(new QueryWrapper<TMeasureProject>().eq("id", task.getProjectItemKey()));
            List<TaskSpecimen> specimen = JSON.parseArray(task.getSampleKey(), TaskSpecimen.class);
            taskSumListDto.setTaskId(task.getId());
            taskSumListDto.setMethodName(method.getMethodName());
            taskSumListDto.setProjectName(project.getProjectName());
            taskSumListDto.setMeasureMode(task.getMeasureType());
            taskSumListDto.setTaskStatus(TaskStatusEnum.UNTESTED.getCode().equals(task.getTaskStatus())?"未测":TaskStatusEnum.TESTED.getCode().equals(task.getTaskStatus())?"已测":"结束");
            if (specimen != null && !specimen.isEmpty()){
                taskSumListDto.setSampleNum((long)specimen.size());
            }else{
                taskSumListDto.setSampleNum(0L);
            }
            taskSumListDto.setMeasureStartTime(task.getStartTime());
            taskSumList.add(taskSumListDto);
        }
        // 2. 时间维度统计
        List<TaskDataNumDto> timeStatistics = new ArrayList<>();
        // 获取当前时间
        LocalDateTime now = LocalDateTime.now();
        // 最近一周
        LocalDateTime oneWeekAgo = now.minusWeeks(1);
        timeStatistics.add(getCompletedTaskCount(oneWeekAgo, now,user.getId(),"最近一周"));
        // 最近一个月
        LocalDateTime oneMonthAgo = now.minusMonths(1);
        timeStatistics.add(getCompletedTaskCount(oneMonthAgo, now,user.getId(),"最近一个月"));
        // 最近半年
        LocalDateTime halfYearAgo = now.minusMonths(6);
        timeStatistics.add(getCompletedTaskCount(halfYearAgo, now,user.getId(),"最近半年"));
        // 最近一年
        LocalDateTime oneYearAgo = now.minusYears(1);
        timeStatistics.add(getCompletedTaskCount(oneYearAgo, now,user.getId(),"最近一年"));
        // 最近三年
        LocalDateTime threeYearsAgo = now.minusYears(3);
        timeStatistics.add(getCompletedTaskCount(threeYearsAgo, now,user.getId(),"最近三年"));
        // 所有时间
        timeStatistics.add(getCompletedTaskCount(null,null,user.getId(),"所有时间"));
        result.setTaskDataNum(timeStatistics);

        List<TMeasureTask> collect = (tasks == null ? new ArrayList<TMeasureTask>() : tasks)
                .stream()
                .filter(task -> task != null && TaskStatusEnum.COMPLETED.getCode().equals(task.getTaskStatus()))
                .collect(Collectors.toList());

        result.setTaskTotal((long) tasks.size());
        result.setTaskCompleteNum((long) collect.size());
        result.setTotal(taskPage.getTotal()); // 总记录数
        result.setPageNum(pageNum);
        result.setPageSize(pageSize);
        result.setPages(taskPage.getPages()); // 总页数
        result.setTaskList(taskSumList);
        return Result.ok(result);
    }

    /**
     * 任务详情
     * @param taskId, deviceNumber
     * @return
     */
    @Override
    public Result<TaskDetailVO> queryTaskDetail(String taskId) {
        //  参数校验
        if (StringUtils.isBlank(taskId)) {
            return Result.fail(YamlMessageSource.getMessageStatic("00000045"));
        }

        try {
            // 查询任务基本信息
            TMeasureTask task = taskManagementMapper.selectById(taskId);
            if (task == null || "1".equals(task.getIsDelete())) {
                return Result.fail(YamlMessageSource.getMessageStatic("00000004"));
            }
            String projectId = "";
            String projectName = "";
            if (StringUtils.isNotBlank(task.getProjectItemKey())) {
                TMeasureProject project = projectManagementMapper.selectById(task.getProjectItemKey());
                if (project != null) {
                    projectId = project.getId(); // 获取项目ID
                    projectName = project.getProjectName();
                }
            }
            String methodId = "";
            String methodName = "";
            if (StringUtils.isNotBlank(task.getMethodKey())) {
                TMeasureMethod method = methodManagementMapper.selectById(task.getMethodKey());
                if (method != null) {
                    methodId = method.getId();
                    methodName = method.getMethodName();
                }
            }


            List<SampleInfoVO> sampleVOList = new ArrayList<>();
            if (StringUtils.isNotBlank(task.getSampleKey())) {
                try {
                    List<TaskSpecimen> specimens = JSONObject.parseArray(task.getSampleKey(), TaskSpecimen.class);
                    if (!CollectionUtils.isEmpty(specimens)) {
                        for (TaskSpecimen specimen : specimens) {
                            if ("0".equals(specimen.getDeleteType()) && specimen.getIsTested() != null){
                                TMeasureSample sample = sampleManagementMapper.selectById(specimen.getSpecimenKey());
                                if (sample != null) {
                                    SampleInfoVO sampleVO = new SampleInfoVO();
                                    sampleVO.setSampleId(sample.getId());
                                    sampleVO.setSampleNumber(sample.getSampleNumber());
                                    sampleVO.setSampleName(sample.getSampleName());
                                    sampleVO.setSamplePosition(sample.getSampleLocation());
                                    sampleVOList.add(sampleVO);
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    sampleVOList = new ArrayList<>();
                }
            }

            // 7. 组装VO
            TaskDetailVO vo = new TaskDetailVO();
            vo.setTaskId(task.getId());
            vo.setProjectId(projectId);
            vo.setProjectName(projectName);
            vo.setMethodId(methodId);
            vo.setMethodName(methodName);

            vo.setTaskName(task.getTaskName());
            vo.setMeasurementType(task.getMeasureType());
            vo.setInstrumentName(task.getDeviceSerialNumber());
            vo.setSamples(sampleVOList);

            return Result.ok(vo);
        } catch (Exception e) {
            return Result.fail("查询任务详情失败：" + e.getMessage());
        }
    }

    /**
     * 用户方法统计
     * @param vo
     * @return
     */
    @Override
    public Result queryUserMethodStatistics(MethodStatisticVo vo) {
        if (vo.getUpdateBy() == null){
            return Result.fail("用户未登录");
        }
        // 获取分页参数，设置默认值
        Integer pageNum = vo.getPageNum() != null ? vo.getPageNum() : 1;
        Integer pageSize = vo.getPageSize() != null ? vo.getPageSize() : 10;

        // 创建分页对象
        Page<TMeasureMethod> page = new Page<>(pageNum, pageSize);
        SysUserPO user = userService.getOne(new QueryWrapper<SysUserPO>().eq("username", vo.getUpdateBy()));
        if (user == null){
            return Result.fail("用户不存在");
        }
        /// 1. 查询当前用户创建的所有方法,以及使用方法任务测量后保存状态为1的个数
        // 根据用户名称查询方法
        QueryWrapper<TMeasureMethod> wrapper = new QueryWrapper<>();
        wrapper.eq("method_creator", user.getUsername());
        List<TMeasureMethod> methods = methodManagementMapper.selectList(wrapper);
        MethodStatisticDto result = new MethodStatisticDto();
        if (methods == null || methods.size() == 0){
            result.setMethodTotal(0L);
            result.setMethodCompleteNum(0L);
            result.setMethodDataNum(new ArrayList<>());
            result.setTotal(0L);
            result.setPages(0L);
            result.setPageNum(pageNum);
            result.setPageSize(pageSize);
            result.setMethodList(new ArrayList<>());
            return Result.ok(result);
        }
        result.setMethodTotal((long) methods.size());
        // methods用stream收集id
        List<String> methodIds = methods.stream().map(TMeasureMethod::getId).collect(Collectors.toList());
        // 根据方法id查询任务
        QueryWrapper<TMeasureTask> taskWrapper = new QueryWrapper<>();
        taskWrapper.in("method_key", methodIds);
        taskWrapper.eq("task_status", Constants.PARAM_TWO);
        taskWrapper.eq("is_delete", "0");
        List<TMeasureTask> tasks = taskManagementMapper.selectList(taskWrapper);
        if (tasks == null || tasks.size() == 0){
            result.setMethodCompleteNum(0L);
        }else{
            result.setMethodCompleteNum((long) tasks.size());
        }
        /// 2. 收集四大测量的方法个数
        List<MethodDataNumDto> methodDataNum = new ArrayList<>();
        // 根据四大测量收集方法个数
        List<String> measureTypeDescriptions = MeasureTypeEnum.getMeasureTypeDescriptions();
        for (String measureTypeDescription : measureTypeDescriptions) {
            MethodDataNumDto methodDataNumDto = new MethodDataNumDto();
            methodDataNumDto.setMeasureMode(measureTypeDescription);
            // 根据方法id和测量模式查询方法个数
            QueryWrapper<TMeasureMethod> methodWrapper = new QueryWrapper<>();
            methodWrapper.eq("measure_type", measureTypeDescription);
            List<TMeasureMethod> collect = methods.stream().filter(method -> method.getMeasurementType().equals(measureTypeDescription)).collect(Collectors.toList());
            methodDataNumDto.setMethodNum((long) collect.size());
            methodDataNum.add(methodDataNumDto);
        }
        result.setMethodDataNum(methodDataNum);
        if (vo.getMethodName() != null){
            wrapper.like("method_name", vo.getMethodName());
        }
        // 分页查询满足当前用户的方法
        Page<TMeasureMethod> methodPage = methodManagementMapper.selectPage(page, wrapper);
        List<TMeasureMethod> methodList = methodPage.getRecords();
        List<MethodSumListDto> methodSumList = new ArrayList<>();
        for (TMeasureMethod method : methodList) {
            // 根据项目id查询项目名称
            TMeasureProject project = projectManagementMapper.selectById(method.getProjectItemKey());
            MethodSumListDto methodSumListDto = new MethodSumListDto();
            methodSumListDto.setMethodId(method.getId());
            methodSumListDto.setMethodName(method.getMethodName());
            methodSumListDto.setCreateBy(method.getMethodCreator());
            methodSumListDto.setCreateTime(method.getMethodCreateTime());
            if (project != null){
                methodSumListDto.setProjectName(project.getProjectName());
            }else{
                methodSumListDto.setProjectName("");
            }
            methodSumListDto.setLastUseTime(method.getMethodUseTime());
            methodSumList.add(methodSumListDto);
        }
        result.setTotal(methodPage.getTotal()); // 总记录数
        result.setPageNum((int) methodPage.getCurrent()); // 当前页码
        result.setPageSize((int) methodPage.getSize());
        result.setPages(methodPage.getPages()); // 总页数
        result.setMethodList(methodSumList);
        return Result.ok(result);
    }

    /**
     * 方法详情
     * @param methodId
     * @return
     */
    @Override
    public Result queryMethodDetail(String methodId) {
        // 判断 methodId 是否为空
        if (StringUtils.isBlank(methodId)) {
            return Result.fail(YamlMessageSource.getMessageStatic("00000045"));
        }
        TMeasureMethod method = methodManagementMapper.selectById(methodId);
        if (method == null || !"0".equals(method.getIsDelete())) {
            return Result.fail(YamlMessageSource.getMessageStatic("00000009"));
        }
        // 构建返回结果容器
        Map<String, Object> resultMap = new HashMap<>();
        resultMap.put("methodName", method.getMethodName());
        resultMap.put("methodId", methodId);
        resultMap.put("measurementType", method.getMeasurementType());

        String projectName = "";
        String projectId = "";
        if (StringUtils.isNotBlank(method.getProjectItemKey())) {
            TMeasureProject project = projectManagementMapper.selectById(method.getProjectItemKey());
            if (project != null && !"1".equals(project.getIsDelete())) {
                projectName = project.getProjectName();
                projectId = project.getId();
            }
        }
        resultMap.put("projectId", projectId);
        resultMap.put("projectName", projectName);
        if (StringUtils.isNotBlank(method.getMeasurementParamInfo())) {
            try {
                Map paramMap = JSON.parseObject(method.getMeasurementParamInfo(),Map.class);
                resultMap.put("measurementParamInfo", paramMap);
            } catch (Exception e) {
                resultMap.put("measurementParamInfo", new HashMap<String, Object>());
            }
        } else {
            resultMap.put("measurementParamInfo", new HashMap<String, Object>());
        }

        return Result.ok(resultMap);
    }

    private TaskDataNumDto getCompletedTaskCount(LocalDateTime oneWeekAgo, LocalDateTime now, String id, String daily) {
        TaskDataNumDto taskDataNumDto = new TaskDataNumDto();

        // 第一个查询：task_status != Constants.PARAM_TWO
        QueryWrapper<TMeasureTask> wrapper1 = new QueryWrapper<>();
        wrapper1.eq("user_id", id);
        if (oneWeekAgo != null && now != null) {
            wrapper1.ge("create_time", oneWeekAgo)
                    .le("create_time", now);
        }
        wrapper1.eq("is_delete", "0");
        wrapper1.ne("task_status", Constants.PARAM_TWO);  // 使用 ne 表示不等于
        Long count = taskManagementMapper.selectCount(wrapper1);

        // 第二个查询：task_status == Constants.PARAM_TWO
        QueryWrapper<TMeasureTask> wrapper2 = new QueryWrapper<>();
        wrapper2.eq("user_id", id);
        if (oneWeekAgo != null && now != null) {
            wrapper2.ge("create_time", oneWeekAgo)
                    .le("create_time", now);
        }
        wrapper2.eq("is_delete", "0");
        wrapper2.eq("task_status", Constants.PARAM_TWO);  // 等于 PARAM_TWO
        Long completeCount = taskManagementMapper.selectCount(wrapper2);

        taskDataNumDto.setDaily(daily);
        taskDataNumDto.setTaskNum(count);
        taskDataNumDto.setCompleteNum(completeCount);
        return taskDataNumDto;
    }


    /*
    方法任务全部数据返回
     */
    @Override
    public Result<MethodAndTasks> getMethodsAndTasksNum(String userKey) {
        LambdaQueryWrapper<SysUserPO> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(SysUserPO::getUsername, userKey);
        SysUserPO user = systemUserManageDAO.selectOne(queryWrapper);
        String userName = user.getUsername();
        Integer methodsNum = methodManagementMapper.getMethodsNum(userName);
        MethodAndTasks methodAndTasks = new MethodAndTasks();
        //总创建方法
        methodAndTasks.setAllMethodNum(methodsNum);
        //已完成方法
        String id = user.getId();
        Set<String> method = taskManagementMapper.selectMethodIds(id);
        Integer size = method.size();
        methodAndTasks.setUsedMethodNum(size);
        String userId = user.getId();
        //查询用户参与的所有任务个数
        Integer tasksNum = taskManagementMapper.getTasksNum(userId);
        methodAndTasks.setAllTaskNum(tasksNum);

        //查询已完成的任务
        Integer finishTaskNum = taskManagementMapper.getFinishTaskNum(userId);
        methodAndTasks.setFinishTaskNum(finishTaskNum);
        //方法统计封装
        try {
            List<SelectMethodsByTypeResult> results = methodManagementMapper.selectMethodsGroupByType();
            methodAndTasks.setMethodCounts(results);
        } catch (Exception e) {
            log.error("根据类型分类查询方法失败", e);
            //查询失败
            return Result.fail(YamlMessageSource.getMessageStatic("00306001"));
        }
        //任务统计封装
        TaskCount taskCount = new TaskCount();
        LocalDateTime now = LocalDateTime.now();

        // 统计各个时间段的数据
        taskCount.setLastWeek(getTaskCountByTimeRange(now.minusWeeks(1), now));
        taskCount.setLastMonth(getTaskCountByTimeRange(now.minusMonths(1), now));
        taskCount.setHalfYear(getTaskCountByTimeRange(now.minusMonths(6), now));
        taskCount.setLastYear(getTaskCountByTimeRange(now.minusYears(1), now));
        taskCount.setThreeYears(getTaskCountByTimeRange(now.minusYears(3), now));
        taskCount.setAll(getTaskCountByTimeRange(null, null)); // 所有数据

        // 将任务统计结果设置到 methodAndTasks 中
        methodAndTasks.setTaskCounts(taskCount);

        return Result.ok(methodAndTasks);


    }

    @Override
    public Result<Page<ProjectManagementDto>> getProjectsManagementList(ProjectQueryParam projectQueryParam) {
        // 参数校验
        if (projectQueryParam == null) {
            //查询参数不能为空
            return Result.fail(YamlMessageSource.getMessageStatic("00306002"));
        }

        // 分页参数默认值设置
        int pageNum = projectQueryParam.getPage() != null ? projectQueryParam.getPage() : 1;
        int pageSize = projectQueryParam.getSize() != null ? projectQueryParam.getSize() : 10;

        Page<TMeasureProject> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<TMeasureProject> queryWrapper = new LambdaQueryWrapper<>();

        // 模糊查询条件（添加空值判断）
        if (projectQueryParam.getKeyword() != null) {
            queryWrapper.like(TMeasureProject::getProjectName, projectQueryParam.getKeyword());
        }

        Page<TMeasureProject> result = projectManagementMapper.selectPage(page, queryWrapper);
        Page<ProjectManagementDto> dtoPage = new Page<>();
        dtoPage.setCurrent(result.getCurrent());
        dtoPage.setSize(result.getSize());
        dtoPage.setTotal(result.getTotal());
        dtoPage.setPages(result.getPages());

        List<ProjectManagementDto> dtoList = result.getRecords().stream()
                .map(this::projectConvertToDto)
                .collect(Collectors.toList());
        dtoPage.setRecords(dtoList);

        return Result.ok(dtoPage);
    }



    private ProjectManagementDto projectConvertToDto(TMeasureProject tMeasureProject) {
        String id = tMeasureProject.getId();
        ProjectManagementDto dto = new ProjectManagementDto();
        BeanUtils.copyProperties(tMeasureProject, dto);
        LambdaQueryWrapper<TMeasureTask> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(TMeasureTask::getProjectItemKey, id);
        List<TMeasureTask> tMeasureTasks = taskManagementMapper.selectList(queryWrapper);
        int size = tMeasureTasks.size();
        Long size1 = (long) size;
        dto.setTaskNum(size1);

        LambdaQueryWrapper<TMeasureMethod> meQueryWrapper = new LambdaQueryWrapper<>();
        meQueryWrapper.eq(TMeasureMethod::getProjectItemKey, id);
        List<TMeasureMethod> tMeasureMethods = methodManagementMapper.selectList(meQueryWrapper);

        int size3 = tMeasureMethods.size();
        Long size4 = (long) size3;
        dto.setMethodNum(size4);
        dto.setId(tMeasureProject.getId());
        return dto;
    }

    private TaskCountByTime getTaskCountByTimeRange(LocalDateTime startTime, LocalDateTime endTime) {
        QueryWrapper<TMeasureTask> queryWrapper = new QueryWrapper<>();

        // 添加时间范围条件
        if (startTime != null) {
            queryWrapper.ge("start_time", startTime);
        }
        if (endTime != null) {
            queryWrapper.le("end_time", endTime);
        }

        // 统计总数
        Long totalCount = taskManagementMapper.selectCount(queryWrapper);

        // 统计完成数量
        QueryWrapper<TMeasureTask> finishedWrapper = new QueryWrapper<>();
        if (startTime != null) {
            finishedWrapper.ge("start_time", startTime);
        }
        if (endTime != null) {
            finishedWrapper.le("end_time", endTime);
        }
        finishedWrapper.eq("is_delete", 1);
        Long finishedCount = taskManagementMapper.selectCount(finishedWrapper);

        // 计算未完成数量
        Long unFinishedCount = totalCount - finishedCount;

        // 封装结果
        TaskCountByTime countByTime = new TaskCountByTime();
        countByTime.setFinishedCount(finishedCount);
        countByTime.setUnFinishedCount(unFinishedCount);

        return countByTime;
    }


    /*
    查询全部任务
     */
    @Override
    public Result<Map<String,Object>> getTasksManagementList(TaskQueryParam taskQueryParam) {
        // 参数校验
        if (taskQueryParam == null) {
            //查询参数不能为空
            return Result.fail(YamlMessageSource.getMessageStatic("00306002"));
        }

        // 分页参数默认值设置
        int pageNum = taskQueryParam.getPage() != null ? taskQueryParam.getPage() : 1;
        int pageSize = taskQueryParam.getSize() != null ? taskQueryParam.getSize() : 10;

        Page<TMeasureTask> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<TMeasureTask> queryWrapper = new LambdaQueryWrapper<>();

        // 模糊查询条件（添加空值判断）
        if (StringUtils.isNotBlank(taskQueryParam.getKeyword())) {
            queryWrapper.like(TMeasureTask::getTaskName, taskQueryParam.getKeyword());
        }

        Page<TMeasureTask> result = taskManagementMapper.selectPage(page, queryWrapper);
        List<TMeasureTask> records = result.getRecords();
        List<TaskManagementDto> dtoList = new ArrayList<>();
        for (TMeasureTask record : records) {
            String proName;
            String methodName;
            if (record.getProjectItemKey()!=null){
                TMeasureProject project = projectManagementMapper.selectById(record.getProjectItemKey());
                proName=project.getProjectName();
            }else{
                proName="";
            }
            if (record.getMethodKey()!=null){
                TMeasureMethod tMeasureMethod = methodManagementMapper.selectById(record.getMethodKey());
                if (tMeasureMethod!=null){
                    methodName=tMeasureMethod.getMethodName();
                }else{
                    methodName="";
                }
            }else {
                methodName="";
            }

            List<TaskSpecimen> specimen = JSON.parseArray(record.getSampleKey(), TaskSpecimen.class);
            TaskManagementDto dto = TaskManagementDto.builder()
                    .id(record.getId())
                    .taskName(record.getTaskName())
                    .projectName(proName)
                    .startTime(record.getStartTime())
                    .sampleKey(specimen != null ? specimen.size() : 0)
                    .methodId(record.getMethodKey())
                    .methodName(methodName)
                    .measureType(record.getMeasureType())
                    .build();
            dtoList.add(dto);
        }
        // 创建map结构用于返回
        Map<String, Object> resultMap = new HashMap<>();
        resultMap.put("current", result.getCurrent());      // 当前页数
        resultMap.put("size", result.getSize());           // 每页记录数
        resultMap.put("total", result.getTotal());         // 总记录数
        resultMap.put("pages", result.getPages());         // 总页数
        resultMap.put("result", dtoList);                 // 数据列表
        return Result.ok(resultMap);
    }

    LaTeXConverter converter = new LaTeXConverter();

    //公式校验
    public Result<String> formulaCheck(FormulaCheckParam formulaCheckParam) {
        R<String> res = converter.verifyFormulasAgainstParamList(formulaCheckParam.getTotalParamList(), formulaCheckParam.getLatexFormulaList());
        return Result.ok(res.getData());
    }

    /**
     * 删除任务
     * @param List
     * @return
     */
    @Override
    public Result<String> deleteTask(Box List) {
        List<String> deleteTaskIdList = List.getParams();
        // 遍历要删除的任务
        for (String deleteTaskId : deleteTaskIdList) {
            try {
                // 根据当前任务id查询当前任务数据
                TMeasureTask tMeasureTask = taskManagementMapper.selectById(deleteTaskId);
                // 取出并转换当前任务的所有样品
                List<TMeasureSample> tMeasureSamples = JSON.parseArray(tMeasureTask.getSampleKey(), TMeasureSample.class);
                if (tMeasureSamples!=null&&tMeasureSamples.size()!=0) {
                    for (TMeasureSample TMeasureSample : tMeasureSamples) {
                        String specimenKey = TMeasureSample.getId();
                        sampleManagementMapper.deleteById(specimenKey);
                    }
                }
                // 查询当前任务是否有测量数据,如果有则进行删除
                LambdaQueryWrapper<TMeasureData> dataWrapper = new LambdaQueryWrapper<>();
                dataWrapper.eq(TMeasureData::getTaskId, deleteTaskId);
                dataWrapper.last("LIMIT 1");
                TMeasureData tMeasureData = dataManagementMapper.selectOne(dataWrapper);
                if (tMeasureData != null) {
                    dataManagementMapper.delete(new LambdaQueryWrapper<TMeasureData>().eq(TMeasureData::getTaskId, deleteTaskId));
                }
                // 查询当前任务是否有分析数据,如果有则进行删除(附加分析图片表数据)
                LambdaQueryWrapper<TMeasureAnalysisData> analyWrapper = new LambdaQueryWrapper<>();
                analyWrapper.eq(TMeasureAnalysisData::getTaskId, deleteTaskId);
                List<TMeasureAnalysisData> analysisData = analysisDataMapper.selectList(analyWrapper);
                if (analysisData != null && analysisData.size() != 0) {
                    for (TMeasureAnalysisData tMeasureAnalysisData : analysisData) {
                        String analysisDataKey = tMeasureAnalysisData.getId();
                        QueryWrapper<TMeasureAnalysisImage> wrapper = new QueryWrapper<>();
                        wrapper.eq("analysis_id", analysisDataKey);
                        TMeasureAnalysisImage byId = analysisImageMapper.selectOne(wrapper);
                        if (byId != null) {
                            analysisImageMapper.delete(wrapper);
                        }
                        analysisDataMapper.deleteById(analysisDataKey);
                    }
                }
                int res = taskManagementMapper.deleteById(deleteTaskId);
                if (res == 0) {
                    //任务删除失败
                    return Result.fail(YamlMessageSource.getMessageStatic("0030101D"));
                }
            } catch (Exception e) {
                int i = taskManagementMapper.deleteById(deleteTaskId);
                if (i == 0) {
                    //任务删除失败
                    return Result.fail(YamlMessageSource.getMessageStatic("0030101D"));
                }
            }

        }
        return Result.ok();
    }

    @Override
    public Result<String> deleteMethod(Box List) {

        if (List == null) {
            //参数不能为空
            return Result.fail(YamlMessageSource.getMessageStatic("00000045"));
        }
        List<String> deleteMethodIdList = List.getParams();
        for (String deleteMethodId : deleteMethodIdList) {

            List<TMeasureTask> tMeasureTasks = taskManagementMapper.selectList(Wrappers.lambdaQuery(TMeasureTask.class)
                    .eq(TMeasureTask::getMethodKey, deleteMethodId));

            int size = tMeasureTasks.size();
            if (size > 0) {
                //该方法包含未完成的任务
                return Result.fail(YamlMessageSource.getMessageStatic("00306006"));
            }
            int i = methodManagementMapper.deleteById(deleteMethodId);
            if (i == 0) {
                //该方法包含未完成的任务
                return Result.fail(YamlMessageSource.getMessageStatic("00306006"));
            }

        }
        //删除成功
        return Result.ok(YamlMessageSource.getMessageStatic("00100022"));
    }

    @Override
    public Result<String> deleteProject(Box List) {
        List<String> deleteProjectIdList = List.getParams();
        for (String deleteProjectId : deleteProjectIdList) {

            LambdaQueryWrapper<TMeasureMethod> tMeasureMethodLambdaQueryWrapper = Wrappers.lambdaQuery(TMeasureMethod.class)
                    .eq(TMeasureMethod::getProjectItemKey, deleteProjectId);
            List<TMeasureMethod> tMeasureMethods = methodManagementMapper.selectList(tMeasureMethodLambdaQueryWrapper);
            int size = tMeasureMethods.size();
            if (size > 0) {
                //该项目包含未删除的方法
                return Result.fail(YamlMessageSource.getMessageStatic("00306007"));
            }
            int i = projectManagementMapper.deleteById(deleteProjectId);
            if (i == 0) {
                //该项目包含未删除的方法
                return Result.fail(YamlMessageSource.getMessageStatic("00306007"));
            }
        }
        return Result.ok();
    }




//    public Result<String> formulaCheck(List<String> paramList, String latexFormula) {
//        String res = converter.verifyAndConvert(paramList, latexFormula);
//        return Result.ok(res);
//}


}
