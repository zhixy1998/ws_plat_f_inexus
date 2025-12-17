/*
 * *
 *  * 版权所有 (C) 2025 北京普析通用仪器有限责任公司。保留所有权利。
 *  * 本软件受商业机密法和版权法保护。
 *  * 本代码仅限与北京普析通用仪器有限责任公司签订有效协议的授权用户内部使用。
 *  * 任何未经明确许可的复制、使用、修改、分发或公开均被严格禁止。
 *
 */

package com.persee.workstation.uv_inexus.dao.factory_inspection;

import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.persee.workstation.uv_inexus.dto.factory_inspection.QueryCalStdCatsDto;
import com.persee.workstation.uv_inexus.dto.factory_inspection.StandardSubstanceDto;
import com.persee.workstation.uv_inexus.po.factory_inspection.CalArtifacts;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@DS("factory_inspection")
@Mapper
public interface CalArtifactsMapper extends BaseMapper<CalArtifacts> {
    @Update("update cal_artifacts set is_delete = 1 where id = #{id}")
    int updateIsDelete(String id);

    IPage<StandardSubstanceDto> selectCustomDtoPage(@Param("page") Page<StandardSubstanceDto> page, @Param("dto") QueryCalStdCatsDto dto);
}