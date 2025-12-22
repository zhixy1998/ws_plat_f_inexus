/*
 * *
 *  * 版权所有 (C) 2025 北京普析通用仪器有限责任公司。保留所有权利。
 *  * 本软件受商业机密法和版权法保护。
 *  * 本代码仅限与北京普析通用仪器有限责任公司签订有效协议的授权用户内部使用。
 *  * 任何未经明确许可的复制、使用、修改、分发或公开均被严格禁止。
 *
 */

package com.persee.workstation.uv_inexus.util.changeUtil;

import com.alibaba.fastjson2.JSONArray;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.persee.workstation.uv_inexus.dao.guide.instrumentDao.DeviceInfoDAO;
import com.persee.workstation.uv_inexus.po.guide.instrumentPo.model.LinearSegment;
import com.persee.workstation.uv_inexus.po.guide.instrumentPo.model.StepPoint;
import com.persee.workstation.uv_inexus.service.guide.instrumentService.DeviceInfoService;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

@Component
public class FormulaUtil {
    @Autowired
    DeviceInfoDAO deviceInfoDAO;
    @Autowired
    DeviceInfoService infoService;
    @Autowired
    MessageUtil messageUtil;

    /*
     * 根据步数计算波长
     * */
    public Double calculateWl(double actualStep, String deviceSerialNumber) {
        try {
            List<LinearSegment> linearSegment = this.getLinearSegment(deviceSerialNumber);
            if (CollectionUtils.isNotEmpty(linearSegment)) {
                // 先查找是否在某个线段范围内
                for (LinearSegment segment : linearSegment) {
                    if (segment.containsActualStep(actualStep)) {
                        return roundToOneDecimal((segment.getK() * actualStep+ segment.getB())/ 50);
                    }
                }

                // 如果不在任何线段范围内，则判断是小于最小值还是大于最大值
                LinearSegment firstSegment = linearSegment.get(0);
                LinearSegment lastSegment = linearSegment.get(linearSegment.size() - 1);

                // 如果小于最小线段的起始点，则使用最小线段计算
                if (actualStep < firstSegment.getMinActualStep()) {
                    return roundToOneDecimal((firstSegment.getK() * actualStep + firstSegment.getB()) / 50);
                }

                // 如果大于最大线段的结束点，则使用最大线段计算
                if (actualStep > lastSegment.getMaxActualStep()) {
                    return roundToOneDecimal((lastSegment.getK() * actualStep + lastSegment.getB()) / 50);
                }
            }
        } catch (Exception e) {
            return actualStep / 50;
        }
        return actualStep / 50;
    }

    /**
     * 根据步长计算波长
     *
     * @param actualStep
     * @param linearSegments
     * @return
     */
    public Double calculateWavelength(double actualStep, List<LinearSegment> linearSegments) {
        try {
            if (CollectionUtils.isNotEmpty(linearSegments)) {
                for (LinearSegment segment : linearSegments) {
                    if (segment.containsActualStep(actualStep)) {
                        return roundToOneDecimal((segment.getK() * actualStep + segment.getB()) / 50);
                    }
                }
                // 如果不在任何线段范围内，则判断是小于最小值还是大于最大值
                LinearSegment firstSegment = linearSegments.get(0);
                LinearSegment lastSegment = linearSegments.get(linearSegments.size() - 1);

                // 如果小于最小线段的起始点，则使用最小线段计算
                if (actualStep < firstSegment.getMinActualStep()) {
                    return roundToOneDecimal((firstSegment.getK() * actualStep + firstSegment.getB()) / 50);
                }

                // 如果大于最大线段的结束点，则使用最大线段计算
                if (actualStep > lastSegment.getMaxActualStep()) {
                    return roundToOneDecimal((lastSegment.getK() * actualStep + lastSegment.getB()) / 50);
                }
            }
        } catch (Exception e) {
            return actualStep / 50;
        }
        return actualStep / 50;
    }


