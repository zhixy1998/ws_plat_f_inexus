package com.persee.workstation.uv_inexus.util.deviceUtil;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.persee.workstation.uv_inexus.constant.Constants;
import com.persee.workstation.uv_inexus.dao.concise.measurementDao.SamplePoolConfigDAO;
import com.persee.workstation.uv_inexus.enums.instrumentEnums.sphereUsedEnum;
import com.persee.workstation.uv_inexus.po.concise.common.FSamplePoolStatus;
import com.persee.workstation.uv_inexus.po.concise.measurementPo.SamplePoolConfigPO;
import com.persee.workstation.uv_inexus.po.guide.instrumentPo.DeviceInfoPO;
import com.persee.workstation.uv_inexus.service.guide.instrumentService.DeviceInfoService;
import com.persee.workstation.uv_inexus.util.changeUtil.ChangeParam;
import com.persee.workstation.uv_inexus.vo.instrumentVo.DeviceAttachmentTypeVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
public class SpectrumResultCalculator {

    @Autowired
    private DeviceInfoService deviceInfoService;//计算ABS T% Es Er
    @Autowired
    SamplePoolConfigDAO dao;
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
        Boolean rsMode = isRs(deviceSerialNumber);
      /*  switch (luminosity) {
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

            case 7:
                result.setEs(calcEsT1960(real));
                break;

            case 8:
                result.setEr(calcErT1960(real));
                break;

            default:
                throw new IllegalArgumentException("未知 Luminosity=" + luminosity);
        }*/
        switch (luminosity) {
            case 1: // Abs
                String tPercent = calcTPercent(origin, real, point, deviceSerialNumber, rsMode);
                result.setAbs(calcAbsFromTPercent(tPercent));
                break;
            case 2: // T%
                result.setTPercent(calcTPercent(origin, real, point, deviceSerialNumber, rsMode));
                break;
            case 3: // Es
                result.setEs(calcEs(real, rsMode));
                break;
            case 4: // Er
                result.setEr(calcEr(real, rsMode));
                break;
            case 5: // Abs(T1960)
                String tPercent1960 = calcTPercent1960(origin, real, point, deviceSerialNumber, rsMode);
                result.setAbs(calcAbsFromTPercent(tPercent1960));
                break;
            case 6: // T%(T1960)
                result.setTPercent(calcTPercent1960(origin, real, point, deviceSerialNumber, rsMode));
                break;
            case 7: // Es(T1960)
                result.setEs(calcEsT1960(real, rsMode));
                break;
            case 8: // Er(T1960)
                result.setEr(calcErT1960(real, rsMode));
                break;
            default:
                throw new IllegalArgumentException("未知 Luminosity=" + luminosity);
        }
        return result;
    }

    // ======================= 核心公式 =======================

    /**
     * 透光率 T%
     */
    public String calcTPercent(ChangeParam origin, ChangeParam real, SpectrumPoint sp, String deviceSerialNumber, boolean rsMode) {
        try {
            double d1 = Double.parseDouble(real.getD());
          /*  double r1 = Double.parseDouble(real.getR());
            double s1 = Double.parseDouble(real.getS());*/
            double r1 = getReferenceSignal(real, rsMode);
            double s1 = getSampleSignal(real, rsMode);
            BigDecimal w = BigDecimal.valueOf(sp.getWavelength());
            String wl = w.stripTrailingZeros().toPlainString();


            Double Z = getZFromDatabase(deviceSerialNumber, wl, rsMode);
            Z = (Z == null) ? 1.0 : Z;

            log.info("Wl={}, Z={}", wl, Z);

            double a1;
            if (origin == null || origin.isEmpty()) {
                a1 = 1.0;
            } else {
                /*double r0 = Double.parseDouble(origin.getR());
                double s0 = Double.parseDouble(origin.getS());*/
                double r0 = getReferenceSignal(origin, rsMode);
                double s0 = getSampleSignal(origin, rsMode);
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

  /*  public String calcTPercent1960(ChangeParam origin, ChangeParam real, SpectrumPoint sp, String deviceSerialNumber) {
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

            double v1 = (d1 - Z * r1) / (1 - Z);
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
    */

    public String calcTPercent1960(ChangeParam origin, ChangeParam real, SpectrumPoint sp,
                                   String deviceSerialNumber, boolean rsMode) {
        try {
            double d1 = Double.parseDouble(real.getD());
            double r1 = getReferenceSignal(real, rsMode);
            double s1 = getSampleSignal(real, rsMode);
            BigDecimal w = BigDecimal.valueOf(sp.getWavelength());
            String wl = w.stripTrailingZeros().toPlainString();

            Double Z = getZFromDatabase2(deviceSerialNumber, wl);
            Z = (Z == null) ? 0.1 : Z;

            double a1;
            if (origin == null || origin.isEmpty()) {
                a1 = 1.0;
            } else {
                double r0 = getReferenceSignal(origin, rsMode);
                double s0 = getSampleSignal(origin, rsMode);
                double d0 = Double.parseDouble(origin.getD());
                double v0 = (d0 - Z * r0) / (1 - Z);
                a1 = (s0 - v0) / (r0 - v0);
            }

            double v1 = (d1 - Z * r1) / (1 - Z);
            double a2 = (s1 - v1) / (r1 - v1);

            double ans = a2 / a1 * 100.0;
            return BigDecimal.valueOf(ans)
                    .setScale(Constants.DEFAULT_DECIMAL_NUM_MAX, RoundingMode.HALF_UP)
                    .toPlainString();
        } catch (Exception e) {
            log.error("T1960 计算异常, origin={}, real={}, sp={}", origin, real, sp, e);
            return "0";
        }
    }

    /**
     * Abs = -log10(T% / 100)
     */
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

    //=====================================

    /**
     * Es：样品能量。SR 完全交换后样品在 R 光路
     */
    private static String calcEs(ChangeParam scan, boolean rsMode) {
        double s = getSampleSignal(scan, rsMode);
        return String.valueOf((s / 8388607d) * 100);
    }

    /**
     * Er：参比能量。SR 完全交换后参比在 S 光路
     */
    private static String calcEr(ChangeParam scan, boolean rsMode) {
        double r = getReferenceSignal(scan, rsMode);
        return String.valueOf((r / 8388607d) * 100);
    }

    /**
     * EsT1960
     */
    private static String calcEsT1960(ChangeParam scan, boolean rsMode) {
        double s = getSampleSignal(scan, rsMode);
        return String.valueOf((s * 64 / 8388607d) * 100);
    }

    /**
     * ErT1960
     */
    private static String calcErT1960(ChangeParam scan, boolean rsMode) {
        double r = getReferenceSignal(scan, rsMode);
        return String.valueOf((r * 64 / 8388607d) * 100);
    }

    /*
     * 是否使用RS交换
     * 先判断是否是积分球，不是就直接返回true
     * true 不使用
     * false 使用*/
    Boolean isRs(String deviceSerialNumber) {
        boolean res = true;
        LambdaQueryWrapper<SamplePoolConfigPO> query = new LambdaQueryWrapper<>();
        query.eq(SamplePoolConfigPO::getDeviceSerialNumber, deviceSerialNumber);
        SamplePoolConfigPO po = dao.selectOne(query);
        //=========================
        List<DeviceAttachmentTypeVO> shpjereTypelist = deviceInfoService.getAttachmentListByName("球");
        Integer currentAttachmentType = po.getAttachmentType();
        if (shpjereTypelist != null && !shpjereTypelist.isEmpty()) {
            for (DeviceAttachmentTypeVO vo : shpjereTypelist) {
                if (Objects.equals(vo.getAttachmentNum(), currentAttachmentType) && !po.getAttachmentTypeName().contains("球")) {
                    return true;
                }
            }
        }
        if (po != null && po.getPoolInfo() != null) {
            // 当前样品池编号(实际存放的使用场景编号)
            Integer currentPoolNum = po.getCurrentPoolNum();
            List<FSamplePoolStatus> PoolStatus = po.getPoolInfo();
            FSamplePoolStatus current = PoolStatus.stream()
                    .filter(s -> currentPoolNum != null && currentPoolNum.equals(s.getPoolNum()))
                    .findFirst()
                    .orElse(null);
            // 样品池用途 实际存放的是 使用rs还是sr；
            // 先判空，找不到池位就保持默认 RS
            if (current != null && sphereUsedEnum.SR.getCode().equals(current.getPoolPurpose())) {
                res = false;
            }
        }
        return res;
    }

    /**
     * 样品信号：RS 样品在 S 光路；SR 完全交换后样品在 R 光路
     */
    private static double getSampleSignal(ChangeParam scan, boolean rsMode) {
        return rsMode ? Double.parseDouble(scan.getS()) : Double.parseDouble(scan.getR());
    }

    /**
     * 参比信号：RS 参比在 R 光路；SR 完全交换后参比在 S 光路
     */
    private static double getReferenceSignal(ChangeParam scan, boolean rsMode) {
        return rsMode ? Double.parseDouble(scan.getR()) : Double.parseDouble(scan.getS());
    }

    private Double getZFromDatabase(String deviceSerialNumber, String wl, boolean rsMode) {
        DeviceInfoPO po = deviceInfoService.getInfo(deviceSerialNumber);
        if (po == null || po.getDarkInfo() == null) {
            return null;
        }
        Map<String, String> darkinfo = rsMode
                ? po.getDarkInfo().get(sphereUsedEnum.RS.getName())
                : po.getDarkInfo().get(sphereUsedEnum.SR.getName());
        if (darkinfo == null || !darkinfo.containsKey(wl)) {
            return null;
        }
        return Double.parseDouble(darkinfo.get(wl));
    }


    Double getZFromDatabase2(String deviceSerialNumber, String wl) {

        try {
            DeviceInfoPO po = deviceInfoService.getInfo(deviceSerialNumber);
            if (po == null || po.getDarkInfo() == null) {
                return null;
            }
            //todo addrs
            Map<String, Map<String, String>> darkInfo = po.getDarkInfo();

            //当前波长
            double x = Double.parseDouble(wl);
            BigDecimal wlBd = BigDecimal.valueOf(x);
            String exactWl = wlBd.stripTrailingZeros().toPlainString();

            //精确匹配
            Map<String, String> darkinfo = null;
           Boolean rsMode = isRs(deviceSerialNumber);
            if (rsMode) {
                darkinfo = darkInfo.get(sphereUsedEnum.RS.getName());
            } else {
                darkinfo = darkInfo.get(sphereUsedEnum.SR.getName());
            }
            if (darkinfo == null) {
                return null;
            }
            String exact = darkinfo.get(exactWl);
            if (exact != null && !exact.isEmpty()) {
                return Double.parseDouble(exact);
            }

            //获取排序后的波长
            List<Double> wavelengths = getSortedWavelengths(deviceSerialNumber, darkinfo,rsMode);

            if (wavelengths.isEmpty()) {
                return null;
            }

            if (wavelengths.size() == 1) {
                String key = BigDecimal.valueOf(wavelengths.get(0)).stripTrailingZeros().toPlainString();
                return Double.parseDouble(darkinfo.get(key));
            }

            //查找
            int left = 0;
            int right = wavelengths.size() - 1;

            while (left <= right) {

                int mid = (left + right) / 2;
                double midVal = wavelengths.get(mid);

                if (midVal == x) {
                    String key = BigDecimal.valueOf(midVal).stripTrailingZeros().toPlainString();
                    return Double.parseDouble(darkinfo.get(key));
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

                BigDecimal bd = BigDecimal.valueOf(minWl);
                String key = bd.stripTrailingZeros().toPlainString();
                String s = darkinfo.get(key);
                if (StringUtils.hasText(s)) {
                    return Double.parseDouble(s);
                } else {
                    return 0.1;
                }

//                return Double.parseDouble(darkInfo.get(key));
            }

            if (left >= wavelengths.size()) {
                // x 比最大波长还大
                double maxWl = wavelengths.get(wavelengths.size() - 1);
                BigDecimal bd = BigDecimal.valueOf(maxWl);
                String key = bd.stripTrailingZeros().toPlainString();
                String s = darkinfo.get(key);
                if (StringUtils.hasText(s)) {
                    return Double.parseDouble(s);
                } else {
                    return 0.1;
                }

            }


            double x1 = wavelengths.get(right);
            double x2 = wavelengths.get(left);

            BigDecimal bd1 = BigDecimal.valueOf(x1);
            BigDecimal bd2 = BigDecimal.valueOf(x2);
            String key1 = bd1.stripTrailingZeros().toPlainString();
            String key2 = bd2.stripTrailingZeros().toPlainString();

            double y1 = Double.parseDouble(darkinfo.get(key1));
            double y2 = Double.parseDouble(darkinfo.get(key2));

            //限制小数位数6位
//            y1 = Math.round(y1 * 1e6) / 1e6;
//            y2 = Math.round(y2 * 1e6) / 1e6;

            if (Math.abs(y2 - y1) < 1e-7) {
                return y1;
            }

            return y1 + (x - x1) * (y2 - y1) / (x2 - x1);

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }


    private List<Double> getSortedWavelengths(String deviceSerialNumber, Map<String, String> darkInfo, boolean rsMode) {
        String cacheKey = deviceSerialNumber + ":" + (rsMode ? "RS" : "SR");
        return wavelengthCache.computeIfAbsent(cacheKey, k -> {
            List<Double> list = new ArrayList<>();

            for (String key : darkInfo.keySet()) {
                try {
                    list.add(Double.parseDouble(key));
                } catch (Exception ignored) {
                }
            }

            Collections.sort(list);
            return list;
        });
    }
}


