package com.persee.workstation.uv_inexus.service.guide.softwareActivation.impl;

import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.persee.workstation.uv_inexus.dao.guide.softwareActivationDao.RemoteAccessMapper;
import com.persee.workstation.uv_inexus.dto.softwareActivation.EncryptedConfigDto;
import com.persee.workstation.uv_inexus.dto.softwareActivation.EncryptedIpsDto;
import com.persee.workstation.uv_inexus.model.Result;
import com.persee.workstation.uv_inexus.po.guide.softwareActivation.EncryptedConfigPO;
import com.persee.workstation.uv_inexus.service.guide.softwareActivation.RemoteAccessService;
import com.persee.workstation.uv_inexus.util.CryptoUtil;
import com.persee.workstation.uv_inexus.util.HardwareUtil;
import com.persee.workstation.uv_inexus.util.LicenseValidator;
import com.persee.workstation.uv_inexus.utils.AESencryption.SwitchStatusObfuscator;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.crypto.SecretKey;
import javax.transaction.Transactional;
import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.security.PrivateKey;
import java.util.*;

@Service
public class RemoteAccessServiceImpl extends ServiceImpl<RemoteAccessMapper, EncryptedConfigPO> implements RemoteAccessService {

    // 注入LicenseService
    @Autowired
    private com.persee.workstation.uv_inexus.service.guide.softwareActivation.impl.LicenseServiceImpl licenseService;

    private static final Map<String, String> map = new HashMap<String, String>() {{
        put("0", "up");
        put("1", "down");
        put("up", "0");
        put("down", "1");
    }};

    @Override
    public Result<EncryptedConfigDto> getConfigList() {
        EncryptedConfigPO po = getOne(null);
        if (po != null) {

            String status = SwitchStatusObfuscator.deobfuscateStatus(po.getIsEnabled());

            String encryptedIpsStr = new String(po.getEncryptedIps(), StandardCharsets.UTF_8);
            List<EncryptedIpsDto> list = JSON.parseArray(encryptedIpsStr, EncryptedIpsDto.class);
            List<String> strings = new ArrayList<>();
            for (EncryptedIpsDto item : list) {
                strings.add(item.getEncryptedIps());
            }
            if (CollectionUtils.isNotEmpty(strings)) {
                String maxcount = new String(po.getEncryptedMaxcount(), StandardCharsets.UTF_8);
                long l = licenseService.stringDeobfuscate(maxcount);
                int size = strings.size();
                if (size > l) {
                    throw new IllegalArgumentException("配置的IP数量超过许可证允许的最大数量");
                }
            }
            EncryptedConfigDto dto = new EncryptedConfigDto(po.getId(), map.get(status), strings);
            return Result.ok(dto);
        }
        return null;
    }

    @Override
    @Transactional
    public Result<Boolean> saveConfig(EncryptedConfigDto configDto) {
        try {
            //开关 返回前端的是0,1  0打开 1关闭  后端库表存的是up  down
            EncryptedConfigPO po = getById(configDto.getId());
            // 状态转换：将"0"/"1"转换为加密状态
            String status = configDto.getSwitchOn();
            String sobfuscatedStatus = map.get(status);
            po.setIsEnabled(SwitchStatusObfuscator.obfuscateStatus(sobfuscatedStatus));

            // 加密IP列表处理
            List<EncryptedIpsDto> ipDtos = new ArrayList<>();
            if (configDto.getEncryptedIps() != null) {
                for (String ip : configDto.getEncryptedIps()) {
                    // 校验IP地址格式
                    if (!isValidIPAddress(ip)) {
                        throw new IllegalArgumentException("IP地址格式不正确: " + ip);
                    }
                    EncryptedIpsDto ipDto = new EncryptedIpsDto();
                    ipDto.setEncryptedIps(ip);
                    ipDtos.add(ipDto);
                }
            }
            if (!ipDtos.isEmpty()) {
                String maxcount = new String(po.getEncryptedMaxcount(), StandardCharsets.UTF_8);
                long l = licenseService.stringDeobfuscate(maxcount);
                int size = ipDtos.size();
                if (size > l) {
                    throw new IllegalArgumentException("配置的IP数量超过许可证允许的最大数量");
                }
            }
            String ipJson = JSON.toJSONString(ipDtos);
            po.setEncryptedIps(ipJson.getBytes());

            boolean b = updateById(po);
            return Result.ok(b);
        } catch (Exception e) {
            return Result.fail("保存配置失败: " + e.getMessage());
        }
    }

