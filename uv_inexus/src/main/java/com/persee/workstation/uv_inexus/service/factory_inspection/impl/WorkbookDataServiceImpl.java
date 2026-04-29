package com.persee.workstation.uv_inexus.service.factory_inspection.impl;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.persee.workstation.uv_inexus.dao.factory_inspection.WorkbookDataMapper;
import com.persee.workstation.uv_inexus.po.factory_inspection.WorkbookData;
import com.persee.workstation.uv_inexus.service.factory_inspection.WorkbookDataService;
import com.persee.workstation.uv_inexus.util.IdGeneratorUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;

/*
 * *
 *  * 版权所有 (C) 2025 北京普析通用仪器有限责任公司。保留所有权利。
 *  * 本软件受商业机密法和版权法保护。
 *  * 本代码仅限与北京普析通用仪器有限责任公司签订有效协议的授权用户内部使用。
 *  * 任何未经明确许可的复制、使用、修改、分发或公开均被严格禁止。
 *
 */
@Slf4j
@Service
public class WorkbookDataServiceImpl extends ServiceImpl<WorkbookDataMapper, WorkbookData> implements WorkbookDataService {
    /**
     * 上传Excel文件并保存数据的方法(保存还没有与任务相互关联的模板)
     *
     * @param workbook     Excel工作簿对象，包含要上传的Excel数据
     * @param templateCode 模板代码，用于标识上传的Excel模板类型
     * @return 上传成功返回true，失败返回false
     */


    /**
     * 上传Excel文件并保存数据的方法(保存还没有与任务相互关联的模板)
     *
     * @param workbook     Excel工作簿对象，包含要上传的Excel数据
     * @param templateCode 模板代码，用于标识上传的Excel模板类型
     * @return 上传成功返回true，失败返回false
     */
    // ... existing code ...

    /**
     * 上传Excel文件并保存数据的方法(保存还没有与任务相互关联的模板)
     *
     * @param workbook     Excel工作簿对象，包含要上传的Excel数据
     * @param templateCode 模板代码，用于标识上传的Excel模板类型
     * @return 上传成功返回true，失败返回false
     */
    public boolean uploadExcel(Workbook workbook, String templateCode) {
        return uploadExcel(workbook, templateCode, null);
    }

    /**
     * 上传Excel文件并保存数据的方法（支持指定黄色高亮列信息）
     *
     * @param workbook     Excel工作簿对象，包含要上传的Excel数据
     * @param templateCode 模板代码，用于标识上传的Excel模板类型
     * @param yellowColumnsMap 黄色高亮列信息 Map<工作表名, 列名列表>
     * @return 上传成功返回true，失败返回false
     */
    public boolean uploadExcel(Workbook workbook, String templateCode, Map<String, List<String>> yellowColumnsMap) {
        // 创建WorkbookData对象用于存储Excel数据
        WorkbookData data = new WorkbookData();

        try {
            log.info("开始处理Excel上传，templateCode: {}", templateCode);
            log.info("工作簿中包含 {} 个工作表", workbook.getNumberOfSheets());

            // 【新增】提取或使用传入的黄色高亮列信息
            Map<String, List<String>> finalYellowColumnsMap = yellowColumnsMap;
            if (finalYellowColumnsMap == null || finalYellowColumnsMap.isEmpty()) {
                finalYellowColumnsMap = extractYellowHighlightedColumns(workbook);
            }

            log.info("黄色列提取结果: {}", finalYellowColumnsMap);

            if (!finalYellowColumnsMap.isEmpty()) {
                String yellowColumnsJson = JSON.toJSONString(finalYellowColumnsMap);
                data.setYellowHighlightedColumns(yellowColumnsJson);
                log.info("设置黄色高亮列信息到data对象: {}", yellowColumnsJson);
            } else {
                log.warn("未找到任何黄色高亮列！");
            }

            // 创建字节数组输出流，用于将Excel数据转换为字节数组
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            // 将workbook内容写入输出流
            workbook.write(outputStream);
            // 生成唯一ID并设置到data对象中
            data.setId(IdGeneratorUtil.generateId());
            // 将Excel数据设置为字节数组并保存到data对象
            data.setWorkbookData(outputStream.toByteArray());
            // 设置数据状态为激活
            data.setIsActive(true);
            // 设置创建时间为当前时间
            data.setCreateTime(LocalDateTime.now());
            // 设置最后访问时间为当前时间
            data.setLastAccessTime(LocalDateTime.now());
            // 设置更新时间为当前时间
            data.setUpdateTime(LocalDateTime.now());
            // 设置更新者为系统
            data.setUpdateBy("sys");
            data.setTaskId(templateCode);
            // 设置模板代码
            data.setTemplateCode(templateCode);

            log.info("准备保存WorkbookData，yellowHighlightedColumns值为: {}", data.getYellowHighlightedColumns());

            // 保存数据并返回结果
            boolean result = this.save(data);
            log.info("保存结果: {}, 生成的ID: {}", result, data.getId());
            return result;
        } catch (IOException e) {
            // 记录错误日志
            log.error("上传Excel文件失败", e);
            // 返回false表示上传失败
            return false;
        }
    }

// ... existing code ...


