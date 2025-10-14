/*
 * *****************************************************************************************************
 * Module name:    **.**
 *
 * Copyright (C) 2025 北京普析通用仪器有限责任公司 All Rights Reserved.
 *                此源代码仅限在公司内部使用。未经公司事先书面许可，任何单位和个人不得以任何方式复制、传播本代码的任何部分。
 * Description:
 *
 *
 * *************************************************************************************
 */

package com.persee.workstation.uv_inexus.service.guide.parseReport;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;

public interface ExportService {
    // 全量导出（新增查询参数支持）
    void exportAllTables(OutputStream outputStream, Map<String, Object> queryParams) throws IOException;

    // 按条件导出（支持ID筛选+查询参数）
    void exportByCondition(OutputStream outputStream, Integer isAll, String exportIds, Map<String, Object> queryParams) throws IOException;
}