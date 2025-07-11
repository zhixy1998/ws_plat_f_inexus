package com.persee.workstation.uv_inexus.controller.parseReport;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

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

    @Autowired
    private ExportService exportService;

    @GetMapping("/com/persee/workstation/uv-inexus/parseReport/export")
    @ApiOperation("导出标准数据")
    public Object exportByCondition(
            @RequestParam(value = "isAll", defaultValue = "1") Integer isAll,
            @RequestParam(value = "exportIds", required = false) String exportIds) {
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            
            if (isAll == 1) {
                // 全量导出
                exportService.exportAllTables(outputStream);
            } else {
                // 按ID筛选导出（必须提供exportIds）
                if (exportIds == null || exportIds.isEmpty()) {
                    return Result.fail(400, "当isAll=0时，必须提供exportIds参数");
                }
                exportService.exportByCondition(outputStream, isAll, exportIds);
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