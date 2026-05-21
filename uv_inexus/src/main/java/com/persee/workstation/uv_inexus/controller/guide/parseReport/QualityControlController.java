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
import com.persee.workstation.uv_inexus.model.Result;
// import com.persee.workstation.uv_inexus.model.ResultCodeEnum;
import com.persee.workstation.uv_inexus.po.guide.parseReport.QualityControl;
import com.persee.workstation.uv_inexus.service.guide.parseReport.QualityControlService;
import com.persee.workstation.uv_inexus.utils.Pagination.PageView;

@RestController
@RequestMapping("/com/persee/workstation/uv-inexus/qualityControl") // 使用与实体相关的路径
public class QualityControlController {

    @Autowired
    private QualityControlService qualityControlService; // 注入服务层

    // 查询所有质控记录
    
    @GetMapping("/")
    public Result<PageView<QualityControl>> findPage(
        @RequestParam(defaultValue = "1") int pageNum,
        @RequestParam(defaultValue = "10") int pageSize,
        @RequestParam Map<String, String> params) {
        
        Page<QualityControl> page = new Page<>(pageNum, pageSize);
        IPage<QualityControl> resultPage = qualityControlService.findPage(page, params);
        return Result.ok(new PageView<>(resultPage)); // 转换为 PageView
    }

    // 根据ID查询质控记录
    @GetMapping("/{id}")
    public Result<QualityControl> findQualityControlById(@PathVariable Integer id) {
        QualityControl qualityControl = qualityControlService.findById(id);
        if (qualityControl == null) {
            //"未找到ID为" + id + "的质控记录"
            return Result.fail("00405001", id); // 自定义错误码和消息
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
