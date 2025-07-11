package com.persee.workstation.uv_inexus.excel;

import java.io.InputStream;
import java.util.Base64;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.poi.openxml4j.opc.OPCPackage;
import org.apache.poi.openxml4j.opc.PackagePart;
import org.apache.poi.xssf.usermodel.XSSFPictureData;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.web.multipart.MultipartFile;

public class ExcelImageImportUtils {

    /**
     * 基于行号的图片提取方法
     */
    public static String extractImageToBase64(MultipartFile file, String imageRef, int rowNum) {
        try (InputStream is = file.getInputStream();
             OPCPackage pkg = OPCPackage.open(is);
             XSSFWorkbook workbook = new XSSFWorkbook(pkg)) {
            
            // 1. 获取所有图片
            XSSFPictureData[] pictures = workbook.getAllPictures().toArray(new XSSFPictureData[0]);
            System.out.println("发现图片数量: " + pictures.length);
            System.out.println("发现图片-=======: " + pictures);
            
            // 2. 根据行号获取图片（假设图片顺序与行号对应）
            // 注意：这里需要根据实际情况调整行号偏移量
            int imageIndex = rowNum; // 假设数据从第2行开始
            
            if (imageIndex >= 0 && imageIndex < pictures.length) {
                System.out.println("使用图片索引: " + imageIndex + " 对应行号: " + rowNum);
                System.out.println("使用图片为: " + imageIndex + " 对应行号: " + pictures[imageIndex]);
                return encodeToBase64(pictures[imageIndex]);
            }
            
            // 3. 如果行号匹配失败，尝试按ID匹配
            String imageId = parseImageId(imageRef);
            System.out.println("尝试ID匹配: " + imageId + "传过来的id：" + imageRef);
            // 精确匹配
            for (XSSFPictureData pic : pictures) {
                try {
                    PackagePart part = pic.getPackagePart();
                    String partName = part.getPartName().getName();
                    System.out.println("part: " + part);
                    System.out.println("partName: " + partName);
                    
                    // 更精确的匹配逻辑
                    if (partName.endsWith("/" + imageId) || partName.equals(imageId)) {
                        System.out.println("精确匹配成功: " + partName);
                        return encodeToBase64(pic);
                    }
                } catch (Exception e) {
                    System.out.println("图片处理异常: " + e.getMessage());
                    e.printStackTrace(); // 打印完整堆栈
                }
            }
            // 4. 如果都失败，返回第一张图片或null
            if (pictures.length > 0) {
                System.out.println("使用第一张图片作为备用");
                return encodeToBase64(pictures[0]);
            }
            
            return null;
        } catch (Exception e) {
            System.out.println("图片提取失败: " + e.getMessage());
            return null;
        }
    }

    private static String encodeToBase64(XSSFPictureData picture) {
        byte[] data = picture.getData();
        String mimeType = picture.getMimeType();
        return "data:" + mimeType + ";base64," + 
               Base64.getEncoder().encodeToString(data);
    }

    private static String parseImageId(String imageRef) {
        // 提取DISPIMG("ID_XXXX",1)中的ID部分
        Pattern pattern = Pattern.compile("\"([^\"]+)\"");
        Matcher matcher = pattern.matcher(imageRef);
        return matcher.find() ? matcher.group(1) : "unknown";
    }
}