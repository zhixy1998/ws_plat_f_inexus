package com.persee.workstation.uv_inexus.controller.parseReport;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.persee.workstation.uv_inexus.dto.parseReport.DetectionDetailDTO;
import com.persee.workstation.uv_inexus.dto.parseReport.DetectionListDTO;
import com.persee.workstation.uv_inexus.dto.parseReport.DetectionParametersDTO;
import com.persee.workstation.uv_inexus.dto.parseReport.MultiTableDataDTO;
import com.persee.workstation.uv_inexus.model.Result;
import com.persee.workstation.uv_inexus.po.parseReport.Detection;
import com.persee.workstation.uv_inexus.service.parseReport.DetectionService;
import com.persee.workstation.uv_inexus.service.parseReport.impl.DetectionServiceImpl;
import com.persee.workstation.uv_inexus.utils.Pagination.PageView;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

@RestController
@RequestMapping("/api/detection")
@Api("标准管理接口文档")
public class DetectionController {

    @Autowired
    private DetectionService detectionService;

    @GetMapping("/noDistinct")
    @ApiOperation("Detection表分页模糊查询（不去重）")
    public Result<PageView<Detection>> findNonDistinctDetectionPage(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam Map<String, String> params) {
        
        // 过滤非法查询字段（仅保留允许的字段）
        Map<String, String> filteredParams = params.entrySet().stream()
                .filter(entry -> DetectionServiceImpl.ALLOWED_QUERY_FIELDS.contains(entry.getKey()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        
        // 构建分页对象（直接使用Detection实体，不转换DTO）
        Page<Detection> page = new Page<>(pageNum, pageSize);
        
        // 调用服务层执行非去重查询
        IPage<Detection> resultPage = detectionService.findPage(page, filteredParams);
        
        return Result.ok(new PageView<>(resultPage));
    }
    // 列表接口
    @GetMapping("/")
    @ApiOperation("列表页+分页+查询")
    public Result<PageView<DetectionListDTO>> findPage(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam Map<String, String> params) {
        
        Map<String, String> filteredParams = params.entrySet().stream()
                .filter(entry -> DetectionServiceImpl.ALLOWED_QUERY_FIELDS.contains(entry.getKey()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        
        Page<DetectionListDTO> page = new Page<>(pageNum, pageSize);
        return Result.ok(new PageView<>(detectionService.findDistinctPage(page, filteredParams)));
    }

    // 详情接口（带identityTypeList）
    @GetMapping("/detail/{id}")
    @ApiOperation("详情接口（通过id查询原始数据）")
    public Result<DetectionDetailDTO> getDetail(@PathVariable Integer id) {
        DetectionDetailDTO detailDTO = detectionService.getDetailById(id);
        return detailDTO != null ? Result.ok(detailDTO) : Result.fail(404, "未找到ID为" + id + "的记录");
    }

    // 新增：参数及子表字段接口
    @GetMapping("/detail/parameters/{id}")
    @ApiOperation("查询测量参数及子表字段详情（id）")
    public Result<DetectionParametersDTO> getParametersById(@PathVariable Integer id) {
        DetectionParametersDTO paramsDTO = detectionService.getParametersById(id);
        return paramsDTO != null ? Result.ok(paramsDTO) : Result.fail(404, "未找到ID为" + id + "的记录");
    }

    @GetMapping("/detail/multi_tables/{id}")
    @ApiOperation("标准公用查询")
    public Result<MultiTableDataDTO> getMultiTablesData(@PathVariable Integer id) {
        MultiTableDataDTO dataDTO = detectionService.getMultiTablesData(id);
        return dataDTO != null ? Result.ok(dataDTO) : Result.fail(404, "未找到ID为" + id + "的记录");
    }
    
    // 其他原有接口保持不变...
    @GetMapping("/{id}")
    @ApiOperation("查询单条记录")
    public Result<Detection> findById(@PathVariable Integer id) {
        Detection detection = detectionService.findById(id);
        return detection != null ? Result.ok(detection) : Result.fail(404, "未找到ID为" + id + "的记录");
    }

    @GetMapping("/industryType")
    @ApiOperation("行业类型统计")
    public Result<List<Map<String, Object>>> countIndustryType() {
        return Result.ok(detectionService.countIndustryType());
    }

    @GetMapping("/industry")
    @ApiOperation("行业统计")
    public Result<List<Map<String, Object>>> countIndustry() {
        return Result.ok(detectionService.countIndustry());
    }

    @PostMapping
    @ApiOperation("新增记录")
    public Result<Void> insert(@RequestBody Detection detection) {
        detectionService.insert(detection);
        return Result.ok();
    }

    @PutMapping
    @ApiOperation("更新记录")
    public Result<Void> update(@RequestBody Detection detection) {
        detectionService.update(detection);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    @ApiOperation("删除单条记录")
    public Result<Void> deleteById(@PathVariable Integer id) {
        detectionService.deleteById(id);
        return Result.ok();
    }

    @DeleteMapping("/batch")
    @ApiOperation("批量删除")
    public Result<Void> deleteBatch(@RequestBody List<Integer> ids) {
        int count = detectionService.deleteBatchIds(ids);
        return count > 0 ? Result.ok() : Result.fail(500, "批量删除失败");
    }
}