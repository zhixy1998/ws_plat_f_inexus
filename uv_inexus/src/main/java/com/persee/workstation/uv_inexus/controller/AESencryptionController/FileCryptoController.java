package com.persee.workstation.uv_inexus.controller.AESencryptionController;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.persee.workstation.uv_inexus.controller.parseReport.ImportController;
import com.persee.workstation.uv_inexus.model.Result;
import com.persee.workstation.uv_inexus.utils.AESencryption.AESEncryptionUtil;
import com.persee.workstation.uv_inexus.utils.AESencryption.ZipFileUtil;

import io.swagger.annotations.ApiOperation;

@RestController
@RequestMapping("/api/crypto")
public class FileCryptoController {
    public static final String FIXED_KEY = "pFFKQ9huIh0taRfethpTv6DLXYJPJw1YGZtrEoSHJp0=";
    public static final String EXCEL_EXTENSION = ".xlsx";
    public static final String TXT_EXTENSION = ".txt";

    @Autowired
    private ImportController importController;

    /**
     * 加密文件夹（ZIP包）：加密所有文件后重新打包
     * @param file 待加密的ZIP包（参数名统一为file）
     */
    @PostMapping("/encrypt")
    @ApiOperation("加密文件夹：将ZIP包中的文件加密后重新打包")
    public ResponseEntity<byte[]> encryptFolder(
            @RequestParam("file") MultipartFile file) throws Exception { // 参数名改为file
        // 校验文件非空
        if (file.isEmpty()) {
            throw new IllegalArgumentException("上传的ZIP文件不能为空");
        }

        // 1. 解压上传的ZIP包
        Map<String, byte[]> fileMap = ZipFileUtil.unzip(file);
        if (fileMap.isEmpty()) {
            throw new IllegalArgumentException("ZIP包为空或格式错误");
        }

        // 2. 加密每个文件的内容
        Map<String, byte[]> encryptedFileMap = new HashMap<>();
        for (Map.Entry<String, byte[]> entry : fileMap.entrySet()) {
            String fileName = entry.getKey();
            byte[] fileData = entry.getValue();
            // 加密文件内容（Base64编码）
            String encryptedContent = AESEncryptionUtil.encryptToBase64(FIXED_KEY, fileData);
            encryptedFileMap.put(fileName, encryptedContent.getBytes(StandardCharsets.UTF_8));
        }

        // 3. 将加密后的文件重新打包为ZIP
        byte[] encryptedZipData = ZipFileUtil.zip(encryptedFileMap);

        // 4. 返回加密后的ZIP包
        String originalName = file.getOriginalFilename();
        String encryptedFileName = originalName != null 
                ? originalName.replace(".zip", "encrypted_folder.enc") 
                : "encrypted_folder.enc";
        
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + encryptedFileName + "\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(encryptedZipData);
    }

    /**
     * 解密文件夹（ZIP包）：解密后分别处理Excel（导入）和TXT（读取内容）
     * @param file 加密的ZIP包（参数名统一为file）
     * @param key 解密密钥（可选）
     */
    @PostMapping("/upgrade")
    @ApiOperation("升级：解密ZIP包并处理内部Excel和TXT文件")
    public ResponseEntity<Result<Map<String, Object>>> decryptAndProcess(
            @RequestParam("file") MultipartFile file, // 参数名改为file
            @RequestParam(value = "key", required = false) String key) throws Exception {
        
        // 校验文件非空
        if (file.isEmpty()) {
            throw new IllegalArgumentException("上传的加密文件不能为空");
        }

        // 1. 解密密钥处理
        String usedKey = (key != null && !key.isEmpty()) ? key : FIXED_KEY;

        // 2. 解压加密的ZIP包
        Map<String, byte[]> encryptedFileMap = ZipFileUtil.unzip(file);
        if (encryptedFileMap.isEmpty()) {
            throw new IllegalArgumentException("加密ZIP包为空或格式错误");
        }

        // 3. 解密每个文件并分类处理
        Map<String, String> txtContentMap = new HashMap<>(); // 存储TXT文件内容
        List<MultipartFile> excelFiles = new ArrayList<>();  // 存储Excel文件

        for (Map.Entry<String, byte[]> entry : encryptedFileMap.entrySet()) {
            String fileName = entry.getKey();
            byte[] encryptedData = entry.getValue();
            
            try {
                // 解密文件内容（Base64解码）
                String encryptedContent = new String(encryptedData, StandardCharsets.UTF_8);
                byte[] decryptedData = AESEncryptionUtil.decryptFromBase64(usedKey, encryptedContent);

                // 区分文件类型处理
                if (fileName.toLowerCase().endsWith(EXCEL_EXTENSION)) {
                    // Excel文件：转换为MultipartFile待导入
                    MultipartFile excelFile = new InMemoryMultipartFile(
                            fileName, 
                            fileName,
                            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                            decryptedData
                    );
                    excelFiles.add(excelFile);
                } else if (fileName.toLowerCase().endsWith(TXT_EXTENSION)) {
                    // TXT文件：读取内容
                    String txtContent = new String(decryptedData, StandardCharsets.UTF_8);
                    txtContentMap.put(fileName, txtContent);
                } else {
                    throw new IllegalArgumentException("不支持的文件类型：" + fileName + "，仅支持.xlsx和.txt");
                }
            } catch (Exception e) {
                throw new RuntimeException("处理文件[" + fileName + "]时出错：" + e.getMessage(), e);
            }
        }

        // 4. 处理Excel文件（导入）
        List<String> importResults = new ArrayList<>();
        if (!excelFiles.isEmpty()) {
            for (MultipartFile excelFile : excelFiles) {
                Result<String> importResult = importController.importExcel(excelFile).getBody();
                if (importResult != null) {
                    importResults.add(excelFile.getOriginalFilename() + "：" + importResult.getMessage());
                } else {
                    importResults.add(excelFile.getOriginalFilename() + "：导入结果未知");
                }
            }
        } else {
            importResults.add("未发现Excel文件");
        }

        // 5. 整理返回结果（TXT内容 + Excel导入结果）
        Map<String, Object> resultData = new HashMap<>();
        resultData.put("txtContents", txtContentMap);       // TXT文件内容
        resultData.put("excelImportResults", importResults); // Excel导入结果
        resultData.put("fileCount", encryptedFileMap.size()); // 总文件数

        return ResponseEntity.ok(Result.ok(resultData).message("升级完成"));
    }

    // 内存中的MultipartFile实现（完善异常处理）
    static class InMemoryMultipartFile implements MultipartFile {
        private final String name;
        private final String originalFilename;
        private final String contentType;
        private final byte[] content;

        public InMemoryMultipartFile(String name, String originalFilename, String contentType, byte[] content) {
            this.name = name;
            this.originalFilename = originalFilename;
            this.contentType = contentType;
            this.content = content != null ? content : new byte[0]; // 避免空指针
        }

        @Override public String getName() { return name; }
        @Override public String getOriginalFilename() { return originalFilename; }
        @Override public String getContentType() { return contentType; }
        @Override public boolean isEmpty() { return content.length == 0; }
        @Override public long getSize() { return content.length; }
        @Override public byte[] getBytes() throws IOException { return content.clone(); } // 返回副本避免篡改
        @Override public InputStream getInputStream() { return new ByteArrayInputStream(content); }
        @Override public void transferTo(File dest) throws IOException, IllegalStateException {
            if (dest == null) {
                throw new IllegalArgumentException("目标文件不能为空");
            }
            try (FileOutputStream fos = new FileOutputStream(dest)) {
                fos.write(content);
            }
        }
    }
}