/*
 * *
 *  * 版权所有 (C) 2025 北京普析通用仪器有限责任公司。保留所有权利。
 *  * 本软件受商业机密法和版权法保护。
 *  * 本代码仅限与北京普析通用仪器有限责任公司签订有效协议的授权用户内部使用。
 *  * 任何未经明确许可的复制、使用、修改、分发或公开均被严格禁止。
 *
 */

package com.persee.workstation.uv_inexus.controller.guide.AESencryptionController;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.persee.workstation.uv_inexus.i18n.YamlMessageSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.persee.workstation.uv_inexus.controller.guide.parseReport.ImportController;
import com.persee.workstation.uv_inexus.dao.guide.parseReport.UpgradeVersionMapper;
import com.persee.workstation.uv_inexus.model.Result;
import com.persee.workstation.uv_inexus.po.guide.parseReport.UpgradeVersion;
import com.persee.workstation.uv_inexus.utils.AESencryption.AESEncryptionUtil;
import com.persee.workstation.uv_inexus.utils.AESencryption.ZipFileUtil;

import io.swagger.annotations.ApiOperation;

@RestController
@RequestMapping("/com/persee/workstation/uv-inexus/crypto")
public class FileCryptoController {
    public static final String FIXED_KEY = "pFFKQ9huIh0taRfethpTv6DLXYJPJw1YGZtrEoSHJp0=";
    public static final String EXCEL_EXTENSION = ".xlsx";
    public static final String TXT_EXTENSION = ".txt";
    // 匹配版本号的正则（v+数字.数字.数字，如v2.1.3）
    private static final Pattern VERSION_PATTERN = Pattern.compile("v\\d+\\.\\d+\\.\\d+");

    @Autowired
    private ImportController importController;
    @Autowired
    private UpgradeVersionMapper upgradeVersionMapper;


