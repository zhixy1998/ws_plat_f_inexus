package com.persee.workstation.uv_inexus.util.deviceUtil;

import com.persee.workstation.uv_inexus.po.guide.instrumentPo.model.LinearSegment;
import com.persee.workstation.uv_inexus.util.changeUtil.ChangeParam;
import com.persee.workstation.uv_inexus.util.changeUtil.FormulaUtil;
import com.persee.workstation.uv_inexus.util.deviceUtil.param.DeviceConnectInfo;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Slf4j
public class SpectrumDataProcessor {                 //计算RSD

    private final FormulaUtil formulaUtil;

    private final DeviceConnectInfo device;


    /** 最近一个完整的光谱点（baseline + scan） */
    private volatile SpectrumPoint currentPoint;

    /** 当前扫描过程的数据点索引（从 0 开始） */
    private int dataPointIndex = 0;

    /** 是否接近扫描结束点的标志（离结束小于10个点时为false） */
    private static volatile boolean isNotNearEnd = true;

    /** 预定义的接近结束的点数阈值 */
    private static final int NEAR_END_THRESHOLD = 10;


    /** 测试阶段使用的固定波长起点、终点、间隔 */
//    private final double startWl = 700.0;
//    private final double endWl = 600.0;
//    private final double interval = 0.2;
//    private final boolean isDescending = true;
//    private final int maxIndex = (int) Math.round(Math.abs(startWl - endWl) / interval);

    public SpectrumDataProcessor(
            FormulaUtil formulaUtil,
            DeviceConnectInfo device
    ) {
        this.formulaUtil = formulaUtil;
        this.device = device;
    }


    /** 扫描开始前重置 index */
    public void reset() {
        dataPointIndex = 0;
        isNotNearEnd = true;

        if (device.getScanWavelengthConfig() == null) {
            log.warn("[SPECTRUM] reset called but scan config not set");
        }
    }


    /** 测试用：步数 -> 真实波长 -> 强制约束波长 -> 拟合 r/s/d */
    public void processStepAndPrintWavelength(ChangeParam param, String deviceSerialNumber) {

        if (param == null || param.getWl() == null) return;

        // ================= 1. 步数 -> 真实波长 =================
        double step;
        try {
            step = Double.parseDouble(param.getWl());
        } catch (NumberFormatException e) {
            log.warn("[SPECTRUM] invalid step wl={}", param.getWl());
            return;
        }

        List<LinearSegment> segments = formulaUtil.getLinearSegment(deviceSerialNumber);
        Double realWavelength = null;
        realWavelength = formulaUtil.calculateWavelengthTwo(step, segments);

        // ================= 2. 强制约束波长 =================
        double constrainedWl = getConstrainedWavelength();

        // ================= 3. 拟合 r/s/d （T800不需要）=================
        // 用步数差计算拟合，避免 int 溢出
        ChangeParam fittedParam = calculateByStep(lastStepParam, param, step);

        // ================= 4. 打印 =================
        //注释打印
        log.info("[SPECTRUM-CONSTRAIN] index={}, step={}, realWl={}, constrainedWl={}",
                dataPointIndex,
                step,
                realWavelength,
                constrainedWl
        );

//        log.info("[SPECTRUM-FITTED] index={}, targetStep={}, r={}, s={}, d={}",
//                dataPointIndex,
//                step,
//                fittedParam.getR(),
//                fittedParam.getS(),
//                fittedParam.getD()
//        );

        // 更新 lastStepParam
        lastStepParam = param;

        // index 前进，边界保护
        dataPointIndex++;
        int maxIndex = getMaxIndex();
        if (dataPointIndex > maxIndex) {
            dataPointIndex = maxIndex;
        }

    }

    /** 根据步数做线性拟合 r/s/d，避免 int 溢出 */
    private ChangeParam lastStepParam = null; // 上一个点，用于拟合

