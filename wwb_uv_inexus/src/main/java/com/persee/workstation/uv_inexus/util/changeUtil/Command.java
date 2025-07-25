package com.persee.workstation.uv_inexus.util.changeUtil;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Command {
    private String cmdId;
    private Object[] params;
}
