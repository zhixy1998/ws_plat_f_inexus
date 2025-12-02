/*
 * *
 *  * 版权所有 (C) 2025 北京普析通用仪器有限责任公司。保留所有权利。
 *  * 本软件受商业机密法和版权法保护。
 *  * 本代码仅限与北京普析通用仪器有限责任公司签订有效协议的授权用户内部使用。
 *  * 任何未经明确许可的复制、使用、修改、分发或公开均被严格禁止。
 *
 */

package com.persee.workstation.uv_inexus.controller.guide.parseReport;

import java.io.IOException;

import com.persee.workstation.uv_inexus.i18n.YamlMessageSource;
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
            //文件上传成功
            return ResponseEntity.ok(Result.ok(YamlMessageSource.getMessageStatic("00404001")));
        } catch (IOException e) {
            System.err.println("11111111111111111");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Result.fail(HttpStatus.INTERNAL_SERVER_ERROR.value(), YamlMessageSource.getMessageStatic("00404002",e.getMessage())));
        } catch (IllegalArgumentException e) {
            System.err.println("22222222222");

            return ResponseEntity.ok((Result<String>)(Result) Result.build(false, HttpStatus.OK.value(), e.getMessage()));
        }
    }
}
