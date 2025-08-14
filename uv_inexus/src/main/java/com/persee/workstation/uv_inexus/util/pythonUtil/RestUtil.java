package com.persee.workstation.uv_inexus.util.pythonUtil;

import com.alibaba.fastjson.JSONObject;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.util.List;

public class RestUtil {

    private static final String baseUrl = "http://192.168.51.26:8000/";

    public static String sum(String formula, String identity, String values) {
        RestTemplate restTemplate = new RestTemplate();
        String url = baseUrl + "algebraic_operations";

        JSONObject jsonObject = new JSONObject();
        jsonObject.put("formula_string", formula);
        jsonObject.put("independent_variable_string", identity);
        jsonObject.put("independent_variable_value", values);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<String> entity = new HttpEntity<>(jsonObject.toJSONString(), headers);

        ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);

        return response.getBody();
    }

    public static String dataFit(FittingParam param) {
        RestTemplate restTemplate = new RestTemplate();
        String url = baseUrl + "least_squares_fitting";

        String jsonString = JSONObject.toJSONString(param);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<String> entity = new HttpEntity<>(jsonString, headers);

        ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);
        return response.getBody();

    }

    //根据吸光度计算拟合浓度
    public static String concentrationCalculate(List<Double> coefficient, Double value, Integer mode) {
        RestTemplate restTemplate = new RestTemplate();
        String url = baseUrl + "calculate_concentration_or_abs";

        JSONObject jsonObject = new JSONObject();
        jsonObject.put("coeffs", coefficient);
        jsonObject.put("value", value);
        jsonObject.put("mode", mode);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<String> entity = new HttpEntity<>(jsonObject.toJSONString(), headers);

        ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);
        return response.getBody();
    }
}
