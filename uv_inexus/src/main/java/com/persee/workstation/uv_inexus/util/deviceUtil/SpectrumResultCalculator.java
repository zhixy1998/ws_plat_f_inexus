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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
public class SpectrumResultCalculator {

    @Autowired
    private DeviceInfoService deviceInfoService;//计算ABS T% Es Er

//    private static final double Z = 1.0;


    private final Map<String, List<Double>> wavelengthCache = new ConcurrentHashMap<>();
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

            case 5:
                String tPercent1960 = calcTPercent1960(origin, real, point, deviceSerialNumber);
                result.setAbs(calcAbsFromTPercent(tPercent1960));
                break;

            case 6:
                result.setTPercent(calcTPercent1960(origin, real, point, deviceSerialNumber));
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
            BigDecimal w = BigDecimal.valueOf(sp.getWavelength());
            String wl = w.stripTrailingZeros().toPlainString();


            Double Z = getZFromDatabase(deviceSerialNumber, wl);
            Z = (Z == null) ? 1.0 : Z;

            log.info("Wl={}, Z={}", wl, Z);

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
            log.error("T% 计算异常, origin={}, real={}, sp={}", origin, real, sp, e);
            return "0";
        }
    }

    public String calcTPercent1960(ChangeParam origin, ChangeParam real,SpectrumPoint sp,String deviceSerialNumber) {
        try {
            double d1 = Double.parseDouble(real.getD());
            double r1 = Double.parseDouble(real.getR());
            double s1 = Double.parseDouble(real.getS());
            BigDecimal w = BigDecimal.valueOf(sp.getWavelength());
            String wl = w.stripTrailingZeros().toPlainString();

//            Double Zobj = sp.getDarkZ();
//            double Z = (Zobj == null) ? 1.0 : Zobj;

            Double Z = getZFromDatabase2(deviceSerialNumber, wl);
            Z = (Z == null) ? 0.1 : Z;


            double a1;
            if (origin == null || origin.isEmpty()) {
                a1 = 1.0;
            } else {
                double r0 = Double.parseDouble(origin.getR());
                double s0 = Double.parseDouble(origin.getS());
                double d0 = Double.parseDouble(origin.getD());

                double v0 = (d0 - Z * r0) / (1 - Z);
                a1 = (s0 - v0) / (r0 - v0);
            }

            double v1 =  (d1 - Z * r1) / (1 - Z);
            double a2 = (s1 - v1) / (r1 - v1);

            double ans = a2 / a1 * 100.0;
            return BigDecimal.valueOf(ans)
                    .setScale(Constants.DEFAULT_DECIMAL_NUM_MAX, RoundingMode.HALF_UP)
                    .toPlainString();

        } catch (Exception e) {
            log.error("T% 计算异常, origin={}, real={}, sp={}", origin, real, sp, e);
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

        if (po != null && po.getDarkInfo() != null) {
            Map<String, String> darkInfo = po.getDarkInfo();
//            return Double.parseDouble(darkInfo.get(wl));  // 根据波长 wl 获取对应的 Z 值
            if (darkInfo.get(wl) != null){
                return Double.parseDouble(darkInfo.get(wl));
            }
            return null;
        }
        return null;  // 如果没有找到 Z 值，返回 null
    }

    Double getZFromDatabase2(String deviceSerialNumber, String wl) {

        try {
            DeviceInfoPO po = deviceInfoService.getInfo(deviceSerialNumber);
            if (po == null || po.getDarkInfo() == null) {
                return null;
            }

            Map<String, String> darkInfo = po.getDarkInfo();

            //当前波长
            double x = Double.parseDouble(wl);

            //精确匹配
            String exact = darkInfo.get(wl);
            if (exact != null && !exact.isEmpty()) {
                return Double.parseDouble(exact);
            }

            //获取排序后的波长
            List<Double> wavelengths = getSortedWavelengths(deviceSerialNumber, darkInfo);

            if (wavelengths.isEmpty()) {
                return null;
            }

            if (wavelengths.size() == 1) {
                return Double.parseDouble(
                        darkInfo.get(String.valueOf(wavelengths.get(0)))
                );
            }

            //查找
            int left = 0;
            int right = wavelengths.size() - 1;

            while (left <= right) {

                int mid = (left + right) / 2;
                double midVal = wavelengths.get(mid);

                if (midVal == x) {
                    return Double.parseDouble(darkInfo.get(String.valueOf(midVal)));
                } else if (midVal < x) {
                    left = mid + 1;
                } else {
                    right = mid - 1;
                }
            }


            //边界处理
            if (right < 0) {
                // x 比最小波长还小
                double minWl = wavelengths.get(0);
                return Double.parseDouble(darkInfo.get(String.valueOf(minWl)));
            }

            if (left >= wavelengths.size()) {
                // x 比最大波长还大
                double maxWl = wavelengths.get(wavelengths.size() - 1);
                return Double.parseDouble(darkInfo.get(String.valueOf(maxWl)));
            }


            double x1 = wavelengths.get(right);
            double x2 = wavelengths.get(left);

            BigDecimal bd1 = BigDecimal.valueOf(x1);
            BigDecimal bd2 = BigDecimal.valueOf(x2);
            String key1 = bd1.stripTrailingZeros().toPlainString();
            String key2 = bd2.stripTrailingZeros().toPlainString();

            double y1 = Double.parseDouble(darkInfo.get(key1));
            double y2 = Double.parseDouble(darkInfo.get(key2));

            if (Math.abs(y2 - y1) < 1e-7) {
                return y1;
            }

            return y1 + (x - x1) * (y2 - y1) / (x2 - x1);

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private List<Double> getSortedWavelengths(String deviceSerialNumber, Map<String, String> darkInfo) {

        return wavelengthCache.computeIfAbsent(deviceSerialNumber, k -> {

            List<Double> list = new ArrayList<>();

            for (String key : darkInfo.keySet()) {
                try {
                    list.add(Double.parseDouble(key));
                } catch (Exception ignored) {}
            }

            Collections.sort(list);
            return list;
        });
    }
}


