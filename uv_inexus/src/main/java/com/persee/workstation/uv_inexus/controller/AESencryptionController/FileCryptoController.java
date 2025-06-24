package com.persee.workstation.uv_inexus.controller.AESencryptionController;

import java.nio.charset.StandardCharsets;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.persee.workstation.uv_inexus.utils.AESencryption.AESEncryptionUtil;

@RestController
@RequestMapping("/api/crypto")
public class FileCryptoController {
    // 确保这个密钥长度符合AES要求（16, 24, 32字节）
    // 并且是Base64编码的，如果长度不是16/24/32字节，Base64编码后的长度会是4的倍数
    // 这里 "pFFKQ9huIh0taRfethpTv6DLXYJPJw1YGZtrEoSHJp0=" 是32字节密钥的Base64编码
    public static final String FIXED_KEY = "pFFKQ9huIh0taRfethpTv6DLXYJPJw1YGZtrEoSHJp0=";

    // 加密文件内容并返回加密文件
    @PostMapping("/encrypt")
    public ResponseEntity<byte[]> encryptFile(
            @RequestParam("file") MultipartFile file) throws Exception {
        
        // 使用定死的密钥进行加密
        String key = FIXED_KEY;
        System.err.println("使用密钥进行加密: " + key);
        
        // 加密文件内容
        String encryptedContent = AESEncryptionUtil.encryptToBase64(key, file.getBytes());
        
        // 生成自定义的加密文件名
        String encryptedFileName = "data_upgrade" + ".enc";
        // 返回加密后的数据
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + encryptedFileName + "\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(encryptedContent.getBytes(StandardCharsets.UTF_8));
    }

    // 解密加密文件并返回解密后的文件
    @PostMapping("/decrypt")
    public ResponseEntity<byte[]> decryptFile(
            @RequestParam("file") MultipartFile encryptedFile,
            @RequestParam("key") String key,
            @RequestParam(value = "originalFilename", required = false) String originalFilename) throws Exception {
        
        // 读取加密文件内容
        String encryptedContent = new String(encryptedFile.getBytes(), StandardCharsets.UTF_8);
        
        // 解密文件
        byte[] decryptedData = AESEncryptionUtil.decryptFromBase64(key, encryptedContent);
        
        // 确定文件名
        String filename = originalFilename != null ? originalFilename : 
                         encryptedFile.getOriginalFilename().replace(".enc", ".xlsx");
        
        // 返回解密后的文件
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(decryptedData);
    }
}
