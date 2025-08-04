package com.persee.workstation.uv_inexus.util.changeUtil;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ChangeParam {
    // 波长
    private String wl;

    // R0
    private String r;

    // S0
    private String s;

    // D0
    private String d;
}
