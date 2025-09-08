package com.persee.workstation.uv_inexus.controller.guide.parseReport;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.persee.workstation.uv_inexus.model.Result;
// import com.alibaba.excel.EasyExcel;
// import com.persee.workstation.uv_inexus.excel.StandardPretreatmentExcelModel;
import com.persee.workstation.uv_inexus.service.guide.parseReport.ImportService;

import io.swagger.annotations.ApiOperation;

@RestController
public class ImportController {

    @Autowired
    private ImportService importService;

    @PostMapping("/com/persee/workstation/uv-inexus/parseReport/import")
    @ApiOperation("导入")
    public ResponseEntity<Result<String>> importExcel(@RequestParam("file") MultipartFile file) {
        try {
            importService.importExcel(file);
            return ResponseEntity.ok(Result.ok("文件上传成功"));
        } catch (IOException e) {
            System.err.println("11111111111111111");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Result.fail(HttpStatus.INTERNAL_SERVER_ERROR.value(), "文件上传失败：" + e.getMessage()));
        } catch (IllegalArgumentException e) {
            System.err.println("22222222222");
            return ResponseEntity.badRequest().body(Result.fail(HttpStatus.BAD_REQUEST.value(), e.getMessage()));
        }
    }
}
