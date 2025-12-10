package com.persee.workstation.uv_inexus.service.factory_inspection;

import com.persee.workstation.uv_inexus.model.Result;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface ExcelImportService {
    /**
     * 导入Excel文件
     * @param file Excel文件
     * @return 导入结果
     */
    Result importExcel(MultipartFile file);

}