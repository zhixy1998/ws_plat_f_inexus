package com.persee.workstation.uv_inexus.dto.sysUserPermissionDto;

import java.util.List;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 前端路由权限DTO
 */
@Data
@ApiModel("前端路由权限结构")
public class RoutePermissionDTO {
    @ApiModelProperty("路由路径（对应前端path）")
    private String path;

    @ApiModelProperty("路由唯一标识（对应前端name）")
    private String name;

    @ApiModelProperty("重定向")
    private String redirect;

    @ApiModelProperty("前端组件路径")
    private String component;

    @ApiModelProperty("路由元信息")
    private RouteMetaDTO meta;

    @ApiModelProperty("子路由列表")
    private List<RoutePermissionDTO> children;

    // 内部类：路由元信息
    @Data
    public static class RouteMetaDTO {
        @ApiModelProperty("路由标题（用于菜单显示）")
        private String title;

        @ApiModelProperty("前端组件路径")
        private String component;

        @ApiModelProperty("是否隐藏（对应前端hidden）")
        private Boolean hidden;

        @ApiModelProperty("是否无需权限（对应前端noAuth）")
        private Boolean noAuth;
    }
}