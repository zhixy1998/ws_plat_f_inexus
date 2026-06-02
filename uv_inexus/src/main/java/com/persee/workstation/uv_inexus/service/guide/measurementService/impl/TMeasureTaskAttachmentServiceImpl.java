/*
 * *
 *  * 版权所有 (C) 2025 北京普析通用仪器有限责任公司。保留所有权利。
 *  * 本软件受商业机密法和版权法保护。
 *  * 本代码仅限与北京普析通用仪器有限责任公司签订有效协议的授权用户内部使用。
 *  * 任何未经明确许可的复制、使用、修改、分发或公开均被严格禁止。
 *
 */

package com.persee.workstation.uv_inexus.service.guide.measurementService.impl;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.persee.workstation.uv_inexus.dao.guide.measurementDao.TMeasureTaskAttachmentMapper;
import com.persee.workstation.uv_inexus.dto.concise.common.FQueryDataDTO;
import com.persee.workstation.uv_inexus.i18n.I18nMessageSource;
import com.persee.workstation.uv_inexus.model.R;
import com.persee.workstation.uv_inexus.po.guide.measurementPo.TMeasureTaskAttachment;
import com.persee.workstation.uv_inexus.service.guide.measurementService.TMeasureTaskAttachmentService;
import org.springframework.stereotype.Service;

@Service
("tMeasureTaskAttachmentService")
public class TMeasureTaskAttachmentServiceImpl extends ServiceImpl<TMeasureTaskAttachmentMapper, TMeasureTaskAttachment> implements TMeasureTaskAttachmentService {
    @Override
    public R<TMeasureTaskAttachment> queryByTaskId(String taskId) {

        LambdaQueryWrapper<TMeasureTaskAttachment> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(TMeasureTaskAttachment::getTaskId, taskId);
        TMeasureTaskAttachment attachment = this.getOne(queryWrapper);
        if (attachment != null) {
            return R.ok(attachment);
        }
        //未找到记录
        return R.fail("00100025");
    }

    @Override
    public Integer queryLastDigit(String taskId) {
        LambdaQueryWrapper<TMeasureTaskAttachment> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(TMeasureTaskAttachment::getTaskId, taskId);
        TMeasureTaskAttachment attachment = this.getOne(queryWrapper);

        if (attachment != null && attachment.getDataParam() != null) {
            FQueryDataDTO dataDTO = JSON.parseObject(attachment.getDataParam(), FQueryDataDTO.class);
            if (dataDTO.getDigit() != null) {
                return dataDTO.getDigit();
            }
        }
        return null;
    }
}
