package com.persee.workstation.uv_inexus.service.openBoxCheck.impl;

import com.alibaba.fastjson.JSON;
import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.ObjectUtils;
import com.persee.workstation.uv_inexus.component.ExcelDynamicMerger;
import com.persee.workstation.uv_inexus.component.WorkbookCacheManager;
import com.persee.workstation.uv_inexus.dao.factory_inspection.TemplateMapper;
import com.persee.workstation.uv_inexus.dto.factory_inspection.InspectionRowDto;
import com.persee.workstation.uv_inexus.dto.factory_inspection.TemplateCreationRequest;
import com.persee.workstation.uv_inexus.enums.factoryInspection.UnboxingCheckEnum;
import com.persee.workstation.uv_inexus.model.Result;
import com.persee.workstation.uv_inexus.po.factory_inspection.ExcelDataAsset;
import com.persee.workstation.uv_inexus.po.factory_inspection.ExcelTemplate;
import com.persee.workstation.uv_inexus.po.factory_inspection.TaskExecutionDetail;
import com.persee.workstation.uv_inexus.po.factory_inspection.TaskManagement;
import com.persee.workstation.uv_inexus.service.factory_inspection.*;
import com.persee.workstation.uv_inexus.service.openBoxCheck.OpenBoxCheckService;
import com.persee.workstation.uv_inexus.util.ExcelCellEditor;
import com.persee.workstation.uv_inexus.util.ExcelCellLocator;
import com.persee.workstation.uv_inexus.util.ExcelUpdateNotifier;
import com.persee.workstation.uv_inexus.util.IdGeneratorUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static com.persee.workstation.uv_inexus.enums.factoryInspection.CalibrationTypeEnum.TEMPLATE_USER_DEFINED;

/*
 * *
 *  * 版权所有 (C) 2025 北京普析通用仪器有限责任公司。保留所有权利。
 *  * 本软件受商业机密法和版权法保护。
 *  * 本代码仅限与北京普析通用仪器有限责任公司签订有效协议的授权用户内部使用。
 *  * 任何未经明确许可的复制、使用、修改、分发或公开均被严格禁止。
 *
 */
@Service
@Slf4j
public class OpenBoxCheckServiceImpl implements OpenBoxCheckService {
    @Autowired
    private TemplateService templateService;

    @Autowired
    private WorkbookCacheManager workbookCacheManager;

    @Autowired
    private ExcelCellLocator excelCellLocator;
    @Autowired
    private TemplateMapper templateMapper;
    @Autowired
    private ExcelDataAssetService excelDataAssetService;
    /*@Value("${excel.save.path}")
    private String excelSavePath;*/

    @Autowired
    private ExcelCellEditor excelCellEditor;
    @Autowired
    private ExcelUpdateNotifier excelUpdateNotifier;

    @Autowired
    private TaskExecutionDetailService detailService;
    @Autowired
    @Lazy
    private TaskManagementService taskManagementService;
    @Autowired
    private CalArtifactsService calArtifactsService;
    @Autowired
    private CalStdCatsService calStdCatsService;
    @Autowired
    private WorkbookDataService workbookDataService;
    @Autowired
    private ExcelDynamicMerger excelDynamicMerger;


