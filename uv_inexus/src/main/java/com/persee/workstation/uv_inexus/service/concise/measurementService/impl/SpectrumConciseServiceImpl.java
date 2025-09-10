package com.persee.workstation.uv_inexus.service.concise.measurementService.impl;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.persee.workstation.uv_inexus.model.Result;
import com.persee.workstation.uv_inexus.po.concise.measurementPo.FMeasureMethod;
import com.persee.workstation.uv_inexus.po.concise.measurementPo.FMeasureTask;
import com.persee.workstation.uv_inexus.po.guide.measurementPo.TMeasureAnalysisData;
import com.persee.workstation.uv_inexus.po.guide.measurementPo.TMeasureMethod;
import com.persee.workstation.uv_inexus.po.guide.measurementPo.model.PeakValleyMarking;
import com.persee.workstation.uv_inexus.service.concise.measurementService.FMeasureMethodService;
import com.persee.workstation.uv_inexus.service.concise.measurementService.FMeasureTaskService;
import com.persee.workstation.uv_inexus.service.concise.measurementService.SpectrumConciseService;
import com.persee.workstation.uv_inexus.service.guide.measurementService.TMeasureMethodService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 简洁模式: 光谱扫描服务业务层实现
 */

@Service("SpectrumConciseService")
public class SpectrumConciseServiceImpl implements SpectrumConciseService {

    @Autowired
    private FMeasureMethodService fMeasureMethodService;

    @Autowired
    private FMeasureTaskService fMeasureTaskService;



    @Autowired
    private TMeasureMethodService tMeasureMethodService;

    /**
     * 查询最近方法数据
     * @param updateBy
     * @param measureType
     * @return
     */
    @Override
    public Result queryNearestMethodData(String updateBy,String measureType) {
        if (updateBy == null){
            return Result.fail("更新人不能为空");
        }

        if (measureType == null){
            return Result.fail("方法类型不能为空");
        }

        QueryWrapper<FMeasureMethod> wrapper = new QueryWrapper<>();
        wrapper.eq("method_creator",updateBy);
        wrapper.eq("is_draft","0");
        wrapper.eq("measurement_type",measureType);
        wrapper.orderByDesc("method_use_time");
        wrapper.last("LIMIT 1");

        FMeasureMethod method = fMeasureMethodService.getById(wrapper);

        if (method==null){
            // todo 查询对应类型的默认方法

        }
        return Result.ok(method);
    }

    /**
     * 查询样品数据
     * @param methodId
     * @return
     */
    @Override
    public Result querySampleDataList(String methodId,String type,Integer digit) {
        if (methodId == null){
            return Result.fail("方法id不能为空");
        }

        // 根据任务id去查询最近创建的任务
        QueryWrapper<FMeasureTask> wrapper = new QueryWrapper<>();
        wrapper.eq("method_id",methodId);
        wrapper.orderByDesc("create_time");
        wrapper.last("LIMIT 1");

        FMeasureTask task = fMeasureTaskService.getOne(wrapper);
        if (task == null){
            return Result.fail("该方法下没有草稿,请新建草稿");
        }

        // 根据当前任务查询
        return null;
    }

    /**
     * 查询最近测量数据
     * @param methodId
     * @return
     */
    @Override
    public Result queryNearestMeasureData(String methodId) {
        return null;
    }

    /**
     * 查找方法按钮
     * @param updateBy
     * @param measureType
     * @param methodName
     * @return
     */
    @Override
    public Result searchMethodButton(Integer peageNum,Integer pageSize,String updateBy, String measureType, String methodName) {
        // 对传入的参数进行校验
        if (updateBy == null){
            return Result.fail("用户不能为空");
        }
        if (measureType == null){
            return Result.fail("方法类型不能为空");
        }
        if (peageNum == null){
            peageNum = 1;
        }
        if (pageSize == null){
            pageSize = 10;
        }
        // 设置查询条件
        QueryWrapper<TMeasureMethod> wrapper = new QueryWrapper<>();
        wrapper.eq("method_creator",updateBy);
        wrapper.eq("measurement_type",measureType);
        if (methodName != null){
            wrapper.like("method_name",methodName);
        }
        // 分页查询
        Page<TMeasureMethod> page = tMeasureMethodService.page(new Page<>(peageNum, pageSize), wrapper);
        return Result.ok(page);
    }








    /**
     * 获取样品的详细数据
     * @param measureData
     * @return
     */
    public List<Map<String, Object>> getSampleData(TMeasureAnalysisData measureData, Integer digit) {
        List<String> list = JSON.parseArray(measureData.getAnalysisResult(), String.class);
        List<PeakValleyMarking> gauges = JSON.parseArray(measureData.getGaugePoint(), PeakValleyMarking.class);
        List<PeakValleyMarking> peaks = JSON.parseArray(measureData.getPeak(), PeakValleyMarking.class);
        List<PeakValleyMarking> grades = JSON.parseArray(measureData.getGrain(), PeakValleyMarking.class);
        List<Map<String, Object>> resultList = new ArrayList<>();
        for (String s : list) {
            String[] split = s.split(",");
            Map<String, Object> map = new HashMap<>();
            if (digit!=null){
                BigDecimal value = new BigDecimal(split[1]);
                value = value.setScale(digit, RoundingMode.HALF_UP);
                map.put("x", split[0]);
                map.put("y", value.stripTrailingZeros().toPlainString());
            }else{
                map.put("x", split[0]);
                map.put("y", split[1]);
            }
            if (gauges != null&&peaks.size()!=0) {
                for (PeakValleyMarking gauge : gauges) {
                    if (split[0].equals(gauge.getX())) {
                        map.put("marked", "1");
                        break;
                    } else {
                        map.put("marked", "0");
                    }
                }
            } else {
                map.put("marked", "0");
            }
            if (peaks != null&&peaks.size()!=0) {
                for (PeakValleyMarking peak : peaks) {
                    if (split[0].equals(peak.getX())) {
                        map.put("feng", "1");
                        break;
                    } else {
                        map.put("feng", "0");
                    }
                }
            } else {
                map.put("feng", "0");
            }
            if (grades != null&&peaks.size()!=0) {
                for (PeakValleyMarking grade : grades) {
                    if (split[0].equals(grade.getX())) {
                        map.put("gu", "1");
                        break;
                    } else {
                        map.put("gu", "0");
                    }
                }
            } else {
                map.put("gu", "0");
            }
            resultList.add(map);
        }
        return resultList;
    }
}
