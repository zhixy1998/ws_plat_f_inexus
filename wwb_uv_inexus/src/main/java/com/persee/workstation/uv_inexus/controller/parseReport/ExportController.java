package com.persee.workstation.uv_inexus.controller.parseReport;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.persee.workstation.uv_inexus.model.Result;
import com.persee.workstation.uv_inexus.service.parseReport.ExportService;

import io.swagger.annotations.ApiOperation;

@RestController
public class ExportController {

    // 允许的筛选参数（与列表筛选保持一致）
    public static final Set<String> ALLOWED_QUERY_FIELDS = new HashSet<String>() {{
        add("standard_name");
        add("industry");
        add("release_date");
        add("startDate");
        add("endDate");
    }};

    @Autowired
    private ExportService exportService;

    @GetMapping("/com/persee/workstation/uv-inexus/parseReport/export")
    @ApiOperation("导出标准数据（支持筛选）")
    public Object exportByCondition(
            @RequestParam(value = "isAll", defaultValue = "1") Integer isAll,
            @RequestParam(value = "exportIds", required = false) String exportIds,
            // 接收所有筛选参数
            @RequestParam Map<String, Object> queryParams) {
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            
            // 过滤出允许的筛选参数（防止无效参数）
            Map<String, Object> validParams = new HashMap<>();
            for (Map.Entry<String, Object> entry : queryParams.entrySet()) {
                if (ALLOWED_QUERY_FIELDS.contains(entry.getKey())) {
                    validParams.put(entry.getKey(), entry.getValue());
                }
            }

            if (isAll == 1) {
                // 全量导出（支持筛选参数）
                exportService.exportAllTables(outputStream, validParams);
            } else {
                // 按ID筛选导出（必须提供exportIds）
                if (exportIds == null || exportIds.isEmpty()) {
                    return Result.fail(400, "当isAll=0时，必须提供exportIds参数");
                }
                exportService.exportByCondition(outputStream, isAll, exportIds, validParams);
            }

            HttpHeaders headers = new HttpHeaders();
            headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=standard_data.xlsx");
            headers.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_OCTET_STREAM_VALUE);

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(outputStream.toByteArray());
        } catch (IOException e) {
            return Result.fail(500, "导出失败: " + e.getMessage());
        } catch (IllegalArgumentException e) {
            return Result.fail(400, e.getMessage());
        } catch (Exception e) {
            return Result.fail(500, "导出异常: " + e.getMessage());
        }
    }
}