    /**
     * 接口1：解析文件版本号和TXT内容（不存储版本号）
     * @param file 上传的加密ZIP包
     * @param key 解密密钥（可选）
     */
    @PostMapping("/parseVersion")
    @ApiOperation("第一步：解析文件版本号和TXT内容")
    public ResponseEntity<Result<Map<String, Object>>> parseVersion(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "key", required = false) String key) throws Exception {
        
        // 1. 校验文件
        if (file.isEmpty()) {
            throw new IllegalArgumentException("上传的文件不能为空");
        }
        String fileName = file.getOriginalFilename();
        if (fileName == null || fileName.isEmpty()) {
            throw new IllegalArgumentException("文件名不能为空");
        }

        // 2. 解析文件名中的版本号
        String parsedVersion = parseVersionFromFileName(fileName);
        if (parsedVersion == null) {
            throw new IllegalArgumentException("文件名中未找到有效的版本号（格式应为vX.X.X，如v2.1.3）");
        }

        // 3. 解密文件并提取TXT内容
        String usedKey = (key != null && !key.isEmpty()) ? key : FIXED_KEY;
        Map<String, byte[]> encryptedFileMap = ZipFileUtil.unzip(file);
        String txtContent = extractSingleTxtContent(encryptedFileMap, usedKey);

        // 4. 查询当前最新版本
        UpgradeVersion latestVersion = upgradeVersionMapper.selectLatestVersion();
        String currentVersion = latestVersion != null ? latestVersion.getVersion() : "无历史版本";
        String currentDescription = latestVersion != null ? latestVersion.getUpdateDescription() : "";

        // 5. 整理返回结果
        Map<String, Object> resultData = new HashMap<>();
        resultData.put("parsedVersion", parsedVersion);
        resultData.put("currentVersion", currentVersion);
        resultData.put("currentDescription", currentDescription); // 返回当前版本的更新说明
        resultData.put("updateDescription", txtContent);          // 返回新版本的更新说明
        resultData.put("fileName", fileName);
        //版本解析成功
        return ResponseEntity.ok(Result.ok(resultData).message(YamlMessageSource.getMessageStatic("00901001")));
    }


    /**
     * 接口2：执行Excel导入并存储版本号和更新说明
     * @param file 上传的加密ZIP包
     * @param key 解密密钥（可选）
     */
    @PostMapping("/upgrade")
    @ApiOperation("第二步：执行Excel导入并存储版本号和更新说明")
    public ResponseEntity<Result<Map<String, Object>>> executeImport(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "key", required = false) String key) throws Exception {
        
        // 1. 校验文件
        if (file.isEmpty()) {
            throw new IllegalArgumentException("上传的文件不能为空");
        }
        String fileName = file.getOriginalFilename();
        if (fileName == null || fileName.isEmpty()) {
            throw new IllegalArgumentException("文件名不能为空");
        }

        // 2. 解析文件名中的版本号
        String parsedVersion = parseVersionFromFileName(fileName);
        if (parsedVersion == null) {
            throw new IllegalArgumentException("文件名中未找到有效的版本号（格式应为vX.X.X，如v2.1.3）");
        }

        // 3. 解密文件并提取Excel和TXT内容
        String usedKey = (key != null && !key.isEmpty()) ? key : FIXED_KEY;
        Map<String, byte[]> encryptedFileMap = ZipFileUtil.unzip(file);
        String updateDescription = extractSingleTxtContent(encryptedFileMap, usedKey);
        List<MultipartFile> excelFiles = extractExcelFiles(encryptedFileMap, usedKey);

        // 4. 执行Excel导入
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

        // 5. 检查版本号是否已存在（核心修改）
        int versionCount = upgradeVersionMapper.existsByVersion(parsedVersion);
        if (versionCount > 0) {
            // 版本已存在，返回错误信息
            Map<String, Object> errorData = new HashMap<>();
            errorData.put("version", parsedVersion);return ResponseEntity.ok(
            Result.fail(errorData)
                .code(400)
                    //当前版本已存在，请核实升级版本
                .message(YamlMessageSource.getMessageStatic("00901002"))
            );
        }

        // 6. 版本不存在，存储版本号和更新说明到数据库
        UpgradeVersion newVersion = new UpgradeVersion();
        newVersion.setVersion(parsedVersion);
        newVersion.setUpdateDescription(updateDescription);
        newVersion.setCreateTime(LocalDateTime.now());
        upgradeVersionMapper.insert(newVersion);

        // 7. 返回导入结果和新版本信息
        Map<String, Object> resultData = new HashMap<>();
        resultData.put("excelImportResults", importResults);
        resultData.put("fileCount", excelFiles.size());
        resultData.put("newVersion", parsedVersion);
        resultData.put("updateDescription", updateDescription);
        //导入并存储版本信息成功
        return ResponseEntity.ok(Result.ok(resultData).message(YamlMessageSource.getMessageStatic("00901003")));
    }


    /**
     * 接口3：制作加密升级包（一个TXT + 一个Excel）
     * @param txtFile 版本说明TXT文件
     * @param excelFile 数据Excel文件
     * @param version 目标版本号（如 v2.1.3）
     * @param key 加密密钥（可选）
     */
    @PostMapping("/generatePackage")
    @ApiOperation("制作加密升级包")
    public ResponseEntity<byte[]> generatePackage(
            @RequestParam("txtFile") MultipartFile txtFile,
            @RequestParam("excelFile") MultipartFile excelFile,
            @RequestParam("version") String version,
            @RequestParam(value = "key", required = false) String key) throws Exception {

        if (txtFile == null || txtFile.isEmpty()) {
            throw new IllegalArgumentException("TXT文件不能为空");
        }
        if (excelFile == null || excelFile.isEmpty()) {
            throw new IllegalArgumentException("Excel文件不能为空");
        }
        if (version == null || version.isEmpty() || !VERSION_PATTERN.matcher(version).matches()) {
            throw new IllegalArgumentException("版本号格式不正确（应为vX.X.X）");
        }

        String txtName = txtFile.getOriginalFilename();
        String excelName = excelFile.getOriginalFilename();
        if (txtName == null || !txtName.toLowerCase().endsWith(TXT_EXTENSION)) {
            throw new IllegalArgumentException("TXT文件后缀必须为" + TXT_EXTENSION);
        }
        if (excelName == null || !excelName.toLowerCase().endsWith(EXCEL_EXTENSION)) {
            throw new IllegalArgumentException("Excel文件后缀必须为" + EXCEL_EXTENSION);
        }

        String usedKey = (key != null && !key.isEmpty()) ? key : FIXED_KEY;

        Map<String, byte[]> filesToZip = new HashMap<>();

        byte[] txtBytes = txtFile.getBytes();
        String txtEncryptedBase64 = AESEncryptionUtil.encryptToBase64(usedKey, txtBytes);
        byte[] txtStoredBytes = txtEncryptedBase64.getBytes(StandardCharsets.UTF_8);
        filesToZip.put(txtName, txtStoredBytes);

        byte[] excelBytes = excelFile.getBytes();
        String excelEncryptedBase64 = AESEncryptionUtil.encryptToBase64(usedKey, excelBytes);
        byte[] excelStoredBytes = excelEncryptedBase64.getBytes(StandardCharsets.UTF_8);
        filesToZip.put(excelName, excelStoredBytes);

        byte[] zipBytes = ZipFileUtil.zip(filesToZip);

        String downloadFileName = "upgrade_" + version + ".enc";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        headers.setContentDispositionFormData("attachment", downloadFileName);

        return ResponseEntity.ok()
                .headers(headers)
                .body(zipBytes);
    }

    /**
     * 辅助方法：从文件名中解析版本号
     */
    private String parseVersionFromFileName(String fileName) {
        Matcher matcher = VERSION_PATTERN.matcher(fileName);
        if (matcher.find()) {
            return matcher.group();
        }
        return null;
    }


    /**
     * 辅助方法：从加密文件中提取单个TXT文件的内容
     */
    private String extractSingleTxtContent(Map<String, byte[]> encryptedFileMap, String key) throws Exception {
        StringBuilder contentBuilder = new StringBuilder();
        
        for (Map.Entry<String, byte[]> entry : encryptedFileMap.entrySet()) {
            String fileName = entry.getKey();
            byte[] encryptedData = entry.getValue();

            if (fileName.toLowerCase().endsWith(TXT_EXTENSION)) {
                String encryptedContent = new String(encryptedData, StandardCharsets.UTF_8);
                byte[] decryptedData = AESEncryptionUtil.decryptFromBase64(key, encryptedContent);
                String txtContent = new String(decryptedData, StandardCharsets.UTF_8);
                contentBuilder.append(txtContent);
                break; // 只取第一个TXT文件
            }
        }
        
        return contentBuilder.toString();
    }


    /**
     * 辅助方法：从加密文件中提取Excel文件
     */
    private List<MultipartFile> extractExcelFiles(Map<String, byte[]> encryptedFileMap, String key) throws Exception {
        List<MultipartFile> excelFiles = new ArrayList<>();
        for (Map.Entry<String, byte[]> entry : encryptedFileMap.entrySet()) {
            String fileName = entry.getKey();
            byte[] encryptedData = entry.getValue();

            if (fileName.toLowerCase().endsWith(EXCEL_EXTENSION)) {
                String encryptedContent = new String(encryptedData, StandardCharsets.UTF_8);
                byte[] decryptedData = AESEncryptionUtil.decryptFromBase64(key, encryptedContent);
                
                MultipartFile excelFile = new InMemoryMultipartFile(
                        fileName, fileName,
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                        decryptedData
                );
                excelFiles.add(excelFile);
            }
        }
        return excelFiles;
    }


    // 内存中的MultipartFile实现
    static class InMemoryMultipartFile implements MultipartFile {
        private final String name;
        private final String originalFilename;
        private final String contentType;
        private final byte[] content;

        public InMemoryMultipartFile(String name, String originalFilename, String contentType, byte[] content) {
            this.name = name;
            this.originalFilename = originalFilename;
            this.contentType = contentType;
            this.content = content != null ? content : new byte[0];
        }

        @Override public String getName() { return name; }
        @Override public String getOriginalFilename() { return originalFilename; }
        @Override public String getContentType() { return contentType; }
        @Override public boolean isEmpty() { return content.length == 0; }
        @Override public long getSize() { return content.length; }
        @Override public byte[] getBytes() throws IOException { return content.clone(); }
        @Override public InputStream getInputStream() { return new ByteArrayInputStream(content); }
        @Override public void transferTo(File dest) throws IOException {
            try (FileOutputStream fos = new FileOutputStream(dest)) {
                fos.write(content);
            }
        }
    }
}
