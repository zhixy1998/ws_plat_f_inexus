/*
 * *
 *  * 版权所有 (C) 2025 北京普析通用仪器有限责任公司。保留所有权利。
 *  * 本软件受商业机密法和版权法保护。
 *  * 本代码仅限与北京普析通用仪器有限责任公司签订有效协议的授权用户内部使用。
 *  * 任何未经明确许可的复制、使用、修改、分发或公开均被严格禁止。
 *
 */

package com.persee.workstation.uv_inexus.controller.factory_inspection;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.persee.workstation.uv_inexus.dto.factory_inspection.CalArtifactsSaveDto;
import com.persee.workstation.uv_inexus.dto.factory_inspection.QueryCalStdCatsDto;
import com.persee.workstation.uv_inexus.dto.factory_inspection.StandardSubstanceDto;
import com.persee.workstation.uv_inexus.model.Result;
import com.persee.workstation.uv_inexus.service.factory_inspection.CalArtifactsService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

@RestController
@RequestMapping("/com/persee/workstation/uv-inexus/factory-inspection/cal-artifacts")
@Api("标准实体物质管理")
public class CalArtifactsController {
    @Autowired
    private CalArtifactsService calArtifactsService;
//    /**
//     * 查询标准物质
//     * @param queryCalArtifactsDto
//     * @return
//     */
//    @ApiOperation("查询标准物质")
//    @PostMapping("/listCalArtifacts")
//    public Result<Page<CalArtifacts>> queryCalArtifacts(@RequestBody QueryCalArtifactsDto queryCalArtifactsDto) {
//        return calArtifactsService.queryCalArtifacts(queryCalArtifactsDto);
//    }
    @ApiOperation("增加/编辑标准物质")
    @PostMapping("/saveCalArtifacts")
    public Result addCalArtifacts(@Valid @RequestBody CalArtifactsSaveDto calArtifacts) {
        return calArtifactsService.addCalArtifacts(calArtifacts);
    }

    @ApiOperation("标准物质列表")
    @PostMapping("/list")
    public Result<IPage<StandardSubstanceDto>> listCalArtifacts(@RequestBody QueryCalStdCatsDto dto) {
        return calArtifactsService.listCalArtifacts(dto);
    }

}
