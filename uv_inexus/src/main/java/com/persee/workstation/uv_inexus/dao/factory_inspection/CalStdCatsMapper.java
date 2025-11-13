package com.persee.workstation.uv_inexus.dao.factory_inspection;

import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.persee.workstation.uv_inexus.po.factory_inspection.CalStdCats;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Update;

@DS("factory_inspection")
@Mapper
public interface CalStdCatsMapper extends BaseMapper<CalStdCats> {
    @Update("update cal_std_cats set is_delete = 1 where id = #{id}")
    int updateIsDelete(String id);
}