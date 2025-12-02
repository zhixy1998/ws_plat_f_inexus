package com.persee.workstation.uv_inexus.service.guide.softwareActivation.impl;

import com.persee.workstation.uv_inexus.service.guide.softwareActivation.LicenseService;
import org.springframework.stereotype.Service;
import java.util.*;

@Service
public class LicenseServiceImpl implements LicenseService {
    private String PRIVATE_KEY_STR="MIIEvQIBADANBgkqhkiG9w0BAQEFAASCBKcwggSjAgEAAoIBAQDENtnYke657E86EhD97FD7spDdD5SYTzJJE9XsDhMTHua61WEqLh5z+A5l6fdWY+HoRar6eJw/V6KACZKrEWVX3BEKL2cTD5CVezqddbHt9RdhZt3Hpd8Uoe5vSPafJ+wpjdeW+j4NXE5iY05ka8wNyZ39aL8wEExH2vMqyzUJ8wmlv+t5nC4Wswjhg2oiwxm/HBKFKVN9FUVg7hmIHNXNkknQbBg657yksvanHlgqDhzYqFTOxvYi3okta3yzCWrj04iHy56kn0WFYyqnAs+7iair6PhhTNImJLbYZUsEoGcPXETX5K0/LJ2x/w/OF0M4T6kAxkUPlKX5DUslflGFAgMBAAECggEAebBe+O7AtLQ3CpfLy/901jUPnNod2Tw8vo+7u5ObBt/CTxspEKTbpCayn0g0anK113zlpkOI+8vY7zmAqfG2/X9xXfS6I1si22I6nw8wU5kOcA0A/OIFcWFgY8pHkbuLvtXGYE9Ajr6xqllWc/da7S4eetwsvp1NAb9r0h2G/myS4erWmYX5NwuiLWnOp437Fx5BCoLtelhVbx3T5ctxddJZ3/Qobo11MgJlC2JuLR6+Kb1ihF6DyspDtnaLX/DGb4gVCJqReGiT2EuPXijjQVN3Er9W7dP994yfPsftyuvAUyn1oHWCxKhqGepw0NLnXFkK7+G+p0+IzUe0pJeo/QKBgQDwSQcVj4ri/1XVdncAFMCqWgaQ9QlUG+53POqPIG1EahFkJG/S/Dtj5wGmXCZ+RP41Z5WGy2CL8CBvmFmJiiPqy8Gs25f8DpDJip5yog1JH+ko+76Nsosi7/Kqk6dPSq+3lskNP9oSQhcfnv/ceiFTv08EqO0tZA36CFdqGZ9rLwKBgQDRC/cXp7dY0/pggIhV6pVF2Yidh48vGEEw1zWDKgjYv5GnJTLbS1isaayO2BGvDj3n9IMZFGFk2u+KHKYPAiXGB6pVWqolYs5O062C3l3wTpR1v3SokFEVDdTWsQR4yGKndCBnfatDuQJg1BdJlCS+iD+cqNd1MfcMM1HNvv8RiwKBgHFJzRhCRDZAMoThRLv6DNiEd4IStaI9RRJc2y4+GKZf5/EyKLLuD0ZL3QBFhDSWbNBbFSmna86umWxkBRsO0V5bUOYh5qeFaohLXecoNmFatUCIaNA/9M+1F9GKqiB9UxmY8xPXHgbruCFFgta9kkb89NF6n9B90wUmt6gjioErAoGAQpodT1wP/i0LSFOrHDRw+qAj9oyKAecoXfczBWcPCn9+WFJxsYx/4+6HG7ubZrqS3qP4YLUtkoRubQJTpdR5VG58xlTsgJzKPjMSxO8z3qwqgJlPAaV5L8wIU55yXkj5pYBKEq7nMdxv9YmdIcQhMNMX8UGZ46zXmPuAxJxlh6ECgYEAhSvxkQq3IlZLfKa3KtyX6wtzhdCwTDLH6Pr9jLufqWhq4vo4Sm8lhxJaVCbHC8rW3DkNnjC0lu5Y8KAjJGHlvcx63HbOXowbT2HHpuP3sdq+URqayMBFD/cgwQmgJRJP1fToSdbDsz2hork58tq956T44b5EMVjpyuTXCEyvzQE=";
    
