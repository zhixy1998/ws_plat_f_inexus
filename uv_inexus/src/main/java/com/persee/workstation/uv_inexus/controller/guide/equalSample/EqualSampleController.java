/*
 * *
 *  * 版权所有 (C) 2025 北京普析通用仪器有限责任公司。保留所有权利。
 *  * 本软件受商业机密法和版权法保护。
 *  * 本代码仅限与北京普析通用仪器有限责任公司签订有效协议的授权用户内部使用。
 *  * 任何未经明确许可的复制、使用、修改、分发或公开均被严格禁止。
 *
 */

package com.persee.workstation.uv_inexus.controller.guide.equalSample;

import com.persee.workstation.uv_inexus.dto.concise.common.EqualSampleParam;
import com.persee.workstation.uv_inexus.model.Result;
import com.persee.workstation.uv_inexus.service.guide.equalSampleService.EqualSampleService;
import io.swagger.annotations.Api;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@Api("平行样")
@RequestMapping("/com/persee/workstation/uv-inexus/dev/equalSample")
public class EqualSampleController {
    @Autowired
    EqualSampleService equalSampleService;

    @PostMapping("addEqualSample")
    public Result<List<Map<String, List<String>>>> addEqualSample(@RequestBody EqualSampleParam equalSampleParam) {
       return equalSampleService.addEqualSample(equalSampleParam);
    }

}
