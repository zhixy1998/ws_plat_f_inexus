/*
 * *
 *  * 版权所有 (C) 2025 北京普析通用仪器有限责任公司。保留所有权利。
 *  * 本软件受商业机密法和版权法保护。
 *  * 本代码仅限与北京普析通用仪器有限责任公司签订有效协议的授权用户内部使用。
 *  * 任何未经明确许可的复制、使用、修改、分发或公开均被严格禁止。
 *
 */

package com.persee.workstation.uv_inexus.controller.guide.parseReport;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.persee.workstation.uv_inexus.i18n.I18nMessageSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.persee.workstation.uv_inexus.model.Result;
import com.persee.workstation.uv_inexus.service.guide.parseReport.ExportService;

import io.swagger.annotations.ApiOperation;

@RestController
@RequestMapping("/com/persee/workstation/uv-inexus/parseReport")
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

    @PostMapping("/export") // 修改为POST请求
    @ApiOperation("导出标准数据（支持筛选，POST方式）")
    public Object exportByCondition(
            @RequestBody ExportRequestParam requestParam) { // 使用自定义对象接收所有参数
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            
            // 过滤出允许的筛选参数（防止无效参数）
            Map<String, Object> validParams = new HashMap<>();
            if (requestParam.getQueryParams() != null) {
                for (Map.Entry<String, Object> entry : requestParam.getQueryParams().entrySet()) {
                    if (ALLOWED_QUERY_FIELDS.contains(entry.getKey())) {
                        validParams.put(entry.getKey(), entry.getValue());
                    }
                }
            }

            Integer isAll = requestParam.getIsAll() != null ? requestParam.getIsAll() : 1;
            String exportIds = requestParam.getExportIds();

            if (isAll == 1) {
                // 全量导出（支持筛选参数）
                exportService.exportAllTables(outputStream, validParams);
            } else {
                // 按ID筛选导出（必须提供exportIds）
                if (exportIds == null || exportIds.isEmpty()) {
                    //当isAll=0时，必须提供exportIds参数
                    return Result.fail("00403001");
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
            //"导出失败: " + e.getMessage()
            return Result.fail("00403002", e.getMessage());
        } catch (IllegalArgumentException e) {
            return Result.fail(400, e.getMessage());
        } catch (Exception e) {
            //"导出异常: " + e.getMessage()
            return Result.fail("00403003", e.getMessage());
        }
    }

    // 自定义请求参数对象（用于POST请求接收参数）
    public static class ExportRequestParam {
        private Integer isAll = 1; // 默认全量导出
        private String exportIds; // 当isAll=0时必填
        private Map<String, Object> queryParams; // 筛选参数（如standard_name、industry等）

        // getter和setter
        public Integer getIsAll() {
            return isAll;
        }

        public void setIsAll(Integer isAll) {
            this.isAll = isAll;
        }

        public String getExportIds() {
            return exportIds;
        }

        public void setExportIds(String exportIds) {
            this.exportIds = exportIds;
        }

        public Map<String, Object> getQueryParams() {
            return queryParams;
        }

        public void setQueryParams(Map<String, Object> queryParams) {
            this.queryParams = queryParams;
        }
    }
}