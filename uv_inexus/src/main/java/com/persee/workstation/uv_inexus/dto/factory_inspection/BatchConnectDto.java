package com.persee.workstation.uv_inexus.dto.factory_inspection;

import com.persee.workstation.uv_inexus.util.deviceUtil.param.SearchInfo;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/*
 * *
 *  * 版权所有 (C) 2025 北京普析通用仪器有限责任公司。保留所有权利。
 *  * 本软件受商业机密法和版权法保护。
 *  * 本代码仅限与北京普析通用仪器有限责任公司签订有效协议的授权用户内部使用。
 *  * 任何未经明确许可的复制、使用、修改、分发或公开均被严格禁止。
 *
 */
@Data
@Slf4j
public class BatchConnectDto {
    private List<SearchInfo> searchInfos;
    private String updateBy;
    private String templateCode;
}
