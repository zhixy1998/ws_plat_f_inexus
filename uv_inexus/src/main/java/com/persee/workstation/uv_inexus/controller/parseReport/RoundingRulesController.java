package com.persee.workstation.uv_inexus.controller.parseReport;

import com.persee.workstation.uv_inexus.model.Result;
// import com.persee.workstation.uv_inexus.model.ResultCodeEnum;
import com.persee.workstation.uv_inexus.po.parseReport.RoundingRules; // 修改了导入的PO类
import com.persee.workstation.uv_inexus.service.parseReport.RoundingRulesService; // 修改了导入的Service类

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/roundingRules") // 修改了请求路径
public class RoundingRulesController { // 修改了控制器类名

    @Autowired
    private RoundingRulesService roundingRulesService; // 修改了Service变量名

    @GetMapping // 查询所有
    public Result<List<RoundingRules>> findAll() { // 修改了返回类型和方法名
        List<RoundingRules> roundingRulesList = roundingRulesService.findAll(); // 修改了变量名和Service方法调用
        return Result.ok(roundingRulesList);
    }

    @GetMapping("/{id}") // 根据ID查询
    public Result<RoundingRules> findById(@PathVariable Integer id) { // 修改了返回类型和方法名
        RoundingRules roundingRules = roundingRulesService.findById(id); // 修改了变量名和Service方法调用
        if (roundingRules == null) {
            return Result.fail(404, "未找到ID为" + id + "的修约规则记录"); // 修改了错误消息
        } else {
            return Result.ok(roundingRules);
        }
    }

    @PostMapping // 新增记录
    public Result<Void> insert(@RequestBody RoundingRules roundingRules) { // 修改了参数类型和方法名
        roundingRulesService.insert(roundingRules); // 修改了Service方法调用
        return Result.ok();
    }

    @PutMapping // 更新记录
    public Result<Void> update(@RequestBody RoundingRules roundingRules) { // 修改了参数类型和方法名
        roundingRulesService.update(roundingRules); // 修改了Service方法调用
        return Result.ok();
    }

    @DeleteMapping("/{id}") // 根据ID删除记录
    public Result<Void> deleteById(@PathVariable Integer id) { // 方法名保持一致，因为逻辑相同
        roundingRulesService.deleteById(id); // Service方法名保持一致
        return Result.ok();
    }
}
