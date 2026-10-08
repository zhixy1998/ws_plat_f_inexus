/*
 * *
 *  * 版权所有 (C) 2025 北京普析通用仪器有限责任公司。保留所有权利。
 *  * 本软件受商业机密法和版权法保护。
 *  * 本代码仅限与北京普析通用仪器有限责任公司签订有效协议的授权用户内部使用。
 *  * 任何未经明确许可的复制、使用、修改、分发或公开均被严格禁止。
 *
 */

package com.persee.workstation.uv_inexus.service.concise.measurementService;


import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.persee.workstation.uv_inexus.dto.concise.common.FMeasureTaskDTO;
import com.persee.workstation.uv_inexus.dto.concise.common.FQueryDataDTO;
import com.persee.workstation.uv_inexus.dto.concise.common.SelectTaskDto;
import com.persee.workstation.uv_inexus.model.Result;
import com.persee.workstation.uv_inexus.po.concise.measurementPo.FMeasureTask;

/**
 * 测量任务表(FMeasureTask)表服务接口
 */
public interface FMeasureTaskService extends IService<FMeasureTask> {

    boolean save(FMeasureTask entity);

    boolean save(FMeasureTask entity, FQueryDataDTO taskAttachment);

    Result<Page<FMeasureTaskDTO>> queryByName(SelectTaskDto selectTaskDto);
}

