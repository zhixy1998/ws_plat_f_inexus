package com.persee.workstation.uv_inexus.service.factory_inspection.impl;

import com.alibaba.excel.util.StringUtils;
import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.persee.workstation.uv_inexus.component.*;
import com.persee.workstation.uv_inexus.dao.factory_inspection.TaskExecutionDetailMapper;
import com.persee.workstation.uv_inexus.dao.factory_inspection.TaskMgrMapper;
import com.persee.workstation.uv_inexus.dao.guide.instrumentDao.DeviceTypeInfoDAO;
import com.persee.workstation.uv_inexus.dto.factory_inspection.*;
import com.persee.workstation.uv_inexus.model.R;
import com.persee.workstation.uv_inexus.model.Result;
import com.persee.workstation.uv_inexus.po.factory_inspection.*;
import com.persee.workstation.uv_inexus.service.factory_inspection.*;
import com.persee.workstation.uv_inexus.service.guide.instrumentService.DeviceManageService;
import com.persee.workstation.uv_inexus.util.ExcelCellLocator;
import com.persee.workstation.uv_inexus.util.IdGeneratorUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ObjectUtils;

import java.io.FileOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static com.persee.workstation.uv_inexus.enums.Cache.CacheEnums.DEFAULT_CACHE;
import static com.persee.workstation.uv_inexus.enums.factoryInspection.CalibrationTypeEnum.*;
import static com.persee.workstation.uv_inexus.enums.factoryInspection.OutFactoryCheckEnum.OUT_FACTORY_CHECK_LIST;

/**
 * 任务管理表(task_management)数据DAO
 *
 * @author
 * @description
 * @since 2025-12-09 18:55:53
 */