    public static ChangeParam calculateByStep(ChangeParam start, ChangeParam end, Double targetWl) {
        if (start == null || end == null) return end;

        double startStep = Double.parseDouble(start.getWl());
        double startWl = startStep / 40.0;
        double endStep = Double.parseDouble(end.getWl());
        double endWl = endStep / 40.0;
        int startR = Integer.parseInt(start.getR());
        int endR = Integer.parseInt(end.getR());
        int startS = Integer.parseInt(start.getS());
        int endS = Integer.parseInt(end.getS());
        int dValue = Integer.parseInt(start.getD());

        // 计算拟合曲线
        double slopeR = (double) (endR - startR) / (endWl - startWl);
        double interceptR = startR - slopeR * startWl;

        double slopeS = (double) (endS - startS) / (endWl - startWl);
        double interceptS = startS - slopeS * startWl;

        // 计算r和s值
        int r = (int) Math.round(slopeR * targetWl + interceptR);
        int s = (int) Math.round(slopeS * targetWl + interceptS);

        // 格式化wl字符串
        String wlStr = String.valueOf(targetWl);

        return new ChangeParam(wlStr, wlStr, String.valueOf(r), String.valueOf(s), String.valueOf(dValue));
    }

    /** 根据当前 index 返回测试阶段强制约束波长 */
//    double getConstrainedWavelength() {
//        double wl = isDescending
//                ? startWl - dataPointIndex * interval
//                : startWl + dataPointIndex * interval;
//
//        // 边界吸附
//        if (isDescending && wl < endWl) wl = endWl;
//        if (!isDescending && wl > endWl) wl = endWl;
//
//        // 保留 interval 精度
//        int decimalPlaces = getDecimalPlaces(interval);
//        return roundToPrecision(wl, decimalPlaces);
//    }

    // ================= 工具方法 =================
    //获取小数位数
    double getConstrainedWavelength() {

        ScanWavelengthConfig cfg = device.getScanWavelengthConfig();
        if (cfg == null) {
            log.warn("[SPECTRUM] scanWavelengthConfig not set, skip constrain");
            return Double.NaN;
        }

        double startWl = cfg.getStartWavelength();
        double endWl = cfg.getEndWavelength();
        double interval = cfg.getInterval();
        boolean isDescending = cfg.getDescending();
        double wl = isDescending
                ? (startWl - dataPointIndex * interval)
                : (startWl + dataPointIndex * interval);

        // 边界吸附
        if (isDescending && wl < endWl) return Double.NaN;
        if (!isDescending && wl > endWl) return Double.NaN;

        //停止条件
        updateNearEndFlag();

        int decimalPlaces = getDecimalPlaces(interval);
        return roundToPrecision(wl, decimalPlaces);
    }

    private int getDecimalPlaces(double interval) {
        String str = String.valueOf(interval);
        int index = str.indexOf(".");
        return index < 0 ? 0 : str.length() - index - 1;
    }

    //四舍五入
    private double roundToPrecision(double value, int decimalPlaces) {
        BigDecimal bd = BigDecimal.valueOf(value);
        return bd.setScale(decimalPlaces, RoundingMode.HALF_UP).doubleValue();
    }

    private int getMaxIndex() {
        ScanWavelengthConfig cfg = device.getScanWavelengthConfig();
        if (cfg == null) return 0;

        return (int) Math.round(
                Math.abs(cfg.getStartWavelength() - cfg.getEndWavelength())
                        / cfg.getInterval()
        );
    }

    private void onBaselinePoint(double constrainedWl, ChangeParam fittedBaselineParam) {

        SpectrumPoint point = new SpectrumPoint(constrainedWl);
        point.setBaselineRsd(fittedBaselineParam);

        // 注意：此时 scan 还没来
        this.currentPoint = point;
    }

    private void onScanPoint(double constrainedWl, ChangeParam fittedScanParam) {

        // 如果 baseline 已经存在，复用
        SpectrumPoint point = this.currentPoint;

        if (point == null || point.getWavelength() != constrainedWl) {
            // 极端保护：baseline 丢了（理论上不该发生）
            point = new SpectrumPoint(constrainedWl);
        }

        point.setScanRsd(fittedScanParam);

        this.currentPoint = point;
    }

    public SpectrumPoint getCurrentPoint() {
        return currentPoint;
    }

    /**
     * 更新是否接近结束的标志
     * 当剩余点数小于阈值时，设置标志为 false
     */
    private void updateNearEndFlag() {
        int maxIndex = getMaxIndex();
        int remainingPoints = maxIndex - dataPointIndex;

        // 当剩余点数小于等于阈值时，设置为 false
        this.isNotNearEnd = remainingPoints > NEAR_END_THRESHOLD;
    }

    public static boolean isNotNearEnd() {
        return isNotNearEnd;
    }
}

