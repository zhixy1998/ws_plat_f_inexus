package com.persee.workstation.uv_inexus.controller.parseReport;

import java.util.List;
import java.util.Map;

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
import com.persee.workstation.uv_inexus.model.Result;
// import com.persee.workstation.uv_inexus.model.ResultCodeEnum;
import com.persee.workstation.uv_inexus.po.parseReport.Detection;
import com.persee.workstation.uv_inexus.service.parseReport.DetectionService;
import com.persee.workstation.uv_inexus.utils.Pagination.PageView;

@RestController
@RequestMapping("/api/detection")
public class DetectionController {

    @Autowired
    private DetectionService detectionService;

    @GetMapping("/")
    public Result<PageView<Detection>> findPage(
        @RequestParam(defaultValue = "1") int pageNum,
        @RequestParam(defaultValue = "10") int pageSize,
        @RequestParam Map<String, String> params) {
        
        Page<Detection> page = new Page<>(pageNum, pageSize);
        IPage<Detection> resultPage = detectionService.findPage(page, params);
        return Result.ok(new PageView<>(resultPage)); // 转换为 PageView
    }

    @GetMapping("/{id}")
    public Result<Detection> findById(@PathVariable Integer id) {
        Detection detection = detectionService.findById(id);
        if (detection == null) {
            return Result.fail(404, "未找到ID为" + id + "的检测记录"); // 使用自定义的错误代码和消息
        } else {
            return Result.ok(detection);
        }
    }

    @GetMapping("/industryType")
    public Result<List<Map<String, Object>>> countIndustryType() {
        List<Map<String, Object>> result = detectionService.countIndustryType();
        return Result.ok(result);
    }
    
    @GetMapping("/industry")
    public Result<List<Map<String, Object>>> countIndustry() {
        List<Map<String, Object>> result = detectionService.countIndustry();
        return Result.ok(result);
    }

    @PostMapping
    public Result<Void> insert(@RequestBody Detection detection) {
        detectionService.insert(detection);
        return Result.ok();
    }

    @PutMapping
    public Result<Void> update(@RequestBody Detection detection) {
        detectionService.update(detection);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    public Result<Void> deleteById(@PathVariable Integer id) {
        detectionService.deleteById(id);
        return Result.ok();
    }
}
