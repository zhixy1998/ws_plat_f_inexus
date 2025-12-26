package com.persee.workstation.uv_inexus.controller.mq;

import com.persee.workstation.uv_inexus.model.Result;
import com.persee.workstation.uv_inexus.mqproducer.ClusterConfig;
import com.persee.workstation.uv_inexus.mqproducer.DynamicMqProducerUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * MQ动态生产者管理控制器
 * 提供生产者的注册、注销及消息发送接口
 */
@RestController
@RequestMapping("/com/persee/workstation/uv-inexus/mq")
@Api(tags = "MQ动态生产者管理")
public class MqController {

    @Autowired
    private DynamicMqProducerUtil dynamicMqProducerUtil;

    /**
     * 注册新的MQ生产者
     *
     * @param config 生产者配置信息，包含clusterId、nameServers等
     * @return 注册结果
     */
    @PostMapping("/register")
    @ApiOperation("注册MQ生产者")
    public Result<String> registerProducer(
            @ApiParam(value = "生产者配置", required = true)
            @RequestBody ClusterConfig config) {
        try {
            dynamicMqProducerUtil.registerProducer(config);
            System.out.println("注册成功"+config.getClusterId()+",nameserver"+config.getNameServers());
            return Result.ok("注册成功");
        } catch (Exception e) {
            return Result.fail("注册失败: " + e.getMessage());
        }
    }

    /**
     * 注销指定ID的MQ生产者
     *
     * @param clusterId 集群ID
     * @return 注销结果
     */
    @DeleteMapping("/unregister")
    @ApiOperation("注销MQ生产者")
    public Result<String> unregisterProducer(
            @ApiParam(value = "集群ID", required = true)
            @RequestParam String clusterId) {
        boolean success = dynamicMqProducerUtil.unregisterProducer(clusterId);
        if (success) {
            return Result.ok("注销成功");
        } else {
            return Result.fail("注销失败，未找到对应生产者");
        }
    }

    /**
     * 发送普通消息
     *
     * @param clusterId 集群ID
     * @param topic     消息主题
     * @param tag       消息标签（可选）
     * @param jsonBody  消息体（JSON格式）
     * @return 发送结果
     */
    @PostMapping("/send/normal")
    @ApiOperation("发送普通消息")
    public Result<String> sendNormal(
            @ApiParam(value = "集群ID", required = true) @RequestParam String clusterId,
            @ApiParam(value = "消息主题", required = true) @RequestParam String topic,
            @ApiParam(value = "消息标签") @RequestParam(required = false) String tag,
            @ApiParam(value = "消息体(JSON)", required = true) @RequestBody String jsonBody) {
        boolean success = dynamicMqProducerUtil.sendNormal(clusterId, topic, tag, jsonBody);
        System.out.println("发送"+clusterId+",topic"+topic+",tag"+tag+",jsonBody"+jsonBody);
        if (success) {
            return Result.ok("发送成功");
        } else {
            return Result.fail("发送失败");
        }
    }

    /**
     * 发送顺序消息
     *
     * @param clusterId 集群ID
     * @param topic     消息主题
     * @param tag       消息标签（可选）
     * @param hashKey   顺序路由Key
     * @param jsonBody  消息体（JSON格式）
     * @return 发送结果
     */
    @PostMapping("/send/orderly")
    @ApiOperation("发送顺序消息")
    public Result<String> sendOrderly(
            @ApiParam(value = "集群ID", required = true) @RequestParam String clusterId,
            @ApiParam(value = "消息主题", required = true) @RequestParam String topic,
            @ApiParam(value = "消息标签") @RequestParam(required = false) String tag,
            @ApiParam(value = "顺序Key", required = true) @RequestParam String hashKey,
            @ApiParam(value = "消息体(JSON)", required = true) @RequestBody String jsonBody) {
        boolean success = dynamicMqProducerUtil.sendOrderly(clusterId, topic, tag, jsonBody, hashKey);
        if (success) {
            return Result.ok("发送成功");
        } else {
            return Result.fail("发送失败");
        }
    }

    /**
     * 发送延时消息
     *
     * @param clusterId  集群ID
     * @param topic      消息主题
     * @param tag        消息标签（可选）
     * @param delayLevel 延时级别（1=1s, 2=5s, 3=10s, ...）
     * @param jsonBody   消息体（JSON格式）
     * @return 发送结果
     */
    @PostMapping("/send/delay")
    @ApiOperation("发送延时消息")
    public Result<String> sendDelay(
            @ApiParam(value = "集群ID", required = true) @RequestParam String clusterId,
            @ApiParam(value = "消息主题", required = true) @RequestParam String topic,
            @ApiParam(value = "消息标签") @RequestParam(required = false) String tag,
            @ApiParam(value = "延时级别(1s/5s/10s...)", required = true) @RequestParam int delayLevel,
            @ApiParam(value = "消息体(JSON)", required = true) @RequestBody String jsonBody) {
        boolean success = dynamicMqProducerUtil.sendDelay(clusterId, topic, tag, jsonBody, delayLevel);
        if (success) {
            return Result.ok("发送成功");
        } else {
            return Result.fail("发送失败");
        }
    }
}
