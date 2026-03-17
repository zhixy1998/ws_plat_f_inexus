package com.persee.workstation.uv_inexus.service.openBoxCheck.impl;

import com.alibaba.fastjson.JSON;
import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
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
    public Result<Map<String, Object>> openBoxCheck(String taskId) {
        HashMap<String, Object> map = new HashMap<>();
        Workbook taskWorkbook = workbookCacheManager.getTaskWorkbook(taskId);
        // 获取所有 sheet 页的名称
        int sheetCount = taskWorkbook.getNumberOfSheets();
        for (int i = 0; i < sheetCount; i++) {
            String sheetName = taskWorkbook.getSheetName(i);
            InspectionRowDto dto = new InspectionRowDto();
            UnboxingCheckEnum checkEnum = UnboxingCheckEnum.fromSheetName(sheetName);

            dto.setInspectionItem(checkEnum.getAcceptanceItem());
            dto.setStandardValue(checkEnum.getAcceptanceCriteriaValue());
            if ("波长示值误差".equals(dto.getInspectionItem())) {
                Sheet sheet = taskWorkbook.getSheet(sheetName);
                List<ExcelCellLocator.CellPosition> positions1 = excelCellLocator.findCellPositions(sheet, "测量值", "氘灯(656.1)");
                List<ExcelCellLocator.CellPosition> positions2 = excelCellLocator.findCellPositions(sheet, "测量值", "氘灯(486.0)");
                // 创建子项目
                List<InspectionRowDto.MeasurementSubItem> subItems = getMeasurementSubItems(positions1, positions2);
                dto.setSubItems(subItems);
            }
            //最后就是测量值，
            if ("基线平直度".equals(dto.getInspectionItem()) || "漂移".equals(dto.getInspectionItem())) {
                Sheet sheet = taskWorkbook.getSheet(sheetName);
                String keyword = "基线平直度".equals(dto.getInspectionItem()) ? "基线平直度" : "漂移检验";
                List<ExcelCellLocator.CellPosition> positions = excelCellLocator.findCellPositions(sheet, "测量值", keyword);
                List<String> values = positions != null ?
                        positions.stream().map(ExcelCellLocator.CellPosition::getValue).collect(Collectors.toList()) :
                        Collections.emptyList();

                // 创建子项目
                List<InspectionRowDto.MeasurementSubItem> subItems = new ArrayList<>();
                InspectionRowDto.MeasurementSubItem subItem = new InspectionRowDto.MeasurementSubItem();
                subItem.setMeasuredValues(values);
                subItems.add(subItem);

                dto.setSubItems(subItems);
            }
            if ("噪声".equals(dto.getInspectionItem())) {
                Sheet sheet = taskWorkbook.getSheet(sheetName);
                List<ExcelCellLocator.CellPosition> positions = excelCellLocator.findCellPositions(sheet, "测量值", "500nm 100%噪声");
                for (ExcelCellLocator.CellPosition position : positions) {
                    List<InspectionRowDto.MeasurementSubItem> subItems = new ArrayList<>();
                    InspectionRowDto.MeasurementSubItem subItem = new InspectionRowDto.MeasurementSubItem();
                    subItem.setMeasuredValues(Collections.singletonList(position.getValue()));
                    subItems.add(subItem);

                    dto.setSubItems(subItems);
                }
            }
            if ("透射比示值误差".equals(dto.getInspectionItem())) {
                Sheet sheet = taskWorkbook.getSheet(sheetName);
                List<ExcelCellLocator.CellPosition> positions = excelCellLocator.findCellPositions(sheet, "误差值", "紫外分光光度计标准物质GBW(E)130066");
                if (CollectionUtils.isNotEmpty(positions)) {
                    ArrayList<String> strings = new ArrayList<>();
                    for (ExcelCellLocator.CellPosition position : positions) {
                        String value = position.getValue();
                        strings.add(value);
                    }
                    // 找出 strings 中的最大值
                    String maxValue = strings.stream()
                            .max(Comparator.naturalOrder())
                            .orElse("");
                    List<InspectionRowDto.MeasurementSubItem> subItems = new ArrayList<>();
                    InspectionRowDto.MeasurementSubItem subItem = new InspectionRowDto.MeasurementSubItem();
                    subItem.setMeasuredValues(Collections.singletonList(maxValue));
                    subItems.add(subItem);

                    dto.setSubItems(subItems);
                } else {
                    List<ExcelCellLocator.CellPosition> positions1 = excelCellLocator.findCellPositions(sheet, "误差值", "10%中性滤光片");
                    List<ExcelCellLocator.CellPosition> positions2 = excelCellLocator.findCellPositions(sheet, "误差值", "20%中性滤光片");
                    List<ExcelCellLocator.CellPosition> positions3 = excelCellLocator.findCellPositions(sheet, "误差值", "30%中性滤光片");
                    ArrayList<ExcelCellLocator.CellPosition> cellPositions = new ArrayList<>();
                    cellPositions.addAll(positions1);
                    cellPositions.addAll(positions2);
                    cellPositions.addAll(positions3);
                    ArrayList<String> strings = new ArrayList<>();

                    for (ExcelCellLocator.CellPosition position : cellPositions) {
                        String value = position.getValue();
                        strings.add(value);
                    }
                    // 找出 strings 中的最大值
                    String maxValue = strings.stream()
                            .max(Comparator.naturalOrder())
                            .orElse("");
                    List<InspectionRowDto.MeasurementSubItem> subItems = new ArrayList<>();
                    InspectionRowDto.MeasurementSubItem subItem = new InspectionRowDto.MeasurementSubItem();
                    subItem.setMeasuredValues(Collections.singletonList(maxValue));
                    subItems.add(subItem);

                    dto.setSubItems(subItems);
                }
            }

            map.put("key", sheetName);
            map.put("value", dto);
        }

        return Result.ok(map);
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
            String templateCode = "TEMP_" + IdGeneratorUtil.generateShortId() + System.currentTimeMillis();

            // 创建新模板
            ExcelTemplate template = new ExcelTemplate();
            template.setTemplateCode(templateCode);
            template.setTemplateName(request.getTemplateName());
            template.setCreateBy(request.getUpdateBy());
            //非内置模板
            template.setCategory(TEMPLATE_USER_DEFINED.getCode());
            template.setRemark(request.getRemark());
            template.setId(IdGeneratorUtil.generateIdWithPrefix("template"));
            template.setParentId(request.getTemplateCode());
            template.setColumnConfig("{}");
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


    private static List<InspectionRowDto.MeasurementSubItem> getMeasurementSubItems(List<ExcelCellLocator.CellPosition> positions1, List<ExcelCellLocator.CellPosition> positions2) {
        List<InspectionRowDto.MeasurementSubItem> subItems = new ArrayList<>();
        ArrayList<String> strings1 = new ArrayList<>();
        ArrayList<String> strings2 = new ArrayList<>();
        for (ExcelCellLocator.CellPosition position : positions1) {
            strings1.add(position.getValue());
        }
        for (ExcelCellLocator.CellPosition position : positions2) {
            strings2.add(position.getValue());
        }

        InspectionRowDto.MeasurementSubItem subItem = new InspectionRowDto.MeasurementSubItem();
        InspectionRowDto.MeasurementSubItem subItem1 = new InspectionRowDto.MeasurementSubItem();
        subItem.setMeasuredValues(strings1);
        subItem1.setMeasuredValues(strings2);
        subItems.add(subItem);
        subItems.add(subItem1);
        return subItems;
    }

}
