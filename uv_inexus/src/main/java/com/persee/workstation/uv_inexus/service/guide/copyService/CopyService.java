package com.persee.workstation.uv_inexus.service.guide.copyService;

import com.persee.workstation.uv_inexus.dto.concise.common.CopyParam;
import com.persee.workstation.uv_inexus.model.Result;

public interface CopyService {

    Result<String> copy(CopyParam copyParam);

}
