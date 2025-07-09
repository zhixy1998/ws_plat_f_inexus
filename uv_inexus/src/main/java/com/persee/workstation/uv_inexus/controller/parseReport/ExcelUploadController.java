package com.persee.workstation.uv_inexus.controller.parseReport;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Base64; // 新增 Base64 支持
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.poi.hssf.usermodel.HSSFClientAnchor;
import org.apache.poi.hssf.usermodel.HSSFPatriarch;
import org.apache.poi.hssf.usermodel.HSSFPicture;
import org.apache.poi.hssf.usermodel.HSSFPictureData;
import org.apache.poi.hssf.usermodel.HSSFShape;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFClientAnchor;
import org.apache.poi.xssf.usermodel.XSSFDrawing;
import org.apache.poi.xssf.usermodel.XSSFPicture;
import org.apache.poi.xssf.usermodel.XSSFPictureData;
import org.apache.poi.xssf.usermodel.XSSFShape;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/excel")
public class ExcelUploadController {

    @PostMapping("/upload")
    public Map<String, Object> uploadExcel(@RequestParam("file") MultipartFile file) {
        Map<String, Object> result = new HashMap<>();
        System.out.println("接收文件: " + file.getOriginalFilename());

        try {
            if (file.isEmpty()) {
                result.put("success", false);
                result.put("message", "请上传文件");
                return result;
            }

            String fileName = file.getOriginalFilename();
            if (!fileName.endsWith(".xlsx") && !fileName.endsWith(".xls")) {
                result.put("success", false);
                result.put("message", "请上传Excel文件（.xlsx 或 .xls）");
                return result;
            }

            try (InputStream inputStream = file.getInputStream();
                 Workbook workbook = getWorkbook(inputStream, fileName)) {
                
                result.put("success", true);
                result.put("fileName", fileName);
                result.put("sheetCount", workbook.getNumberOfSheets());
                List<Map<String, Object>> allImages = new ArrayList<>();

                for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
                    Sheet sheet = workbook.getSheetAt(i);
                    String sheetName = sheet.getSheetName();
                    int rowCount = sheet.getPhysicalNumberOfRows();
                    
                    System.out.println("\n工作表 " + (i + 1) + ": " + sheetName);
                    System.out.println("  行数: " + rowCount);
                    
                    if (rowCount > 0) {
                        Row firstRow = sheet.getRow(0);
                        int cellCount = firstRow.getPhysicalNumberOfCells();
                        System.out.println("  列数: " + cellCount);
                    }

                    // 提取当前工作表的图片（包含 Base64）
                    List<Map<String, Object>> sheetImages = extractImagesFromSheet(sheet);
                    allImages.addAll(sheetImages);
                    
                    System.out.println("  图片数量: " + sheetImages.size());
                    for (int imgIdx = 0; imgIdx < sheetImages.size(); imgIdx++) {
                        Map<String, Object> imgInfo = sheetImages.get(imgIdx);
                        System.out.println("    图片 " + (imgIdx + 1) + ": " + imgInfo.get("mimeType") + 
                                           ", 大小: " + imgInfo.get("sizeKB") + "KB, 位置: [" +
                                           imgInfo.get("startRow") + "," + imgInfo.get("startCol") + "]");
                        // 打印 Base64 前 100 个字符（避免控制台输出过长）
                        System.out.println("    Base64 预览: " + ((String) imgInfo.get("base64")).substring(0, 100) + "...");
                    }
                }
                
                result.put("totalImages", allImages.size());
                result.put("images", allImages);
                result.put("message", "Excel解析成功");
            } catch (Exception e) {
                result.put("success", false);
                result.put("message", "解析Excel失败: " + e.getMessage());
                e.printStackTrace();
            }

        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "处理请求失败: " + e.getMessage());
            e.printStackTrace();
        }
        return result;
    }

    // 根据文件格式创建Workbook
    private Workbook getWorkbook(InputStream inputStream, String fileName) throws IOException {
        if (fileName.endsWith(".xlsx")) {
            return new XSSFWorkbook(inputStream); // .xlsx格式
        } else if (fileName.endsWith(".xls")) {
            return new HSSFWorkbook(inputStream); // .xls格式
        } else {
            throw new IllegalArgumentException("不支持的文件格式: " + fileName);
        }
    }

    // 提取单元格值（保持不变）
    private String getCellValueAsString(Cell cell) {
        if (cell == null) return "null";
        
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return cell.getDateCellValue().toString();
                } else {
                    return String.valueOf(cell.getNumericCellValue());
                }
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                return cell.getCellFormula();
            default:
                return "unknown type";
        }
    }

    // 提取工作表中的悬浮图片（支持.xlsx和.xls，并添加Base64编码）
    private List<Map<String, Object>> extractImagesFromSheet(Sheet sheet) {
        List<Map<String, Object>> images = new ArrayList<>();

        // 处理 .xlsx 格式（XSSF）
        if (sheet instanceof XSSFSheet) {
            XSSFSheet xssfSheet = (XSSFSheet) sheet;
            XSSFDrawing drawing = xssfSheet.getDrawingPatriarch();
            if (drawing != null) {
                for (XSSFShape shape : drawing.getShapes()) {
                    if (shape instanceof XSSFPicture) {
                        XSSFPicture picture = (XSSFPicture) shape;
                        XSSFPictureData pictureData = picture.getPictureData();
                        
                        Map<String, Object> imgInfo = new HashMap<>();
                        imgInfo.put("type", "xlsx");
                        imgInfo.put("mimeType", pictureData.getMimeType());
                        imgInfo.put("sizeKB", pictureData.getData().length / 1024.0);
                        imgInfo.put("sheetName", xssfSheet.getSheetName());
                        
                        // 图片位置
                        XSSFClientAnchor anchor = picture.getClientAnchor();
                        imgInfo.put("startRow", anchor.getRow1());
                        imgInfo.put("startCol", anchor.getCol1());
                        imgInfo.put("endRow", anchor.getRow2());
                        imgInfo.put("endCol", anchor.getCol2());
                        
                        // 新增：转换为 Base64
                        String base64 = Base64.getEncoder().encodeToString(pictureData.getData());
                        imgInfo.put("base64", base64);
                        
                        images.add(imgInfo);
                    }
                }
            }
        }

        // 处理 .xls 格式（HSSF）
        else if (sheet instanceof HSSFSheet) {
            HSSFSheet hssfSheet = (HSSFSheet) sheet;
            HSSFPatriarch patriarch = hssfSheet.getDrawingPatriarch();
            if (patriarch != null) {
                for (HSSFShape shape : patriarch.getChildren()) {
                    if (shape instanceof HSSFPicture) {
                        HSSFPicture picture = (HSSFPicture) shape;
                        HSSFPictureData pictureData = picture.getPictureData();
                        
                        Map<String, Object> imgInfo = new HashMap<>();
                        imgInfo.put("type", "xls");
                        imgInfo.put("mimeType", pictureData.getMimeType());
                        imgInfo.put("sizeKB", pictureData.getData().length / 1024.0);
                        imgInfo.put("sheetName", hssfSheet.getSheetName());
                        
                        // 图片位置
                        HSSFClientAnchor anchor = picture.getClientAnchor();
                        imgInfo.put("startRow", anchor.getRow1());
                        imgInfo.put("startCol", anchor.getCol1());
                        imgInfo.put("endRow", anchor.getRow2());
                        imgInfo.put("endCol", anchor.getCol2());
                        
                        // 新增：转换为 Base64
                        String base64 = Base64.getEncoder().encodeToString(pictureData.getData());
                        imgInfo.put("base64", base64);
                        
                        images.add(imgInfo);
                    }
                }
            }
        }

        return images;
    }
}