@Slf4j
@Service
@DS("factory_inspection")
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
    private TaskItemStatusService taskItemStatusService;

    @Autowired
    private WorkbookCacheManager workbookCacheManager;
    @Autowired
    private EhcacheUtil ehcacheUtil;
    @Lazy
    @Autowired
    private DeviceManageService deviceManageService;
    @Autowired
    @Lazy
    private DefaultTaskManager defaultTaskManager;
    @Autowired
    private SmartExcelContentConverter smartExcelContentConverter;
    @Autowired
    private ExcelCellLocator excelCellLocator;

    @Autowired
    private TaskDeviceTypeCache taskDeviceTypeCache;

    @Autowired
    private DeviceTypeInfoDAO deviceTypeInfoDAO;
    @Override
    public Result getList(TaskMgrQueryDto queryDto) {
        try {


            log.info("查询任务管理列表");
            log.info("查询条件：{}", queryDto);
//        Page<TaskManagement> page = new Page<>(queryDto.getPageNum(), queryDto.getPageSize());
            IPage<TaskManagement> page = new Page<>(queryDto.getPageNum(), queryDto.getPageSize());

//        LambdaQueryWrapper<TaskManagement> wrapper = new LambdaQueryWrapper<>();
//        wrapper.like(TaskManagement::getDetectName, queryDto.getQuery())
//                .or().like(TaskManagement::getOperationInstrument, queryDto.getQuery())
//                .or().like(TaskManagement::getUseTemplate, queryDto.getQuery());

            IPage<TaskManagement> managementPage = taskMgrMapper.getMyPage(page, queryDto.getQuery(), queryDto.getUpdateBy());
//        managementPage.getRecords().forEach(task -> {
//            ExcelTemplate excelTemplate = templateService.getById(task.getUseTemplate());
//            if (excelTemplate != null) {
//                task.setTemplateCode(excelTemplate.getTemplateCode());
//                task.setTemplateName(excelTemplate.getTemplateName());
//            }
//        });
            return Result.ok(managementPage);
        } catch (Exception e) {
            log.error("查询任务管理列表失败", e);
        }
        return Result.fail("查询任务管理列表失败");
    }

    @Override
    public Result<TaskCheckItemDto> showTemplate(String id) {
        try {
            TaskManagement management = null;
//            if (DEFAULT_TASK.getCode().equals(id)) {
//                // 自动检查并确保默认任务存在
//                String defaultTaskId = defaultTaskManager.getDefaultTaskId();
//                management = this.getById(defaultTaskId);
//            } else {
            management = this.getById(id);
//            }


            TaskCheckItemDto dto = new TaskCheckItemDto();
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
                List<TaskCheckItemDto.CheckItem> checkItems = this.buildCheckItems(list, excelTemplate,id);
                dto.setCheckItems(checkItems);
            }
            return Result.ok(dto);
        } catch (Exception e) {
            log.error("查询任务模板失败", e);
            return Result.fail("查询任务模板失败");
        }
    }

    @Override
    public Result<HashMap<String, String>> newTask(TaskAddDto task) {
        try {
            R<String> device = deviceManageService.getConnectDevice(task.getCreateBy());
            log.info("新建任务");
            //看一下新的名字有没有重名
            if (this.checkTaskName(task.getDetectName())) {
                return Result.fail("任务名称已存在");
            }
            TaskManagement management = new TaskManagement();
            management.setId(IdGeneratorUtil.generateIdWithPrefix("task"));
            management.setCompletionStatus(NOT_STARTED.getCode());
            LambdaQueryWrapper<ExcelTemplate> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(ExcelTemplate::getTemplateCode, task.getUseTemplate());
            ExcelTemplate template = templateService.getOne(queryWrapper);
            BeanUtils.copyProperties(task, management);
            management.setUseTemplate(template.getId());
            management.setOperationInstrument(task.getSerialNumber());
            management.setResultTemplate(task.getUseTemplate());
            management.setContentCategory(TEMPLATE_USER_DEFINED_TASK.getCode());
            management.setCreateBy(task.getCreateBy());
            if (device.getData() != null) {
                management.setOperationInstrument(device.getData());
            }
            if (StringUtils.isNotBlank(task.getTaskType())) {
                management.setTaskType(task.getTaskType());
            }
            boolean save = this.save(management);
            if (!save) {
                return Result.fail(null);
            }
            // 为新任务初始化任务执行详情和检测项状态
            initializeTaskExecutionDetails(management.getId(), task.getUseTemplate());

            if (save) {
                HashMap<String, String> map = new HashMap<>();
//                ExcelTemplate template = templateService.getById(management.getUseTemplate());
                map.put("taskId", management.getId());
                map.put("templateCode", task.getUseTemplate());

                ExcelTemplate excelTemplate = new ExcelTemplate();
                excelTemplate.setId(template.getId());
                excelTemplate.setUseCount(template.getUseCount() + 1);
                excelTemplate.setLastUsed(new Date());
                templateService.updateById(excelTemplate);
                return Result.ok(map);
            }
            return Result.ok(null);
        } catch (Exception e) {
            log.error("新建任务失败", e);
        }
        return Result.fail(null);
    }

    /**
     * 为新任务初始化任务执行详情记录和检测项状态
     *
     * @param taskId       任务ID
     * @param templateCode 模板编码
     */
    private void initializeTaskExecutionDetails(String taskId, String templateCode) {
        try {
            // 查询模板对应的所有检测项
            LambdaQueryWrapper<ExcelDataAsset> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(ExcelDataAsset::getTemplateCode, templateCode);
            List<ExcelDataAsset> assets = dataAssetService.list(wrapper);

            if (CollectionUtils.isEmpty(assets)) {
                log.warn("模板 {} 没有检测项", templateCode);
                return;
            }

            // 为每个检测项创建任务执行详情记录和状态记录
            List<TaskItemStatus> statusList = new ArrayList<>();

            for (ExcelDataAsset asset : assets) {

                // 创建任务检测项状态（用于记录测量状态）
                TaskItemStatus status = new TaskItemStatus();
                status.setId(IdGeneratorUtil.generateId());
                status.setTaskId(taskId);
                status.setItemId(asset.getId());
                status.setMeasured("0");
                status.setCheckResult("0");
                statusList.add(status);
            }


            if (!statusList.isEmpty()) {
                taskItemStatusService.saveBatch(statusList);
                log.info("为任务 {} 初始化了 {} 个检测项状态", taskId, statusList.size());
            }
        } catch (Exception e) {
            log.error("初始化任务执行详情失败，任务ID: {}", taskId, e);
        }
    }
/* /**
 * 为任务初始化任务执行详情和检测项状态
 */
