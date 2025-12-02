package com.persee.workstation.uv_inexus.dao.guide.softwareActivationDao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.persee.workstation.uv_inexus.po.guide.softwareActivation.EncryptedConfigPO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface RemoteAccessMapper extends BaseMapper<EncryptedConfigPO> {
}
