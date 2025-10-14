/*
 * *****************************************************************************************************
 * Module name:    **.**
 *
 * Copyright (C) 2025 北京普析通用仪器有限责任公司 All Rights Reserved.
 *                此源代码仅限在公司内部使用。未经公司事先书面许可，任何单位和个人不得以任何方式复制、传播本代码的任何部分。
 * Description:
 *
 *
 * *************************************************************************************
 */

package com.persee.workstation.uv_inexus.excel;

import java.io.IOException;
import java.util.Base64;

import org.apache.poi.ss.usermodel.ClientAnchor;
import org.apache.poi.ss.usermodel.Drawing;
import org.apache.poi.ss.usermodel.Picture;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.util.Units;

public class ExcelImageExportUtils {

    // 调整后的尺寸（宽度放大2倍，高度适当降低）
    private static final int CELL_WIDTH_PX = 360;  // 宽度200像素（约26字符）
    private static final int CELL_HEIGHT_PX = 120; // 高度120像素（约90磅）
    private static final int CELL_PADDING_PX = 2;  // 边距2像素

    public static String insertImage(String base64Image, String id, Sheet sheet, int rowNum, int colNum) throws IOException {
        try {
            // 1. 强制设置单元格尺寸
            setCellSize(sheet, rowNum, colNum);

            // 2. 处理图片数据
            byte[] imageBytes = getValidImageBytes(base64Image, id);
            if (imageBytes == null) return null;

            // 3. 添加到工作簿
            int pictureIdx = sheet.getWorkbook().addPicture(
                imageBytes, 
                Workbook.PICTURE_TYPE_PNG
            );

            // 4. 创建绘图对象和锚点
            Drawing<?> drawing = sheet.createDrawingPatriarch();
            ClientAnchor anchor = createFitAnchor(drawing, colNum, rowNum);

            // 5. 插入并调整图片
            Picture picture = drawing.createPicture(anchor, pictureIdx);
            adjustImage(picture, sheet, colNum, rowNum);
            
            return "Image-" + id + "-" + rowNum + "-" + colNum;
        } catch (Exception e) {
            throw new IOException("图片插入失败: " + e.getMessage(), e);
        }
    }

    private static void setCellSize(Sheet sheet, int rowNum, int colNum) {
        // 设置列宽（200像素）
        sheet.setColumnWidth(colNum, (int)(CELL_WIDTH_PX / 7.5 * 256));
        
        // 设置行高（120像素）
        Row row = sheet.getRow(rowNum) != null ? sheet.getRow(rowNum) : sheet.createRow(rowNum);
        row.setHeight((short)(CELL_HEIGHT_PX * 20 / 1.33));
    }

    private static ClientAnchor createFitAnchor(Drawing<?> drawing, int colNum, int rowNum) {
        return drawing.createAnchor(
            Units.pixelToEMU(CELL_PADDING_PX),
            Units.pixelToEMU(CELL_PADDING_PX),
            Units.pixelToEMU(CELL_WIDTH_PX - CELL_PADDING_PX * 2),
            Units.pixelToEMU(CELL_HEIGHT_PX - CELL_PADDING_PX * 2),
            colNum, rowNum,
            colNum + 1, rowNum + 1
        );
    }

    private static void adjustImage(Picture picture, Sheet sheet, int colNum, int rowNum) {
        // 获取图片原始尺寸
        double imgWidth = picture.getImageDimension().getWidth();
        double imgHeight = picture.getImageDimension().getHeight();
        
        // 计算可用空间
        double availWidth = CELL_WIDTH_PX - CELL_PADDING_PX * 2;
        double availHeight = CELL_HEIGHT_PX - CELL_PADDING_PX * 2;
        
        // 计算缩放比例（保持宽高比）
        double scale = Math.min(availWidth / imgWidth, availHeight / imgHeight);
        picture.resize(scale);
    }

    private static byte[] getValidImageBytes(String base64Image, String id) {
        if (base64Image == null || base64Image.trim().isEmpty()) {
            System.err.println("错误: 空图片数据 ID: " + id);
            return null;
        }
        try {
            String imageData = base64Image.startsWith("data:image") ? 
                base64Image.substring(base64Image.indexOf(",") + 1) : 
                base64Image;
            return Base64.getDecoder().decode(imageData.trim());
        } catch (Exception e) {
            System.err.println("Base64解码失败 ID: " + id);
            return null;
        }
    }
}