/*
 * *
 *  * 版权所有 (C) 2025 北京普析通用仪器有限责任公司。保留所有权利。
 *  * 本软件受商业机密法和版权法保护。
 *  * 本代码仅限与北京普析通用仪器有限责任公司签订有效协议的授权用户内部使用。
 *  * 任何未经明确许可的复制、使用、修改、分发或公开均被严格禁止。
 *
 */

package com.persee.workstation.uv_inexus.excel;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

public class ImageToBase64 {
    public static String convertImageToBase64(String imagePath) throws IOException {
        File file = new File(imagePath);
        byte[] imageBytes = Files.readAllBytes(file.toPath());
        return java.util.Base64.getEncoder().encodeToString(imageBytes);
    }
}

// import org.apache.poi.poifs.filesystem.POIFSFileSystem;
// import org.apache.poi.xssf.usermodel.XSSFWorkbook;
// import org.apache.poi.xssf.usermodel.XSSFPictureData;

// import java.io.ByteArrayOutputStream;
// import java.io.IOException;
// import java.io.InputStream;
// import java.util.List;

// public class ImageToBase64 {

//     public static String convertImageToBase64(String excelFilePath) throws IOException {
//         try (InputStream is = ImageToBase64.class.getResourceAsStream(excelFilePath);
//              POIFSFileSystem fs = new POIFSFileSystem(is);
//              XSSFWorkbook workbook = new XSSFWorkbook(fs)) {

//             List<XSSFPictureData> pictures = workbook.getAllPictures();
//             if (pictures != null && !pictures.isEmpty()) {
//                 byte[] pictureData = pictures.get(0).getData();
//                 return java.util.Base64.getEncoder().encodeToString(pictureData);
//             }
//         }
//         return null;
//     }
// }
