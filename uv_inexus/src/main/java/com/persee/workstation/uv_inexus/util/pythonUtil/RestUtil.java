/*
 * *
 *  * 版权所有 (C) 2025 北京普析通用仪器有限责任公司。保留所有权利。
 *  * 本软件受商业机密法和版权法保护。
 *  * 本代码仅限与北京普析通用仪器有限责任公司签订有效协议的授权用户内部使用。
 *  * 任何未经明确许可的复制、使用、修改、分发或公开均被严格禁止。
 *
 */

package com.persee.workstation.uv_inexus.util.pythonUtil;

import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@Service("RestUtil")
@Slf4j
public class RestUtil {
    @Value("${python-code.baseUrl}")
    String baseUrl;

//    private static final String baseUrl = "http://192.168.51.38:8002/";

    public String sum(String formula, String identity, String values) {

        // 创建带有超时设置的 RestTemplate
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();

        // 设置连接超时（单位：毫秒）
        factory.setConnectTimeout(3000);    // 5秒连接超时
        factory.setReadTimeout(5000);       // 10秒读取超时

        RestTemplate restTemplate = new RestTemplate(factory);

        String url = baseUrl + "algebraic_operations";

        JSONObject jsonObject = new JSONObject();
        jsonObject.put("formula_string", formula);
        jsonObject.put("independent_variable_string", identity);
        jsonObject.put("independent_variable_value", values);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<String> entity = new HttpEntity<>(jsonObject.toJSONString(), headers);

        try {
            ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);
            return response.getBody();
        } catch (Exception e) {
            log.info("计算异常：{}", e.getMessage());
            JSONObject error = new JSONObject();
            error.put("code", "404");

            return error.toJSONString();
        }

    }

    public String dataFit(FittingParam param) {

        // 创建带有超时设置的 RestTemplate
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();

        // 设置连接超时（单位：毫秒） // 5秒连接超时
        factory.setConnectTimeout(3000);
        // 10秒读取超时
        factory.setReadTimeout(5000);


        RestTemplate restTemplate = new RestTemplate(factory);
        String url = baseUrl + "least_squares_fitting";

        String jsonString = JSONObject.toJSONString(param);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<String> entity = new HttpEntity<>(jsonString, headers);

        try {
            ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);
            return response.getBody();
        } catch (Exception e) {
            log.info("曲线拟合：{}", e.getMessage());
            JSONObject error = new JSONObject();
            error.put("code", "404");

            return error.toJSONString();
        }

    }

    //根据吸光度计算拟合浓度
    public String concentrationCalculate(List<Double> coefficient, Double value, Integer mode) {
        // 创建带有超时设置的 RestTemplate
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();

        // 设置连接超时（单位：毫秒） // 5秒连接超时
        factory.setConnectTimeout(3000);
        // 10秒读取超时
        factory.setReadTimeout(5000);
        RestTemplate restTemplate = new RestTemplate(factory);
        String url = baseUrl + "calculate_concentration_or_abs";

        JSONObject jsonObject = new JSONObject();
        jsonObject.put("coeffs", coefficient);
        jsonObject.put("value", value);
        jsonObject.put("mode", mode);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<String> entity = new HttpEntity<>(jsonObject.toJSONString(), headers);
        try {
            ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);
            if (response.getStatusCode().is2xxSuccessful()) {
                return response.getBody();
            } else {
                log.info("HTTP request failed with status code: {}", response.getStatusCode());
                return null;
            }
        } catch (Exception e) {
            log.info("计算拟合浓度异常：{}", e.getMessage());
            return null;
        }
    }
}
