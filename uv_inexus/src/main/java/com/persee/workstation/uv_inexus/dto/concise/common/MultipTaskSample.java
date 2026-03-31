package com.persee.workstation.uv_inexus.dto.concise.common;

import lombok.Data;

import java.util.List;

@Data
public class MultipTaskSample {
    private List<String> taskIds;
    private String measureType;
    private Integer digit;
    private String deviceSerialNumber;
}
