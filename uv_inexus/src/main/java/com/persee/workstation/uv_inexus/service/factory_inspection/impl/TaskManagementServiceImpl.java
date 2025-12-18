package com.persee.workstation.uv_inexus.service.factory_inspection.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.persee.workstation.uv_inexus.component.WorkbookCacheManager;
import com.persee.workstation.uv_inexus.dao.factory_inspection.TaskExecutionDetailMapper;
import com.persee.workstation.uv_inexus.dao.factory_inspection.TaskMgrMapper;
import com.persee.workstation.uv_inexus.dto.factory_inspection.*;
import com.persee.workstation.uv_inexus.model.Result;
import com.persee.workstation.uv_inexus.po.factory_inspection.*;
import com.persee.workstation.uv_inexus.service.factory_inspection.*;
import com.persee.workstation.uv_inexus.util.IdGeneratorUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ObjectUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.persee.workstation.uv_inexus.enums.factoryInspection.CalibrationTypeEnum.*;

/**
 * 任务管理表(task_management)数据DAO
 *
 * @author
 * @description
 * @since 2025-12-09 18:55:53
 */
@Slf4j
@Service
public class TaskManagementServiceImpl extends ServiceImpl<TaskMgrMapper, TaskManagement> implements TaskManagementService {
    @Autowired
    private TemplateService templateService;
    @Autowired
    private TaskExecutionDetailMapper taskExecutionDetailMapper;
    @Autowired
    private TaskMgrMapper taskMgrMapper;
    @Autowired
    private ExcelDataAssetService dataAssetService;
    @Autowired
    private CalArtifactsService calArtifactsService;
    @Autowired
    private TaskExecutionDetailService taskExecutionDetailService;
    @Autowired
    private CalStdCatsService catsService;
    @Autowired
    private WorkbookCacheManager workbookCacheManager;

    @Override
    public Result getList(TaskMgrQueryDto queryDto) {

        log.info("查询任务管理列表");
        log.info("查询条件：{}", queryDto);
        Page<TaskManagement> page = new Page<>(queryDto.getPageNum(), queryDto.getPageSize());
        LambdaQueryWrapper<TaskManagement> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(TaskManagement::getDetectName, queryDto.getQuery());
        Page<TaskManagement> managementPage = this.page(page, wrapper);
        managementPage.getRecords().forEach(task -> {
            task.setTemplateCode(templateService.getById(task.getUseTemplate()).getTemplateCode());
        });
        return Result.ok(managementPage);
    }

    @Override
    public Result<TaskCheckItemDto> showTemplate(String id) {
        try {


            TaskCheckItemDto dto = new TaskCheckItemDto();
            TaskManagement management = this.getById(id);
            if (management == null) {
                return Result.fail("任务不存在");
            }
            // 设置任务基本信息
            dto.setTaskId(management.getId());
            dto.setTaskName(management.getDetectName());
            //查询出使用模板的id
            String useTemplate = management.getUseTemplate();
            if (useTemplate != null) {
                //查询出模板内容
                ExcelTemplate excelTemplate = templateService.getById(useTemplate);
                if (excelTemplate != null) {
                    dto.setTemplateId(excelTemplate.getId());
                    dto.setTemplateName(excelTemplate.getTemplateName());
                }
                // 查询任务关联的检测项和标准物质
//                LambdaQueryWrapper<TaskExecutionDetail> wrapper = new LambdaQueryWrapper<>();
//                wrapper.eq(TaskExecutionDetail::getTaskId, id);
//                List<TaskExecutionDetail> list = taskExecutionDetailMapper.selectList(wrapper);
//                if (CollectionUtils.isEmpty(list)){
//                    log.info("默认任务，还没有选择标准物质！！！");
//                    log.info("赋予默认标准物质，特征值和标准值使用模板默认的");
//
//                }
                List<TaskExecutionDetail> list = taskExecutionDetailMapper.initTaskCal(dto.getTaskId());
                // 构建检测项列表
                List<TaskCheckItemDto.CheckItem> checkItems = this.buildCheckItems(list, excelTemplate);
                dto.setCheckItems(checkItems);
            }
            return Result.ok(dto);
        } catch (Exception e) {
            log.error("查询任务模板失败", e);
            return Result.fail("查询任务模板失败");
        }
    }