    /**
     * 根据步长计算波长2(光谱扫描基线/扫描调用)[修约小数位数10位]
     *
     * @param actualStep
     * @param linearSegments
     * @return
     */
    public Double calculateWavelengthTwo(double actualStep, List<LinearSegment> linearSegments) {
        try {
            if (CollectionUtils.isNotEmpty(linearSegments)) {
                for (LinearSegment segment : linearSegments) {
                    if (segment.containsActualStep(actualStep)) {
                        return roundToOneDecimalTwo((segment.getK() * actualStep + segment.getB()) / 50);
                    }
                }
                // 如果不在任何线段范围内，则判断是小于最小值还是大于最大值
                LinearSegment firstSegment = linearSegments.get(0);
                LinearSegment lastSegment = linearSegments.get(linearSegments.size() - 1);

                // 如果小于最小线段的起始点，则使用最小线段计算
                if (actualStep < firstSegment.getMinActualStep()) {
                    return roundToOneDecimalTwo((firstSegment.getK() * actualStep + firstSegment.getB()) / 50);
                }

                // 如果大于最大线段的结束点，则使用最大线段计算
                if (actualStep > lastSegment.getMaxActualStep()) {
                    return roundToOneDecimalTwo((lastSegment.getK() * actualStep + lastSegment.getB()) / 50);
                }
            }
        } catch (Exception e) {
            return actualStep / 50;
        }
        return actualStep / 50;
    }

    private Double roundToOneDecimal(Double value) {
        if (value == null) {
            return null;
        }
        BigDecimal bd = new BigDecimal(value.toString());
        bd = bd.setScale(1, RoundingMode.HALF_UP);
        return bd.doubleValue();
    }

    private Double roundToOneDecimalTwo(Double value) {
        if (value == null) {
            return null;
        }
        BigDecimal bd = new BigDecimal(value.toString());
        bd = bd.setScale(10, RoundingMode.HALF_UP);
        return bd.doubleValue();
    }

    /**
     * 获取公式
     * @param deviceSerialNumber
     * @return
     */
    public List<LinearSegment> getLinearSegment(String deviceSerialNumber) {
        String formula = deviceInfoDAO.getFormula(deviceSerialNumber);
        return JSONArray.parseArray(formula, LinearSegment.class);
    }

    /*
     * 根据波长计算步数
     * */
    public Double calculateStep(double wl, String deviceSerialNumber) {
        try {
            List<LinearSegment> linearSegment = getLinearSegment(deviceSerialNumber);
            if (CollectionUtils.isNotEmpty(linearSegment)) {
                for (LinearSegment segment : linearSegment) {
                    if (segment.containsWl(wl)) {
                        return roundToOneDecimal((wl * 50 - segment.getB()) / segment.getK());
                    }
                }
                // 如果不在任何线段范围内，则判断是小于最小值还是大于最大值
                LinearSegment firstSegment = linearSegment.get(0);
                LinearSegment lastSegment = linearSegment.get(linearSegment.size() - 1);

                // 如果小于最小线段的起始点，则使用最小线段计算
                if (wl*50 < firstSegment.getMinTheoryStep()) {
                    return roundToOneDecimal((wl * 50 - firstSegment.getB()) / firstSegment.getK());
                }

                // 如果大于最大线段的结束点，则使用最大线段计算
                if (wl*50 > lastSegment.getMaxActualStep()) {
                    return roundToOneDecimal((wl * 50 - lastSegment.getB()) / lastSegment.getK());
                }
            }
        } catch (Exception e) {
            return wl * 50;
        }
        return wl * 50;
    }

    /**
     * 根据波长计算步数
     *
     * @param wl
     * @param list
     * @return
     */
    public Double calculateStepByWavelength(double wl, List<LinearSegment> list) {
        try {
            if (CollectionUtils.isNotEmpty(list)) {
                for (LinearSegment segment : list) {
                    if (segment.containsWl(wl)) {
                        return roundToOneDecimal((wl * 50 - segment.getB() )/ segment.getK());
                    }
                }
            }
        } catch (Exception e) {
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
    public String addFormula(Object obj, String deviceSerialNumber) {
        List<StepPoint> stepPoints = new ArrayList<>();

        if (obj instanceof List<?>) {
            List<?> rawList = (List<?>) obj;
            if (!rawList.isEmpty() && rawList.get(0) instanceof StepPoint) {
                stepPoints = (List<StepPoint>) obj; // 安全转换
            } else {
                return "";
            }
        } else {
            return "";
        }
        if (CollectionUtils.isEmpty(stepPoints) || StringUtils.isBlank(deviceSerialNumber)) {
            return "";
        }
        stepPoints.sort(new Comparator<StepPoint>() {
            @Override
            public int compare(StepPoint s1, StepPoint s2) {
                return Double.compare(s1.getActualStep(), s2.getActualStep());
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
        return JSONArray.from(list).toString();
    }

}
