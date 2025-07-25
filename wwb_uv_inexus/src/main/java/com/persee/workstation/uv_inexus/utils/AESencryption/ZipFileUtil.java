package com.persee.workstation.uv_inexus.utils.AESencryption;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream;
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;
import org.springframework.web.multipart.MultipartFile;

/**
 * ZIP压缩包处理工具类
 */
public class ZipFileUtil {

    /**
     * 解压ZIP文件，返回文件Map（文件名 -> 字节数组）
     */
    public static Map<String, byte[]> unzip(MultipartFile zipFile) throws IOException {
        Map<String, byte[]> fileMap = new HashMap<>();
        try (ZipArchiveInputStream zipIn = new ZipArchiveInputStream(zipFile.getInputStream())) {
            ZipArchiveEntry entry;
            while ((entry = zipIn.getNextZipEntry()) != null) {
                if (!entry.isDirectory()) {
                    byte[] buffer = new byte[1024];
                    ByteArrayOutputStream out = new ByteArrayOutputStream();
                    int len;
                    while ((len = zipIn.read(buffer)) != -1) {
                        out.write(buffer, 0, len);
                    }
                    fileMap.put(entry.getName(), out.toByteArray());
                    out.close();
                }
            }
        }
        return fileMap;
    }

    /**
     * 将文件Map打包为ZIP字节数组
     */
    public static byte[] zip(Map<String, byte[]> fileMap) throws IOException {
        ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
        try (ZipArchiveOutputStream zipOut = new ZipArchiveOutputStream(byteOut)) {
            for (Map.Entry<String, byte[]> entry : fileMap.entrySet()) {
                ZipArchiveEntry zipEntry = new ZipArchiveEntry(entry.getKey());
                zipOut.putArchiveEntry(zipEntry);
                zipOut.write(entry.getValue());
                zipOut.closeArchiveEntry();
            }
        }
        return byteOut.toByteArray();
    }
}
