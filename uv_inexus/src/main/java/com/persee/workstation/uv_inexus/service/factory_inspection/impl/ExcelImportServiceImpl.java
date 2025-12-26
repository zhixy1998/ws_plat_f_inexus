package com.persee.workstation.uv_inexus.service.factory_inspection.impl;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.ExcelReader;
import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.read.listener.ReadListener;
import com.alibaba.excel.read.metadata.ReadSheet;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.baomidou.dynamic.datasource.annotation.DS;
import com.persee.workstation.uv_inexus.dao.factory_inspection.TaskExecutionDetailMapper;
import com.persee.workstation.uv_inexus.model.Result;
import com.persee.workstation.uv_inexus.po.factory_inspection.CalStdCats;
import com.persee.workstation.uv_inexus.po.factory_inspection.ExcelDataAsset;
import com.persee.workstation.uv_inexus.po.factory_inspection.ExcelTemplate;
import com.persee.workstation.uv_inexus.po.factory_inspection.TaskExecutionDetail;
import com.persee.workstation.uv_inexus.service.factory_inspection.CalStdCatsService;
import com.persee.workstation.uv_inexus.service.factory_inspection.ExcelDataAssetService;
import com.persee.workstation.uv_inexus.service.factory_inspection.ExcelImportService;
import com.persee.workstation.uv_inexus.service.factory_inspection.TemplateService;
import com.persee.workstation.uv_inexus.util.IdGeneratorUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;

import static com.persee.workstation.uv_inexus.enums.factoryInspection.CalibrationTypeEnum.*;

@Slf4j
@DS("factory_inspection")
@Service
public class ExcelImportServiceImpl implements ExcelImportService {

    @Autowired
    private TemplateService templateService;

