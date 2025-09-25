package com.persee.workstation.uv_inexus.service.guide.equalSampleService;

import com.baomidou.mybatisplus.extension.service.IService;
import com.persee.workstation.uv_inexus.dto.concise.common.EqualAndSample;
import com.persee.workstation.uv_inexus.dto.concise.common.EqualSampleParam;
import com.persee.workstation.uv_inexus.model.Result;

import java.util.List;
import java.util.Map;

public interface EqualSampleService extends IService<EqualAndSample> {
    Result<List<Map<String, List<String>>>> addEqualSample(EqualSampleParam equalSampleParam);
}
