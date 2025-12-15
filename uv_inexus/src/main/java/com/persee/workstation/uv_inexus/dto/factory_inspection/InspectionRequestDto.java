package com.persee.workstation.uv_inexus.dto.factory_inspection;

import lombok.Data;

/*
 * *
 *  * 版权所有 (C) 2025 北京普析通用仪器有限责任公司。保留所有权利。
 *  * 本软件受商业机密法和版权法保护。
 *  * 本代码仅限与北京普析通用仪器有限责任公司签订有效协议的授权用户内部使用。
 *  * 任何未经明确许可的复制、使用、修改、分发或公开均被严格禁止。
 *
 */
//单项检测请求
@Data
public class InspectionRequestDto {
    //检测项id
    private String itemId;
    //校零物质id
    private String calibId;
    //检测物id
    private String matId;
}
