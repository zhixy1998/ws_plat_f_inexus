package com.persee.workstation.uv_inexus.util;

import com.persee.workstation.uv_inexus.constant.Constants;
import com.persee.workstation.uv_inexus.enums.measurementEnums.WaveLengthEnum;
import com.persee.workstation.uv_inexus.util.changeUtil.ChangeParam;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.List;

@Slf4j
public class DataCountUtil {

    public static String changeTPercentOrAbs(String abs, String tPercent) {

        if (abs != null) {
            double i = Double.parseDouble(abs);
            log.info("吸光度不为空，计算透光率");
            double v = Math.pow(10, -i) * 100;
            System.out.println("透光率:" + v);
            BigDecimal bd = new BigDecimal(v);
            bd = bd.setScale(Constants.DEFAULT_DECIMAL_NUM_MAX, RoundingMode.HALF_UP);

            return String.valueOf(bd);
        }
        if (tPercent != null) {
            double d = Double.parseDouble(tPercent);
            log.info("透光率不为空，计算吸光度");
            double re = - Math.log10(d / 100.0);
            System.out.println("吸光度:" + re);
            BigDecimal bd = new BigDecimal(re);
            bd = bd.setScale(Constants.DEFAULT_DECIMAL_NUM_MAX, RoundingMode.HALF_UP);

            return String.valueOf(bd);
        }
        return null;
    }


    public static String changeTPercentOrAbs2(String abs, String tPercent) {
        if (abs != null) {
            // 吸光度 -> 透光率
            BigDecimal absorbance = new BigDecimal(abs);
            log.info("吸光度不为空，计算透光率");

            // v = 10^(-abs) * 100
            double powerValue = Math.pow(10, absorbance.negate().doubleValue());
            BigDecimal result = BigDecimal.valueOf(powerValue)
                    .multiply(new BigDecimal("100"))
                    .setScale(10, RoundingMode.HALF_UP); // 保留小数点后10位
            DecimalFormat df = new DecimalFormat("0.0000000000"); // 固定小数点后10位
            String formatted = df.format(result);
            return formatted;
        }

        if (tPercent != null) {
            // 透光率 -> 吸光度
            BigDecimal t = new BigDecimal(tPercent);
            log.info("透光率不为空，计算吸光度");

            BigDecimal ratio = t.divide(new BigDecimal("100"), MathContext.DECIMAL128);

            // -log10(ratio)
            double logValue = Math.log10(ratio.doubleValue());
            BigDecimal absorbance = BigDecimal.valueOf(-logValue)
                    .setScale(10, RoundingMode.HALF_UP); // 保留小数点后10位
            DecimalFormat df = new DecimalFormat("0.0000000000"); // 固定小数点后10位
            String formatted = df.format(absorbance);
            return formatted;
        }

        return null;
    }

    public static String countTPercent(ChangeParam origin, ChangeParam real, Double z){

        double d1 = Double.parseDouble(real.getD());
        double r1 = Double.parseDouble(real.getR());
        double s1 = Double.parseDouble(real.getS());
        double a1;
        if (origin == null){
            a1 = 1;
            System.out.println("a1:" + a1);
        }else{
            double r0 = Double.parseDouble(origin.getR());
            double s0 = Double.parseDouble(origin.getS());
            double d0 = Double.parseDouble(origin.getD());

            double v0 = z * d0;
            double m1 = r0- v0;
            double m2 = s0- v0;
            a1 = m2/m1;
            System.out.println("a1:" + a1);
        }

        double v1 = z * d1;
        double m3 = r1- v1;
        double m4 = s1- v1;
        double a2 = m4/m3;
        System.out.println("a2:" + a2);

        double ans = a2/a1 * 100;
        System.out.println("ans:" + ans);
        BigDecimal bd = new BigDecimal(ans);
        bd = bd.setScale(Constants.DEFAULT_DECIMAL_NUM_MAX, RoundingMode.HALF_UP);
        return String.valueOf(bd);
    }

    public static String countErOrEs(ChangeParam real, String identity){

        String value = null;
        if(identity.equals(WaveLengthEnum.es.getCode())){
            double s = Double.parseDouble(real.getS());
            value = String.valueOf((s / (4095 * 32)) * 100);
        }
        if(identity.equals(WaveLengthEnum.er.getCode())){
            double r = Double.parseDouble(real.getR());
            value = String.valueOf((r / (4095 * 32)) * 100);
        }
        if (StringUtils.hasText(value)) {
            BigDecimal bd = new BigDecimal(value);
            bd = bd.setScale(Constants.DEFAULT_DECIMAL_NUM_MAX, RoundingMode.HALF_UP);
            value = String.valueOf(bd);
        }
        return value;
    }

    /**
     * 使用BigDecimal格式化数字到指定小数位数
     * @param value 需要格式化的值
     * @return 格式化后的字符串
     */
    public static String formatValueWithBigDecimal(Integer digit, String value) {
        try {
            BigDecimal bd = new BigDecimal(value);
            bd = bd.setScale(digit, RoundingMode.HALF_UP);
            return bd.toString();
        } catch (NumberFormatException e) {
            return value; // 如果转换失败，返回原始值
        }
    }

    public static Double formatValue(Integer digit, Double value) {
        try {
            BigDecimal bd = new BigDecimal(value);
            bd = bd.setScale(digit, RoundingMode.HALF_UP);
            return Double.valueOf(String.valueOf(bd));
        } catch (NumberFormatException e) {
            return value; // 如果转换失败，返回原始值
        }
    }



    private static double formatValue(Integer digit, double value) {
        BigDecimal bd = new BigDecimal(value);
        bd = bd.setScale(digit, RoundingMode.HALF_UP);
        return bd.doubleValue();
    }

    /**
     * 计算数组的平均值
     */
    public static double calculateMean(List<Double> data, Integer  digit) {
        if (data == null || data.isEmpty()) {
            throw new IllegalArgumentException("数据不能为空");
        }

        double sum = 0.0;
        for (double value : data) {
            value = formatValue(digit, value);
            sum += value;
        }
        double ans = sum / data.size();
        return formatValue(digit, ans);
    }
    /**
     * 计算样本标准差 (Sample Standard Deviation)
     */
    public static Double calculateSD(List<Double> data, Integer digit) {
        if (data == null || data.size() < 2) {
            log.error("数据至少需要两个值来计算标准差");
            return null;
        }

        double mean = calculateMean(data, digit);
        double sumSquaredDifferences = 0.0;

        for (double value : data) {
            value = formatValue(digit, value);
            double difference = value - mean;
            sumSquaredDifferences += difference * difference;
        }

        // 使用 N-1 作为分母 (样本标准差)
        double ans = Math.sqrt(sumSquaredDifferences / (data.size() - 1));
        return formatValue(digit, ans);
    }

    /**
     * 计算相对标准差 (RSD)
     */
    public static Double calculateRSD(List<Double> data, Integer digit) {
        if (data == null || data.size() < 2) {
            log.info("数据至少需要两个值来计算RSD");
            return null;
        }

        double mean = calculateMean(data, digit);
        if (mean == 0) {
            log.info("平均值为0时无法计算相对标准差");
            return null;
        }

        Double sd = calculateSD(data, digit);
        if (sd == null) {
            log.info("样本标准差计算出错");
            return null;
        }
        double ans = (sd / Math.abs(mean)) * 100;
        return formatValue(digit, ans);
    }
}
