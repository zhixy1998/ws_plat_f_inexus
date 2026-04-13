package com.persee.workstation.uv_inexus.enums.factoryInspection;

import lombok.Getter;

/*
 * *
 *  * 版权所有 (C) 2025 北京普析通用仪器有限责任公司。保留所有权利。
 *  * 本软件受商业机密法和版权法保护。
 *  * 本代码仅限与北京普析通用仪器有限责任公司签订有效协议的授权用户内部使用。
 *  * 任何未经明确许可的复制、使用、修改、分发或公开均被严格禁止。
 *
 */
@Getter
public enum TemplateTypeEnum {
     //出厂检验模板
    OUT_FACTORY_CHECK(2, "出厂检验模板"),
    //检定校准模板
    CALIBRATION_CHECK(1, "检定校准模板"),
    //开箱验证模板
    OPEN_BOX_CHECK(3, "开箱验证模板");
    private final int code;
    private final String desc;
    TemplateTypeEnum(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}
