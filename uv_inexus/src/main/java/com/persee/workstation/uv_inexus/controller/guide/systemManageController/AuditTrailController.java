/*
 * *
 *  * 版权所有 (C) 2025 北京普析通用仪器有限责任公司。保留所有权利。
 *  * 本软件受商业机密法和版权法保护。
 *  * 本代码仅限与北京普析通用仪器有限责任公司签订有效协议的授权用户内部使用。
 *  * 任何未经明确许可的复制、使用、修改、分发或公开均被严格禁止。
 *
 */

package com.persee.workstation.uv_inexus.controller.guide.systemManageController;

import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.persee.workstation.uv_inexus.model.Result; // 复用你项目中已有的Result类
import com.persee.workstation.uv_inexus.po.OperateLog.OperateLogPO;
import com.persee.workstation.uv_inexus.service.guide.OperateLog.OperateLogService;
import com.persee.workstation.uv_inexus.utils.Pagination.PageView;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 系统管理Controller - 操作日志查询接口
 */
@RestController
@RequestMapping("/com/persee/workstation/uv-inexus/log")
@RequiredArgsConstructor
@Slf4j
@Api(tags = "审计追踪-操作日志查询")
public class AuditTrailController {

    // 注入日志服务
    private final OperateLogService operateLogService;

    /**
     * 操作日志分页查询（支持模糊查询）
     * @param pageNum 页码（默认1）
     * @param pageSize 页大小（默认10）
     * @param params 模糊查询参数（可选：username-用户名、module-操作模块、description-操作描述等）
     * @return 分页结果（包含总记录数、当前页数据）
     */
    @GetMapping("/")
    @ApiOperation(value = "日志分页查询", notes = "支持按用户名、操作模块、操作描述模糊查询，默认按记录时间倒序")
    public Result<PageView<OperateLogPO>> getLogPage(
            @ApiParam(value = "页码，默认1", required = false) @RequestParam(defaultValue = "1") int pageNum,
            @ApiParam(value = "页大小，默认10", required = false) @RequestParam(defaultValue = "10") int pageSize,
            @ApiParam(value = "模糊查询参数（如username=admin&module=用户管理）", required = false) @RequestParam Map<String, String> params) {
        
        // 1. 构建MyBatis-Plus分页对象
        Page<OperateLogPO> page = new Page<>(pageNum, pageSize);
        
        // 2. 调用Service查询分页结果
        com.baomidou.mybatisplus.core.metadata.IPage<OperateLogPO> resultPage = 
                operateLogService.findLogPage(page, params);
        
        // 3. 转换为项目统一的PageView格式，返回结果
        return Result.ok(new PageView<>(resultPage));
    }
}