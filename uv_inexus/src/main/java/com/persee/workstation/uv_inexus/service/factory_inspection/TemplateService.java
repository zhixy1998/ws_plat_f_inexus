package com.persee.workstation.uv_inexus.service.factory_inspection;

import com.baomidou.mybatisplus.extension.service.IService;
import com.persee.workstation.uv_inexus.dto.factory_inspection.TemplateCreationRequest;
import com.persee.workstation.uv_inexus.dto.factory_inspection.TemplateQeqDto;
import com.persee.workstation.uv_inexus.dto.factory_inspection.TemplateUpdateRequest;
import com.persee.workstation.uv_inexus.model.Result;
import com.persee.workstation.uv_inexus.po.factory_inspection.ExcelTemplate;

import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;

public interface TemplateService extends IService<ExcelTemplate> {
    Result<List<String>> getHeaders(String templateCode);

    Result createTemplate(TemplateCreationRequest request);

    Result updateTemplate(TemplateUpdateRequest request);
    Result editExcel(MultipartFile file, Map<String, Set<String>> sheetRetainRows) throws IOException;
    Result buildResultTemplate(String templateCode);


    /**
     * 导出Excel文件
     * @param templateCode 模板编码
     * @param response HTTP响应
     * @return 导出结果
     */
    Result exportExcel(String templateCode, HttpServletResponse response);

    /**
     * 获取结构化数据
     * @param templateCode 模板编码
     * @return 结构化数据列表
     */
    Result<List<Map<String, Object>>> getStructuredData(String templateCode);

    /**
     * 根据字段分组查询数据
     * @param templateCode 模板编码
     * @param groupByField 分组字段
     * @return 分组数据
     */
    Result<Map<String, List<Map<String, Object>>>> getGroupedData(String templateCode, String groupByField);

    Result<List<Map<String, Object>>> getCustomStructuredData(String templateCode, List<String> selectedFields);

    Result getTemplateList(TemplateQeqDto templateQeqDto);
}
