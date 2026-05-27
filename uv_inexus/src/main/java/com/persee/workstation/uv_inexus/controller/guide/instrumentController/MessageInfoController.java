package com.persee.workstation.uv_inexus.controller.guide.instrumentController;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.persee.workstation.uv_inexus.dto.messageDto.MessageDeleteDTO;
import com.persee.workstation.uv_inexus.dto.messageDto.MessageInfoQueryDTO;
import com.persee.workstation.uv_inexus.model.Result;
import com.persee.workstation.uv_inexus.po.guide.instrumentPo.UserReminderMutePO;
import com.persee.workstation.uv_inexus.service.guide.instrumentService.MessageInfoService;
import com.persee.workstation.uv_inexus.service.guide.instrumentService.MessageRecycleService;
import com.persee.workstation.uv_inexus.service.guide.instrumentService.UserReminderMuteService;
import com.persee.workstation.uv_inexus.vo.messageVo.MessageInfoVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Api(tags = "消息管理")
@RestController
@RequestMapping("/com/persee/workstation/uv-inexus/messages")
public class MessageInfoController {

    @Autowired
    MessageInfoService messageInfoService;

    @Autowired
    UserReminderMuteService userReminderMuteService;

    @Autowired
    MessageRecycleService messageRecycleService;

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
    public Result<Integer> countUnreadMessage(@RequestParam(required = false) String userName) {
        Integer count = messageInfoService.countUnreadMessage(userName);
        return Result.ok(count);
    }

    @ApiOperation("一键已读所有消息")
    @GetMapping("/readAll")
    public Result<Boolean> readAll(@RequestParam(required = false) String userName) {
        boolean success = messageInfoService.readAllMessage(userName);
        if (success) {
            return Result.ok(true);
        } else {
            return Result.fail("操作失败");
        }
    }

/**
 * 不再提醒（按类屏蔽）接口
 * 该接口用于根据用户名、提醒类型和屏蔽范围来设置不再提醒
 *
 * @param userName 用户名，用于标识需要设置不再提醒的用户
 * @param reminderType 提醒类型，用于指定需要屏蔽的提醒类别
 * @param muteScope 屏蔽范围，默认值为0，用于指定屏蔽的范围大小
 * @return 返回操作结果，包含操作是否成功的状态信息
 */
    @ApiOperation("不再提醒（按类屏蔽）")
    @PostMapping("/muteReminder")
    public Result<Boolean> muteReminder(@RequestParam String userName,
                                         @RequestParam Integer reminderType,
                                         @RequestParam(defaultValue = "0") Integer muteScope) {
    // 调用用户提醒屏蔽服务，执行不再提醒的操作
        userReminderMuteService.muteReminder(userName, reminderType, muteScope);
    // 返回操作成功的结果
        return Result.ok(true);
    }

/**
 * 恢复提醒的API接口
 * 通过用户名和提醒类型来取消提醒的静默状态
 *
 * @param userName 用户名，用于标识需要恢复提醒的用户
 * @param reminderType 提醒类型，用于指定需要恢复的提醒类别
 * @return 返回操作结果，包含成功状态和数据
 */
    @ApiOperation("恢复提醒")  // Swagger API文档注解，描述接口功能
    @PostMapping("/unmuteReminder")  // HTTP请求映射，指定POST请求路径为/unmuteReminder
    public Result<Boolean> unmuteReminder(@RequestParam String userName,  // 请求参数：用户名
                                           @RequestParam Integer reminderType) {  // 请求参数：提醒类型
        userReminderMuteService.unmuteReminder(userName, reminderType);  // 调用服务层方法取消提醒静默
        return Result.ok(true);  // 返回操作成功结果，数据为true
    }

/**
 * 获取用户屏蔽列表接口
 * 该接口用于根据用户名获取该用户的所有屏蔽列表信息
 *
 * @param userName 用户名，用于查询该用户的屏蔽列表
 * @return 返回Result对象，包含用户屏蔽列表数据
 */
    @ApiOperation("获取用户屏蔽列表")
    @GetMapping("/muteList")
    public Result<List<UserReminderMutePO>> getMuteList(@RequestParam String userName) {
    // 调用服务层方法获取用户屏蔽列表
        List<UserReminderMutePO> list = userReminderMuteService.getUserMuteList(userName);
    // 返回成功响应，包含屏蔽列表数据
        return Result.ok(list);
    }

/**
 * 分页查询回收站消息列表接口
 * 该接口用于获取回收站中的消息列表，支持分页查询
 *
 * @param query 查询条件对象，包含分页信息和查询条件
 * @return 返回Result对象，包含分页的消息列表数据
 */
    @ApiOperation("分页查询回收站消息列表")  // Swagger接口注解，描述接口功能
    @PostMapping("/recycleList")  // POST请求映射，指定接口访问路径
    public Result<IPage<MessageInfoVO>> getRecycleList(@RequestBody MessageInfoQueryDTO query) {
    // 调用回收站服务获取分页消息列表
        IPage<MessageInfoVO> list = messageRecycleService.getRecycleList(query);
    // 返回成功结果，包含查询到的消息列表数据
        return Result.ok(list);
    }

/**
 * 从回收站恢复消息的接口方法
 * @ApiOperation: Swagger注解，用于描述API的功能
 * @GetMapping: HTTP请求映射，处理GET请求
 * @RequestParam: 获取请求参数中的messageId
 *
 * @param messageId 要恢复的消息ID
 * @return 返回操作结果，成功返回true，失败返回错误信息
 */
    @ApiOperation("从回收站恢复消息")
    @GetMapping("/restoreMessage")
    public Result<Boolean> restoreMessage(@RequestParam String messageId) {
    // 调用消息回收站服务层的恢复消息方法
        Boolean success = messageRecycleService.restoreMessage(messageId);
    // 判断恢复操作是否成功
        if (success) {
        // 如果成功，返回成功结果
            return Result.ok(true);
        } else {
        // 如果失败，返回失败结果及错误信息
            return Result.fail("恢复失败，消息可能不存在");
        }
    }
}
