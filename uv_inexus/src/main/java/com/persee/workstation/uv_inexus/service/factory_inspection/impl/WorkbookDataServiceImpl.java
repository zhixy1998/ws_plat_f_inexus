package com.persee.workstation.uv_inexus.service.factory_inspection.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.persee.workstation.uv_inexus.dao.factory_inspection.WorkbookDataMapper;
import com.persee.workstation.uv_inexus.po.factory_inspection.WorkbookData;
import com.persee.workstation.uv_inexus.service.factory_inspection.WorkbookDataService;
import com.persee.workstation.uv_inexus.util.IdGeneratorUtil;
import org.apache.poi.ss.usermodel.Workbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;

/*
 * *
 *  * 版权所有 (C) 2025 北京普析通用仪器有限责任公司。保留所有权利。
 *  * 本软件受商业机密法和版权法保护。
 *  * 本代码仅限与北京普析通用仪器有限责任公司签订有效协议的授权用户内部使用。
 *  * 任何未经明确许可的复制、使用、修改、分发或公开均被严格禁止。
 *
 */
@Service
public class WorkbookDataServiceImpl extends ServiceImpl<WorkbookDataMapper, WorkbookData> implements WorkbookDataService {
    /**
     * 上传Excel文件并保存数据的方法
     *
     * @param workbook     Excel工作簿对象，包含要上传的Excel数据
     * @param templateCode 模板代码，用于标识上传的Excel模板类型
     * @return 上传成功返回true，失败返回false
     */
    public boolean uploadExcel(Workbook workbook, String templateCode) {
        // 创建WorkbookData对象用于存储Excel数据
        WorkbookData data = new WorkbookData();

        try {
            // 创建字节数组输出流，用于将Excel数据转换为字节数组
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            // 将workbook内容写入输出流
            workbook.write(outputStream);
            // 生成唯一ID并设置到data对象中
            data.setId(IdGeneratorUtil.generateId());
            // 将Excel数据设置为字节数组并保存到data对象
            data.setWorkbookData(outputStream.toByteArray());
            // 设置数据状态为激活
            data.setIsActive(true);
            // 设置创建时间为当前时间
            data.setCreateTime(LocalDateTime.now());
            // 设置最后访问时间为当前时间
            data.setLastAccessTime(LocalDateTime.now());
            // 设置更新时间为当前时间
            data.setUpdateTime(LocalDateTime.now());
            // 设置更新者为系统
            data.setUpdateBy("sys");
            data.setTaskId(templateCode);
            // 设置模板代码
            data.setTemplateCode(templateCode);
            // 保存数据并返回结果
            return this.save(data);
        } catch (IOException e) {
            // 记录错误日志
            log.error("上传Excel文件失败", e);
            // 返回false表示上传失败
            return false;
        }
    }
}
