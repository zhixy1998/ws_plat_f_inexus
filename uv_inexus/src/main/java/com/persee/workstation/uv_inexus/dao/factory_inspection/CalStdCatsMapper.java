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
import com.persee.workstation.uv_inexus.dto.factory_inspection.MaterialStatisticsDto;
import com.persee.workstation.uv_inexus.po.factory_inspection.CalStdCats;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@DS("factory_inspection")
@Mapper
public interface CalStdCatsMapper extends BaseMapper<CalStdCats> {
    @Update("update cal_std_cats set is_delete = 1 where id = #{id}")
    int updateIsDelete(String id);

    @Select("SELECT tcsc.substance_name AS name,COUNT(tca.id) AS value FROM t_cal_std_cats tcsc LEFT JOIN  t_cal_artifacts tca  ON tcsc.id = tca.category_id  AND tca.is_delete = '0'  WHERE tcsc.is_delete = '0' GROUP BY tcsc.substance_name")
    List<MaterialStatisticsDto> getCalStdCatsStats();
}