package com.persee.workstation.uv_inexus.controller.factory_inspection;

import io.swagger.annotations.Api;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.file.Path;
import java.nio.file.Paths;

/*
 * *
 *  * 版权所有 (C) 2025 北京普析通用仪器有限责任公司。保留所有权利。
 *  * 本软件受商业机密法和版权法保护。
 *  * 本代码仅限与北京普析通用仪器有限责任公司签订有效协议的授权用户内部使用。
 *  * 任何未经明确许可的复制、使用、修改、分发或公开均被严格禁止。
 *
 *
 */
@Slf4j
@RestController
@Api("文件，提供文件预览等")
@RequestMapping("/com/persee/workstation/uv-inexus/factory-inspection/file")
public class FileController {
    @Value("${excel.save.path}")
    private String excelSavePath;

    /**
     * 文件预览接口
     */
    @GetMapping("/preview")
    public ResponseEntity<Resource> preview(@RequestParam String fileName,
                                            @RequestParam(required = false) String sheetName) {
        try {
            String filePath = excelSavePath + fileName;
            Path path = Paths.get(filePath);
            Resource resource = new UrlResource(path.toUri());

            if (resource.exists()) {
                return ResponseEntity.ok()
                        .contentType(MediaType.valueOf("application/vnd.openxmlformats-office-document.spreadsheetml.sheet"))
                        .header(HttpHeaders.CONTENT_DISPOSITION,
                                "inline;filename=\"" + fileName + "\"")
                        .body(resource);
            }
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

}
