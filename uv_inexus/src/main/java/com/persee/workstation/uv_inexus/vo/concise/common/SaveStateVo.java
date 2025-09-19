package com.persee.workstation.uv_inexus.vo.concise.common;

import lombok.Data;

@Data
public class SaveStateVo {
    private String taskName;
    private String methodName;
    private Boolean saveState;
}
