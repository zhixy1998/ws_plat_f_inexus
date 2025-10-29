package com.persee.workstation.uv_inexus.util.changeUtil;

import com.alibaba.fastjson2.JSONArray;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.persee.workstation.uv_inexus.dao.guide.instrumentDao.DeviceInfoDAO;
import com.persee.workstation.uv_inexus.model.Result;
import com.persee.workstation.uv_inexus.po.guide.instrumentPo.model.LinearSegment;
import com.persee.workstation.uv_inexus.po.guide.instrumentPo.model.StepPoint;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class FormulaUtil {
    @Autowired
    DeviceInfoDAO deviceInfoDAO;

    /*
    * 根据步数计算波长
    * */
    public double calculateWl(double actualStep, String deviceSerialNumber) {
        try {
            List<LinearSegment> linearSegment = getLinearSegment(deviceSerialNumber);
            if (CollectionUtils.isNotEmpty(linearSegment)) {
                for (LinearSegment segment : linearSegment) {
                    if (segment.containsActualStep(actualStep)) {
                        return (segment.getK() * actualStep + segment.getB()) / 50;
                    }
                }
            }
        }catch (Exception e){
            return actualStep / 50;
        }
        return actualStep / 50;
    }
    public List<LinearSegment> getLinearSegment(String deviceSerialNumber){
        String formula = deviceInfoDAO.getFormula(deviceSerialNumber);
        return JSONArray.parseArray(formula, LinearSegment.class);
    }

    /*
    * 根据波长计算步数
    * */
    public double calculateStep(double wl, String deviceSerialNumber) {
        try {
            List<LinearSegment> linearSegment = getLinearSegment(deviceSerialNumber);
            if (CollectionUtils.isNotEmpty(linearSegment)){
            for (LinearSegment segment : linearSegment) {
                if (segment.containsWl(wl)) {
                    return (wl * 50 - segment.getB()) / segment.getK();
                }
            }
            }
        }catch (Exception e){
            return wl * 50;
        }
        return wl * 50;
    }

    public HashMap<String, Double> getLine(StepPoint p1, StepPoint p2) {

        // 处理垂直线特殊情况
        if (Math.abs(p1.getActualStep() - p2.getActualStep()) < 1e-8) {
            System.out.println("垂直线");
        }

        double k = (p2.getTheoryStep() - p1.getTheoryStep()) / (p2.getActualStep() - p1.getActualStep());
        double b = p1.getTheoryStep() - k * p1.getActualStep();

        // 处理水平线特殊情况
        if (Math.abs(k) < 1e-8) {
            System.out.println("水平线");
        }
        HashMap<String, Double> map = new HashMap<>();
        map.put("k", k);
        map.put("b", b);
        return map;
    }

    //添加计算公式
    public Result addFormula(List<StepPoint> stepPoints, String deviceSerialNumber) {
        if (CollectionUtils.isEmpty(stepPoints)|| StringUtils.isBlank(deviceSerialNumber)){
            return Result.fail("stepPoints，deviceSerialNumber不能为空！！！");
        }
        stepPoints.sort(new Comparator<StepPoint>() {
            @Override
            public int compare(StepPoint s1, StepPoint s2) {
                return Double.compare(s1.getActualStep(),s2.getActualStep());
            }
        });
        //要存到数据库中的json串
        ArrayList<LinearSegment> list = new ArrayList<>();
        for (int i = 0; i < stepPoints.size(); i++) {
            if (stepPoints.size() == i + 1) {
                break;
            }
            StepPoint point1 = stepPoints.get(i);
            StepPoint point2 = stepPoints.get(i + 1);
            HashMap<String, Double> line = getLine(point1, point2);
            Double k = MapUtils.getDouble(line, "k");
            Double b = MapUtils.getDouble(line, "b");
            list.add(new LinearSegment(point1.getActualStep(), point2.getActualStep(), point1.getTheoryStep(), point2.getTheoryStep(), k, b));
        }
        int i=deviceInfoDAO.updateFormula(deviceSerialNumber, JSONArray.from(list).toString());
        return i!=0?Result.ok():Result.fail();
    }
}
