package com.persee.workstation.uv_inexus.service.parseReport.impl;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.ExcelWriter;
import com.alibaba.excel.write.metadata.WriteSheet;
import com.persee.workstation.uv_inexus.dao.parseReport.*;
import com.persee.workstation.uv_inexus.excel.*;
import com.persee.workstation.uv_inexus.po.parseReport.*;
import com.persee.workstation.uv_inexus.service.parseReport.ExportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

@Service
public class ExportServiceImpl implements ExportService {

    @Autowired
    private CalculationFormulaMapper calculationFormulaMapper;

    @Autowired
    private QualityControlMapper qualityControlMapper;

    @Autowired
    private RoundingRulesMapper roundingRulesMapper;

    @Autowired
    private DetectionMapper standardDetectionMapper;

    @Autowired
    private StandardPretreatmentMapper standardPretreatmentMapper;

    @Override
    public void exportAllTables(OutputStream outputStream) throws IOException {
        // 查询所有表的数据
        List<CalculationFormula> calculationFormulas = calculationFormulaMapper.selectAll();
        List<QualityControl> qualityControls = qualityControlMapper.selectAll();
        List<RoundingRules> roundingRules = roundingRulesMapper.selectAll();
        List<Detection> standardDetections = standardDetectionMapper.selectAll();
        List<StandardPretreatment> standardPretreatments = standardPretreatmentMapper.selectAll();

        // 创建 Excel 写入器
        ExcelWriter excelWriter = EasyExcel.write(outputStream).build();

        // 写入第一个 Sheet
        WriteSheet writeSheet1 = EasyExcel.writerSheet("标准信息表")
                .head(StandardDetectionExcelModel.class) // 假设你有一个对应的 Excel 模型类
                .build();
        excelWriter.write(standardDetections, writeSheet1);

        // 写入第二个 Sheet
        WriteSheet writeSheet2 = EasyExcel.writerSheet("前处理信息表")
                .head(StandardPretreatmentExcelModel.class) // 假设你有一个对应的 Excel 模型类
                .build();
        excelWriter.write(standardPretreatments, writeSheet2);

        // 写入第三个 Sheet
        WriteSheet writeSheet3 = EasyExcel.writerSheet("计算公式表")
                .head(CalculationFormulaExcelModel.class) // 假设你有一个对应的 Excel 模型类
                .build();
        excelWriter.write(calculationFormulas, writeSheet3);

        // 写入第四个 Sheet
        WriteSheet writeSheet4 = EasyExcel.writerSheet("质量控制表")
                .head(QualityControlExcelModel.class) // 假设你有一个对应的 Excel 模型类
                .build();
        excelWriter.write(qualityControls, writeSheet4);

        // 写入第五个 Sheet
        WriteSheet writeSheet5 = EasyExcel.writerSheet("修约表")
                .head(RoundingRulesExcelModel.class) // 假设你有一个对应的 Excel 模型类
                .build();
        excelWriter.write(roundingRules, writeSheet5);

        // 关闭 Excel 写入器
        excelWriter.finish();
    }
}