    private String PUBLIC_KEY_STR="MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAxDbZ2JHuuexPOhIQ/exQ+7KQ3Q+UmE8ySRPV7A4TEx7mutVhKi4ec/gOZen3VmPh6EWq+nicP1eigAmSqxFlV9wRCi9nEw+QlXs6nXWx7fUXYWbdx6XfFKHub0j2nyfsKY3Xlvo+DVxOYmNOZGvMDcmd/Wi/MBBMR9rzKss1CfMJpb/reZwuFrMI4YNqIsMZvxwShSlTfRVFYO4ZiBzVzZJJ0GwYOue8pLL2px5YKg4c2KhUzsb2It6JLWt8swlq49OIh8uepJ9FhWMqpwLPu4moq+j4YUzSJiS22GVLBKBnD1xE1+StPyydsf8PzhdDOE+pAMZFD5Sl+Q1LJX5RhQIDAQAB";

    public String getPrivateKeyStr() {
        if (PRIVATE_KEY_STR != null && !PRIVATE_KEY_STR.trim().isEmpty() && !PRIVATE_KEY_STR.equals("\"\"")) {
            System.out.println("使用配置文件中的私钥");
            return PRIVATE_KEY_STR;
        }
        return "";
    }
    
    public String getPublicKeyStr() {
        if (PUBLIC_KEY_STR != null && !PUBLIC_KEY_STR.trim().isEmpty() && !PUBLIC_KEY_STR.equals("\"\"")) {
            System.out.println("使用配置文件中的公钥");
            return PUBLIC_KEY_STR;
        }
        return "";
    }
    public  Map<String, String> parseLicense(String licenseContent) {
        Map<String, String> licenseMap = new HashMap<>();

        // 按行分割内容
        String[] lines = licenseContent.split("\n");

        for (String line : lines) {
            // 忽略空行或注释行（以#开头）
            if (line.trim().isEmpty() || line.startsWith("#")) {
                continue;
            }

            // 按等号分割键值对
            int equalsIndex = line.indexOf('=');
            if (equalsIndex != -1) {
                String key = line.substring(0, equalsIndex).trim();
                String value = line.substring(equalsIndex + 1).trim();
                licenseMap.put(key, value);
            }
        }

        return licenseMap;
    }


    //加密
    public  String stringObfuscate(long number) {
        // 先进行数字混淆
        long obfuscated = practicalBitOperation(number);
        // 转为字符串后进行Base64编码
        String encoded = Base64.getEncoder().encodeToString(String.valueOf(obfuscated).getBytes());
        return encoded;
    }
   //解密
   public long stringDeobfuscate(String obfuscatedStr) {
        // Base64解码
        byte[] decodedBytes = Base64.getDecoder().decode(obfuscatedStr);
        String numberStr = new String(decodedBytes);
        long obfuscated = Long.parseLong(numberStr);
        // 进行数字反混淆
        return reversePracticalBitOperation(obfuscated);
    }


    /**
     * 更实用的位运算混淆方案
     */
    private  long practicalBitOperation(long x) {
        // 使用线性同余生成器的思想
        long a = 1666525L;
        long c = 101325123L;
        long m = 42948541126L;  // 2^32

        long result = ((x * a + c) % m) & 0xFFFFFFFFL;

        // 再进行位翻转
        result = Long.reverse(result) >>> 32;  // 只取低32位

        return result;
    }

    /**
     * 对应的逆向操作
     */
    private  long reversePracticalBitOperation(long value) {
        // 先逆向位翻转（再次翻转即为原值）
        long step1 = Long.reverse(value) >>> 32;

        // 逆向线性同余运算
        long a = 1666525L;
        long c = 101325123L;
        long m = 42948541126L;

        // 计算a的模逆元
        long aInverse = modInverse(a, m);

        // 逆向运算
        long original = ((step1 - c + m) % m * aInverse) % m;

        return original;
    }

    /**
     * 计算模逆元（扩展欧几里得算法）
     */
    private  long modInverse(long a, long m) {
        long m0 = m;
        long y = 0, x = 1;

        if (m == 1) return 0;

        while (a > 1) {
            long q = a / m;
            long t = m;

            m = a % m;
            a = t;
            t = y;

            y = x - q * y;
            x = t;
        }

        if (x < 0) x += m0;

        return x;
    }

}