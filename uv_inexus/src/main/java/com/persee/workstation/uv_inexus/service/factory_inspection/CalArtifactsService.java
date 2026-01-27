/*
 * *
 *  * 版权所有 (C) 2025 北京普析通用仪器有限责任公司。保留所有权利。
 *  * 本软件受商业机密法和版权法保护。
 *  * 本代码仅限与北京普析通用仪器有限责任公司签订有效协议的授权用户内部使用。
 *  * 任何未经明确许可的复制、使用、修改、分发或公开均被严格禁止。
 *
 */

package com.persee.workstation.uv_inexus.service.factory_inspection;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.persee.workstation.uv_inexus.dto.factory_inspection.BatchDelDto;
import com.persee.workstation.uv_inexus.dto.factory_inspection.CalArtifactsSaveDto;
import com.persee.workstation.uv_inexus.dto.factory_inspection.QueryCalStdCatsDto;
import com.persee.workstation.uv_inexus.dto.factory_inspection.StandardSubstanceDto;
import com.persee.workstation.uv_inexus.model.Result;
import com.persee.workstation.uv_inexus.po.factory_inspection.CalArtifacts;


public interface CalArtifactsService extends IService<CalArtifacts> {

    /**
     * 新增
     * @param calArtifacts
     * @return
     */
    Result addCalArtifacts(CalArtifactsSaveDto calArtifacts);

    /**
     * 删除
     * @param id
     * @return
     */
    boolean delCalArtifacts(String id);

    /**
     * 批量查询
     * @param dto
     * @return
     */
    Result<IPage<StandardSubstanceDto>> listCalArtifacts(QueryCalStdCatsDto dto);

    /**
     * 批量删除
     * @param ids
     * @return
     */
    Result<String> batchDelCalStdCats(BatchDelDto ids);
}
