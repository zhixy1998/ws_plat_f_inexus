/*
 * *
 *  * 版权所有 (C) 2025 北京普析通用仪器有限责任公司。保留所有权利。
 *  * 本软件受商业机密法和版权法保护。
 *  * 本代码仅限与北京普析通用仪器有限责任公司签订有效协议的授权用户内部使用。
 *  * 任何未经明确许可的复制、使用、修改、分发或公开均被严格禁止。
 *
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
