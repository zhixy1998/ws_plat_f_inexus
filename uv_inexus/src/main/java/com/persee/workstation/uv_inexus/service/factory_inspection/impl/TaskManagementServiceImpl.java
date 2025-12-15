package com.persee.workstation.uv_inexus.service.factory_inspection.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.persee.workstation.uv_inexus.dao.factory_inspection.TaskExecutionDetailMapper;
import com.persee.workstation.uv_inexus.dao.factory_inspection.TaskMgrMapper;
import com.persee.workstation.uv_inexus.dto.factory_inspection.TaskCheckItemDto;
import com.persee.workstation.uv_inexus.dto.factory_inspection.TaskMgrQueryDto;
import com.persee.workstation.uv_inexus.model.Result;
import com.persee.workstation.uv_inexus.po.factory_inspection.*;
import com.persee.workstation.uv_inexus.service.factory_inspection.CalArtifactsService;
import com.persee.workstation.uv_inexus.service.factory_inspection.ExcelDataAssetService;
import com.persee.workstation.uv_inexus.service.factory_inspection.TaskManagementService;
import com.persee.workstation.uv_inexus.service.factory_inspection.TemplateService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.persee.workstation.uv_inexus.enums.factoryInspection.StandardMaterialCategory.MEASUREMENT;
import static com.persee.workstation.uv_inexus.enums.factoryInspection.StandardMaterialCategory.ZERO;

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

    @Override
    public Result getList(TaskMgrQueryDto queryDto) {

        log.info("查询任务管理列表");
        log.info("查询条件：{}", queryDto);
        Page<TaskManagement> page = new Page<>(queryDto.getPageNum(), queryDto.getPageSize());
        LambdaQueryWrapper<TaskManagement> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(TaskManagement::getDetectName, queryDto.getQuery());
        return Result.ok(this.page(page, wrapper));
    }

    @Override
    public Result showTemplate(String id) {
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
                LambdaQueryWrapper<TaskExecutionDetail> wrapper = new LambdaQueryWrapper<>();
                wrapper.eq(TaskExecutionDetail::getTaskId, id);
                List<TaskExecutionDetail> list = taskExecutionDetailMapper.selectList(wrapper);

                // 构建检测项列表
                List<TaskCheckItemDto.CheckItem> checkItems = this.buildCheckItems(list, excelTemplate);
                dto.setCheckItems(checkItems);
            }
            return Result.ok(dto);
        }catch (Exception e){
            log.error("查询任务模板失败", e);
            return Result.fail("查询任务模板失败");
        }
    }

    private List<TaskCheckItemDto.CheckItem> buildCheckItems(List<TaskExecutionDetail> details, ExcelTemplate template) {
        ArrayList<TaskCheckItemDto.CheckItem> checkItems = new ArrayList<>();
        //所有的检测项
        LambdaQueryWrapper<ExcelDataAsset> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ExcelDataAsset::getTemplateCode, template.getTemplateCode());
        List<ExcelDataAsset> list = dataAssetService.list(wrapper);
        if (!CollectionUtils.isEmpty(list)) {
            list.forEach(asset -> {
                Map<String, Object> dataMap = asset.getDataMap();

                TaskCheckItemDto.CheckItem item = new TaskCheckItemDto.CheckItem();
                item.setItemId(asset.getId());
                item.setItemName(MapUtils.getString(dataMap, "项目名称"));
                item.setGroupName(MapUtils.getString(dataMap, "检测类别"));
                //不为空证明上次有用过，依旧保留上次的
                if (CollectionUtils.isNotEmpty(details)) {
                    //获取校零物质的id
                    List<TaskExecutionDetail> detailList = details.stream()
                            .filter(detail -> detail.getStandardType().equals(ZERO.getCode()))
                            .filter(detail -> detail.getDetectItemId().equals(asset.getId())).collect(Collectors.toList());
                    if (CollectionUtils.isNotEmpty(detailList)) {
                        CalArtifacts calArtifact = calArtifactsService.getById(detailList.get(0).getStandardId());
                        TaskCheckItemDto.Standard standard = new TaskCheckItemDto.Standard();
                        standard.setStandardId(calArtifact.getId());
                        standard.setStandardName(calArtifact.getName());
                        item.setZeroStandard(standard);
                    }
                    //获取测量物质的id
                    List<TaskExecutionDetail> detailList1 = details.stream()
                            .filter(detail -> detail.getStandardType().equals(MEASUREMENT.getCode()))
                            .filter(detail -> detail.getDetectItemId().equals(asset.getId())).collect(Collectors.toList());
                    if (CollectionUtils.isNotEmpty(detailList1)) {
                        CalArtifacts calArtifact = calArtifactsService.getById(detailList1.get(0).getStandardId());
                        TaskCheckItemDto.Standard standard = new TaskCheckItemDto.Standard();
                        standard.setStandardId(calArtifact.getId());
                        standard.setStandardName(calArtifact.getName());
                        item.setMeasurementStandard(standard);
                    }

                }
                LambdaQueryWrapper<TaskExecutionDetail> queryWrapper = new LambdaQueryWrapper<>();
                queryWrapper.eq(TaskExecutionDetail::getDetectItemId, item.getItemId())
                        .eq(TaskExecutionDetail::getStandardType, ZERO.getCode())
                        .isNull(TaskExecutionDetail::getTaskId);
                //这里查出来的是标准物质种类的id
                TaskExecutionDetail details1 = taskExecutionDetailMapper.selectOne(queryWrapper);
                LambdaQueryWrapper<CalArtifacts> wrapper1 = new LambdaQueryWrapper<>();
                wrapper1.eq(CalArtifacts::getCategoryId, details1.getStandardId());
                //这里获取的是标准物质实体，以供选择
                List<CalArtifacts> list1 = calArtifactsService.list(wrapper1);
                if (CollectionUtils.isNotEmpty(list1)){
                    ArrayList<TaskCheckItemDto.Standard> standards = new ArrayList<>();
                    for (CalArtifacts a : list1) {
                        TaskCheckItemDto.Standard standard = new TaskCheckItemDto.Standard();
                        standard.setStandardId(a.getId());
                        standard.setStandardName(a.getName());
                        standards.add(standard);
                    }
                    item.setZeroStandards(standards);
                }


                LambdaQueryWrapper<TaskExecutionDetail> queryWrapper2 = new LambdaQueryWrapper<>();
                queryWrapper2.eq(TaskExecutionDetail::getDetectItemId, item.getItemId())
                        .eq(TaskExecutionDetail::getStandardType, MEASUREMENT.getCode())
                        .isNull(TaskExecutionDetail::getTaskId);
                //这里查出来的是标准物质种类的id
                TaskExecutionDetail details2 = taskExecutionDetailMapper.selectOne(queryWrapper2);
                LambdaQueryWrapper<CalArtifacts> wrapper2 = new LambdaQueryWrapper<>();
                wrapper2.eq(CalArtifacts::getCategoryId, details2.getStandardId());
                //这里获取的是标准物质实体，以供选择
                List<CalArtifacts> list2 = calArtifactsService.list(wrapper2);
                if (CollectionUtils.isNotEmpty(list2)){
                    ArrayList<TaskCheckItemDto.Standard> standards = new ArrayList<>();
                    for (CalArtifacts a : list2) {
                        TaskCheckItemDto.Standard standard = new TaskCheckItemDto.Standard();
                        standard.setStandardId(a.getId());
                        standard.setStandardName(a.getName());
                        standards.add(standard);
                    }
                    item.setMeasurementStandards(standards);
                }
            });
        }
        // 需要从模板中获取所有检测项，然后匹配已选择的标准物质
        return Collections.emptyList(); // 示例返回空列表
    }
}