/*private void initializeTaskDetailsAndStatus(String taskId, String templateCode) {
    try {
        // 查询模板对应的所有检测项
        LambdaQueryWrapper<ExcelDataAsset> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ExcelDataAsset::getTemplateCode, templateCode);
        List<ExcelDataAsset> assets = dataAssetService.list(wrapper);

        if (CollectionUtils.isEmpty(assets)) {
            return;
        }

        // 为每个检测项创建任务执行详情记录和状态记录
        List<TaskExecutionDetail> detailList = new ArrayList<>();
        List<TaskItemStatus> statusList = new ArrayList<>();

        for (ExcelDataAsset asset : assets) {
            // 创建任务执行详情（用于绑定标准物质）
            TaskExecutionDetail detail = new TaskExecutionDetail();
            detail.setId(IdGeneratorUtil.generateId());
            detail.setTaskId(taskId);
            detail.setItemId(asset.getId());
            detailList.add(detail);

            // 创建任务检测项状态（用于记录测量状态）
            TaskItemStatus status = new TaskItemStatus();
            status.setId(IdGeneratorUtil.generateId());
            status.setTaskId(taskId);
            status.setItemId(asset.getId());
            status.setMeasured("0");
            status.setCheckResult("0");
            statusList.add(status);
        }

        if (!detailList.isEmpty()) {
            taskExecutionDetailService.saveBatch(detailList);
        }

        if (!statusList.isEmpty()) {
            taskItemStatusService.saveBatch(statusList);
        }
    } catch (Exception e) {
        // 记录日志但不影响任务创建
    }
}*/

    //这个需要完整的复制
    // task_management   task_execution_detail
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result newTaskByCurrent(TaskCopyDto taskCreateDto) {
        try {
//看一下新的名字有没有重名
            if (this.checkTaskName(taskCreateDto.getNewTaskName())) {
                return Result.fail("任务名称已存在");
            }

//            R<String> device = deviceManageService.getConnectDevice(taskCreateDto.getUpdateBy());
            TaskManagement management = taskMgrMapper.selectById(taskCreateDto.getCurrentTaskId());
            TaskManagement task = new TaskManagement();
            BeanUtils.copyProperties(management, task);
            task.setId(IdGeneratorUtil.generateIdWithPrefix("task"));
            task.setDetectName(taskCreateDto.getNewTaskName());
            task.setCreated(LocalDateTime.now());
            task.setUpdated(null);
            task.setCompletionStatus(NOT_STARTED.getCode());
            task.setStartTime(null);
            task.setOperationInstrument(taskCreateDto.getSerialNumber());
            task.setContentCategory(TEMPLATE_USER_DEFINED_TASK.getCode());
            task.setCreateBy(taskCreateDto.getUpdateBy());
            int i = taskMgrMapper.insert(task);
            //获取之前的关联关系
            LambdaQueryWrapper<TaskExecutionDetail> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(TaskExecutionDetail::getTaskId, taskCreateDto.getCurrentTaskId());
            log.info("dangqianrenwu id" + taskCreateDto.getCurrentTaskId());
            List<TaskExecutionDetail> detailList = taskExecutionDetailMapper.selectList(wrapper);
            //构建新任务的关联关系
            if (!CollectionUtils.isEmpty(detailList)) {
                detailList.forEach(detail -> {
                    detail.setId(IdGeneratorUtil.generateId());
                    detail.setTaskId(task.getId());
                });
            }

            boolean b = taskExecutionDetailService.saveBatch(detailList);
            log.info("dfsfs" + detailList.size());

            HashMap<String, String> map = new HashMap<>();
            ExcelTemplate template = templateService.getById(task.getUseTemplate());
            map.put("taskId", task.getId());
            map.put("templateCode", template.getTemplateCode());

            // 为新任务初始化任务执行详情和检测项状态
            initializeTaskExecutionDetails(task.getId(), template.getTemplateCode());

            ExcelTemplate excelTemplate = new ExcelTemplate();
            excelTemplate.setId(template.getId());
            excelTemplate.setUseCount(template.getUseCount() + 1);
            excelTemplate.setLastUsed(new Date());
            templateService.updateById(excelTemplate);
            return Result.ok(map);

        } catch (Exception e) {
            log.error("复制任务失败", e);
        }
        return Result.fail();
    }

    @Override
    public Result<List<TaskStatsDto>> taskCount(String updateBy) {
        LambdaQueryWrapper<TaskManagement> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TaskManagement::getCreateBy, updateBy);
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
        return Result.ok(true);
