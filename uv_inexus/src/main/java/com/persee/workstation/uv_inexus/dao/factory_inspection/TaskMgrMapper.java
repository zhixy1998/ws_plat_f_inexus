package com.persee.workstation.uv_inexus.dao.factory_inspection;

import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.persee.workstation.uv_inexus.po.factory_inspection.TaskManagement;
import org.apache.ibatis.annotations.Mapper;

/**
 * 任务管理表(task_management)数据Mapper
 *
 * @author 
 * @since 2025-12-09 18:55:53
 * @description 
*/
@DS("factory_inspection")
@Mapper
public interface TaskMgrMapper extends BaseMapper<TaskManagement> {

}
