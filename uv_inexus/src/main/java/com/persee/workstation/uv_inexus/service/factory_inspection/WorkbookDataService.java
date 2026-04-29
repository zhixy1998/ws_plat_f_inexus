package com.persee.workstation.uv_inexus.service.factory_inspection;

import com.baomidou.mybatisplus.extension.service.IService;
import com.persee.workstation.uv_inexus.po.factory_inspection.WorkbookData;
import org.apache.poi.ss.usermodel.Workbook;

import java.util.List;
import java.util.Map;

/*
 * *
 *  * 版权所有 (C) 2025 北京普析通用仪器有限责任公司。保留所有权利。
 *  * 本软件受商业机密法和版权法保护。
 *  * 本代码仅限与北京普析通用仪器有限责任公司签订有效协议的授权用户内部使用。
 *  * 任何未经明确许可的复制、使用、修改、分发或公开均被严格禁止。
 *
 */
public interface WorkbookDataService extends IService<WorkbookData> {
    /**
     * 上传Excel文件
     * @param workbook
     * @param templateCode
     * @return
     */
//     boolean uploadExcel(Workbook workbook, String templateCode);
//    public interface WorkbookDataService extends IService<WorkbookData> {
        /**
         * 上传Excel文件
         * @param workbook
         * @param templateCode
         * @return
         */
        boolean uploadExcel(Workbook workbook, String templateCode);

        /**
         * 上传Excel文件并指定黄色高亮列信息
         * @param workbook Excel工作簿
         * @param templateCode 模板编码
         * @param yellowColumnsMap 黄色高亮列信息 Map<工作表名, 列名列表>
         * @return 是否成功
         */
        boolean uploadExcel(Workbook workbook, String templateCode, Map<String, List<String>> yellowColumnsMap);
    }

