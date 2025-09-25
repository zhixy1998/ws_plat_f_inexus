package com.persee.workstation.uv_inexus.service.concise.measurementService;

import com.persee.workstation.uv_inexus.model.R;

import java.util.List;

public interface ConcisePublicUtilService {


    R<Boolean> changeSampleLocation(String taskId, List<Integer> usefulLocation);
}