    @Override
    public Result newTask(TaskAddDto task) {
        TaskManagement management = new TaskManagement();
        management.setId(IdGeneratorUtil.generateIdWithPrefix("task"));
        management.setCompletionStatus(NOT_STARTED.getCode());
        BeanUtils.copyProperties(task, management);
        return Result.ok(this.save(management));
    }

    //这个需要完整的复制
    // task_management   task_execution_detail
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result newTaskByCurrent(TaskCopyDto taskCreateDto) {
        TaskManagement management = taskMgrMapper.selectById(taskCreateDto.getCurrentTaskId());
        TaskManagement task = new TaskManagement();
        BeanUtils.copyProperties(management, task);
        task.setId(IdGeneratorUtil.generateIdWithPrefix("task"));
        task.setDetectName(taskCreateDto.getNewTaskName());
        task.setCreated(LocalDateTime.now());
        task.setUpdated(null);
        int i = taskMgrMapper.insert(task);
        //获取之前的关联关系
        LambdaQueryWrapper<TaskExecutionDetail> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TaskExecutionDetail::getTaskId, taskCreateDto.getCurrentTaskId());
        List<TaskExecutionDetail> detailList = taskExecutionDetailMapper.selectList(wrapper);
        //构建新任务的关联关系
        if (!CollectionUtils.isEmpty(detailList)) {
            detailList.forEach(detail -> {
                detail.setId(IdGeneratorUtil.generateId());
                detail.setTaskId(task.getId());
            });
        }
        boolean b = taskExecutionDetailService.saveBatch(detailList);
        return Result.ok(b);
    }

    @Override
    public Result<List<TaskStatsDto>> taskCount() {
        LambdaQueryWrapper<TaskManagement> wrapper = new LambdaQueryWrapper<>();
        wrapper.ge(TaskManagement::getCreated, LocalDateTime.now().minusMonths(6));
        ArrayList<TaskStatsDto> dtos = new ArrayList<>();
        //获取半年内创建的任务
        List<TaskManagement> taskList = taskMgrMapper.selectList(wrapper);
        // 按照使用模板和具体月份分组统计
        // 按照 "YYYY-MM" 和模板进行分组统计

        Map<String, Map<String, Long>> groupedStats = taskList.stream()
                .collect(Collectors.groupingBy(
                        // 外层 key：年月字符串
                        task -> task.getCreated().getYear() + "-" + String.format("%02d", task.getCreated().getMonthValue()),
                        Collectors.groupingBy(
                                // 内层 key：模板 code
                                task -> {
                                    String templateId = task.getUseTemplate();
                                    if (templateId != null) {
                                        ExcelTemplate template = templateService.getById(templateId);
                                        return template != null ? template.getTemplateName() : "未知模板";
                                    }
                                    return "无模板";
                                },
                                Collectors.counting()
                        )
                ));
        // 转换为 List<TaskStatsDto>
        groupedStats.forEach((month, templateStats) -> {
            TaskStatsDto dto = new TaskStatsDto();
            dto.setTime(month);
            dto.setTemplateTasks(templateStats);
            dtos.add(dto);
        });
        return Result.ok(dtos);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result saveCheckItem(TaskDetectionDTO dto) {
        try {
            // 先删除原有的任务执行详情
            LambdaQueryWrapper<TaskExecutionDetail> deleteWrapper = new LambdaQueryWrapper<>();
            deleteWrapper.eq(TaskExecutionDetail::getTaskId, dto.getTaskId());
            taskExecutionDetailMapper.delete(deleteWrapper);

            // 构建新的任务执行详情列表
            List<TaskExecutionDetail> detailList = new ArrayList<>();

            // 处理校零标准
            if (dto.getItems() != null) {
                for (TaskDetectionDTO.DetectionItem item : dto.getItems()) {
                    // 处理校零标准
                    if (item.getZeroId() != null) {
                        TaskExecutionDetail zeroDetail = new TaskExecutionDetail();
                        zeroDetail.setId(IdGeneratorUtil.generateId());
                        zeroDetail.setTaskId(dto.getTaskId());
                        zeroDetail.setItemId(item.getId());
                        zeroDetail.setStandardId(item.getZeroId());
                        zeroDetail.setStandardType(ZERO.getCode()); // 校零标准类型
                        detailList.add(zeroDetail);
                    }

                    // 处理测量标准
                    if (item.getMeasureId() != null) {
                        TaskExecutionDetail measurementDetail = new TaskExecutionDetail();
                        measurementDetail.setId(IdGeneratorUtil.generateId());
                        measurementDetail.setTaskId(dto.getTaskId());
                        measurementDetail.setItemId(item.getId());
                        measurementDetail.setStandardId(item.getMeasureId());
                        measurementDetail.setStandardType(MEASUREMENT.getCode()); // 测量标准类型
                        detailList.add(measurementDetail);
                    }
                }
            }

            // 批量保存新的任务执行详情
            if (!detailList.isEmpty()) {
                taskExecutionDetailService.saveBatch(detailList);
            }

            return Result.ok("保存成功");
        } catch (Exception e) {
            log.error("保存检测项失败", e);
            throw new RuntimeException("保存检测项失败", e);
        }
    }

    @Override
    public Result<Boolean> checkTaskUsage(TaskUsageDto dto) {
        return Result.ok(!workbookCacheManager.isTaskBeingEditedByOthers(dto.getTaskId(), dto.getUpdateBy()));
    }

    //    private List<TaskCheckItemDto.CheckItem> buildCheckItems(List<TaskExecutionDetail> details, ExcelTemplate template) {
//        ArrayList<TaskCheckItemDto.CheckItem> checkItems = new ArrayList<>();
//        //所有的检测项
//        LambdaQueryWrapper<ExcelDataAsset> wrapper = new LambdaQueryWrapper<>();
//        wrapper.eq(ExcelDataAsset::getTemplateCode, template.getTemplateCode());
//        List<ExcelDataAsset> list = dataAssetService.list(wrapper);
//        if (!CollectionUtils.isEmpty(list)) {
//            list.forEach(asset -> {
//                Map<String, Object> dataMap = asset.getDataMap();
//
//                TaskCheckItemDto.CheckItem item = new TaskCheckItemDto.CheckItem();
//                item.setItemId(asset.getId());
//                item.setItemName(MapUtils.getString(dataMap, "项目名称"));
//                item.setGroupName(MapUtils.getString(dataMap, "检测类别"));
//                //不为空证明上次有用过，依旧保留上次的
//                if (CollectionUtils.isNotEmpty(details)) {
//                    //获取校零物质的id
//                    List<TaskExecutionDetail> detailList = details.stream()
//                            .filter(detail -> detail.getStandardType().equals(ZERO.getCode()))
//                            .filter(detail -> detail.getDetectItemId().equals(asset.getId())).collect(Collectors.toList());
//                    if (CollectionUtils.isNotEmpty(detailList)) {
//                        CalArtifacts calArtifact = calArtifactsService.getById(detailList.get(0).getStandardId());
//                        TaskCheckItemDto.Standard standard = new TaskCheckItemDto.Standard();
//                        standard.setStandardId(calArtifact.getId());
//                        standard.setStandardName(calArtifact.getName());
//                        item.setZeroStandard(standard);
//                    }
//                    //获取测量物质的id
//                    List<TaskExecutionDetail> detailList1 = details.stream()
//                            .filter(detail -> detail.getStandardType().equals(MEASUREMENT.getCode()))
//                            .filter(detail -> detail.getDetectItemId().equals(asset.getId())).collect(Collectors.toList());
//                    if (CollectionUtils.isNotEmpty(detailList1)) {
//                        CalArtifacts calArtifact = calArtifactsService.getById(detailList1.get(0).getStandardId());
//                        TaskCheckItemDto.Standard standard = new TaskCheckItemDto.Standard();
//                        standard.setStandardId(calArtifact.getId());
//                        standard.setStandardName(calArtifact.getName());
//                        item.setMeasurementStandard(standard);
//                    }
//
//                }
//                LambdaQueryWrapper<TaskExecutionDetail> queryWrapper = new LambdaQueryWrapper<>();
//                queryWrapper.eq(TaskExecutionDetail::getDetectItemId, item.getItemId())
//                        .eq(TaskExecutionDetail::getStandardType, ZERO.getCode())
//                        .isNull(TaskExecutionDetail::getTaskId);
//                //这里查出来的是标准物质种类的id
//                TaskExecutionDetail details1 = taskExecutionDetailMapper.selectOne(queryWrapper);
//                if (!ObjectUtils.isEmpty(details1)) {
//                    LambdaQueryWrapper<CalArtifacts> wrapper1 = new LambdaQueryWrapper<>();
//                    wrapper1.eq(CalArtifacts::getCategoryId, details1.getStandardId());
//                    //这里获取的是标准物质实体，以供选择
//                    List<CalArtifacts> list1 = calArtifactsService.list(wrapper1);
//                    if (CollectionUtils.isNotEmpty(list1)) {
//                        ArrayList<TaskCheckItemDto.Standard> standards = new ArrayList<>();
//                        for (CalArtifacts a : list1) {
//                            TaskCheckItemDto.Standard standard = new TaskCheckItemDto.Standard();
//                            standard.setStandardId(a.getId());
//                            standard.setStandardName(a.getName());
//                            standards.add(standard);
//                        }
//                        item.setZeroStandards(standards);
//                    }
//                }
//                if (ObjectUtils.isEmpty(details1)) {
//                    item.setZeroStandards(null);
//                }
//
//
//                LambdaQueryWrapper<TaskExecutionDetail> queryWrapper2 = new LambdaQueryWrapper<>();
//                queryWrapper2.eq(TaskExecutionDetail::getDetectItemId, item.getItemId())
//                        .eq(TaskExecutionDetail::getStandardType, MEASUREMENT.getCode())
//                        .isNull(TaskExecutionDetail::getTaskId);
//                //这里查出来的是标准物质种类的id
//                TaskExecutionDetail details2 = taskExecutionDetailMapper.selectOne(queryWrapper2);
//                if (ObjectUtils.isEmpty(details2)) {
//                    item.setMeasurementStandards(null);
//                }
//                if (!ObjectUtils.isEmpty(details2)) {
//                    LambdaQueryWrapper<CalArtifacts> wrapper2 = new LambdaQueryWrapper<>();
//                    wrapper2.eq(CalArtifacts::getCategoryId, details2.getStandardId());
//                    //这里获取的是标准物质实体，以供选择
//                    List<CalArtifacts> list2 = calArtifactsService.list(wrapper2);
//                    if (CollectionUtils.isNotEmpty(list2)) {
//                        ArrayList<TaskCheckItemDto.Standard> standards = new ArrayList<>();
//                        for (CalArtifacts a : list2) {
//                            TaskCheckItemDto.Standard standard = new TaskCheckItemDto.Standard();
//                            standard.setStandardId(a.getId());
//                            standard.setStandardName(a.getName());
//                            standards.add(standard);
//                        }
//                        item.setMeasurementStandards(standards);
//                    }
//                }
//                checkItems.add(item);
//            });
//        }
//        // 需要从模板中获取所有检测项，然后匹配已选择的标准物质
//        return checkItems; // 示例返回空列表
//    }

    /**
     * 根据任务执行详情和模板构建检查项列表
     *
     * @param details  任务执行详情列表
     * @param template 模板信息
     * @return 检查项列表
     */
    private List<TaskCheckItemDto.CheckItem> buildCheckItems(List<TaskExecutionDetail> details, ExcelTemplate template) {
        ArrayList<TaskCheckItemDto.CheckItem> checkItems = new ArrayList<>();
        // 获取所有检测项
        List<ExcelDataAsset> assets = getExcelDataAssets(template);
        if (CollectionUtils.isEmpty(assets)) {
            return checkItems;
        }
        // 处理每个检测项
        for (ExcelDataAsset asset : assets) {
            TaskCheckItemDto.CheckItem item = createBasicCheckItem(asset);
            // 设置已选择的标准物质
            if (CollectionUtils.isNotEmpty(details)) {
                setPreviouslySelectedStandards(item, details);
            }
            // 设置可选择的标准物质列表
            setAvailableStandards(item);

            checkItems.add(item);
        }

        return checkItems;
    }

    /**
     * 根据模板获取所有Excel数据资产
     *
     * @param template 模板信息
     * @return Excel数据资产列表
     */
    private List<ExcelDataAsset> getExcelDataAssets(ExcelTemplate template) {
        LambdaQueryWrapper<ExcelDataAsset> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ExcelDataAsset::getTemplateCode, template.getTemplateCode());
        return dataAssetService.list(wrapper);
    }

    /**
     * 根据Excel数据资产创建基础检查项
     *
     * @param asset Excel数据资产
     * @return 基础检查项
     */
    private TaskCheckItemDto.CheckItem createBasicCheckItem(ExcelDataAsset asset) {
        TaskCheckItemDto.CheckItem item = new TaskCheckItemDto.CheckItem();
        Map<String, Object> dataMap = asset.getDataMap();
        item.setItemId(asset.getId());
        item.setItemName(MapUtils.getString(dataMap, "项目名称"));
        item.setGroupName(MapUtils.getString(dataMap, "检测类别"));
        return item;
    }

    /**
     * 设置检查项之前选择的标准物质
     *
     * @param item    检查项
     * @param details 任务执行详情列表
     */
    private void setPreviouslySelectedStandards(TaskCheckItemDto.CheckItem item, List<TaskExecutionDetail> details) {
        // 设置校零标准
        setPreviouslySelectedStandard(item, details, ZERO.getCode(), true);
        // 设置测量标准
        setPreviouslySelectedStandard(item, details, MEASUREMENT.getCode(), false);
    }

    /**
     * 设置检查项之前选择的特定类型标准物质
     *
     * @param item           检查项
     * @param details        任务执行详情列表
     * @param standardType   标准物质类型
     * @param isZeroStandard 是否为校零标准
     */
    private void setPreviouslySelectedStandard(TaskCheckItemDto.CheckItem item, List<TaskExecutionDetail> details,
                                               String standardType, boolean isZeroStandard) {
        List<TaskExecutionDetail> filteredDetails = details.stream()
                .filter(detail -> detail.getStandardType().equals(standardType))
                .filter(detail -> detail.getItemId().equals(item.getItemId()))
                .collect(Collectors.toList());

        if (CollectionUtils.isNotEmpty(filteredDetails)) {
            String id = filteredDetails.get(0).getStandardId();
            CalArtifacts calArtifact = calArtifactsService.getById(id);
            TaskCheckItemDto.Standard standard = new TaskCheckItemDto.Standard();
            if (calArtifact == null) {
                CalStdCats cats = catsService.getById(id);
                standard.setStandardId(cats.getId());
                standard.setStandardName(cats.getName());
            }
            if (calArtifact != null) {
                standard.setStandardId(calArtifact.getId());
                standard.setStandardName(calArtifact.getName());
            }
            if (isZeroStandard) {
                item.setZeroStandard(standard);
            } else {
                item.setMeasurementStandard(standard);
            }
        }
    }

    /**
     * 设置检查项可用的标准物质列表
     *
     * @param item 检查项
     */
    private void setAvailableStandards(TaskCheckItemDto.CheckItem item) {
        // 设置可用的校零标准
        setAvailableStandardsForType(item, ZERO.getCode(), true);
        // 设置可用的测量标准
        setAvailableStandardsForType(item, MEASUREMENT.getCode(), false);
    }

    /**
     * 设置检查项可用的特定类型标准物质列表
     *
     * @param item           检查项
     * @param standardType   标准物质类型
     * @param isZeroStandard 是否为校零标准
     */
    private void setAvailableStandardsForType(TaskCheckItemDto.CheckItem item, String standardType, boolean isZeroStandard) {
        LambdaQueryWrapper<TaskExecutionDetail> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(TaskExecutionDetail::getItemId, item.getItemId())
                .eq(TaskExecutionDetail::getStandardType, standardType)
                .isNull(TaskExecutionDetail::getTaskId);

        TaskExecutionDetail detail = taskExecutionDetailMapper.selectOne(queryWrapper);

        if (ObjectUtils.isEmpty(detail)) {
            if (isZeroStandard) {
                item.setZeroStandards(null);
            } else {
                item.setMeasurementStandards(null);
            }
            return;
        }

        LambdaQueryWrapper<CalArtifacts> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CalArtifacts::getCategoryId, detail.getStandardId());
//这边是把默认的标准物种类放进去，方便前端取
        CalStdCats cats = catsService.getById(detail.getStandardId());
        ArrayList<TaskCheckItemDto.Standard> standards = new ArrayList<>();
        TaskCheckItemDto.Standard standardCal = new TaskCheckItemDto.Standard();
        standardCal.setStandardId(cats.getId());
        standardCal.setStandardName(cats.getName());
        standards.add(standardCal);


        List<CalArtifacts> artifacts = calArtifactsService.list(wrapper);
        if (CollectionUtils.isNotEmpty(artifacts)) {
            for (CalArtifacts artifact : artifacts) {
                TaskCheckItemDto.Standard standard = new TaskCheckItemDto.Standard();
                standard.setStandardId(artifact.getId());
                standard.setStandardName(artifact.getName());
                standards.add(standard);
            }
        }
        if (isZeroStandard) {
            item.setZeroStandards(standards);
        } else {
            item.setMeasurementStandards(standards);
        }
    }

}