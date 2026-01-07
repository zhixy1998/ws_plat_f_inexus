package com.persee.workstation.uv_inexus.service.guide.measurementService;

import com.persee.workstation.uv_inexus.model.R;

import java.util.List;

public interface PublicUtilService {
    R<Boolean> changeSampleLocation(String taskId, List<Integer> usefulLocation);
}
