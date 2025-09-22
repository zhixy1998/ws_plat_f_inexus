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