//        if (StringUtils.equals(dto.getTaskId(), DEFAULT_TASK.getCode())) {
//            LambdaQueryWrapper<TaskManagement> wrapper = new LambdaQueryWrapper<>();
//            wrapper.eq(TaskManagement::getContentCategory, TEMPLATE_BUILT_IN_TASK.getCode())
//                    .eq(TaskManagement::getCreateBy, dto.getUpdateBy());
//            //这里查询出内置任务
//            TaskManagement one = this.getOne(wrapper);
//            dto.setTaskId(one.getId());
//        }
//
//        String taskId = dto.getTaskId();
//        String updateBy = dto.getUpdateBy();
//
//        // 先检查是否正在被其他用户编辑
//        if (workbookCacheManager.isTaskBeingEditedByOthers(taskId, updateBy)) {
//            String editor = workbookCacheManager.getCurrentEditor(taskId);
//            log.info("任务正在被其他用户编辑：taskId={}, 当前用户={}, 编辑者={}", taskId, updateBy, editor);
//            return Result.ok(false);  // 返回 false 表示不能使用
//        }
//
//        // 尝试获取编辑锁
//        boolean acquired = workbookCacheManager.acquireEditLock(taskId, updateBy);
//        if (acquired) {
//            log.info("获取任务编辑锁成功：taskId={}, updateBy={}", taskId, updateBy);
//            return Result.ok(true);  // 返回 true 表示可以使用
//        } else {
//            // 获取失败，说明被其他用户占用了
//            String editor = workbookCacheManager.getCurrentEditor(taskId);
//            log.warn("获取任务编辑锁失败：taskId={}, 当前用户={}, 占用者={}", taskId, updateBy, editor);
//            return Result.ok(false);  // 返回 false 表示不能使用
//        }
    }


    @Override
    public Result<List<TaskCheckItemDto.Standard>> standardMaterialList(String taskId) {
        ArrayList<TaskCheckItemDto.Standard> standards = new ArrayList<>();
        List<TaskExecutionDetail> details = taskExecutionDetailMapper.initTaskCal(taskId);

        TaskManagement management = this.getById(taskId);
        String templateId = management.getUseTemplate();
        ExcelTemplate excelTemplate = templateService.getById(templateId);
        LambdaQueryWrapper<ExcelDataAsset> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ExcelDataAsset::getTemplateCode, excelTemplate.getTemplateCode());
        List<ExcelDataAsset> dataAssetList = dataAssetService.list(wrapper);
        List<TaskExecutionDetail> list = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(dataAssetList)) {
            for (ExcelDataAsset asset : dataAssetList) {
//
//            }
//            dataAssetList.forEach(asset -> {
                Map<String, Object> dataMap = asset.getDataMap();
                String string = MapUtils.getString(dataMap, "暗电流物质");
                if (StringUtils.isNotBlank(string)) {
                    LambdaQueryWrapper<CalStdCats> queryWrapper = new LambdaQueryWrapper<>();
                    queryWrapper.eq(CalStdCats::getName, string);
                    List<CalStdCats> list1 = catsService.list(queryWrapper);
                    if (CollectionUtils.isNotEmpty(list1)) {
                        CalStdCats cats = list1.get(0);
                        TaskCheckItemDto.Standard standard = new TaskCheckItemDto.Standard();
                        standard.setStandardId(cats.getId());
                        standard.setStandardName(cats.getName());
                        standards.add(standard);
                        break;
                    }
                }
            }
        }
        // 使用stream的filter方法优化嵌套循环，提高性能
        list = details.stream()
                .filter(detail -> dataAssetList.stream()
                        .anyMatch(asset -> StringUtils.equals(asset.getId(), detail.getItemId())))
                .collect(Collectors.toList());

        List<String> standardIds = list.stream()
                .map(TaskExecutionDetail::getStandardId)
                .distinct()
                .collect(Collectors.toList());
        List<CalStdCats> calStdCats = catsService.listByIds(standardIds);
        if (CollectionUtils.isNotEmpty(calStdCats)) {
            calStdCats.forEach(cal -> {
                TaskCheckItemDto.Standard standard = new TaskCheckItemDto.Standard();
                standard.setStandardId(cal.getId());
                standard.setStandardName(cal.getName());
                standards.add(standard);
            });
        }
        List<CalArtifacts> calArtifacts = calArtifactsService.listByIds(standardIds);
        if (CollectionUtils.isNotEmpty(calArtifacts)) {
            calArtifacts.forEach(cal -> {
                TaskCheckItemDto.Standard standard = new TaskCheckItemDto.Standard();
                standard.setStandardId(cal.getId());
                standard.setStandardName(cal.getName());
                standards.add(standard);
            });
        }
        // 对 standards 列表进行去重处理，根据 standardId 去重
        List<TaskCheckItemDto.Standard> distinctStandards = new ArrayList<>(standards.stream()
                .collect(Collectors.toMap(
                        TaskCheckItemDto.Standard::getStandardId,
                        standard -> standard,
                        (existing, replacement) -> existing
                ))
                .values());
        return Result.ok(distinctStandards);