    @Autowired
    private ExcelDataAssetService excelDataAssetService;
    @Autowired
    private CalStdCatsService calStdCatsService;
    @Autowired
    private TaskExecutionDetailMapper taskExecutionDetailMapper;
    @Value("${excel.save.path}")
    private String excelSavePath;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result importExcel(MultipartFile file) {
        try {
//            /*构建要在根目录创建的文件名，路径*/
//            String projectRoot = System.getProperty("user.dir");
//            // 生成唯一文件名并保存
//            String originalFilename = file.getOriginalFilename();
//            String fileExtension = originalFilename != null && originalFilename.contains(".") ?
//                    originalFilename.substring(originalFilename.lastIndexOf(".")) : "";
//            String uniqueFilename = IdGeneratorUtil.generateIdWithPrefix("IE") + fileExtension;
//            String fullPath = projectRoot+"/"+excelSavePath + uniqueFilename;
//

            /*保存模板信息*/
            // 生成模板编码
            String templateCode = "TEMP_" + System.currentTimeMillis();
            String sheeetName = "检测项信息";
            // 读取Excel文件，获取表头信息
            List<List<String>> headList = new ArrayList<>();
            try (InputStream inputStream = file.getInputStream()) {
                ExcelReader excelReader = EasyExcel.read(inputStream).build();
                ReadSheet readSheet = EasyExcel.readSheet(sheeetName).headRowNumber(0).registerReadListener(new HeadReadListener(headList)).build();
                excelReader.read(readSheet);
            }

            if (headList.isEmpty() || headList.get(0).isEmpty()) {
                return Result.fail("Excel文件没有表头信息");
            }

            // 创建模板
            ExcelTemplate template = new ExcelTemplate();
            template.setId(IdGeneratorUtil.generateIdWithPrefix("template"));
            template.setTemplateCode(templateCode);
            //设置文件名
            String originalFilename = file.getOriginalFilename();
            String filenameWithoutExtension = originalFilename;
            if (originalFilename != null && originalFilename.lastIndexOf(".") != -1) {
                filenameWithoutExtension = originalFilename.substring(0, originalFilename.lastIndexOf("."));
            }
            template.setTemplateName(filenameWithoutExtension);
            template.setStatus(1); // 启用状态
            // 构建列配置
            List<ExcelTemplate.ColumnConfig> columnConfigs = new ArrayList<>();
            List<String> headers = headList.get(0);
            for (int i = 0; i < headers.size(); i++) {
                ExcelTemplate.ColumnConfig config = new ExcelTemplate().new ColumnConfig();
                config.setOrder(i);
                config.setExcelHeader(headers.get(i));
                config.setDataType("STRING");
                columnConfigs.add(config);
            }
            columnConfigs.sort(Comparator.comparingInt(ExcelTemplate.ColumnConfig::getOrder));
            template.setColumnConfig(JSON.toJSONString(columnConfigs));

            // 保存模板
            templateService.save(template);

            /*保存数据信息*/
            List<LinkedHashMap<String, Object>> dataList = new ArrayList<>();
            try (InputStream inputStream = file.getInputStream()) {
                ExcelReader excelReader = EasyExcel.read(inputStream).build();
                ReadSheet readSheet = EasyExcel.readSheet(sheeetName).headRowNumber(1).registerReadListener(new DataReadListener(dataList, headers)).build();
                excelReader.read(readSheet);
            }


            // 新增标准物质种类
            Set<String> standardMaterials = new HashSet<>();
            Map<String, Map<String, Object>> assetStandardsMap = new HashMap<>();
            //标准物质的特征值和标准值
            Map<String, Map<String, Object>> standardMaterialsMap = new HashMap<>();
            // 保存数据资产
            int rowIndex = 1; // 从第1行开始计数（跳过表头）
            ArrayList<ExcelDataAsset> list = new ArrayList<>();
            for (LinkedHashMap<String, Object> dataMap : dataList) {
                String s = JSON.toJSONString(dataMap);
                ExcelDataAsset asset = new ExcelDataAsset();
                asset.setId(IdGeneratorUtil.generateIdWithPrefix("asset"));
                asset.setTemplateCode(templateCode);
                asset.setBatchNo(String.valueOf(rowIndex++));
                asset.setStructuredData(s);
                String type = PHOTOMETRIC_SCAN.getCode();
                if (s.contains("WlRange")) {
                    type = SPECTRAL_SCAN.getCode();
                }
                if (s.contains("ResponseTime")) {
                    type = TIME_SCAN.getCode();
                }
                asset.setImportStatus(type);
                list.add(asset);

                String zeroMaterial = MapUtils.getString(dataMap, ZERO.getDescription());
                String measurementMaterial = MapUtils.getString(dataMap, MEASUREMENT.getDescription());
                String stdValue = MapUtils.getString(dataMap, STANDARD.getDescription());
                String characteristicValue = MapUtils.getString(dataMap, CHARACTERISTIC.getDescription());

                standardMaterialsMap.put(measurementMaterial, new HashMap<String, Object>() {{
                    put("s", stdValue);
                    put("c", characteristicValue);
                }});
                standardMaterials.add(zeroMaterial);
                standardMaterials.add(measurementMaterial);

                Map<String, Object> standards = new HashMap<>();
                standards.put(ZERO.getDescription(), zeroMaterial);
                standards.put(MEASUREMENT.getDescription(), measurementMaterial);
                assetStandardsMap.put(asset.getId(), standards);
            }
            // 创建 Map
            Map<String, ExcelDataAsset> assetMap = list.stream()
                    .collect(Collectors.toMap(ExcelDataAsset::getId, item -> item));

            // 批量保存标准物质种类
            List<CalStdCats> standardCategories = standardMaterials.stream()
                    .filter(Objects::nonNull)
                    .map(material -> {
                        CalStdCats cat = new CalStdCats();
                        if (standardMaterialsMap.containsKey(material) && !BLACK_BLOCK.getDescription().equals(material) && !AIR.getDescription().equals(material)) {
                            Map<String, Object> map = standardMaterialsMap.get(material);
                            cat.setStandardValue(MapUtils.getString(map, "s"));
                            cat.setCharacteristicValue(MapUtils.getString(map, "c"));
                        }
                        cat.setId(IdGeneratorUtil.generateIdWithPrefix("cal"));
                        cat.setName(material);
                        cat.setRemark("导入的默认标准物质种类");
                        return cat;
                    })
                    .collect(Collectors.toList());

            Map<String, CalStdCats> standardCategoryMap = standardCategories.stream()
                    .collect(Collectors.toMap(CalStdCats::getId, item -> item));


            if (CollectionUtils.isNotEmpty(standardCategories)) {
                calStdCatsService.saveBatch(standardCategories);
            }

            // 建立标准物质名称到ID的映射，减少数据库查询次数
            Map<String, String> materialNameToIdMap = standardCategories.stream()
                    .collect(Collectors.toMap(CalStdCats::getName, CalStdCats::getId));

            // 保存任务执行详情
            List<TaskExecutionDetail> taskDetails = new ArrayList<>();
            for (Map.Entry<String, Map<String, Object>> entry : assetStandardsMap.entrySet()) {
                String assetId = entry.getKey();
                Map<String, Object> standards = entry.getValue();


                // 校零物质任务详情
                String zeroMaterial = MapUtils.getString(standards, ZERO.getDescription());
                if (zeroMaterial != null) {
                    TaskExecutionDetail zeroDetail = new TaskExecutionDetail();
                    zeroDetail.setId(IdGeneratorUtil.generateId());
                    zeroDetail.setItemId(assetId);
                    zeroDetail.setStandardId(materialNameToIdMap.get(zeroMaterial));
                    zeroDetail.setStandardType(ZERO.getCode());
                    taskDetails.add(zeroDetail);

                    ExcelDataAsset excelDataAsset = assetMap.get(assetId);
                    Map<String, Object> dataMap = excelDataAsset.getDataMap();
                    String z = MapUtils.getString(dataMap, ZERO.getDescription());
                    JSONObject js = new JSONObject();
                    CalStdCats cats = standardCategoryMap.get(zeroDetail.getStandardId());
                    js.put("id", zeroDetail.getStandardId());
                    js.put("name", z);
                    js.put("characteristicValue", cats.getCharacteristicValue());
                    js.put("standardValue", cats.getStandardValue());
                    dataMap.put(ZERO.getDescription(),js);
                    excelDataAsset.setStructuredData(JSON.toJSONString(dataMap));
                    assetMap.put(assetId, excelDataAsset);
                }

                // 测量物质任务详情
                String measurementMaterial = MapUtils.getString(standards, MEASUREMENT.getDescription());
                if (measurementMaterial != null) {
                    TaskExecutionDetail measurementDetail = new TaskExecutionDetail();
                    measurementDetail.setId(IdGeneratorUtil.generateId());
                    measurementDetail.setItemId(assetId);
                    measurementDetail.setStandardId(materialNameToIdMap.get(measurementMaterial));
                    measurementDetail.setStandardType(MEASUREMENT.getCode());
                    taskDetails.add(measurementDetail);

                    ExcelDataAsset excelDataAsset = assetMap.get(assetId);
                    Map<String, Object> dataMap = excelDataAsset.getDataMap();
                    String z = MapUtils.getString(dataMap, MEASUREMENT.getDescription());
                    JSONObject js = new JSONObject();
                    CalStdCats cats = standardCategoryMap.get(measurementDetail.getStandardId());
                    js.put("id", measurementDetail.getStandardId());
                    js.put("name", z);
                    js.put("characteristicValue", cats.getCharacteristicValue());
                    js.put("standardValue", cats.getStandardValue());
                    dataMap.put(MEASUREMENT.getDescription(),js);
                    excelDataAsset.setStructuredData(JSON.toJSONString(dataMap));
                    assetMap.put(assetId, excelDataAsset);

                }
            }
            List<ExcelDataAsset> collect = new ArrayList<>(assetMap.values());
            excelDataAssetService.saveBatch(collect);

            // 批量插入任务执行详情
            if (CollectionUtils.isNotEmpty(taskDetails)) {
                taskDetails.forEach(taskExecutionDetailMapper::insert);
            }
//            // 保存文件
//            File destFile = new File(fullPath);
//            file.transferTo(destFile);


            Map<String, Object> result = new HashMap<>();
            result.put("templateCode", templateCode);
            result.put("dataCount", dataList.size());

            return Result.ok(result);
        } catch (Exception e) {
            log.error("导入Excel失败", e);
            return Result.fail("导入失败: " + e.getMessage());
        }
    }

