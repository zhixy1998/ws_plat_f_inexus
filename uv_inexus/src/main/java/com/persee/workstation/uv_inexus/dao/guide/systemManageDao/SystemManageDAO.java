/*
 * *
 *  * 版权所有 (C) 2025 北京普析通用仪器有限责任公司。保留所有权利。
 *  * 本软件受商业机密法和版权法保护。
 *  * 本代码仅限与北京普析通用仪器有限责任公司签订有效协议的授权用户内部使用。
 *  * 任何未经明确许可的复制、使用、修改、分发或公开均被严格禁止。
 *
 */

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
