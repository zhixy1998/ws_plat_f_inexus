package com.persee.workstation.uv_inexus.po.guide.systemManagePo;

import com.persee.workstation.uv_inexus.vo.systemManageVo.ConfigVO;
import com.persee.workstation.uv_inexus.vo.systemManageVo.LongDistanceConfigVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiModel;
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
}
