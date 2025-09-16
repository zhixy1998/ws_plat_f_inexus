package com.persee.workstation.uv_inexus.dao.guide.systemManageDao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.persee.workstation.uv_inexus.po.guide.systemManagePo.SystemManagePO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface SystemManageDAO extends BaseMapper<SystemManagePO> {

    @Update("update t_system_manage set language=#{language} where id=11111111")
    Integer changeLanguage(@Param("language") String language);

    @Update("update t_system_manage set configuration=#{configJson} where id=11111111")
    Integer setConfiguration(@Param("configJson") String configJson);

    @Update("update t_system_manage set remote_data_access =#{remoteDataAccess},remote_control=#{remoteControl} where id=11111111")
    Integer longDistanceConfig(@Param("remoteDataAccess")Integer remoteDataAccess,@Param("remoteControl")Integer remoteControl);
}
