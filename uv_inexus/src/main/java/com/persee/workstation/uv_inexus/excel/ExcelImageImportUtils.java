/*
 * *
 *  * 版权所有 (C) 2025 北京普析通用仪器有限责任公司。保留所有权利。
 *  * 本软件受商业机密法和版权法保护。
 *  * 本代码仅限与北京普析通用仪器有限责任公司签订有效协议的授权用户内部使用。
 *  * 任何未经明确许可的复制、使用、修改、分发或公开均被严格禁止。
 *
 */

package com.persee.workstation.uv_inexus.excel;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xssf.usermodel.XSSFClientAnchor;
import org.apache.poi.xssf.usermodel.XSSFPicture;
import org.apache.poi.xssf.usermodel.XSSFPictureData;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.springframework.web.multipart.MultipartFile;

/**
 * Excel图片提取工具类，支持悬浮图片和精确位置匹配
 */
public class ExcelImageImportUtils {

    /**
     * 从Excel中提取所有图片并按工作表分组
     * @param file Excel文件
     * @return 按工作表名称分组的图片信息列表
     */
    public static Map<String, List<ImageInfo>> extractAllImages(MultipartFile file) throws IOException {
        Map<String, List<ImageInfo>> sheetImages = new HashMap<>();

        try (InputStream is = file.getInputStream();
             Workbook workbook = WorkbookFactory.create(is)) {

            // 遍历所有工作表
            for (int sheetIdx = 0; sheetIdx < workbook.getNumberOfSheets(); sheetIdx++) {
                Sheet sheet = workbook.getSheetAt(sheetIdx);
                String sheetName = sheet.getSheetName();
                List<ImageInfo> images = new ArrayList<>();

                // 处理XLSX格式的图片（主要处理悬浮图片）
                if (sheet instanceof XSSFSheet) {
                    XSSFSheet xssfSheet = (XSSFSheet) sheet;
                    // 获取工作表中的绘图对象（包含悬浮图片）
                    if (xssfSheet.getDrawingPatriarch() != null) {
                        xssfSheet.getDrawingPatriarch().getShapes().forEach(shape -> {
                            if (shape instanceof XSSFPicture) {
                                XSSFPicture picture = (XSSFPicture) shape;
                                XSSFClientAnchor anchor = picture.getClientAnchor();
                                
                                // 创建图片信息对象
                                ImageInfo info = new ImageInfo();
                                info.setSheetName(sheetName);
                                info.setRow(anchor.getRow1());  // 图片起始行
                                info.setCol(anchor.getCol1());  // 图片起始列
                                info.setPictureData(picture.getPictureData());
                                info.setBase64(encodeToBase64(picture.getPictureData()));
                                info.setMimeType(picture.getPictureData().getMimeType());
                                info.setSize(picture.getPictureData().getData().length);
                                
                                images.add(info);
                            }
                        });
                    }
                }

                sheetImages.put(sheetName, images);
            }
        }
        return sheetImages;
    }

    /**
     * 根据Excel行号查找匹配的图片
     * @param images 图片列表
     * @param excelRowNum Excel中的行号（从1开始）
     * @return 匹配的图片Base64字符串，未找到则返回null
     */
    public static String findImageByRow(List<ImageInfo> images, int excelRowNum) {
        if (images == null || images.isEmpty()) {
            return null;
        }
        
        System.out.println("尝试为Excel行号 " + excelRowNum + " 匹配图片...");
        
        // 1. 优先按精确行号和合理列范围匹配（假设图片在A-D列，即0-3）
        for (ImageInfo img : images) {
            if (img.getRow() == excelRowNum && img.getCol() >= 0 && img.getCol() <= 3) {
                System.out.println("精确匹配成功: 行=" + img.getRow() + ", 列=" + img.getCol() + 
                                   ", 大小=" + img.getSize() + "字节");
                return img.getBase64();
            }
        }
        
        // 2. 按精确行号匹配（不限制列）
        for (ImageInfo img : images) {
            if (img.getRow() == excelRowNum) {
                System.out.println("精确行号匹配成功（列超出范围）: 行=" + img.getRow() + 
                                   ", 列=" + img.getCol() + ", 大小=" + img.getSize() + "字节");
                return img.getBase64();
            }
        }
        
        // 3. 容错：匹配相邻行（±2范围内）且列在合理范围
        for (ImageInfo img : images) {
            int rowDiff = Math.abs(img.getRow() - excelRowNum);
            if (rowDiff <= 2 && img.getCol() >= 0 && img.getCol() <= 3) {
                System.out.println("容错匹配成功: 目标行=" + excelRowNum + 
                                   ", 匹配行=" + img.getRow() + ", 列=" + img.getCol() + 
                                   ", 行差异=" + rowDiff + ", 大小=" + img.getSize() + "字节");
                return img.getBase64();
            }
        }
        
        // 4. 宽松容错：匹配相邻行（±2范围内）不限制列
        for (ImageInfo img : images) {
            int rowDiff = Math.abs(img.getRow() - excelRowNum);
            if (rowDiff <= 2) {
                System.out.println("宽松容错匹配成功: 目标行=" + excelRowNum + 
                                   ", 匹配行=" + img.getRow() + ", 列=" + img.getCol() + 
                                   ", 行差异=" + rowDiff + ", 大小=" + img.getSize() + "字节");
                return img.getBase64();
            }
        }
        
        System.out.println("未找到匹配图片: 行=" + excelRowNum);
        return null;
    }

    /**
     * 将图片数据编码为Base64字符串
     */
    private static String encodeToBase64(XSSFPictureData pictureData) {
        byte[] data = pictureData.getData();
        return "data:" + pictureData.getMimeType() + ";base64," + 
               Base64.getEncoder().encodeToString(data);
    }

    /**
     * 图片信息封装类
     */
    public static class ImageInfo {
        private String sheetName;
        private int row;
        private int col;
        private XSSFPictureData pictureData;
        private String base64;
        private String mimeType;
        private int size;

        // Getters and Setters
        public String getSheetName() { return sheetName; }
        public void setSheetName(String sheetName) { this.sheetName = sheetName; }
        public int getRow() { return row; }
        public void setRow(int row) { this.row = row; }
        public int getCol() { return col; }
        public void setCol(int col) { this.col = col; }
        public XSSFPictureData getPictureData() { return pictureData; }
        public void setPictureData(XSSFPictureData pictureData) { this.pictureData = pictureData; }
        public String getBase64() { return base64; }
        public void setBase64(String base64) { this.base64 = base64; }
        public String getMimeType() { return mimeType; }
        public void setMimeType(String mimeType) { this.mimeType = mimeType; }
        public int getSize() { return size; }
        public void setSize(int size) { this.size = size; }
    }
}