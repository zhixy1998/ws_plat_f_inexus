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
import com.persee.workstation.uv_inexus.dto.parseReport.DetectionListDTO;
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

    @GetMapping("/")
    @ApiOperation("列表页+分页+查询（按standard_number去重）")
    public Result<PageView<DetectionListDTO>> findPage(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam Map<String, String> params) {
        
        // 过滤非法参数
        Map<String, String> filteredParams = params.entrySet().stream()
                .filter(entry -> DetectionServiceImpl.ALLOWED_QUERY_FIELDS.contains(entry.getKey()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        
        Page<DetectionListDTO> page = new Page<>(pageNum, pageSize);
        IPage<DetectionListDTO> resultPage = detectionService.findDistinctPage(page, filteredParams);
        return Result.ok(new PageView<>(resultPage));
    }

    @GetMapping("/{id}")
    @ApiOperation("查询某一条")
    public Result<Detection> findById(@PathVariable Integer id) {
        Detection detection = detectionService.findById(id);
        if (detection == null) {
            return Result.fail(404, "未找到ID为" + id + "的检测记录");
        } else {
            return Result.ok(detection);
        }
    }

    @GetMapping("/industryType")
    @ApiOperation("类型饼图")
    public Result<List<Map<String, Object>>> countIndustryType() {
        List<Map<String, Object>> result = detectionService.countIndustryType();
        return Result.ok(result);
    }
    
    @GetMapping("/industry")
    @ApiOperation("行业饼图")
    public Result<List<Map<String, Object>>> countIndustry() {
        List<Map<String, Object>> result = detectionService.countIndustry();
        return Result.ok(result);
    }

    @PostMapping
    @ApiOperation("新增")
    public Result<Void> insert(@RequestBody Detection detection) {
        detectionService.insert(detection);
        return Result.ok();
    }

    @PutMapping
    @ApiOperation("修改")
    public Result<Void> update(@RequestBody Detection detection) {
        detectionService.update(detection);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    @ApiOperation("删除")
    public Result<Void> deleteById(@PathVariable Integer id) {
        detectionService.deleteById(id);
        return Result.ok();
    }
    
    @DeleteMapping("/batch")
    @ApiOperation("批量删除")
    public Result<Void> deleteBatch(@RequestBody List<Integer> ids) {
        int result = detectionService.deleteBatchIds(ids);
        if (result > 0) {
            return Result.ok();
        } else {
            return Result.fail(500, "批量删除失败");
        }
    }
}