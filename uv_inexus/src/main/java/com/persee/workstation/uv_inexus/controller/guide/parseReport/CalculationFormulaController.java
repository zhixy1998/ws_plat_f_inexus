/*
 * *
 *  * 版权所有 (C) 2025 北京普析通用仪器有限责任公司。保留所有权利。
 *  * 本软件受商业机密法和版权法保护。
 *  * 本代码仅限与北京普析通用仪器有限责任公司签订有效协议的授权用户内部使用。
 *  * 任何未经明确许可的复制、使用、修改、分发或公开均被严格禁止。
 *
 */

package com.persee.workstation.uv_inexus.controller.guide.parseReport;

import java.util.Map;

import com.persee.workstation.uv_inexus.i18n.I18nMessageSource;
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
import com.persee.workstation.uv_inexus.po.guide.parseReport.CalculationFormula;
import com.persee.workstation.uv_inexus.service.guide.parseReport.CalculationFormulaService;
import com.persee.workstation.uv_inexus.utils.Pagination.PageView;

@RestController
@RequestMapping("/com/persee/workstation/uv-inexus/calculationFormula")  // 修改路径以匹配新的实体类型
public class CalculationFormulaController {

    @Autowired
    private CalculationFormulaService calculationFormulaService; // 修改服务类名称

    @GetMapping("/")
    public Result<PageView<CalculationFormula>> findPage(
        @RequestParam(defaultValue = "1") int pageNum,
        @RequestParam(defaultValue = "10") int pageSize,
        @RequestParam Map<String, String> params) {
        
        Page<CalculationFormula> page = new Page<>(pageNum, pageSize);
        IPage<CalculationFormula> resultPage = calculationFormulaService.findPage(page, params);
        return Result.ok(new PageView<>(resultPage)); // 转换为 PageView
    }
    
    @GetMapping("/{id}")
    public Result<CalculationFormula> findById(@PathVariable Integer id) { // 修改返回类型
        CalculationFormula calculationFormula = calculationFormulaService.findById(id); // 修改服务方法调用
        if (calculationFormula == null) {
            //"未找到ID为" + id + "的计算公式记录"
            return Result.fail("00401001", id); // 修改错误消息
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
