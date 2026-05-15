package com.persee.workstation.uv_inexus.controller.systemConfig;

/*
 * *
 *  * 版权所有 (C) 2025 北京普析通用仪器有限责任公司。保留所有权利。
 *  * 本软件受商业机密法和版权法保护。
 *  * 本代码仅限与北京普析通用仪器有限责任公司签订有效协议的授权用户内部使用。
 *  * 任何未经明确许可的复制、使用、修改、分发或公开均被严格禁止。
 *
*/

import com.persee.workstation.uv_inexus.dao.guide.systemManageDao.SystemManageDAO;
import com.persee.workstation.uv_inexus.i18n.LocaleContext;
import com.persee.workstation.uv_inexus.model.Result;
import com.persee.workstation.uv_inexus.po.guide.systemManagePo.SystemManagePO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/i18n")
@Api(tags = "国际化管理")
@Slf4j
public class I18nController {
    @Autowired
    SystemManageDAO systemManageDao;
    @Value("${sys-manage-default.defaultId}")
    String defaultId;
    @PostMapping("/setLocale")
    @ApiOperation("设置当前语言")
    public Result<String> setLocale(@RequestParam String locale, HttpServletRequest request) {

        // 2. 构造对象并更新数据库
        SystemManagePO updatePO = new SystemManagePO();
        updatePO.setId(defaultId);
        updatePO.setLanguage(locale);
        systemManageDao.updateById(updatePO);

        // 3. 立即更新当前线程上下文，让本次响应生效
        LocaleContext.setLocale(locale);

        return Result.ok();
    }


    @GetMapping("/getLocale")
    @ApiOperation("获取当前语言")
    public Result<String> getLocale() {
        SystemManagePO systemManagePO = systemManageDao.selectById(defaultId);
        return Result.ok(systemManagePO.getLanguage());
    }
}
