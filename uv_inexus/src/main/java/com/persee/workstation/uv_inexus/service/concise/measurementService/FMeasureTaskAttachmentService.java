package com.persee.workstation.uv_inexus.service.concise.measurementService;

import com.baomidou.mybatisplus.extension.service.IService;
import com.persee.workstation.uv_inexus.model.R;
import com.persee.workstation.uv_inexus.po.concise.measurementPo.FMeasureTaskAttachment;

/**
 * 任务附属表(FMeasureTaskAttachment)表服务接口
 */
public interface FMeasureTaskAttachmentService extends IService<FMeasureTaskAttachment> {
    R<FMeasureTaskAttachment> queryByTaskId(String taskId);
}
