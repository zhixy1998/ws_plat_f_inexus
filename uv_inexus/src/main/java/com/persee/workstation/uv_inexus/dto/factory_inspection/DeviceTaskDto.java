package com.persee.workstation.uv_inexus.dto.factory_inspection;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/*
 * *
 *  * 版权所有 (C) 2025 北京普析通用仪器有限责任公司。保留所有权利。
 *  * 本软件受商业机密法和版权法保护。
 *  * 本代码仅限与北京普析通用仪器有限责任公司签订有效协议的授权用户内部使用。
 *  * 任何未经明确许可的复制、使用、修改、分发或公开均被严格禁止。
 *
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class DeviceTaskDto implements Serializable {
    private static final long serialVersionUID = 1L;
    private String deviceSerialNumber;
    private String taskId;
//    private String taskName;
    private String templateCode;
    private String status;
}
