package com.persee.workstation.uv_inexus.service.parseReport;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;

public interface ExportService {
    // 全量导出（新增查询参数支持）
    void exportAllTables(OutputStream outputStream, Map<String, Object> queryParams) throws IOException;

    // 按条件导出（支持ID筛选+查询参数）
    void exportByCondition(OutputStream outputStream, Integer isAll, String exportIds, Map<String, Object> queryParams) throws IOException;
}