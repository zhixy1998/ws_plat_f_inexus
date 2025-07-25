package com.persee.workstation.uv_inexus.util;

import com.persee.workstation.uv_inexus.util.changeUtil.ChangeParam;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Slf4j
public class DataCountUtil {

    public static String changeTPercentOrAbs(String abs, String tPercent) {

        if (abs != null) {
            double i = Double.parseDouble(abs);
            log.info("吸光度不为空，计算透光率");
            double v = Math.pow(10, -i) * 100;
            System.out.println("透光率:" + v);
            return String.format("%.2f", v);
        }
        if (tPercent != null) {
            double d = Double.parseDouble(tPercent);
            log.info("透光率不为空，计算吸光度");
            double re = - Math.log10(d / 100.0);
            System.out.println("吸光度:" + re);
            return String.format("%.2f", re);
        }
        return null;
    }


    public static String changeTPercentOrAbs2(String abs, String tPercent) {

        if (abs != null)
        {
            if (abs != null) {
                // 将 abs 转换成 double
                double i = Double.parseDouble(abs);
                log.info("吸光度不为空，计算透光率");

                // 打印 abs 值，确保它被正确解析
                System.out.println("Abs: " + i);

                // 将 i 转换为 BigDecimal
                BigDecimal absValue = BigDecimal.valueOf(i);
                System.out.println("Abs as BigDecimal: " + absValue);

                // 获取整数值的负数
                int exponent = -(int) i;  // 将 i 转为负的 int 类型

                // 打印 exponent
                System.out.println("Exponent: " + exponent);

                // 计算 10 的 -i 次方
                BigDecimal tenPow = BigDecimal.TEN.pow(exponent);  // 计算10的-exponent次方
                System.out.println("BigDecimal.TEN.pow(exponent): " + tenPow);

                // 计算 10 的 -i 次方乘以 100
                BigDecimal v = tenPow.multiply(BigDecimal.valueOf(100));  // 乘以100
                System.out.println("透光率计算值 (v): " + v);

                // 返回格式化后的透光率
                return String.format("%.2f", v);
            }
        }
        if (tPercent != null) {
            BigDecimal d = new BigDecimal(tPercent);
            log.info("透光率不为空，计算吸光度");

            // 使用 BigDecimal 计算吸光度，避免精度丢失
            BigDecimal re = BigDecimal.valueOf(-Math.log10(d.doubleValue() / 100.0));
            System.out.println("吸光度:" + re);

            // 保留两位小数
            return re.setScale(2, RoundingMode.HALF_UP).toString();
        }
        return null;
    }

    public static String countTPercent(ChangeParam origin, ChangeParam real, Double z){

        System.out.println("origin:" + origin.getD());
        System.out.println("origin:" + origin.getR());
        System.out.println("origin:" + origin.getS());

        System.out.println("real:" + real.getD());
        System.out.println("real:" + real.getR());
        System.out.println("real:" + real.getS());

        double r0 = Double.parseDouble(origin.getR());
        double s0 = Double.parseDouble(origin.getS());
        double d0 = Double.parseDouble(origin.getD());

        double d1 = Double.parseDouble(real.getD());
        double r1 = Double.parseDouble(real.getR());
        double s1 = Double.parseDouble(real.getS());

        double v0 = z * d0;
        double m1 = r0- v0;
        double m2 = s0- v0;
        double a1 = m2/m1;
        System.out.println("a1:" + a1);

        double v1 = z * d1;
        double m3 = r1- v1;
        double m4 = s1- v1;
        double a2 = m4/m3;
        System.out.println("a2:" + a2);

        double ans = a2/a1 * 100;
        System.out.println("ans:" + ans);
        return String.valueOf(ans);
    }
}