    /**
     * 表头读取监听器
     */
    private static class HeadReadListener implements ReadListener<Map<Integer, String>> {
        private final List<List<String>> headList;

        public HeadReadListener(List<List<String>> headList) {
            this.headList = headList;
        }

        @Override
        public void invoke(Map<Integer, String> data, AnalysisContext context) {
            List<String> headers = new ArrayList<>();
            Set<String> seenHeaders = new HashSet<>();
            for (int i = 0; i < data.size(); i++) {
                String header = data.get(i);
                // 如果表头已存在，添加序号区分
                if (seenHeaders.contains(header)) {
                    int suffix = 1;
                    String newHeader = header + "_" + suffix;
                    while (seenHeaders.contains(newHeader)) {
                        suffix++;
                        newHeader = header + "_" + suffix;
                    }
                    header = newHeader;
                }
                headers.add(header);
                seenHeaders.add(header);
            }
            headList.add(headers);
        }


        @Override
        public void doAfterAllAnalysed(AnalysisContext context) {
            // 处理完成
        }
    }

    /**
     * 数据读取监听器
     */
    private static class DataReadListener implements ReadListener<Map<Integer, String>> {
        private final List<LinkedHashMap<String, Object>> dataList;
        private final List<String> headers;

        public DataReadListener(List<LinkedHashMap<String, Object>> dataList, List<String> headers) {
            this.dataList = dataList;
            this.headers = headers;
        }

        @Override
        public void invoke(Map<Integer, String> data, AnalysisContext context) {
            LinkedHashMap<String, Object> rowData = new LinkedHashMap<>();
            for (int i = 0; i < headers.size(); i++) {
                rowData.put(headers.get(i), data.get(i));
            }
            dataList.add(rowData);
        }

        @Override
        public void doAfterAllAnalysed(AnalysisContext context) {
            // 处理完成
        }
    }
}