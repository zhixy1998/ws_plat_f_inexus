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

package com.persee.workstation.uv_inexus.controller.factory_inspection;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.persee.workstation.uv_inexus.component.ExcelDynamicMerger;
import com.persee.workstation.uv_inexus.dto.factory_inspection.*;
import com.persee.workstation.uv_inexus.model.Result;
import com.persee.workstation.uv_inexus.service.factory_inspection.ExcelImportService;
import com.persee.workstation.uv_inexus.service.factory_inspection.TemplateService;
import com.persee.workstation.uv_inexus.service.factory_inspection.WorkbookDataService;
import com.persee.workstation.uv_inexus.util.ExcelCellEditor;
import com.persee.workstation.uv_inexus.util.ExcelCellLocator;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Slf4j
@RestController
@Api("模板管理")
@RequestMapping("/com/persee/workstation/uv-inexus/factory-inspection/template")
public class TemplateController {
    @Autowired
    private TemplateService templateService;

    @Autowired
    private ExcelImportService excelImportService;
    @Autowired
    private ExcelCellLocator excelCellLocator;
    @Autowired
    private ExcelCellEditor excelCellEditor;
    @Autowired
    private ExcelDynamicMerger excelDynamicMerger;
    @Autowired
    private WorkbookDataService workbookDataService;

    //    @Autowired
//    private ExcelExportService excelExportService;
    //导入的是模板文件，目前这个只导入原始最全的模板
    @PostMapping("/import")
    @ApiOperation("导入Excel文件")
    public Result importExcel(
            @RequestBody MultipartFile file) {
        return excelImportService.importExcel(file);
    }

    // 根据自定义检测模板code值来依据完整结果模板创建自定义检测结果模板
    @GetMapping("/buildResultTemplate")
    @ApiOperation("构建结果模板")
    Result buildResultTemplate(String templateCode) {
        return templateService.buildResultTemplate(templateCode,null);
    }

    @GetMapping("/export")
    @ApiOperation("导出Excel文件")
    public Result exportExcel(
            @RequestParam("templateCode") String templateCode,
            HttpServletResponse response) {
        return templateService.exportExcel(templateCode, response);
    }

    @GetMapping("/data")
    @ApiOperation("获取结构化数据")
    public Result<List<Map<String, Object>>> getStructuredData(
            @RequestParam("templateCode") String templateCode) {
        return templateService.getStructuredData(templateCode);
    }

    //新增模板后弹出后以检测项 分组返回
    @GetMapping("/data/grouped")
    @ApiOperation("获取分组数据")
    public Result<Map<String, List<Map<String, Object>>>> getGroupedData(
            @RequestParam("templateCode") String templateCode,
            @RequestParam("groupByField") String groupByField) {
        return templateService.getGroupedData(templateCode, groupByField);
    }

    @GetMapping("/data/list")
    @ApiOperation("查看模板")
    public Result<Map<String, List<Map<String, Object>>>> getGroupedData(
            @RequestParam("templateCode") String templateCode) {
        return templateService.getGroupedData(templateCode, "检测类别");
    }

//    @GetMapping("/headers")
//    @ApiOperation("获取模板字段列表")
//    public Result<List<String>> getAvailableHeaders(@RequestParam String templateCode) {
//        // 调用 service 获取字段列表
//        return templateService.getHeaders(templateCode);
//    }

//    @PostMapping("/data/custom/{templateCode}")
//    @ApiOperation("按自定义字段返回结构化数据")
//    public Result<List<Map<String, Object>>> getCustomStructuredData(
//            @PathVariable String templateCode,
//            @RequestBody List<String> selectedFields) {
//        return templateService.getCustomStructuredData(templateCode, selectedFields);
//    }

    //根据原有的内置模板进行新建模板
    @PostMapping("/template/create")
    @ApiOperation("创建新结构化模板")
    public Result<String> createNewTemplate(@RequestBody @Valid TemplateCreationRequest request) {
        try {
            // 参数校验
            if (request == null) {
                return Result.fail("请求参数不能为空");
            }
            // 检查模板是否已存在
//            Result<List<String>> existingHeaders = templateService.getHeaders(request.getTemplateCode());
//            if (existingHeaders.getCode() == 200) {
//                return Result.fail("模板代码已存在");
//            }

            // 调用服务创建模板
            Result<String> result = templateService.createTemplate(request);

            // 记录日志
            if (result.getCode() == 200) {
                log.info("成功创建模板: {}", request.getTemplateCode());
            } else {
                log.error("创建模板失败: {}, 错误信息: {}", request.getTemplateCode(), result.getMessage());
            }

            return result;
        } catch (Exception e) {
            log.error("创建模板时发生异常: {}", request.getTemplateCode(), e);
            return Result.fail("创建模板失败: " + e.getMessage());
        }
    }

    //修改模板
    @PutMapping("/template/update")
    @ApiOperation("更新现有结构化模板")
    public Result updateTemplate(@RequestBody TemplateUpdateRequest request) {
        try {
            // 参数校验
            if (request == null) {
                return Result.fail("请求参数不能为空");
            }
            if (request.getTemplateCode() == null || request.getTemplateCode().trim().isEmpty()) {
                return Result.fail("模板代码不能为空");
            }
            if (request.getSelectedFields() == null || request.getSelectedFields().isEmpty()) {
                return Result.fail("选中字段不能为空");
            }

            // 检查模板是否存在
            Result<List<String>> existingHeaders = templateService.getHeaders(request.getTemplateCode());
            if (existingHeaders.getCode() != 200) {
                return Result.fail("模板不存在: " + request.getTemplateCode());
            }

            // 调用服务更新模板
            Result result = templateService.updateTemplate(request);

            // 记录日志
            if (result.getCode() == 200) {
                log.info("成功更新模板: {}", request.getTemplateCode());
            } else {
                log.error("更新模板失败: {}, 错误信息: {}", request.getTemplateCode(), result.getMessage());
            }

            return result;
        } catch (Exception e) {
            log.error("更新模板时发生异常: {}", request.getTemplateCode(), e);
            return Result.fail("更新模板失败: " + e.getMessage());
        }
    }

