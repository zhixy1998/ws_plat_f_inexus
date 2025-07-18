package com.persee.workstation.uv_inexus.util.pythonUtil;

import com.alibaba.fastjson.JSONObject;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

public class RestUtil {

    private static final String baseUrl = "http://192.168.32.62:8002/";

    public static String sum(String formula, String identity, String values) {
        RestTemplate restTemplate = new RestTemplate();
        String url = baseUrl + "algebraic_operations";

        JSONObject jsonObject = new JSONObject();
        jsonObject.put("formula_string", formula);
        jsonObject.put("independent_ariable_string", identity);
        jsonObject.put("independent_ariable_value", values);

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
}
