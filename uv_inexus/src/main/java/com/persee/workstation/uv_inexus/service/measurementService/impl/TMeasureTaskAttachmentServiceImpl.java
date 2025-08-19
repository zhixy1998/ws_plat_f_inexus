package com.persee.workstation.uv_inexus.service.measurementService.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.persee.workstation.uv_inexus.dao.measurementDao.TMeasureTaskAttachmentDao;
import com.persee.workstation.uv_inexus.po.measurementPo.TMeasureTaskAttachment;
import com.persee.workstation.uv_inexus.service.measurementService.TMeasureTaskAttachmentService;
import org.springframework.stereotype.Service;

@Service
("tMeasureTaskAttachmentService")
public class TMeasureTaskAttachmentServiceImpl extends ServiceImpl<TMeasureTaskAttachmentDao, TMeasureTaskAttachment> implements TMeasureTaskAttachmentService {
}
