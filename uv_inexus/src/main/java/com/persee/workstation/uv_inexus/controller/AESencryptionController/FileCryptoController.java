package com.persee.workstation.uv_inexus.controller.AESencryptionController;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

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

@RestController
@RequestMapping("/api/crypto")
public class FileCryptoController {
    public static final String FIXED_KEY = "pFFKQ9huIh0taRfethpTv6DLXYJPJw1YGZtrEoSHJp0=";

    @Autowired
    private ImportController importController;

    // 加密方法保持不变
    @PostMapping("/encrypt")
    public ResponseEntity<byte[]> encryptFile(
            @RequestParam("file") MultipartFile file) throws Exception {
        String key = FIXED_KEY;
        String encryptedContent = AESEncryptionUtil.encryptToBase64(key, file.getBytes());
        String encryptedFileName = "data_upgrade" + ".enc";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + encryptedFileName + "\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(encryptedContent.getBytes(StandardCharsets.UTF_8));
    }

    // 修改后的解密方法，解密后直接调用导入接口
    @PostMapping("/upgrade")
    public ResponseEntity<Result<String>> decryptAndImport(
            @RequestParam("file") MultipartFile encryptedFile,
            @RequestParam(value = "key", required = false) String key) throws Exception {
        
        // 使用固定密钥或传入的密钥
        String usedKey = (key != null && !key.isEmpty()) ? key : FIXED_KEY;
        
        // 读取并解密文件
        String encryptedContent = new String(encryptedFile.getBytes(), StandardCharsets.UTF_8);
        byte[] decryptedData = AESEncryptionUtil.decryptFromBase64(usedKey, encryptedContent);
        
        // 将解密后的数据转换为MultipartFile
        MultipartFile decryptedFile = new InMemoryMultipartFile(
                "file",
                encryptedFile.getOriginalFilename().replace(".enc", ".xlsx"),
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                decryptedData
        );
        
        // 调用现有的导入接口
        return importController.importExcel(decryptedFile);
    }
}

// 内存中的MultipartFile实现
class InMemoryMultipartFile implements MultipartFile {
    private final String name;
    private final String originalFilename;
    private final String contentType;
    private final byte[] content;

    public InMemoryMultipartFile(String name, String originalFilename, String contentType, byte[] content) {
        this.name = name;
        this.originalFilename = originalFilename;
        this.contentType = contentType;
        this.content = content;
    }

    @Override public String getName() { return name; }
    @Override public String getOriginalFilename() { return originalFilename; }
    @Override public String getContentType() { return contentType; }
    @Override public boolean isEmpty() { return content == null || content.length == 0; }
    @Override public long getSize() { return content.length; }
    @Override public byte[] getBytes() { return content; }
    @Override public java.io.InputStream getInputStream() { return new ByteArrayInputStream(content); }
    @Override public void transferTo(java.io.File dest) throws IOException, IllegalStateException {
        new java.io.FileOutputStream(dest).write(content);
    }
}