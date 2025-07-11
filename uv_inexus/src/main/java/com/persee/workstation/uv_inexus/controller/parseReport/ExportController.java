package com.persee.workstation.uv_inexus.controller.parseReport;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.persee.workstation.uv_inexus.model.Result;
import com.persee.workstation.uv_inexus.service.parseReport.ExportService;

import io.swagger.annotations.ApiOperation;

@RestController
public class ExportController {

    @Autowired
    private ExportService exportService;

    @GetMapping("/api/parseReport/export")
    @ApiOperation("导出")
    public Object exportAllTables() {
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            exportService.exportAllTables(outputStream);

            HttpHeaders headers = new HttpHeaders();
            headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=allTables.xlsx");
            headers.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_OCTET_STREAM_VALUE);

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(outputStream.toByteArray());
        } catch (IOException e) {
            return Result.fail(500, "导出失败: " + e.getMessage());
        }
    }
}

