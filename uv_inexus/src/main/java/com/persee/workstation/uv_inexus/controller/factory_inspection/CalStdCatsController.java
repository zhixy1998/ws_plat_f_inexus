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

package com.persee.workstation.uv_inexus.controller.factory_inspection;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.persee.workstation.uv_inexus.dto.factory_inspection.CalStdCatsSaveDto;
import com.persee.workstation.uv_inexus.dto.factory_inspection.QueryCalStdCatsDto;
import com.persee.workstation.uv_inexus.model.Result;
import com.persee.workstation.uv_inexus.po.factory_inspection.CalStdCats;
import com.persee.workstation.uv_inexus.service.factory_inspection.CalStdCatsService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;


import javax.validation.Valid;
import java.util.Map;


@RestController
@Api("标准物质类型管理")
@RequestMapping("/com/persee/workstation/uv-inexus/factory-inspection/measurement-parameter")
public class CalStdCatsController {
    @Autowired
    private CalStdCatsService calStdService;
    /**
     * 添加编辑标准物种类
     * @param
     * @return
     */
    @PostMapping("/saveCalStdCats")
    @ApiOperation("添加标准物种类")
    public Result addCalStdCats(@Valid @RequestBody CalStdCatsSaveDto cats) {
        return calStdService.addCalStdCats(cats);
    }
//    /**
//     * 修改标准物种类
//     * @param cats
//     * @return
//     */
//    @PostMapping("/editCalStdCats")
//    @ApiOperation("修改标准物种类")
//    public Result editCalStdCats(@RequestBody CalStdCats cats) {
//        return calStdService.editCalStdCats(cats);
//    }
    /**
     * 删除标准物种类
     * @param id
     * @return
     */
    @GetMapping("/delCalStdCats")
    @ApiOperation("删除标准物种类")
    public Result delCalStdCats(@RequestParam(value = "id", required = true) String id) {
        return calStdService.delCalStdCats(id);
    }
    /**
     * 查询标准物种类
     * @param queryCalStdCatsDto
     * @return
     */
    @PostMapping("/listCalStdCats")
    @ApiOperation("查询标准物种类")
    public Result<Page<CalStdCats>> listCalStdCats(@RequestBody QueryCalStdCatsDto queryCalStdCatsDto) {
        return calStdService.listCalStdCats(queryCalStdCatsDto);
    }
    /**
     * 获取标准物质名列表
     * @return
     */
    @GetMapping("/getCalStdCatsNameList")
    @ApiOperation("获取标准物质名列表")
    public Result<Map> getCalStdCatsNameList() {
        return calStdService.getCalStdCatsNameList();
    }
}
