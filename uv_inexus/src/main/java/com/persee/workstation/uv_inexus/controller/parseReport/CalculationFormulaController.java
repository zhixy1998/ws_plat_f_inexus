package com.persee.workstation.uv_inexus.controller.parseReport;

import com.persee.workstation.uv_inexus.model.Result;
// import com.persee.workstation.uv_inexus.model.ResultCodeEnum;
import com.persee.workstation.uv_inexus.po.parseReport.CalculationFormula;
import com.persee.workstation.uv_inexus.service.parseReport.CalculationFormulaService;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/calculationFormula")  // 修改路径以匹配新的实体类型
public class CalculationFormulaController {

    @Autowired
    private CalculationFormulaService calculationFormulaService; // 修改服务类名称

    @GetMapping
    public Result<List<CalculationFormula>> findAll() { // 修改返回类型和方法名
        List<CalculationFormula> calculationFormulas = calculationFormulaService.findAll(); // 修改服务方法调用
        return Result.ok(calculationFormulas);
    }

    @GetMapping("/{id}")
    public Result<CalculationFormula> findById(@PathVariable Integer id) { // 修改返回类型
        CalculationFormula calculationFormula = calculationFormulaService.findById(id); // 修改服务方法调用
        if (calculationFormula == null) {
            return Result.fail(404, "未找到ID为" + id + "的计算公式记录"); // 修改错误消息
        } else {
            return Result.ok(calculationFormula);
        }
    }

    @PostMapping
    public Result<Void> insert(@RequestBody CalculationFormula calculationFormula) { // 修改参数类型
        calculationFormulaService.insert(calculationFormula); // 修改服务方法调用
        return Result.ok();
    }

    @PutMapping
    public Result<Void> update(@RequestBody CalculationFormula calculationFormula) { // 修改参数类型
        calculationFormulaService.update(calculationFormula); // 修改服务方法调用
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    public Result<Void> deleteById(@PathVariable Integer id) {
        calculationFormulaService.deleteById(id); // 修改服务方法调用
        return Result.ok();
    }
}
