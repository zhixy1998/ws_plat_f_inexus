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
public enum TemplateKindEnum {
    //1检定校准模板2出厂检验模板3开箱检验模板
    DETECTION_CALIBRATION(1, "检定校准模板"),
    FACTORY_INSPECTION(2, "出厂检验模板"),
    OPEN_BOX_INSPECTION(3, "开箱检验模板");
    private Integer status;
    private String name;

    TemplateKindEnum(Integer status, String name) {
        this.status = status;
        this.name = name;
    }
}
