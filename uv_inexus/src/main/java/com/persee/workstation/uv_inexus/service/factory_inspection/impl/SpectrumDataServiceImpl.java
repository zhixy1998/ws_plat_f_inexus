package com.persee.workstation.uv_inexus.service.factory_inspection.impl;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.alibaba.fastjson2.TypeReference;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.persee.workstation.uv_inexus.dao.factory_inspection.SpectrumDataMapper;
import com.persee.workstation.uv_inexus.po.factory_inspection.SpectrumData;
import com.persee.workstation.uv_inexus.service.factory_inspection.SpectrumDataService;
import com.persee.workstation.uv_inexus.websocket.factory_inspection.ExcelWebSocketHandler;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/*
 * *
 *  * 版权所有 (C) 2025 北京普析通用仪器有限责任公司。保留所有权利。
 *  * 本软件受商业机密法和版权法保护。
 *  * 本代码仅限与北京普析通用仪器有限责任公司签订有效协议的授权用户内部使用。
 *  * 任何未经明确许可的复制、使用、修改、分发或公开均被严格禁止。
 *
 */
@Service
public class SpectrumDataServiceImpl extends ServiceImpl<SpectrumDataMapper, SpectrumData> implements SpectrumDataService {
    @Override
    public List<ExcelWebSocketHandler.MultiLineSpectrumData> findByTaskIdAndItemId(String taskId, String itemId) {
        LambdaQueryWrapper<SpectrumData> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SpectrumData::getTaskId, taskId)
                .eq(SpectrumData::getItemId, itemId)
                .orderByDesc(SpectrumData::getMeasurementTime); // 按测量时间倒序
        List<SpectrumData> dataList = this.list(wrapper);

        return dataList.stream()
                .map(this::convertToMultiLineSpectrumData)
                .collect(Collectors.toList());
    }
    private ExcelWebSocketHandler.MultiLineSpectrumData convertToMultiLineSpectrumData(SpectrumData spectrumData) {
        ExcelWebSocketHandler.MultiLineSpectrumData multiLineData = new ExcelWebSocketHandler.MultiLineSpectrumData();
        multiLineData.setTaskId(spectrumData.getTaskId());
        multiLineData.setItemId(spectrumData.getItemId());

        if (spectrumData.getSpectrumData() != null) {
            try {
                Map<String, List<List<JSONObject>>> dataMap =
                        JSON.parseObject(spectrumData.getSpectrumData(),
                                new TypeReference<Map<String, List<List<JSONObject>>>>() {});
                multiLineData.setData(dataMap);
//                // 从Map中获取第一个值或特定键的值
//                if (!dataMap.isEmpty()) {
//                    List<List<JSONObject>> firstValue = dataMap.values().iterator().next();
//                    multiLineData.setData(firstValue);
//                }
            } catch (Exception e) {
                log.error("反序列化波形数据失败", e);
            }
        }
        return multiLineData;
    }

}
