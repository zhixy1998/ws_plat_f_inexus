package com.persee.workstation.uv_inexus.controller.parseReport;

import com.persee.workstation.uv_inexus.model.Result; // 引入你定义的 Result 类
import com.persee.workstation.uv_inexus.po.parseReport.StandardPretreatment;
import com.persee.workstation.uv_inexus.service.parseReport.StandardPretreatmentService;
// import com.persee.workstation.uv_inexus.model.ResultCodeEnum; // 引入你定义的 ResultCodeEnum 枚举
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/standardPretreatment")
public class StandardPretreatmentController {

    @Autowired
    private StandardPretreatmentService standardPretreatmentService; // 修正了变量名大小写，建议与类名区分

    // 获取所有标准预处理记录
    @GetMapping
    public Result<List<StandardPretreatment>> getAllStandardPretreatments() {
        List<StandardPretreatment> standardPretreatments = standardPretreatmentService.findAll();
        return Result.ok(standardPretreatments); // 返回成功结果，包含数据列表
    }

    // 根据ID获取标准预处理记录
    @GetMapping("/{id}")
    public Result<StandardPretreatment> getStandardPretreatmentById(@PathVariable Integer id) {
        StandardPretreatment standardPretreatment = standardPretreatmentService.findById(id);
        if (standardPretreatment == null) {
            return Result.fail(404, "未找到ID为" + id + "的检测记录"); // 使用自定义的错误代码和消息
        } else {
            return Result.ok(standardPretreatment);
        }
    }

    // 创建新的标准预处理记录
    @PostMapping
    public Result<Void> createStandardPretreatment(@RequestBody StandardPretreatment standardPretreatment) {
        standardPretreatmentService.insert(standardPretreatment);
        return Result.ok(null); // 返回成功结果，通常不包含数据，所以用 Void 并传 null
    }

    // 更新标准预处理记录
    @PutMapping
    public Result<Void> updateStandardPretreatment(@RequestBody StandardPretreatment standardPretreatment) {
        standardPretreatmentService.update(standardPretreatment);
        return Result.ok(null); // 返回成功结果，通常不包含数据，所以用 Void 并传 null
    }

    // 根据ID删除标准预处理记录
    @DeleteMapping("/{id}")
    public Result<Void> deleteStandardPretreatment(@PathVariable Integer id) {
        standardPretreatmentService.deleteById(id);
        return Result.ok(null); // 返回成功结果，通常不包含数据，所以用 Void 并传 null
    }
}
