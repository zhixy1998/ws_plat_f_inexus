/*
 * *
 *  * 版权所有 (C) 2025 北京普析通用仪器有限责任公司。保留所有权利。
 *  * 本软件受商业机密法和版权法保护。
 *  * 本代码仅限与北京普析通用仪器有限责任公司签订有效协议的授权用户内部使用。
 *  * 任何未经明确许可的复制、使用、修改、分发或公开均被严格禁止。
 *
 */

package com.persee.workstation.uv_inexus.constant;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.net.URL;

@Component
@Data
public class PythonPathConstant {

    @Value("${python-code.baseUrl}")
    private String httpUrl;

    // 最小二乘接口路径
    private String leastSquareFit;
    // 计算浓度或吸收率接口路径
    private String concentrationOrAbsorptionRate;
    // 代数运算接口路径
    private String algebraicOperation;
    // 寻峰接口路径
    private String peakSearching;
    // 寻谷接口路径
    private String valleySearching;
    // 寻峰寻谷接口路径
    private String peakSearchingAndValleySearching;
    // 光谱四则运算接口路径
    private String spectrumFourOperation;
    // 光谱变换接口路径
    private String spectrumTransformation;
    // 消除台阶
    private String removeStepOrSpikes;
    // 消除尖刺
    private String removeSpikes;
    // 微分
    private String differentiated;
    // 平滑
    private String smoothed;

    @PostConstruct
    public void init() {
        // 验证基础配置
        validateBaseUrl();

        // 初始化所有路径
        this.leastSquareFit = buildUrl("least_squares_fitting");
        this.concentrationOrAbsorptionRate = buildUrl("calculate_concentration_or_abs");
        this.algebraicOperation = buildUrl("algebraic_operations");
        this.peakSearching = buildUrl("get_peak");
        this.valleySearching = buildUrl("get_valley");
        this.peakSearchingAndValleySearching = buildUrl("get_peaks_and_valleys");
        this.spectrumFourOperation = buildUrl("spectral_arithmetic_operations");
        this.spectrumTransformation = buildUrl("spectrum_transform");
        this.removeStepOrSpikes = buildUrl("detect_step_skipe");
        this.removeSpikes = buildUrl("remove_spikes");
        this.differentiated = buildUrl("differentiated");
        this.smoothed = buildUrl("smoothed");

        // 输出初始化日志（生产环境可删除）
        logInitializedUrls();
    }

    /**
     * 验证基础URL配置
     */
    private void validateBaseUrl() {
        if (httpUrl == null || httpUrl.trim().isEmpty()) {
            throw new IllegalStateException("Python服务基础URL未配置，请检查application.yml中的python-code.baseUrl配置");
        }

        // 去除首尾空格
        httpUrl = httpUrl.trim();

        // 验证URL格式
        if (!httpUrl.startsWith("http://") && !httpUrl.startsWith("https://")) {
            throw new IllegalArgumentException("Python服务基础URL必须以http://或https://开头，当前配置: " + httpUrl);
        }

        // 确保URL以斜杠结尾
        if (!httpUrl.endsWith("/")) {
            httpUrl = httpUrl + "/";
        }
    }

    /**
     * 构建完整的URL
     */
    private String buildUrl(String endpoint) {
        String url = httpUrl + endpoint;

        // 验证URL格式
        try {
            new URL(url).toURI();
            return url;
        } catch (Exception e) {
            throw new IllegalArgumentException("构建Python服务URL失败: " + url + ", 错误: " + e.getMessage());
        }
    }

    /**
     * 输出初始化日志（用于调试，生产环境可移除）
     */
    private void logInitializedUrls() {
        System.out.println("=== Python服务URL初始化完成 ===");
        System.out.println("基础URL: " + httpUrl);
        System.out.println("最小二乘拟合: " + leastSquareFit);
        System.out.println("寻峰接口: " + peakSearching);
        System.out.println("寻谷接口: " + valleySearching);
        System.out.println("消除台阶: " + removeStepOrSpikes);
        System.out.println("消除尖刺: " + removeSpikes);
        System.out.println("=== 初始化结束 ===");
    }

    /**
     * 获取基础URL（如果需要）
     */
    public String getBaseUrl() {
        return httpUrl;
    }
}