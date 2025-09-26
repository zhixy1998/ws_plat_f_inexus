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


//    public static Double formatValue(Integer digit, Double value) {
//        try {
//            BigDecimal bd = new BigDecimal(value);
//            bd = bd.setScale(digit, RoundingMode.HALF_UP);
//            return Double.valueOf(String.valueOf(bd));
//        } catch (NumberFormatException e) {
//            return value; // 如果转换失败，返回原始值
//        }
//    }
public static Double formatValue(Integer digit, Double value) {
    if (value == null || digit == null) {
        return value;
    }

    if (value.isInfinite() || value.isNaN()) {
        return value;
    }

    if (value == 0.0) {
        return 0.0;
    }

    // 处理负值（规则3.2.5）
    boolean isNegative = value < 0;
    double absValue = Math.abs(value);

    double resultValue;

    if (digit >= 0) {
        // 修约到小数位
        resultValue = roundToDecimalPlaces(absValue, digit);
    } else {
        // 修约到整数位
        resultValue = roundToIntegerPlace(absValue, -digit);
    }

    return isNegative ? -resultValue : resultValue;
}

    /**
     * 修约到指定小数位数（重新实现，确保精度）
     */
    private static double roundToDecimalPlaces(double value, int decimalPlaces) {
        if (decimalPlaces < 0) return value;

        // 使用BigDecimal确保精度
        BigDecimal bd = new BigDecimal(String.valueOf(value));

        // 设置足够的小数位数
        bd = bd.setScale(decimalPlaces + 10, BigDecimal.ROUND_DOWN);

        // 将数值放大10^(decimalPlaces+1)倍，使得我们需要判断的数字出现在整数位置
        int scale = decimalPlaces + 1;
        BigDecimal multiplier = BigDecimal.ONE.movePointRight(scale);
        BigDecimal scaledValue = bd.multiply(multiplier);

        // scaledValue现在包含：整数部分 + 1位需要判断的小数

        // 获取整数部分（即保留部分 + 第一位判断数字）
        BigDecimal integerPart = scaledValue.setScale(0, BigDecimal.ROUND_DOWN);

        // 获取小数部分（即第二位及以后的判断数字）
        BigDecimal fractionalPart = scaledValue.subtract(integerPart);

        // 第一位判断数字（个位数）
        int firstJudgeDigit = integerPart.remainder(BigDecimal.TEN).intValue();

        // 保留的末位数字（十位数）
        int lastKeptDigit = integerPart.divide(BigDecimal.TEN).remainder(BigDecimal.TEN).intValue();

        // 检查5之后是否有非0数字（看小数部分是否大于0）
        boolean hasNonZeroAfterFive = false;
        if (firstJudgeDigit == 5) {
            if (fractionalPart.compareTo(BigDecimal.ZERO) > 0) {
                hasNonZeroAfterFive = true;
            }
        }

        // 构建基础值（去掉第一位判断数字）
        BigDecimal baseValue = integerPart.divide(BigDecimal.TEN).setScale(0, BigDecimal.ROUND_DOWN);

        // 应用修约规则
        BigDecimal resultScaled;
        if (firstJudgeDigit < 5) {
            // 规则3.2.1：小于5则舍去
            resultScaled = baseValue;
        } else if (firstJudgeDigit > 5) {
            // 规则3.2.2：大于5则进一
            resultScaled = baseValue.add(BigDecimal.ONE);
        } else {
            // 规则3.2.3和3.2.4：等于5的情况
            if (hasNonZeroAfterFive) {
                // 规则3.2.3：5之后有非0数字则进一
                resultScaled = baseValue.add(BigDecimal.ONE);
            } else {
                // 规则3.2.4：5之后无数字或皆为0时，看保留的末位数字的奇偶性
                if (lastKeptDigit % 2 == 1) {
                    // 奇进偶舍：末位是奇数则进一
                    resultScaled = baseValue.add(BigDecimal.ONE);
                } else {
                    // 末位是偶数则舍去
                    resultScaled = baseValue;
                }
            }
        }

        // 恢复原始量级
        BigDecimal result = resultScaled.movePointLeft(decimalPlaces);
        return result.doubleValue();
    }

    /**
     * 修约到指定整数位
     */
    private static double roundToIntegerPlace(double value, int place) {
        if (place <= 0) return value;

        BigDecimal interval = BigDecimal.ONE;
        for (int i = 0; i < place; i++) {
            interval = interval.multiply(BigDecimal.TEN);
        }

        BigDecimal bdValue = new BigDecimal(String.valueOf(value));
        BigDecimal scaledValue = bdValue.divide(interval, 20, BigDecimal.ROUND_HALF_UP);

        double roundedScaledValue = roundToDecimalPlaces(scaledValue.doubleValue(), 0);
        BigDecimal result = new BigDecimal(String.valueOf(roundedScaledValue)).multiply(interval);
        return result.doubleValue();
    }

    // 测试方法
    public static void main(String[] args) {
        System.out.println("=== 重点测试12.0000055 ===");
        System.out.println("12.0085000 修约到3位小数: " + formatValue(3, -12.0085000)); // 应得12.000006
        System.out.println("12.0055000 修约到5位小数: " + formatValue(3, -12.0055000)); // 应得12.00001
        System.out.println("12.0000055 修约到4位小数: " + formatValue(4, 12.0000055)); // 应得12.0000

        System.out.println("\n=== 测试规则3.2.3（5之后有非0数字）===");
        System.out.println("10.5002 修约到个位: " + formatValue(0, 10.5002)); // 应得11
        System.out.println("12.0000055 修约到6位小数: " + formatValue(6, 12.0000055)); // 应得12.000006

        System.out.println("\n=== 测试规则3.2.4（奇进偶舍）===");
        System.out.println("1.5 修约到个位: " + formatValue(0, 1.5)); // 应得2（奇进）
        System.out.println("2.5 修约到个位: " + formatValue(0, 2.5)); // 应得2（偶舍）
    }
//    private static double formatValue(Integer digit, double value) {
//        BigDecimal bd = new BigDecimal(value);
//        bd = bd.setScale(digit, RoundingMode.HALF_UP);
//        return bd.doubleValue();
//    }

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
