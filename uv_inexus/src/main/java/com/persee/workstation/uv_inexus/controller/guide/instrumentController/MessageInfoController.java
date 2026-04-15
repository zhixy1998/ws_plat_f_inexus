/*
 * *
 *  * 版权所有 (C) 2025 北京普析通用仪器有限责任公司。保留所有权利。
 *  * 本软件受商业机密法和版权法保护。
 *  * 本代码仅限与北京普析通用仪器有限责任公司签订有效协议的授权用户内部使用。
 *  * 任何未经明确许可的复制、使用、修改、分发或公开均被严格禁止。
 *
 */

package com.persee.workstation.uv_inexus.controller.guide.instrumentController;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.persee.workstation.uv_inexus.dto.messageDto.MessageDeleteDTO;
import com.persee.workstation.uv_inexus.dto.messageDto.MessageInfoQueryDTO;
import com.persee.workstation.uv_inexus.model.Result;
import com.persee.workstation.uv_inexus.service.guide.instrumentService.MessageInfoService;
import com.persee.workstation.uv_inexus.vo.messageVo.MessageInfoVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@Api(tags = "消息管理")
@RestController
@RequestMapping("/com/persee/workstation/uv-inexus/messages")
public class MessageInfoController {

    @Autowired
    MessageInfoService messageInfoService;

    @ApiOperation("分页查询消息列表")
    @PostMapping("/listMessages")
    public Result<IPage<MessageInfoVO>> getMessageInfoList(@RequestBody MessageInfoQueryDTO query) {
        IPage<MessageInfoVO> list = messageInfoService.getMessageInfoList(query);
        return Result.ok(list);
    }

    @ApiOperation("获取单条消息详情")
    @GetMapping("/getMessageInfo")
    public Result<MessageInfoVO> getMessageInfo(@RequestParam String messageId) {
        MessageInfoVO vo = messageInfoService.getMessageInfo(messageId);
        if (vo == null) {
            return Result.fail("消息不存在");
        }
        return Result.ok(vo);
    }

    @ApiOperation("删除单条消息")
    @GetMapping("/deleteOneMessage")
    public Result<Boolean> deleteMessageInfo(@RequestParam String messageId) {
        boolean success = messageInfoService.deleteMessageInfo(messageId);
        if (success) {
            return Result.ok(true);
        } else {
            return Result.fail("删除失败，消息可能不存在");
        }
    }

    @ApiOperation("批量删除消息")
    @PostMapping("/deleteBatch")
    public Result<Boolean> deleteMessageInfoBatch(@RequestBody MessageDeleteDTO params) {
        boolean success = messageInfoService.deleteMessageInfoBatch(params);
        if (success) {
            return Result.ok(true);
        } else {
            return Result.fail("批量删除失败，请检查ID列表");
        }
    }

    @ApiOperation("统计未读消息数量")
    @GetMapping("/count/unread")
    public Result<Integer> countUnreadMessage() {
        Integer count = messageInfoService.countUnreadMessage();
        return Result.ok(count);
    }

    @ApiOperation("一键已读所有消息")
    @GetMapping("/readAll")
    public Result<Boolean> readAll() {
        boolean success = messageInfoService.readAllMessage();
        if (success) {
            return Result.ok(true);
        } else {
            return Result.fail("操作失败");
        }
    }
}