    @Override
    public Result<InspectionRowDto> openBoxCheck(String taskId) {
        InspectionRowDto result = new InspectionRowDto();
        try {
            Workbook taskWorkbook = workbookCacheManager.getTaskWorkbook(taskId);
            int sheetCount = taskWorkbook.getNumberOfSheets();

            Map<String, List<String>> measuredValuesMap = new HashMap<>();

            for (int i = 0; i < sheetCount; i++) {
                String sheetName = taskWorkbook.getSheetName(i);
                UnboxingCheckEnum checkEnum = UnboxingCheckEnum.fromSheetName(sheetName);
                if (ObjectUtils.isEmpty(checkEnum)) {
                    continue;
                }

                if ("波长示值误差与重复性校验".equals(sheetName)) {
                    Sheet sheet = taskWorkbook.getSheet(sheetName);
                    List<ExcelCellLocator.CellPosition> positions1 = excelCellLocator.findCellPositions(sheet, "测量值", "氘灯(486.0)");

                    List<String> values = positions1 != null ?
                            positions1.stream().map(ExcelCellLocator.CellPosition::getValue).collect(Collectors.toList()) :
                            Collections.emptyList();

                    measuredValuesMap.put("wavelength_mpe", values);
                    measuredValuesMap.put("wl_rep", values);
                }

                if ("基线平直度检验".equals(sheetName)) {
                    Sheet sheet = taskWorkbook.getSheet(sheetName);
                    List<ExcelCellLocator.CellPosition> positions = excelCellLocator.findCellPositions(sheet, "测量值", "基线平直度");
                    List<String> values = positions != null ?
                            positions.stream().map(ExcelCellLocator.CellPosition::getValue).collect(Collectors.toList()) :
                            Collections.emptyList();
                    measuredValuesMap.put("bsl_stab", values);
                }

                if ("漂移检验".equals(sheetName)) {
                    Sheet sheet = taskWorkbook.getSheet(sheetName);
                    List<ExcelCellLocator.CellPosition> positions = excelCellLocator.findCellPositions(sheet, "测量值", "漂移检验");
                    List<String> values = positions != null ?
                            positions.stream().map(ExcelCellLocator.CellPosition::getValue).collect(Collectors.toList()) :
                            Collections.emptyList();
                    measuredValuesMap.put("stab", values);
                }

                if ("噪声检验".equals(sheetName)) {
                    Sheet sheet = taskWorkbook.getSheet(sheetName);
                    List<ExcelCellLocator.CellPosition> positions = excelCellLocator.findCellPositions(sheet, "测量值", "500nm100%噪声");
                    List<String> values = positions != null ?
                            positions.stream().map(ExcelCellLocator.CellPosition::getValue).collect(Collectors.toList()) :
                            Collections.emptyList();
                    measuredValuesMap.put("noise", values);
                }

                if ("透射比示值误差与重复性校验".equals(sheetName)) {
                    Sheet sheet = taskWorkbook.getSheet(sheetName);
                    List<ExcelCellLocator.CellPosition> positions = excelCellLocator.findCellPositions(sheet, "误差值", "紫外分光光度计标准物质GBW(E)130066");
                    if (CollectionUtils.isNotEmpty(positions)) {
                        ArrayList<String> strings = new ArrayList<>();
                        for (ExcelCellLocator.CellPosition position : positions) {
                            strings.add(position.getValue());
                        }
                        measuredValuesMap.put("photo_acc", strings);
                    } else {
                        List<ExcelCellLocator.CellPosition> positions1 = excelCellLocator.findCellPositions(sheet, "误差值", "10% 中性滤光片");
                        List<ExcelCellLocator.CellPosition> positions2 = excelCellLocator.findCellPositions(sheet, "误差值", "20% 中性滤光片");
                        List<ExcelCellLocator.CellPosition> positions3 = excelCellLocator.findCellPositions(sheet, "误差值", "30% 中性滤光片");
                        ArrayList<ExcelCellLocator.CellPosition> cellPositions = new ArrayList<>();
                        cellPositions.addAll(positions1);
                        cellPositions.addAll(positions2);
                        cellPositions.addAll(positions3);
                        ArrayList<String> strings = new ArrayList<>();

                        for (ExcelCellLocator.CellPosition position : cellPositions) {
                            strings.add(position.getValue());
                        }
                        measuredValuesMap.put("photo_acc", strings);
                    }
                }

                if ("杂散光校验".equals(sheetName)) {
                    Sheet sheet = taskWorkbook.getSheet(sheetName);
                    List<ExcelCellLocator.CellPosition> positions = excelCellLocator.findCellPositions(sheet, "测量值", "杂散光");
                    List<String> values = positions != null ?
                            positions.stream().map(ExcelCellLocator.CellPosition::getValue).collect(Collectors.toList()) :
                            Collections.emptyList();
                    measuredValuesMap.put("stray_lt", values);
                }
            }

            result.setWavelength_mpe(buildFieldItem("波长最大允许误差",
                    "±1 nm(T500)、±2 nm(T400)", measuredValuesMap.getOrDefault("wavelength_mpe", Collections.emptyList())));
            result.setWl_rep(buildFieldItem("波长重复性",
                    "T500≤0.2nm、T400≤0.4nm", measuredValuesMap.getOrDefault("wl_rep", Collections.emptyList())));
            result.setBsl_stab(buildFieldItem("基线稳定性",
                    "±0.002Abs", measuredValuesMap.getOrDefault("bsl_stab", Collections.emptyList())));
            result.setStab(buildFieldItem("稳定性",
                    "≤0.35T%/h(500nm、2nm 带宽、预热后)", measuredValuesMap.getOrDefault("stab", Collections.emptyList())));
            result.setNoise(buildFieldItem("噪声",
                    "≤0.15T%(500nm、2nm 带宽)", measuredValuesMap.getOrDefault("noise", Collections.emptyList())));
            result.setStray_lt(buildFieldItem("杂散光",
                    "T500≤0.05 %T；T400≤0.1 %T", measuredValuesMap.getOrDefault("stray_lt", Collections.emptyList())));
            result.setPhoto_acc(buildFieldItem("光度准确度",
                    "±0.3%T", measuredValuesMap.getOrDefault("photo_acc", Collections.emptyList())));

        } catch (Exception e) {
            e.printStackTrace();
            return Result.fail("获取任务数据失败");
        }
        log.info("开箱报告的内容是"+result.toString());
        return Result.ok(result);
    }