    @Override
    public Result<LicenseValidator.ValidationResult> validateLicenseFile(MultipartFile file) {
        try {
            String hardwareFingerprint = HardwareUtil.getHardwareFingerprint();

            System.out.println("开始验证许可证文件:");
            System.out.println("- 硬件指纹: " + hardwareFingerprint);
            System.out.println("- 文件名: " + file.getOriginalFilename());

            // 读取文件内容
            String content = new String(file.getBytes(), StandardCharsets.UTF_8);
            System.out.println("- 文件内容长度: " + content.length());

            // 检查是否为加密文件
            if (content.startsWith("#Encrypted License File")) {
                System.out.println("检测到加密的许可证文件");

                // 解析加密文件内容
                Properties props = new Properties();
                props.load(new StringReader(content));

                String encryptedData = props.getProperty("encrypted.data");
                String encryptedKey = props.getProperty("encrypted.key");

                System.out.println("加密数据: " + encryptedData);

                // 使用临时生成的私钥解密
                String privateKeyStr = licenseService.getPrivateKeyStr();

                if (privateKeyStr == null || privateKeyStr.isEmpty()) {
                    System.err.println("私钥为空，无法解密");
                    return Result.fail(new LicenseValidator.ValidationResult(false, "私钥为空，无法解密"));
                }

                try {
                    PrivateKey privateKey = CryptoUtil.stringToPrivateKey(privateKeyStr);
                    String aesKeyStr = CryptoUtil.decryptRSA(encryptedKey, privateKey);
                    SecretKey aesKey = CryptoUtil.stringToSecretKey(aesKeyStr);
                    content = CryptoUtil.decryptAES(encryptedData, aesKey);
                } catch (Exception e) {
                    System.err.println("解密失败: " + e.getMessage());
                    e.printStackTrace();
                    return Result.fail(new LicenseValidator.ValidationResult(false, "解密许可证文件失败: " + e.getMessage()));
                }
            }

            // 使用无数据库验证方法
            LicenseValidator validator = new LicenseValidator();
            validator.setPublicKey(licenseService.getPublicKeyStr());
            LicenseValidator.ValidationResult result = validator.validateLicenseFileContent(content, hardwareFingerprint);

            System.out.println("文件验证结果: " + result.isValid());
            Boolean b = this.saveConfig(content);
            return Result.ok(result);
        } catch (IOException e) {
            System.err.println("读取文件失败: " + e.getMessage());
            return Result.fail(new LicenseValidator.ValidationResult(false, "读取文件失败: " + e.getMessage()));
        } catch (Exception e) {
            System.err.println("验证过程中发生错误: " + e.getMessage());
            return Result.fail(new LicenseValidator.ValidationResult(false, "验证过程中发生错误: " + e.getMessage()));
        }
    }

    @Override
    public Boolean saveConfig(String content) {
        Map<String, String> map = licenseService.parseLicense(content);
        Long count = MapUtils.getLong(map, "licence.max.ip.count");
        // 混淆
        String s = licenseService.stringObfuscate(count);
        System.out.println(s);
        String hardwareFingerprint = HardwareUtil.getHardwareFingerprint();
        EncryptedConfigPO po = new EncryptedConfigPO();
        po.setId(hardwareFingerprint);
        po.setIsEnabled(SwitchStatusObfuscator.obfuscateStatus("up"));
        po.setEncryptedMaxcount(s.getBytes());
        po.setEncryptedIps(new byte[0]);
        return save(po);
    }

    /**
     * 校验IP地址格式是否正确
     *
     * @param ip IP地址字符串
     * @return 是否为有效的IP地址
     */
    private boolean isValidIPAddress(String ip) {
        if (ip == null || ip.isEmpty()) {
            return false;
        }

        // IPv4地址校验
        String ipv4Pattern = "^((25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$";
        return ip.matches(ipv4Pattern);
    }


}