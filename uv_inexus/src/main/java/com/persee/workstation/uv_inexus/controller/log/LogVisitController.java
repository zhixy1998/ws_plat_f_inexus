/*
 * *
 *  * 版权所有 (C) 2025 北京普析通用仪器有限责任公司。保留所有权利。
 *  * 本软件受商业机密法和版权法保护。
 *  * 本代码仅限与北京普析通用仪器有限责任公司签订有效协议的授权用户内部使用。
 *  * 任何未经明确许可的复制、使用、修改、分发或公开均被严格禁止。
 *
 */

package com.persee.workstation.uv_inexus.controller.log;

import com.persee.workstation.uv_inexus.config.logging.SysLog;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 前端访问页面日志记录控制器
 * 仅用于记录前端访问了什么页面，action 与 module 由前端请求参数传入
 */
@RestController
@RequestMapping("/log")
@Api(tags = "前端访问日志接口")
public class LogVisitController {

    /**
     * 记录前端访问页面
     * 使用 void 返回，不影响前端流程；日志由切面入库
     * @param action 访问动作（例如：进入页面、离开页面等）
     * @param module 业务模块（例如：系统管理、用户管理等）
     */
    @PostMapping("/visit")
    @ApiOperation("记录前端访问页面")
    @SysLog(action = "", module = "", recordParams = false, recordResult = false)
    public void visit(@RequestParam String action, @RequestParam String module) {
        // 方法体无需实现，日志由切面 SysLogAspect 统一处理并写入数据库
    }
}