    //从新的Excel中进行sheet页筛选，和指定sheet的行筛选
    @PostMapping("/edit-excel")
    public Result editExcel(
            @RequestParam("file") MultipartFile file,
            @RequestParam Map<String, String> sheetRetainRows) throws IOException {

        // 解析参数：{"Sheet1": "row1,row2", "Sheet2": "rowA,rowB"}
        Map<String, Set<String>> params = parseParams(sheetRetainRows);
        return templateService.editExcel(file, params);
    }

    private Map<String, Set<String>> parseParams(Map<String, String> input) {
        Map<String, Set<String>> result = new HashMap<>();
        input.forEach((sheet, rows) ->
                result.put(sheet, new HashSet<>(Arrays.asList(rows.split(","))))
        );
        return result;
    }

    //获取模板列表
    @PostMapping("/template/list")
    @ApiOperation("获取模板列表")
    public Result<Page<TemplateResponseDto>> getTemplateList(@RequestBody TemplateQeqDto templateQeqDto) {
        return templateService.getTemplateList(templateQeqDto);
    }

    @GetMapping("/template/delete")
    @ApiOperation("删除模板")
    public Result<String> deleteTemplate(@RequestParam("templateCode") String templateCode) {
        return templateService.deleteTemplate(templateCode);
    }

    /**
     * 批量删除模板
     *
     * @param
     * @return
     */
    @PostMapping("/batch-delete")
    @ApiOperation("批量删除模板")
    public Result<String> batchDeleteTemplate(@RequestBody BatchDelDto batchDelDto) {
        List<String> list = batchDelDto.getTemplateCodes();
        if (CollectionUtils.isEmpty(list)) {
            return Result.fail("请选择要删除的模板");
        }
        return templateService.batchDeleteTemplate(list);
    }

    /**
     * 下拉框列表
     */
    @GetMapping("/material/options")
    @ApiOperation("下拉框列表")
    public Result<List<Map<String, String>>> getMaterialOptions(@RequestParam String id) {
        return templateService.getMaterialOptions(id);
    }
//    /**
//     * 新增未知检测项，要知道是哪个检测类别的
//     */
//    public Result<String> addUnknownItem(@RequestBody UnknownItemDto unknownItemDto) {
//        return templateService.addUnknownItem(unknownItemDto);
//    }

    @GetMapping("/lll")
    public ResponseEntity<String> exportExcel(HttpServletResponse response) {
        Map<Integer, List<Object>> columnData = buildColumnData();

        try (FileInputStream fis = new FileInputStream("D:\\exp333ile.xlsx")) {
            Workbook workbook = new XSSFWorkbook(fis);
            int lastRow = workbook.getSheet("波长示值误差与重复性校验").getLastRowNum();
            // 在第10行插入数据（rowIndex=10）
//            excelDynamicMerger.insertDynamicRowWithMerge(workbook, "波长示值误差与重复性校验", 10, columnData,mergeConfig);
// Step 3：增强插入逻辑 (添加边界校验)
            excelDynamicMerger.insertDynamicMergedRow(
                    workbook,
                    "波长示值误差与重复性校验",
                    lastRow + 1,          // 插入行位置
                    columnData
            );
            // 配置响应头
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setHeader("Content-Disposition", "attachment; filename=exported_file.xlsx");

            // 写入响应流
            try (OutputStream out = response.getOutputStream()) {
                workbook.write(out);
            }

            return ResponseEntity.ok("导出成功");
        } catch (Exception e) {
            log.error("导出Excel文件时发生异常", e);
        }
        return null;
    }

    /**
     * 构造列数据（键：Excel列索引，值：该列的所有单元格数据）
     * 示例：第0列合并6行，值为"项目A"；第1列6个特征值...
     */

    // Step 2：完善列数据构建
    private Map<Integer, List<Object>> buildColumnData() {
        Map<Integer, List<Object>> data = new HashMap<>(12);

        // 项目名称列 (跨6行合并)
        data.put(0, Collections.singletonList("9595"));

        // 特征值列 (6个字符串)
        data.put(1, Arrays.asList("222.7", "333.8", "444.5", "555.0", "666.4", "777.0"));

        // 空列处理 (使用nCopies保证行数一致)
        IntStream.rangeClosed(2, 7).forEach(i ->
                data.put(i, Collections.nCopies(6, ""))
        );

        // 动态转换数值类型
        data.put(8, Arrays.asList(3, 4, 5, 6, 7, 3).stream()
                .map(Object::toString)
                .collect(Collectors.toList()));
        data.put(9, Arrays.asList(2, 2, 2, 2, 2, 2).stream()
                .map(Object::toString)
                .collect(Collectors.toList()));
// 空列处理 (使用nCopies保证行数一致)
        data.put(10, Collections.nCopies(6, ""));
        return data;
    }


}
