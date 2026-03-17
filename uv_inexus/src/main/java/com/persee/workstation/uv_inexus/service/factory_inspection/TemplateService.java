package com.persee.workstation.uv_inexus.service.factory_inspection;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.persee.workstation.uv_inexus.dto.factory_inspection.TemplateCreationRequest;
import com.persee.workstation.uv_inexus.dto.factory_inspection.TemplateQeqDto;
import com.persee.workstation.uv_inexus.dto.factory_inspection.TemplateResponseDto;
import com.persee.workstation.uv_inexus.dto.factory_inspection.TemplateUpdateRequest;
import com.persee.workstation.uv_inexus.model.Result;
import com.persee.workstation.uv_inexus.po.factory_inspection.ExcelTemplate;
import com.persee.workstation.uv_inexus.util.ExcelCellEditor;
import com.persee.workstation.uv_inexus.util.ExcelCellLocator;
import org.apache.poi.ss.usermodel.Sheet;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;

public interface TemplateService extends IService<ExcelTemplate> {
    /**
     * 获取模板头
     * @param templateCode
     * @return
     */
    Result<List<String>> getHeaders(String templateCode);

    /**
     * 创建模板
     * @param request
     * @return
     */
    Result createTemplate(TemplateCreationRequest request);

    /**
     * 修改模板
     * @param request
     * @return
     */
    Result updateTemplate(TemplateUpdateRequest request);

    /**
     * 编辑模板
     * @param file
     * @param sheetRetainRows
     * @return
     * @throws IOException
     */
    Result editExcel(MultipartFile file, Map<String, Set<String>> sheetRetainRows) throws IOException;

    /**
     * 构建结果模板
     * @param templateCode
     * @param rowsNotInTemplate
     * @return
     */
    Result buildResultTemplate(String templateCode,List<Map<String, Object>> rowsNotInTemplate);

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
     *
     * @param templateCode 模板编码
     * @param groupByField 分组字段
     * @param type
     * @return 分组数据
     */
    Result<Map<String, List<Map<String, Object>>>> getGroupedData(String templateCode, String groupByField,String checkCategory);
    Result<Map<String, List<Map<String, Object>>>> getGroupedData(String templateCode);

    Result<List<Map<String, Object>>> getCustomStructuredData(String templateCode, List<String> selectedFields);

    Result<Page<TemplateResponseDto>> getTemplateList(TemplateQeqDto templateQeqDto);
    //写入多个，用,分开   写入一个或者多个cell全写入成一个值
    void updateCellValues(Sheet sheet, ExcelCellLocator excelCellLocator,
                          ExcelCellEditor excelCellEditor, String valueType,
                          String projectName, String cellValue);

    /**
     * 修改单元格值一个
     * @param sheet
     * @param excelCellLocator
     * @param excelCellEditor
     * @param valueType
     * @param projectName
     * @param positionIndex
     * @param cellValue
     */
    void updateCellValue(Sheet sheet, ExcelCellLocator excelCellLocator,
                         ExcelCellEditor excelCellEditor, String valueType,
                         String projectName, int positionIndex, String cellValue);

    /**
     * 修改单元格值一个
     * @param sheet
     * @param excelCellLocator
     * @param excelCellEditor
     * @param valueType
     * @param projectName
     * @param featureValue
     * @param cellValue
     * @param positionIndex
     */
    void updateCellValue(Sheet sheet, ExcelCellLocator excelCellLocator,
                         ExcelCellEditor excelCellEditor, String valueType,
                         String projectName, String featureValue, String cellValue,int positionIndex);

    /**
     * 删除模板
     * @param templateCode
     * @return
     */
    Result<String> deleteTemplate(String templateCode);

    /**
     * 批量删除模板
     * @param list
     * @return
     */
    Result<String> batchDeleteTemplate(List<String> list);

    /**
     * 获取物料选项
     * @param id
     * @return
     */
    Result<List<Map<String, String>>> getMaterialOptions(String id);
}
