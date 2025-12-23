package com.persee.workstation.uv_inexus.controller.factory_inspection;

import com.persee.workstation.uv_inexus.model.R;
import com.persee.workstation.uv_inexus.model.Result;
import com.persee.workstation.uv_inexus.service.concise.measurementService.FSamplePoolConfigService;
import com.persee.workstation.uv_inexus.vo.factory_inspection.SamplePoolInfoVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/*
 * *
 *  * 版权所有 (C) 2025 北京普析通用仪器有限责任公司。保留所有权利。
 *  * 本软件受商业机密法和版权法保护。
 *  * 本代码仅限与北京普析通用仪器有限责任公司签订有效协议的授权用户内部使用。
 *  * 任何未经明确许可的复制、使用、修改、分发或公开均被严格禁止。
 *
 */

@Slf4j
@RestController
@Api("检定校准样品池相关")
@RequestMapping("/com/persee/workstation/uv-inexus/factory-inspection/sample-pool")
public class SamplePoolController {
    @Autowired
    private FSamplePoolConfigService samplePoolConfigService;

    @GetMapping("querySampleAttachmentInfo")
    @ApiOperation("查询附件信息")

    public Result<SamplePoolInfoVO> querySampleAttachmentInfo(@RequestParam String deviceSerialNumber) {
        R<SamplePoolInfoVO> r = samplePoolConfigService.queryCalibrationSampleInfo(deviceSerialNumber);
        if (r.getSuccess()) {
            return Result.ok(r.getData());
        } else {
            return Result.fail(r.getMessage());
        }
    }


}
