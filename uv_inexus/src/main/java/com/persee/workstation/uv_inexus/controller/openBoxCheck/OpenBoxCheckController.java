package com.persee.workstation.uv_inexus.controller.openBoxCheck;

import com.persee.workstation.uv_inexus.dto.factory_inspection.InspectionRowDto;
import com.persee.workstation.uv_inexus.dto.factory_inspection.TemplateCreationRequest;
import com.persee.workstation.uv_inexus.model.Result;
import com.persee.workstation.uv_inexus.service.openBoxCheck.OpenBoxCheckService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

/*
 * *
 *  * 版权所有 (C) 2025 北京普析通用仪器有限责任公司。保留所有权利。
 *  * 本软件受商业机密法和版权法保护。
 *  * 本代码仅限与北京普析通用仪器有限责任公司签订有效协议的授权用户内部使用。
 *  * 任何未经明确许可的复制、使用、修改、分发或公开均被严格禁止。
 *
 */
@RestController
@RequestMapping("/com/persee/workstation/uv-inexus/factory-inspection/open-box-check")
@Api(tags = "开箱验证")
@Slf4j
public class OpenBoxCheckController {
    @Autowired
    private OpenBoxCheckService openBoxCheckService;
    /**
     * 验收报告
     */
    @RequestMapping("/acceptanceReport")
    @ApiOperation("验收报告")
    public Result<InspectionRowDto> openBoxCheck(@RequestParam String taskId) {
        return openBoxCheckService.openBoxCheck(taskId);
    }
    /**
     * 修改开箱验证对应的模板，形成新模板并绑定开箱任务
     */
    //根据原有的内置模板进行新建模板
    @PostMapping("/template/create")
    @ApiOperation("创建新结构化模板")
    public Result<String> createNewTemplate(@RequestBody @Valid TemplateCreationRequest request) {
        try {
            // 参数校验
            if (request == null) {
                return Result.fail("00618005");
            }
            // 调用服务创建模板
            Result<String> result = openBoxCheckService.editUnboxingTemplate(request);

            // 记录日志
            if (result.getCode() == 200) {
                log.info("成功创建模板: {}", request.getTemplateCode());
            } else {
                log.error("创建模板失败: {}, 错误信息: {}", request.getTemplateCode(), result.getMessage());
            }

            return result;
        } catch (Exception e) {
            log.error("创建模板时发生异常: {}", request.getTemplateCode(), e);
            return Result.fail("00618006", e.getMessage());
        }
    }

}
