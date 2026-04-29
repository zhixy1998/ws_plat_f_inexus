package com.persee.workstation.uv_inexus.controller.concise.measurementController;

import com.persee.workstation.uv_inexus.dto.concise.common.Box;
import com.persee.workstation.uv_inexus.dto.concise.common.FMethodParamRequestDto;
import com.persee.workstation.uv_inexus.dto.measurementDto.experimentTest.MethodExportRequest;
import com.persee.workstation.uv_inexus.model.Result;
import com.persee.workstation.uv_inexus.po.guide.measurementPo.TaskQueryParam;
import com.persee.workstation.uv_inexus.service.concise.measurementService.ManageDataService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/*
 * *
 *  * 版权所有 (C) 2025 北京普析通用仪器有限责任公司。保留所有权利。
 *  * 本软件受商业机密法和版权法保护。
 *  * 本代码仅限与北京普析通用仪器有限责任公司签订有效协议的授权用户内部使用。
 *  * 任何未经明确许可的复制、使用、修改、分发或公开均被严格禁止。
 *
 */
@RequestMapping("/com/persee/workstation/uv-inexus/experimenttest/sys/classicMode/")
@RestController
@Api("经典模式 - 数据管理")
public class ClassicManageDataController {
    @Autowired
    private ManageDataService manageDataService;
    
    /**
     * 列出经典模式的方法管理列表
     * @return
     */
    @GetMapping("listOfMethods")
    @ApiOperation("获取方法列表")
    public Result listOfMethods(@RequestParam(required = false) String methodName,
                                                    @RequestParam Integer pageNum,
                                                    @RequestParam Integer pageSize,
                                                    @RequestParam String sort,
                                                    @RequestParam(required = false) String updateBy){
        return manageDataService.listOfMethods(methodName,pageNum,pageSize, sort,updateBy);
    }

    /**
     * 经典模式: 获取任务列表
     * @param taskQueryParam
     * @return
     */
    @PostMapping("/getTasksManagementList")
    @ApiOperation("获取任务列表")
    public Result<Map<String,Object>> getTasksManagementList(@RequestBody TaskQueryParam taskQueryParam) {
        return manageDataService.getTasksManagementList(taskQueryParam);
    }

    /**
     * 经典模式: 删除任务
     * @param deleteTaskIdList
     * @return
     */
    @PostMapping("/deleteTask")
    @ApiOperation("任务删除")
    public Result<String> deleteTask(@RequestBody Box deleteTaskIdList) {
        return manageDataService.deleteTask(deleteTaskIdList);
    }
    
    /**
     * 导出方法
     * @param ids 方法 ID 列表
     * @return Excel 文件
     */
    @PostMapping("methodExport")
    @ApiOperation("导出方法")
    public ResponseEntity<ByteArrayResource> exportMethod(@RequestBody MethodExportRequest ids) throws IOException {
        List<com.persee.workstation.uv_inexus.po.concise.measurementPo.FMeasureMethod> methods = manageDataService.exportMethod(ids.getIds());
        if (methods.isEmpty()) {
            throw new RuntimeException("方法不存在");
        }
        // 导出为 Excel 格式
        byte[] excelBytes = manageDataService.exportMethodToExcel(methods);
        // 转换成字节数组资源
        ByteArrayResource resource = new ByteArrayResource(excelBytes);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=methods.xlsx")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .contentLength(resource.contentLength())
                .body(resource);
    }
    
    /**
     * 导入方法
     * @param file Excel 文件
     * @return 导入结果
     */
    @PostMapping("methodImport")
    @ApiOperation("导入方法")
    public Result importMethod(@RequestParam("file") MultipartFile file) {
        return manageDataService.importMethodFromExcel(file);
    }
    
    /**
     * 删除方法
     * @param deleteMethodIdList 方法 ID 列表
     * @return 删除结果
     */
    @PostMapping("/deleteMethod")
    @ApiOperation("方法删除")
    public Result<String> deleteMethod(@RequestBody Box deleteMethodIdList) {
        return manageDataService.deleteMethod(deleteMethodIdList);
    }
    
    /**
     * 查询方法详情
     * @param methodId 方法 ID
     * @return 方法详情
     */
    @GetMapping("methodDetail")
    @ApiOperation("查询方法详情")
    public Result queryMethodDetail(@RequestParam String methodId) {
        return manageDataService.queryMethodDetail(methodId);
    }
    
    /**
     * 查询所有项目
     * @param updateBy 用户标识
     * @return 项目列表
     */
    @GetMapping("queryAllProject")
    @ApiOperation("查询所有项目接口")
    public Result queryAllProject(@RequestParam String updateBy) {
        return manageDataService.queryAllProject(updateBy);
    }
    
    /**
     * 方法名称重复性检查
     * @param methodName 方法名称
     * @param measureType 测量类型
     * @return 是否重复
     */
    @GetMapping("methodDuplicationCheck")
    @ApiOperation("方法：名称重复性检查")
    public Result methodDuplicationCheck(@RequestParam String methodName,
                                        @RequestParam String measureType) {
        return manageDataService.methodDuplicationCheck(methodName, measureType);
    }
    
    /**
     * 新建方法
     * @param methodDto 方法参数
     * @return 创建结果
     */
    @PostMapping("createMethodPort")
    @ApiOperation("新建方法接口")
    public Result createMethodPort(@RequestBody FMethodParamRequestDto methodDto) {
        return manageDataService.createMethodPort(methodDto);
    }
}
