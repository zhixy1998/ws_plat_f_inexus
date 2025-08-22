package com.persee.workstation.uv_inexus.service.measurementService;

import com.baomidou.mybatisplus.extension.service.IService;
import com.persee.workstation.uv_inexus.model.R;
import com.persee.workstation.uv_inexus.po.measurementPo.TMeasureTaskAttachment;
/**
 * 任务附属表(TMeasureTaskAttachment)表服务接口
 *
 * @author makejava
 * @since 2025-06-12 10:05:08
 */
public interface TMeasureTaskAttachmentService extends IService<TMeasureTaskAttachment> {

    R<TMeasureTaskAttachment> queryByTaskId(String taskId);
}
