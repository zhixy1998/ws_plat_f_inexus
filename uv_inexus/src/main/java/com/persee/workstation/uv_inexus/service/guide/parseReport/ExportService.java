/*
 * *
 *  * 版权所有 (C) 2025 北京普析通用仪器有限责任公司。保留所有权利。
 *  * 本软件受商业机密法和版权法保护。
 *  * 本代码仅限与北京普析通用仪器有限责任公司签订有效协议的授权用户内部使用。
 *  * 任何未经明确许可的复制、使用、修改、分发或公开均被严格禁止。
 *
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