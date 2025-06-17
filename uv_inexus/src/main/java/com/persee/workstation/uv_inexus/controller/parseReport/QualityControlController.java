package com.persee.workstation.uv_inexus.controller.parseReport;

import com.persee.workstation.uv_inexus.model.Result;
// import com.persee.workstation.uv_inexus.model.ResultCodeEnum;
import com.persee.workstation.uv_inexus.po.parseReport.QualityControl;
import com.persee.workstation.uv_inexus.service.parseReport.QualityControlService;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/qualityControl") // 使用与实体相关的路径
public class QualityControlController {

    @Autowired
    private QualityControlService qualityControlService; // 注入服务层

    // 查询所有质控记录
    @GetMapping
    public Result<List<QualityControl>> findAllQualityControls() {
        List<QualityControl> qualityControls = qualityControlService.findAll();
        return Result.ok(qualityControls);
    }

    // 根据ID查询质控记录
    @GetMapping("/{id}")
    public Result<QualityControl> findQualityControlById(@PathVariable Integer id) {
        QualityControl qualityControl = qualityControlService.findById(id);
        if (qualityControl == null) {
            return Result.fail(404, "未找到ID为" + id + "的质控记录"); // 自定义错误码和消息
        } else {
            return Result.ok(qualityControl);
        }
    }

    // 添加质控记录
    @PostMapping
    public Result<Void> insertQualityControl(@RequestBody QualityControl qualityControl) {
        qualityControlService.insert(qualityControl);
        return Result.ok(); // 成功返回空数据
    }

    // 更新质控记录
    @PutMapping
    public Result<Void> updateQualityControl(@RequestBody QualityControl qualityControl) {
        qualityControlService.update(qualityControl);
        return Result.ok(); // 成功返回空数据
    }

    // 根据ID删除质控记录
    @DeleteMapping("/{id}")
    public Result<Void> deleteQualityControl(@PathVariable Integer id) {
        qualityControlService.deleteById(id);
        return Result.ok(); // 成功返回空数据
    }
}
