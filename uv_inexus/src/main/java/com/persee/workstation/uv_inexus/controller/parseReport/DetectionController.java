package com.persee.workstation.uv_inexus.controller.parseReport;

import com.persee.workstation.uv_inexus.model.Result;
// import com.persee.workstation.uv_inexus.model.ResultCodeEnum;
import com.persee.workstation.uv_inexus.po.parseReport.Detection;
import com.persee.workstation.uv_inexus.service.parseReport.DetectionService;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/detection")
public class DetectionController {

    @Autowired
    private DetectionService detectionService;

    @GetMapping
    public Result<List<Detection>> findAll() {
        List<Detection> detections = detectionService.findAll();
        return Result.ok(detections);
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
