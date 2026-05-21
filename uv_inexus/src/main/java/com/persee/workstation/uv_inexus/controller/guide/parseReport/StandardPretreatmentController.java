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

// import com.persee.workstation.uv_inexus.model.ResultCodeEnum; // 引入你定义的 ResultCodeEnum 枚举
import com.persee.workstation.uv_inexus.i18n.YamlMessageSource;
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
import com.persee.workstation.uv_inexus.model.Result; // 引入你定义的 Result 类
import com.persee.workstation.uv_inexus.po.guide.parseReport.StandardPretreatment;
import com.persee.workstation.uv_inexus.service.guide.parseReport.StandardPretreatmentService;
import com.persee.workstation.uv_inexus.utils.Pagination.PageView;

@RestController
@RequestMapping("/com/persee/workstation/uv-inexus/standardPretreatment")
public class StandardPretreatmentController {

    @Autowired
    private StandardPretreatmentService standardPretreatmentService; // 修正了变量名大小写，建议与类名区分

    // 获取所有标准预处理记录
    @GetMapping("/")
    public Result<PageView<StandardPretreatment>> findPage(
        @RequestParam(defaultValue = "1") int pageNum,
        @RequestParam(defaultValue = "10") int pageSize,
        @RequestParam Map<String, String> params) {
        
        Page<StandardPretreatment> page = new Page<>(pageNum, pageSize);
        IPage<StandardPretreatment> resultPage = standardPretreatmentService.findPage(page, params);
        return Result.ok(new PageView<>(resultPage)); // 转换为 PageView
    }

    // 根据ID获取标准预处理记录
    @GetMapping("/{id}")
    public Result<StandardPretreatment> getStandardPretreatmentById(@PathVariable Integer id) {
        StandardPretreatment standardPretreatment = standardPretreatmentService.findById(id);
        if (standardPretreatment == null) {
            //"未找到ID为" + id + "的检测记录"
            return Result.fail("004007001", id); // 使用自定义的错误代码和消息
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
