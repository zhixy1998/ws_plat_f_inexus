package com.persee.workstation.uv_inexus.service.concise.measurementService.impl;

import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.persee.workstation.uv_inexus.dao.concise.measurementDao.FMeasureTaskAttachmentMapper;
import com.persee.workstation.uv_inexus.model.R;
import com.persee.workstation.uv_inexus.po.concise.measurementPo.FMeasureTaskAttachment;
import com.persee.workstation.uv_inexus.service.concise.measurementService.FMeasureTaskAttachmentService;
import org.springframework.stereotype.Service;

/**
 * 测量任务附件服务实现类(FMeasureTaskAttachment)
 */
@Service
("FMeasureTaskAttachmentService")
@DS("concise")
public class FMeasureTaskAttachmentServiceImpl extends ServiceImpl<FMeasureTaskAttachmentMapper, FMeasureTaskAttachment> implements FMeasureTaskAttachmentService {
    @Override
    public R<FMeasureTaskAttachment> queryByTaskId(String taskId) {
        LambdaQueryWrapper<FMeasureTaskAttachment> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(FMeasureTaskAttachment::getTaskId, taskId);
        FMeasureTaskAttachment attachment = this.getOne(queryWrapper);
        if (attachment != null) {
            return R.ok(attachment);
        }
        return R.fail("未找到记录");
    }
}