    /**
     * 提取所有工作表中RGB(255, 192, 0)黄色高亮的列名
     *
     * @param workbook Excel工作簿
     * @return Map<工作表名称, 黄色列名列表>
     */
    private Map<String, List<String>> extractYellowHighlightedColumns(Workbook workbook) {
        Map<String, List<String>> result = new HashMap<>();

        for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
            Sheet sheet = workbook.getSheetAt(i);
            String sheetName = sheet.getSheetName();

            log.debug("检查工作表[{}]", sheetName);

            List<String> yellowColumns = findTargetYellowHeaders(sheet);
            if (!yellowColumns.isEmpty()) {
                result.put(sheetName, yellowColumns);
                log.info("工作表[{}]中找到{}个黄色高亮列: {}", sheetName, yellowColumns.size(), yellowColumns);
            } else {
                log.debug("工作表[{}]中未找到黄色高亮列", sheetName);
            }
        }

        return result;
    }

    /**
     * 查找RGB(255, 192, 0)黄色背景的表头列名
     *
     * @param sheet 工作表
     * @return 黄色列名列表
     */
    private List<String> findTargetYellowHeaders(Sheet sheet) {
        List<String> result = new ArrayList<>();

        Row headerRow = sheet.getRow(0);
        if (headerRow == null) {
            log.debug("工作表[{}]的第一行为空", sheet.getSheetName());
            return result;
        }

        int cellCount = 0;
        for (Cell cell : headerRow) {
            cellCount++;
            if (cell == null) {
                continue;
            }

            String cellValue = "";
            try {
                cellValue = cell.getStringCellValue();
            } catch (Exception e) {
                log.debug("单元格无法获取字符串值: {}", e.getMessage());
                continue;
            }

            if (cellValue == null || cellValue.trim().isEmpty()) {
                continue;
            }

            boolean isYellow = isTargetYellowBackground(cell);
            log.debug("检查单元格[{}]: 值='{}', 是否黄色={}", cellCount, cellValue, isYellow);

            if (isYellow) {
                result.add(cellValue);
            }
        }

        log.debug("工作表[{}]共检查{}个单元格，找到{}个黄色列", sheet.getSheetName(), cellCount, result.size());
        return result;
    }

    // ... existing code ...

    /**
     * 判断单元格是否为特定黄色背景 RGB(255, 192, 0)
     *
     * @param cell 单元格
     * @return true表示是目标黄色背景
     */
    private boolean isTargetYellowBackground(Cell cell) {
        if (cell == null) {
            return false;
        }

        CellStyle style = cell.getCellStyle();
        Color fillColor = style.getFillForegroundColorColor();

        if (fillColor == null) {
            log.debug("单元格无填充色");
            return false;
        }

        if (!(fillColor instanceof org.apache.poi.xssf.usermodel.XSSFColor)) {
            log.debug("单元格颜色类型不是XSSFColor: {}", fillColor.getClass().getName());
            return false;
        }

        org.apache.poi.xssf.usermodel.XSSFColor xssfColor =
                (org.apache.poi.xssf.usermodel.XSSFColor) fillColor;

        try {
            byte[] rgb = xssfColor.getRGB();
            if (rgb != null && rgb.length >= 3) {
                int r = rgb[0] & 0xFF;
                int g = rgb[1] & 0xFF;
                int b = rgb[2] & 0xFF;

                log.debug("单元格RGB值: R={}, G={}, B={}", r, g, b);

                // 使用范围判断黄色（与ExcelReader.java保持一致）
                // 黄色的特征：红色和绿色分量高，蓝色分量低
                boolean isYellow = r > 200 && g > 150 && b < 100;

                if (isYellow) {
                    log.debug("✓ 匹配到黄色（R>200, G>150, B<100）");
                } else {
                    log.debug("✗ 不是黄色");
                }

                return isYellow;
            } else {
                log.debug("RGB数组为空或长度不足: {}", rgb != null ? rgb.length : "null");
            }
        } catch (Exception e) {
            log.warn("获取单元格颜色失败: {}", e.getMessage(), e);
        }

        return false;
    }

// ... existing code ...


}
