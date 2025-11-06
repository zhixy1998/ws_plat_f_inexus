/*
 * *****************************************************************************************************
 * Module name:    **.**
 *
 * Copyright (C) 2025 北京普析通用仪器有限责任公司 All Rights Reserved.
 *                此源代码仅限在公司内部使用。未经公司事先书面许可，任何单位和个人不得以任何方式复制、传播本代码的任何部分。
 * Description:
 *
 *
 * *************************************************************************************
 */

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
            double re = -Math.log10(d / 100.0);
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

            double v = ratio.doubleValue();
            if (v < 0) {
                v = -v;
            }

            // -log10(ratio)
            double logValue = Math.log10(v);
            BigDecimal absorbance = BigDecimal.valueOf(-logValue)
                    .setScale(10, RoundingMode.HALF_UP); // 保留小数点后10位
            DecimalFormat df = new DecimalFormat("0.0000000000"); // 固定小数点后10位
            String formatted = df.format(absorbance);
            return formatted;
        }

        return null;
    }

    public static String countTPercent(ChangeParam origin, ChangeParam real, Double z) {

        try {

            double d1 = Double.parseDouble(real.getD());
            double r1 = Double.parseDouble(real.getR());
            double s1 = Double.parseDouble(real.getS());
            double a1;
            if (origin == null || origin.isEmpty()) {
                a1 = 1;
                System.out.println("a1:" + a1);
            } else {
                double r0 = Double.parseDouble(origin.getR());
                double s0 = Double.parseDouble(origin.getS());
                double d0 = Double.parseDouble(origin.getD());

                double v0 = z * d0;
                double m1 = r0 - v0;
                double m2 = s0 - v0;
                a1 = m2 / m1;
                System.out.println("a1:" + a1);
            }

            double v1 = z * d1;
            double m3 = r1 - v1;
            double m4 = s1 - v1;
            double a2 = m4 / m3;
            System.out.println("a2:" + a2);

            double ans = a2 / a1 * 100;
            System.out.println("ans:" + ans);
            BigDecimal bd = new BigDecimal(ans);
            bd = bd.setScale(Constants.DEFAULT_DECIMAL_NUM_MAX, RoundingMode.HALF_UP);
            return String.valueOf(bd);
        } catch (Exception e) {
            return "0";
        }
    }

    public static String countErOrEs(ChangeParam real, String identity) {

        String value = null;
        if (identity.equals(WaveLengthEnum.es.getCode())) {
            double s = Double.parseDouble(real.getS());
            value = String.valueOf((s / 8388607) * 100);
        }
        if (identity.equals(WaveLengthEnum.er.getCode())) {
            double r = Double.parseDouble(real.getR());
            value = String.valueOf((r / 8388607) * 100);
        }
        if (StringUtils.hasText(value)) {
            BigDecimal bd = new BigDecimal(value);
            bd = bd.setScale(Constants.DEFAULT_DECIMAL_NUM_MAX, RoundingMode.HALF_UP);
            value = String.valueOf(bd);
        }
        return value;
    }

    /**
     * 有效位数修约
     */
    public static String formatToSignificantDigits(Integer significantDigits, Double value) {
        if (value == 0.0) {
            StringBuilder zeros = new StringBuilder("0.");
            for (int i = 0; i < significantDigits - 1; i++) {
                zeros.append("0");
            }
            return zeros.toString();
        }

        // 计算值的指数部分
        int exponent = (int) Math.floor(Math.log10(Math.abs(value)));

        // 计算需要的小数位数
        int decimalPlaces = significantDigits - 1 - exponent;

        // 使用BigDecimal确保精度
        BigDecimal bd = BigDecimal.valueOf(value);

        if (decimalPlaces >= 0) {
            // 小数情况或整数但位数不足
            // 直接使用 roundToDecimalPlaces 实现四舍六入五成双修约
            bd = DataCountUtil.roundToDecimalPlaces(bd, decimalPlaces);

            // 格式化为指定位数的小数
            StringBuilder formatPattern = new StringBuilder("0");
            if (decimalPlaces > 0) {
                formatPattern.append(".");
                for (int i = 0; i < decimalPlaces; i++) {
                    formatPattern.append("0");
                }
            }
            DecimalFormat df = new DecimalFormat(formatPattern.toString());
            return df.format(bd);
        } else {
            // 整数位数超过有效数字位数，使用科学计数法
            // 先将数字调整为1位整数形式
            double normalized = value * Math.pow(10, -exponent);
            BigDecimal normalizedBd = BigDecimal.valueOf(normalized);
            // 修约到指定有效位数-1位小数，使用四舍六入五成双规则
            normalizedBd = DataCountUtil.roundToDecimalPlaces(normalizedBd, significantDigits - 1);

            // 构造科学计数法表示 1.23*10^3 格式
            StringBuilder scientificPattern = new StringBuilder("0.");
            for (int i = 0; i < significantDigits - 1; i++) {
                scientificPattern.append("0");
            }
            DecimalFormat scientificFormat = new DecimalFormat(scientificPattern.toString());
            return scientificFormat.format(normalizedBd) + "*10^" + exponent;
        }
    }


    // 重载方法，支持BigDecimal输入
    public static String formatToSignificantDigits(Integer significantDigits, BigDecimal value) {
        return formatToSignificantDigits(significantDigits, value.doubleValue());
    }

    /**
     * 使用BigDecimal格式化数字到指定小数位数
     *
     * @param value 需要格式化的值
     * @return 格式化后的字符串
     */
    public static String formatValueWithBigDecimal(Integer digit, String value) {
        try {
            BigDecimal bd = new BigDecimal(value);

            boolean isNegative = bd.compareTo(BigDecimal.ZERO) < 0;
            bd = bd.abs();

            if (digit >= 0) {
                bd = roundToDecimalPlaces(bd, digit);
            } else {
                bd = roundToIntegerPlace(bd, -digit);
            }
            bd = bd.setScale(digit, RoundingMode.HALF_UP);

            if (isNegative) {
                bd = bd.negate();
            }

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
        double tValue = Math.abs(value);

        BigDecimal absValue = BigDecimal.valueOf(tValue);

        BigDecimal resultValue;

        if (digit >= 0) {
            // 修约到小数位
            resultValue = roundToDecimalPlaces(absValue, digit);
        } else {
            // 修约到整数位
            resultValue = roundToIntegerPlace(absValue, -digit);
        }
        resultValue = resultValue.setScale(digit, RoundingMode.HALF_UP);


        return isNegative ? -resultValue.doubleValue() : resultValue.doubleValue();
    }

//    public static BigDecimal formatValue(Integer digit, BigDecimal value) {
//        if (value == null || digit == null) {
//            return value;
//        }
//

    /// /        if (value.isInfinite() || value.isNaN()) {
    /// /            return value;
    /// /        }
    /// /
//        // 处理0值情况
//        if (value.compareTo(BigDecimal.ZERO) == 0) {
//            return BigDecimal.ZERO;
//        }
//
//        BigDecimal absValue = value.abs();
//
//        BigDecimal resultValue;
//
//        if (digit >= 0) {
//            // 修约到小数位
//            resultValue = roundToDecimalPlaces(absValue, digit);
//        } else {
//            // 修约到整数位
//            resultValue = roundToIntegerPlace(absValue, -digit);
//        }
//
//        return value.signum() < 0 ? resultValue.negate() : resultValue;
//    }

    //补零
    public static String addZero(Double value, int digit) {
        BigDecimal decimal = BigDecimal.valueOf(value);
        decimal = decimal.setScale(digit, RoundingMode.HALF_UP);
        String ans = String.valueOf(decimal);

        StringBuilder s = new StringBuilder(ans);
        String[] split = s.toString().split("\\.");
        int length = split[1].length();
        if (length < digit) {
            for (int i = 0; i < digit - length; i++) {
                s.append("0");
            }
            ans = s.toString();
        }
        return ans;
    }

    public static Object convertSciToDecimal(Object obj) {
        if (obj == null) { // 额外处理null，避免空指针
            return null;
        }
        String str = String.valueOf(obj);
        // 核心逻辑：只要含e或E，就尝试转小数
        if (str.toLowerCase().contains("e")) {
            try {
                // 尝试转成Double再格式化，成功则返回小数格式
                DecimalFormat df = new DecimalFormat("0.##########");
                return df.format(Double.parseDouble(str));
            } catch (NumberFormatException e) {
                // 转失败（如哈希串），直接返回原字符串
                return str;
            }
        } else {
            // 不含e/E，直接返回原字符串（封装为Object）
            return str;
        }
    }


    /**
     * 修约到指定小数位数（重新实现，确保精度）
     */
    public static BigDecimal roundToDecimalPlaces(BigDecimal value, int decimalPlaces) {
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
        return resultScaled.movePointLeft(decimalPlaces);
    }

    /**
     * 修约到指定整数位
     */
    private static BigDecimal roundToIntegerPlace(BigDecimal value, int place) {
        if (place <= 0) return value;

        BigDecimal interval = BigDecimal.ONE;
        for (int i = 0; i < place; i++) {
            interval = interval.multiply(BigDecimal.TEN);
        }

        BigDecimal bdValue = new BigDecimal(String.valueOf(value));
        BigDecimal scaledValue = bdValue.divide(interval, 20, BigDecimal.ROUND_HALF_UP);

        BigDecimal roundedScaledValue = roundToDecimalPlaces(scaledValue, 0);
        return roundedScaledValue.multiply(interval);
    }

    // 测试方法
    public static void main(String[] args) {
        System.out.println("=== 重点测试12.0000055 ===");
        System.out.println("12.35 修约到3位小数: " + formatValue(-3, 12.35000)); // 应得12.000006
//        System.out.println("12.0055000 修约到5位小数: " + formatValue(3, -12.0055000)); // 应得12.00001
//        System.out.println("12.0000055 修约到4位小数: " + formatValue(4, 12.0000055)); // 应得12.0000
//        Double v = formatValue(4, 12.0000055);
//        String s = addZero(v, 4);
//        System.out.println( s);
//
//        System.out.println("\n=== 测试规则3.2.3（5之后有非0数字）===");
//        System.out.println("10.5002 修约到个位: " + formatValue(0, 10.5002)); // 应得11
//        System.out.println("12.0000055 修约到6位小数: " + formatValue(6, 12.0000055)); // 应得12.000006
//
//        System.out.println("\n=== 测试规则3.2.4（奇进偶舍）===");
//        System.out.println("1.5 修约到个位: " + formatValue(0, 1.5)); // 应得2（奇进）
//        System.out.println("2.5 修约到个位: " + formatValue(0, 2.5)); // 应得2（偶舍）
    }
//    private static double formatValue(Integer digit, double value) {
//        BigDecimal bd = new BigDecimal(value);
//        bd = bd.setScale(digit, RoundingMode.HALF_UP);
//        return bd.doubleValue();
//    }

    /**
     * 计算数组的平均值
     */
    public static double calculateMean(List<Double> data, Integer digit) {
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
