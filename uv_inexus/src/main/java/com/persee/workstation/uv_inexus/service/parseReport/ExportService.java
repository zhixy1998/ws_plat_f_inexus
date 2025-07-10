package com.persee.workstation.uv_inexus.service.parseReport;

import java.io.IOException;
import java.io.OutputStream;

public interface ExportService {
    void exportAllTables(OutputStream outputStream) throws IOException;

    // 新增：按条件导出（isAll + exportIds）
    void exportByCondition(OutputStream outputStream, Integer isAll, String exportIds) throws IOException;
}