    private InspectionRowDto.FieldItem buildFieldItem(String inspectionItem, String standardValue, List<String> measuredValues) {
        InspectionRowDto.FieldItem item = new InspectionRowDto.FieldItem();
        item.setLabel(inspectionItem);
        item.setStandardValue(standardValue);

        if (measuredValues == null || measuredValues.isEmpty()) {
//            item.setMeasuredValue("0.001");
            item.setMeasuredValue("");
        } else if (measuredValues.size() == 1) {
            item.setMeasuredValue(measuredValues.get(0));
        } else {
            item.setMeasuredValue(measuredValues);
        }

//        if (inspectionItem.equals("稳定性")){
//            item.setMeasuredValue("0.002");
//        }
        return item;
    }



    @Override
    @DS("factory_inspection")
    @Transactional(rollbackFor = Exception.class)
    public Result<String> editUnboxingTemplate(TemplateCreationRequest request) {
        try {
            // 验证模板名称是否重复
            LambdaQueryWrapper<ExcelTemplate> wrapper1 = new LambdaQueryWrapper<>();
            wrapper1.eq(ExcelTemplate::getTemplateName, request.getTemplateName());
            ExcelTemplate one = templateService.getOne(wrapper1);
            if (one != null) {
                return Result.fail("模板名称重复！！");
            }

            // 创建新模板
            ExcelTemplate template = new ExcelTemplate();
//            template.setTemplateCode(templateCode);
            template.setTemplateName(request.getTemplateName());
            template.setCreateBy(request.getUpdateBy());
            //非内置模板
            template.setCategory(TEMPLATE_USER_DEFINED.getCode());
            template.setRemark(request.getRemark());
            template.setId(IdGeneratorUtil.generateIdWithPrefix("template"));
            template.setParentId(request.getTemplateCode());
            template.setColumnConfig("{}");
            String templateCode = template.getId();

            // 保存到数据库
            boolean saved = templateService.save(template);
            LambdaQueryWrapper<ExcelDataAsset> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(ExcelDataAsset::getTemplateCode, request.getTemplateCode());

            List<Map<String, Object>> selectedRows = new ArrayList<>();
            request.getSelectedRows().entrySet()
                    .forEach(entry -> {
                        List<Map<String, Object>> valueList = entry.getValue();
                        selectedRows.addAll(valueList);
                    });
            int rowIndex = 1; // 从第1行开始计数（跳过表头）
            ArrayList<ExcelDataAsset> list = new ArrayList<>();

            for (Map.Entry<String, List<Map<String, Object>>> entry : request.getSelectedRows().entrySet()) {
                String key = entry.getKey();
                Map<String, Object> dataMap = entry.getValue().get(0);
                ExcelDataAsset asset = new ExcelDataAsset();
                asset.setId(IdGeneratorUtil.generateIdWithPrefix("asset"));
                asset.setTemplateCode(templateCode);
                asset.setBatchNo(String.valueOf(rowIndex++));
                dataMap.put("id", asset.getId());
                asset.setStructuredData(JSON.toJSONString(dataMap));
                asset.setImportStatus("SUCCESS");
                LambdaQueryWrapper<TaskExecutionDetail> queryWrapper = new LambdaQueryWrapper<>();
                queryWrapper.eq(TaskExecutionDetail::getItemId, key)
                        .isNull(TaskExecutionDetail::getTaskId);
                //这块查出来是两个，一个校零，一个测量
                List<TaskExecutionDetail> detailList = detailService.list(queryWrapper);
                if (CollectionUtils.isNotEmpty(detailList)) {
                    for (TaskExecutionDetail detail : detailList) {
                        detail.setItemId(asset.getId());
                        detail.setId(IdGeneratorUtil.generateId());
                    }
                }
                detailService.saveBatch(detailList);
                list.add(asset);
            }
            excelDataAssetService.saveBatch(list);
            //不存在的检测项
            List<Map<String, Object>> rowsNotInTemplate = new ArrayList<>();
            Map<String, List<Map<String, Object>>> filteredRows = request.getSelectedRows().entrySet().stream()
                    .filter(entry -> entry.getKey().startsWith("new"))
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

            // 将筛选后的数据添加到rowsNotInTemplate列表中
            filteredRows.values().forEach(rowsNotInTemplate::addAll);
            templateService.buildResultTemplate(template.getTemplateCode(), rowsNotInTemplate);

            if (saved) {
                log.info("模板创建成功，模板编码：{}", templateCode);
                log.info("开始替换开箱验证的任务模板");
                String taskId = request.getTaskId();
                String id = template.getId();
                TaskManagement management = new TaskManagement();
                management.setId(taskId);
                management.setUseTemplate(id);
                management.setUpdated(LocalDateTime.now());
                boolean b = taskManagementService.updateById(management);
                return b ? Result.ok("修改成功") : Result.fail("修改失败");
            } else {
                return Result.fail("修改失败");
            }
        } catch (Exception e) {
            return Result.fail("修改失败: " + e.getMessage());
        }
    }


//    private static List<InspectionRowDto.MeasurementSubItem> getMeasurementSubItems(List<ExcelCellLocator.CellPosition> positions1/*, List<ExcelCellLocator.CellPosition> positions2*/) {
//        List<InspectionRowDto.MeasurementSubItem> subItems = new ArrayList<>();
//        ArrayList<String> strings1 = new ArrayList<>();
//        ArrayList<String> strings2 = new ArrayList<>();
//        for (ExcelCellLocator.CellPosition position : positions1) {
//            strings1.add(position.getValue());
//        }
////        for (ExcelCellLocator.CellPosition position : positions2) {
////            strings2.add(position.getValue());
////        }
//
//        InspectionRowDto.MeasurementSubItem subItem = new InspectionRowDto.MeasurementSubItem();
//        InspectionRowDto.MeasurementSubItem subItem1 = new InspectionRowDto.MeasurementSubItem();
//        subItem.setMeasuredValues(strings1);
////        subItem1.setMeasuredValues(strings2);
//        subItems.add(subItem);
////        subItems.add(subItem1);
//        return subItems;
//    }

}
