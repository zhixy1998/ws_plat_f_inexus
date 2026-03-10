/*
 * *
 *  * 版权所有 (C) 2025 北京普析通用仪器有限责任公司。保留所有权利。
 *  * 本软件受商业机密法和版权法保护。
 *  * 本代码仅限与北京普析通用仪器有限责任公司签订有效协议的授权用户内部使用。
 *  * 任何未经明确许可的复制、使用、修改、分发或公开均被严格禁止。
 *
 */

package com.persee.workstation.uv_inexus.enums.measurementEnums;

import lombok.Getter;

@Getter
public enum TableHeadILocateEnums {
    system("system", "系统预置表头"),
    sample("sample", "样品表头"),
    location("location", "位置"),
    data("data","测量页用户新增表头"),
    curve("curve", "曲线表头，用于参数绑定"),
    measure("measure", "后端添加测量表头"),
    other("other", "其他表头,如采样时间，备注等"),
    remark("remark", "备注"),
    analyze("analyze", "方法来的分析表头");

    private final String identity;

    private final String name;


    TableHeadILocateEnums(String identity, String name){
        this.identity = identity;
        this.name = name;
    }

    public static String getName(String identity) {
        for (TableHeadILocateEnums c : TableHeadILocateEnums.values()) {
            if (c.getIdentity().equals(identity)) {
                return c.name;
            }
        }
        return null;
    }
}
