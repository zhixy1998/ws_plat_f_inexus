/*
 * *
 *  * 版权所有 (C) 2025 北京普析通用仪器有限责任公司。保留所有权利。
 *  * 本软件受商业机密法和版权法保护。
 *  * 本代码仅限与北京普析通用仪器有限责任公司签订有效协议的授权用户内部使用。
 *  * 任何未经明确许可的复制、使用、修改、分发或公开均被严格禁止。
 *
 */

package com.persee.workstation.uv_inexus.dto.sysUserPermissionDto;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 用户新增/修改数据传输对象
 */
@Data
@ApiModel(description = "用户新增/修改DTO")
public class UserSaveDTO {

    @ApiModelProperty(value = "用户ID（修改时必传，新增时不传）")
    private String id;

    @ApiModelProperty(value = "用户名", required = true)
    @NotBlank(message = "用户名不能为空")
    @Size(min = 3, max = 50, message = "用户名长度必须在3-50个字符之间")
    private String username;

    @ApiModelProperty(value = "密码（新增时必传，修改时可选）")
    @Size(min = 6, message = "密码长度不能少于6个字符")
    private String password;

    @ApiModelProperty(value = "真实姓名")
    private String realName;

    @ApiModelProperty(value = "手机号")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;

    @ApiModelProperty(value = "邮箱")
    @Email(message = "邮箱格式不正确")
    private String email;

    @ApiModelProperty(value = "角色id")
    @Pattern(regexp = "^[0-9]+$", message = "角色ID必须为数字")
    private String roleId;

    @ApiModelProperty(value = "状态（1-启用，0-禁用）", required = true)
    private Integer status;

    // 新增：关联的小组ID（前端传递，用于绑定用户和小组）
    @ApiModelProperty(value = "所属小组ID", required = true)
    @NotBlank(message = "小组ID不能为空")  // 如果用户必须属于一个小组，添加非空校验
    private String groupId;
}