//        return Result.ok(standards);
    }

    @Override
    public Result<Boolean> setStandardMaterial(SetSamplePoolDto dto) {
        try {
            String taskId = dto.getTaskId();
            // 使用 Object 类型获取缓存
            Object cachedObject = ehcacheUtil.get(DEFAULT_CACHE.getCacheName(), taskId, String.class, Object.class);

            List<SetSamplePoolDto> dtoList;
            if (cachedObject instanceof List) {
                // 进行类型检查和转换
                dtoList = (List<SetSamplePoolDto>) cachedObject;
                if (CollectionUtils.isNotEmpty(dtoList)) {
                    // 使用迭代器安全地修改集合
                    Iterator<SetSamplePoolDto> iterator = dtoList.iterator();
                    while (iterator.hasNext()) {
                        SetSamplePoolDto existing = iterator.next();
                        if (existing.getPoolNum().equals(dto.getPoolNum())) {
                            iterator.remove(); // 先移除
                            break;
                        }
                    }
                    // 添加更新后的对象
                    dtoList.add(dto);
                } else {
                    dtoList.add(dto);
                }
            } else {
                dtoList = new ArrayList<>();
                dtoList.add(dto);
            }

            // 使用 Object 类型存储
            ehcacheUtil.put(DEFAULT_CACHE.getCacheName(), taskId, dtoList, String.class, Object.class);
            return Result.ok(true);
        } catch (Exception e) {
            log.error("设置标准物失败", e);
            return Result.fail(false);
        }
    }

    @Override
    public Result<TaskManagement> getTaskDetail(String taskId, String updateBy) {
        try {
            if (DEFAULT_TASK.getCode().equals(taskId)) {
                String defaultTaskId = defaultTaskManager.getDefaultTaskId(updateBy);
                TaskManagement management = this.getById(defaultTaskId);
                ExcelTemplate excelTemplate = templateService.getById(management.getUseTemplate());
                management.setTemplateName(excelTemplate.getTemplateName());
                management.setTemplateCode(excelTemplate.getTemplateCode());

                return Result.ok(management);
            }
            if (OPEN_BOX_INSPECTION.getCode().equals(taskId)) {
                TaskManagement management = null;
                R<String> device = deviceManageService.getConnectDevice(updateBy);
                String data = device.getData();
                LambdaQueryWrapper<TaskManagement> wrapper = new LambdaQueryWrapper<>();
                //查找的是开箱验证任务
                wrapper.eq(TaskManagement::getTaskType, "2")
                        .eq(TaskManagement::getOperationInstrument, data);
                List<TaskManagement> list = this.list(wrapper);
                if (CollectionUtils.isEmpty(list)) {
                    LambdaQueryWrapper<TaskManagement> wrapper1 = new LambdaQueryWrapper<>();
                    //查找的是开箱验证任务
                    wrapper1.eq(TaskManagement::getTaskType, "2")
                            .isNull(TaskManagement::getOperationInstrument);
                    list = this.list(wrapper1);
                    if (CollectionUtils.isEmpty(list)){
                        LambdaQueryWrapper<TaskManagement> wrapper2 = new LambdaQueryWrapper<>();
                        //查找的是开箱验证任务
                        wrapper2.eq(TaskManagement::getTaskType, "2");
                        list = this.list(wrapper2);
                    }
                }
                management = list.get(0);


                if (management.getOperationInstrument() == null) {
                    management.setOperationInstrument(data);
                }
                String instrument = management.getOperationInstrument() == null ? "" : management.getOperationInstrument().trim();
                String deviceData = data == null ? "" : data.trim();
                if (!instrument.equals(deviceData)) {
                    TaskAddDto addDto = new TaskAddDto();
                    addDto.setDetectName(management.getDetectName() + System.currentTimeMillis());
                    ExcelTemplate template = templateService.getById(management.getUseTemplate());
                    addDto.setUseTemplate(template.getTemplateCode());
                    addDto.setContentCategory(management.getContentCategory());
                    addDto.setCreateBy(updateBy);
                    addDto.setUpdateBy(updateBy);
                    addDto.setSerialNumber(data);
                    addDto.setCreated(LocalDateTime.now());
                    addDto.setUpdated(LocalDateTime.now());
                    addDto.setTaskType(INSTALLATION_ACCEPTANCE.getCode());
                    Result<HashMap<String, String>> task = newTask(addDto);
                    if (task.getCode() == 200) {
                        String newTaskId = task.getData().get("taskId");
                        TaskManagement newManagement = this.getById(newTaskId);
                        ExcelTemplate excelTemplate = templateService.getById(newManagement.getUseTemplate());
                        newManagement.setTemplateName(excelTemplate.getTemplateName());
                        newManagement.setTemplateCode(excelTemplate.getTemplateCode());
                        return Result.ok(newManagement);
                    }
                } else {
                    ExcelTemplate excelTemplate = templateService.getById(management.getUseTemplate());
                    management.setTemplateName(excelTemplate.getTemplateName());
                    management.setTemplateCode(excelTemplate.getTemplateCode());
                    R<String> device1 = deviceManageService.getConnectDevice(updateBy);
                    String data1 = device1.getData();
                    management.setOperationInstrument(data1);
                    TaskManagement management1 = new TaskManagement();
                    management1.setId(management.getId());
                    management1.setOperationInstrument(data1);
                    taskMgrMapper.updateById(management1);
                    return Result.ok(management);
                }

            }
            if (FACTORY_INSPECTION.getCode().equals(taskId)) {
//                TaskManagement task = defaultTaskManager.createFactoryInspectionTask(updateBy);
                Object cachedObject = ehcacheUtil.get(DEFAULT_CACHE.getCacheName(), OUT_FACTORY_CHECK_LIST.getCode(), String.class, Object.class);
                List<DeviceTaskDto> dtoList;
                if (cachedObject instanceof List) {
                    @SuppressWarnings("unchecked")
                    List<DeviceTaskDto> checkedList = (List<DeviceTaskDto>) cachedObject;
                    dtoList = checkedList;
                } else {
                    dtoList = new ArrayList<DeviceTaskDto>();
                }
                if (!CollectionUtils.isEmpty(dtoList)) {
                    DeviceTaskDto dto = dtoList.get(0);
                    String taskId1 = dto.getTaskId();
                    TaskManagement task = this.getById(taskId1);
                    return Result.ok(task);
                }
                return Result.fail(null);
            }
            TaskManagement management = this.getById(taskId);
            ExcelTemplate excelTemplate = templateService.getById(management.getUseTemplate());
            management.setTemplateName(excelTemplate.getTemplateName());
            return Result.ok(management);
        } catch (Exception e) {
            log.error("获取任务详情失败", e);
        }
        return Result.fail(null);
    }

    @Override
    public Result<String> renameTask(TaskRenameDto dto) {
        try {
            // 检查任务是否存在
            TaskManagement task = this.getById(dto.getTaskId());
            if (task == null) {
                return Result.fail("任务不存在");
            }
            if (StringUtils.equals(dto.getTaskName(), task.getDetectName())) {
                return Result.ok("任务名称未改变");
            }
            //看一下新的名字有没有重名
            if (this.checkTaskName(dto.getTaskName())) {
                return Result.fail("任务名称已存在");
            }
            task.setDetectName(dto.getTaskName());
            this.updateById(task);
            return Result.ok("修改成功");
        } catch (Exception e) {
            log.error("修改任务名称失败", e);
            return Result.fail("修改任务名称失败");
        }
    }

    @Override
    public Result<String> deleteTask(String taskId) {
        try {
            TaskManagement task = this.getById(taskId);

            if (task == null) {
                return Result.fail("任务不存在");
            }
//            if (TEMPLATE_BUILT_IN_TASK.getCode().equals(task.getContentCategory())) {
//                return Result.fail("内置任务不能删除");
//            }
            if (this.removeById(taskId)) {
                return Result.ok("删除成功");
            } else {
                return Result.fail("删除任务失败");
            }
        } catch (Exception e) {
            log.error("删除任务失败", e);
            return Result.fail("删除任务失败");
        }
    }

    @Override
    public Result<String> batchDelete(BatchDelDto batchDelDto) {
        try {
            List<String> taskIds = batchDelDto.getTaskIds();
            if (CollectionUtils.isEmpty(taskIds)) {
                return Result.fail("请选择要删除的任务");
            }
            boolean b = this.removeBatchByIds(taskIds);
            return b ? Result.ok("删除成功") : Result.fail("删除失败");
        } catch (Exception e) {
            log.error("批量删除任务失败", e);
        }
        return null;
    }

    @Override
    public Result<Map<String, Object>> generateReport(String taskId) {
        try {


            Workbook taskWorkbook = workbookCacheManager.getTaskWorkbook(taskId);
            Map<String, Object> smartContent = smartExcelContentConverter.convertToSimpleStructure(taskWorkbook);
            String s = taskDeviceTypeCache.get(taskId);
            smartContent.put("toolType", taskId);
            TaskManagement management = this.getById(taskId);
            if (management != null) {
                if (StringUtils.isNotBlank(management.getOperationInstrument())) {
                    List<String> list = deviceTypeInfoDAO.queryDeviceTypeBySerialNumber(management.getOperationInstrument());
                    if (CollectionUtils.isNotEmpty(list)) {
                        smartContent.put("operationInstrument", list.get(0));
                    }
                }
            }
            return Result.ok(smartContent);
        } catch (Exception e) {
            log.error("生成报告失败", e);
        }
        return Result.fail(null);
    }

    @Override
    public Result<List<SetSamplePoolDto>> getStandardMaterial(String taskId) {
        // 使用 Object 类型获取缓存
        Object cachedObject = ehcacheUtil.get(DEFAULT_CACHE.getCacheName(), taskId, String.class, Object.class);

        List<SetSamplePoolDto> dtoList;
        if (cachedObject instanceof List) {
            // 进行类型检查和转换
            dtoList = (List<SetSamplePoolDto>) cachedObject;
            if (CollectionUtils.isEmpty(dtoList)) {
                SetSamplePoolDto setSamplePoolDto = new SetSamplePoolDto();
                TaskCheckItemDto.Standard standard = new TaskCheckItemDto.Standard();
                LambdaQueryWrapper<CalStdCats> wrapper = new LambdaQueryWrapper<>();
                wrapper.eq(CalStdCats::getName, AIR.getDescription());
                List<CalStdCats> list = catsService.list(wrapper);
                if (CollectionUtils.isNotEmpty(list)) {
                    CalStdCats cats = list.get(0);
                    standard.setStandardId(cats.getId());
                    standard.setStandardName(cats.getName());
                }
                setSamplePoolDto.setPoolNum(1);
                setSamplePoolDto.setPoolName("1号");
                setSamplePoolDto.setStandard(standard);
                dtoList.add(setSamplePoolDto);
                ehcacheUtil.put(DEFAULT_CACHE.getCacheName(), taskId, dtoList, String.class, Object.class);
            }
        } else {
            dtoList = new ArrayList<>();
            SetSamplePoolDto setSamplePoolDto = new SetSamplePoolDto();
            TaskCheckItemDto.Standard standard = new TaskCheckItemDto.Standard();
            LambdaQueryWrapper<CalStdCats> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(CalStdCats::getName, AIR.getDescription());
            List<CalStdCats> list = catsService.list(wrapper);
            if (CollectionUtils.isNotEmpty(list)) {
                CalStdCats cats = list.get(0);
                standard.setStandardId(cats.getId());
                standard.setStandardName(cats.getName());
            }
            setSamplePoolDto.setPoolNum(1);
            setSamplePoolDto.setPoolName("1号");
            setSamplePoolDto.setStandard(standard);
            dtoList.add(setSamplePoolDto);
            ehcacheUtil.put(DEFAULT_CACHE.getCacheName(), taskId, dtoList, String.class, Object.class);
        }
        return Result.ok(dtoList);
    }

    @Override
    public Result<Boolean> clearSamplePool(String taskId) {
        try {
            List<SetSamplePoolDto> dtoList = new ArrayList<>();
            ehcacheUtil.put(DEFAULT_CACHE.getCacheName(), taskId, dtoList, String.class, Object.class);
        } catch (Exception e) {
            log.error("清空样本池失败", e);
        }
        return Result.ok(true);
    }


    /**
     * 导出指定名称的 Sheet 页到文件，保留所有格式并计算公式
     *
     * @param workbook  Excel 工作簿对象
     * @param sheetName 要导出的 sheet 名称
     * @param filePath  导出文件路径
     */
    public void exportSheetWithCompleteFormat(Workbook workbook, String sheetName, String filePath) {
        if (workbook == null || StringUtils.isBlank(sheetName) || StringUtils.isBlank(filePath)) {
            return;
        }

        try (FileOutputStream fileOut = new FileOutputStream(filePath)) {
            // 获取指定 sheet
            Sheet sourceSheet = workbook.getSheet(sheetName);
            if (sourceSheet == null) {
                log.warn("未找到名为 {} 的 sheet", sheetName);
                return;
            }

            // 创建新的工作簿，只包含目标 sheet
            Workbook outputWorkbook = new XSSFWorkbook();
            Sheet outputSheet = outputWorkbook.createSheet(sheetName);

            // 计算最大列数
            int maxColumnIndex = 0;
            for (Row row : sourceSheet) {
                if (row.getLastCellNum() > maxColumnIndex) {
                    maxColumnIndex = row.getLastCellNum();
                }
            }

            // 复制列宽
            for (int i = 0; i < maxColumnIndex; i++) {
                outputSheet.setColumnWidth(i, sourceSheet.getColumnWidth(i));
            }

            // 复制行数据并计算公式
            for (Row row : sourceSheet) {
                Row newRow = outputSheet.createRow(row.getRowNum());

                // 复制行高
                if (row.getHeight() > 0) {
                    newRow.setHeight(row.getHeight());
                }

                for (Cell cell : row) {
                    Cell newCell = newRow.createCell(cell.getColumnIndex());
                    copyCellDataWithFormulaCalculation(cell, newCell, outputWorkbook); // 使用输出工作簿

                    // ✅ 修复：在目标工作簿中创建新的样式
                    // 替换第556行及后续相关代码
                    // 修改第557-559行代码
                    if (cell.getCellStyle() != null) {
                        CellStyle newStyle = outputWorkbook.createCellStyle();

                        // 修复：将short改为int或进行类型转换
                        CellStyle sourceCellStyle = cell.getCellStyle();
                        int fontIndex = sourceCellStyle.getFontIndex(); // 获取字体索引 - 改为int类型

                        // 或者使用类型转换：short fontIndex = (short) sourceCellStyle.getFontIndex();

                        Font sourceFont = sourceSheet.getWorkbook().getFontAt((short) fontIndex); // 使用时再转换为short

                        // 在目标工作簿中创建新字体
                        Font newFont = outputWorkbook.createFont();
                        newFont.setFontName(sourceFont.getFontName());
                        newFont.setFontHeightInPoints(sourceFont.getFontHeightInPoints());
                        newFont.setBold(sourceFont.getBold());
                        newFont.setColor(sourceFont.getColor());
                        newFont.setItalic(sourceFont.getItalic());
                        newFont.setUnderline(sourceFont.getUnderline());
                        newFont.setTypeOffset(sourceFont.getTypeOffset());
                        newFont.setStrikeout(sourceFont.getStrikeout());

                        newStyle.setFont(newFont);

                        // 复制其他样式属性
                        newStyle.setAlignment(sourceCellStyle.getAlignment());
                        newStyle.setVerticalAlignment(sourceCellStyle.getVerticalAlignment());
                        newStyle.setFillPattern(sourceCellStyle.getFillPattern());
                        newStyle.setFillForegroundColor(sourceCellStyle.getFillForegroundColor());
                        newStyle.setFillBackgroundColor(sourceCellStyle.getFillBackgroundColor());
                        newStyle.setBorderTop(sourceCellStyle.getBorderTop());
                        newStyle.setTopBorderColor(sourceCellStyle.getTopBorderColor());
                        newStyle.setBorderBottom(sourceCellStyle.getBorderBottom());
                        newStyle.setBottomBorderColor(sourceCellStyle.getBottomBorderColor());
                        newStyle.setBorderLeft(sourceCellStyle.getBorderLeft());
                        newStyle.setLeftBorderColor(sourceCellStyle.getLeftBorderColor());
                        newStyle.setBorderRight(sourceCellStyle.getBorderRight());
                        newStyle.setRightBorderColor(sourceCellStyle.getRightBorderColor());
                        newStyle.setDataFormat(sourceCellStyle.getDataFormat());
                        newStyle.setWrapText(sourceCellStyle.getWrapText());

                        newCell.setCellStyle(newStyle);
                    }


                }
            }

            // 复制合并单元格
            for (CellRangeAddress range : sourceSheet.getMergedRegions()) {
                outputSheet.addMergedRegion(range);
            }

            // 写入文件
            outputWorkbook.write(fileOut);
            log.info("成功导出 sheet {} 到文件: {}", sheetName, filePath);

        } catch (IOException e) {
            log.error("导出 sheet 失败", e);
        }
    }


    /**
     * 复制单元格数据，如果为公式则计算结果
     *
     * @param sourceCell 源单元格
     * @param targetCell 目标单元格
     * @param workbook   工作簿对象
     */
    private void copyCellDataWithFormulaCalculation(Cell sourceCell, Cell targetCell, Workbook workbook) {
        switch (sourceCell.getCellType()) {
            case STRING:
                targetCell.setCellValue(sourceCell.getStringCellValue());
                break;
            case NUMERIC:
                targetCell.setCellValue(sourceCell.getNumericCellValue());
                break;
            case BOOLEAN:
                targetCell.setCellValue(sourceCell.getBooleanCellValue());
                break;
            case FORMULA:
                // 计算公式结果
                double result = sourceCell.getNumericCellValue(); // 这里会自动计算公式
                targetCell.setCellValue(result);
                break;
            case ERROR:
                targetCell.setCellErrorValue(sourceCell.getErrorCellValue());
                break;
            default:
                break;
        }
    }

    private boolean checkTaskName(String taskName) {
        //如果新的名字和已有的一样
        return this.list(new LambdaQueryWrapper<TaskManagement>().eq(TaskManagement::getDetectName, taskName)).size() > 0;
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
    private List<TaskCheckItemDto.CheckItem> buildCheckItems(List<TaskExecutionDetail> details, ExcelTemplate template,String taskId) {
        ArrayList<TaskCheckItemDto.CheckItem> checkItems = new ArrayList<>();
        // 获取所有检测项
        List<ExcelDataAsset> assets = getExcelDataAssets(template);
        if (CollectionUtils.isEmpty(assets)) {
            return checkItems;
        }
        // 查询任务的所有检测项状态
//        LambdaQueryWrapper<TaskItemStatus> statusWrapper = new LambdaQueryWrapper<>();
        // 需要从details中获取taskId
//        String taskId = CollectionUtils.isNotEmpty(details) ? details.get(0).getTaskId() : null;
        Map<String, TaskItemStatus> statusMap = new HashMap<>();
        if (taskId != null) {
            LambdaQueryWrapper<TaskItemStatus> statusWrapper = new LambdaQueryWrapper<>();
            statusWrapper.eq(TaskItemStatus::getTaskId, taskId);
            List<TaskItemStatus> statusList = taskItemStatusService.list(statusWrapper);
            if (CollectionUtils.isNotEmpty(statusList)) {
                statusMap = statusList.stream()
                        .collect(Collectors.toMap(
                                TaskItemStatus::getItemId,
                                status -> status,
                                (existing, replacement) -> existing
                        ));
            }
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

            // 从任务检测项状态表中获取测量状态和检测结果
            TaskItemStatus status = statusMap.get(asset.getId());
            if (status != null) {
                item.setMeasured(status.getMeasured() != null ? status.getMeasured() : "0");
                item.setCheckResult(status.getCheckResult() != null ? status.getCheckResult() : "0");
            } else {
                // 如果没有对应的状态记录，设置为默认值
                item.setMeasured("0");
                item.setCheckResult("0");
            }
            checkItems.add(item);
        }

        //先按照group排序，再按照itemName排序
        checkItems.sort(Comparator.comparing(TaskCheckItemDto.CheckItem::getGroupName).thenComparing(TaskCheckItemDto.CheckItem::getItemName));
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