/*
 * *****************************************************************************************************
 * Module name:    **.**
 *
 * Copyright (C) 2025 北京普析通用仪器有限责任公司 All Rights Reserved.
 *                此源代码仅限在公司内部使用。未经公司事先书面许可，任何单位和个人不得以任何方式复制、传播本代码的任何部分。
 * Description:
 *
 *
 * *************************************************************************************
 */

package com.persee.workstation.uv_inexus.dao.guide.equalSampleMapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.persee.workstation.uv_inexus.dto.concise.common.EqualAndSample;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;

@Mapper
public interface EqualSampleMapper extends BaseMapper<EqualAndSample> {

    @Insert("INSERT INTO t800_uv_inexus_concise.f_t_measure_equal_sample " +
            "(sample_id, task_id, equal_sample_key, create_user, create_time) " +
            "VALUES (#{sampleId}, #{taskId}, #{equalSampleKey}, #{createBy}, #{created})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertin(EqualAndSample equalAndSample);
}
