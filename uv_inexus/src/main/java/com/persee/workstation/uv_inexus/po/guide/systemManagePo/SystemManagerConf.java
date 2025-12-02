/*
 * *
 *  * 版权所有 (C) 2025 北京普析通用仪器有限责任公司。保留所有权利。
 *  * 本软件受商业机密法和版权法保护。
 *  * 本代码仅限与北京普析通用仪器有限责任公司签订有效协议的授权用户内部使用。
 *  * 任何未经明确许可的复制、使用、修改、分发或公开均被严格禁止。
 *
 */

package com.persee.workstation.uv_inexus.po.guide.systemManagePo;

import com.persee.workstation.uv_inexus.vo.systemManageVo.ConfigVO;
import com.persee.workstation.uv_inexus.vo.systemManageVo.LongDistanceConfigVO;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
public class SystemManagerConf {
    @ApiModelProperty("语言")
    private String language;
    @ApiModelProperty("认证物联代理配置信息")
    private ConfigVO config;
    @ApiModelProperty("远程访问配置")
    private LongDistanceConfigVO longDistanceConfigVO;
    @ApiModelProperty("经典模式或者向导模式开关  1：经典模式 2：向导模式")
    private Integer modeType;

}
