package com.persee.workstation.uv_inexus.util.deviceUtil;

import com.persee.workstation.uv_inexus.constant.Constants;
import com.persee.workstation.uv_inexus.po.guide.instrumentPo.DeviceInfoPO;
import com.persee.workstation.uv_inexus.service.guide.instrumentService.DeviceInfoService;
import com.persee.workstation.uv_inexus.util.changeUtil.ChangeParam;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.Map;

@Service
@Slf4j
public class SpectrumResultCalculator {

    @Autowired
    private DeviceInfoService deviceInfoService;//计算ABS T% Es Er

    private static final double Z = 1.0;

    /**
     * @param luminosity 1=Abs, 2=T%, 3=Es, 4=Er
     */
    public SpectrumCalcResult calculate(
            int luminosity,
            SpectrumPoint point,
            SpectrumSourceType sourceType,
            String deviceSerialNumber
    ) {

        ChangeParam origin;
        ChangeParam real;

        if (sourceType == SpectrumSourceType.SINGLE) {
            origin = point.getZeroRsd();
            real = point.getMeasureRsd();
        } else {
            origin = point.getBaselineRsd();
            real = point.getScanRsd();
        }

        if (real == null) {
            log.warn("real rsd 为空, wl={}", point.getWavelength());
            return null;
        }

        SpectrumCalcResult result = new SpectrumCalcResult();
        result.setWavelength(point.getWavelength());

        switch (luminosity) {
            case 1: // Abs
                String tPercent = calcTPercent(origin, real, point, deviceSerialNumber);
                result.setAbs(calcAbsFromTPercent(tPercent));
                break;

            case 2: // T%
                result.setTPercent(calcTPercent(origin, real, point, deviceSerialNumber));
                break;

            case 3: // Es
                result.setEs(calcEs(real));
                break;

            case 4: // Er
                result.setEr(calcEr(real));
                break;

            default:
                throw new IllegalArgumentException("未知 Luminosity=" + luminosity);
        }

        return result;
    }

    // ======================= 核心公式 =======================

    /** 透光率 T% */
    public String calcTPercent(ChangeParam origin, ChangeParam real,SpectrumPoint sp,String deviceSerialNumber) {
        try {
            double d1 = Double.parseDouble(real.getD());
            double r1 = Double.parseDouble(real.getR());
            double s1 = Double.parseDouble(real.getS());
            String wl = String.valueOf(sp.getWavelength());

//            Double Zobj = sp.getDarkZ();
//            double Z = (Zobj == null) ? 1.0 : Zobj;

            Double Z = getZFromDatabase(deviceSerialNumber, wl);
            Z = (Z == null) ? 1.0 : Z;

            double a1;
            if (origin == null || origin.isEmpty()) {
                a1 = 1.0;
            } else {
                double r0 = Double.parseDouble(origin.getR());
                double s0 = Double.parseDouble(origin.getS());
                double d0 = Double.parseDouble(origin.getD());

                double v0 = Z * d0;
                a1 = (s0 - v0) / (r0 - v0);
            }

            double v1 = Z * d1;
            double a2 = (s1 - v1) / (r1 - v1);

            double ans = a2 / a1 * 100.0;
            return BigDecimal.valueOf(ans)
                    .setScale(Constants.DEFAULT_DECIMAL_NUM_MAX, RoundingMode.HALF_UP)
                    .toPlainString();

        } catch (Exception e) {
            log.error("T% 计算异常", e);
            return "0";
        }
    }

    /** Abs = -log10(T% / 100) */
    public static String calcAbsFromTPercent(String tPercent) {
        try {
            BigDecimal d = new BigDecimal(tPercent);
            BigDecimal min = new BigDecimal("1.0e-10");
            if (d.compareTo(min) < 0) {
                d = min;
            }

            double ratio = d.divide(BigDecimal.valueOf(100), MathContext.DECIMAL128).doubleValue();
            double abs = -Math.log10(ratio);

            return BigDecimal.valueOf(abs)
                    .setScale(Constants.DEFAULT_DECIMAL_NUM_MAX, RoundingMode.HALF_UP)
                    .toPlainString();
        } catch (Exception e) {
            log.error("Abs 计算异常", e);
            return "-10";
        }
    }

    /** Es */
    private static String calcEs(ChangeParam scan) {
        double s = Double.parseDouble(scan.getS());
        return String.valueOf((s / 8388607d) * 100);
    }

    /** Er */
    private static String calcEr(ChangeParam scan) {
        double r = Double.parseDouble(scan.getR());
        return String.valueOf((r / 8388607d) * 100);
    }

    private Double getZFromDatabase(String deviceSerialNumber, String wl) {

        DeviceInfoPO po = deviceInfoService.getInfo(deviceSerialNumber);

        if (po != null && po.getDarkInfo1() != null) {
            Map<String, String> darkInfo = po.getDarkInfo1();
            return Double.parseDouble(darkInfo.get(wl));  // 根据波长 wl 获取对应的 Z 值
        }
        return null;  // 如果没有找到 Z 值，返回 null
    }
}


