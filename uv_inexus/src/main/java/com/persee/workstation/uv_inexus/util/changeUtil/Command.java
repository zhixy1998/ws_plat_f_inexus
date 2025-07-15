package com.persee.workstation.uv_inexus.util.changeUtil;

import lombok.Data;

@Data
public class Command {
    private String cmdId;
    private Object[] params;
}
