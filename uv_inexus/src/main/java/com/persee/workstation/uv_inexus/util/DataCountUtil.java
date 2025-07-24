package com.persee.workstation.uv_inexus.util;

import com.persee.workstation.uv_inexus.util.changeUtil.ChangeParam;
import lombok.extern.slf4j.Slf4j;

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

        double m1 = r0-z*d0;
        double m2 = s0-z*d0;
        double a1 = m1/m2;
        System.out.println("a1:" + a1);

        double m3 = r1-z*d1;
        double m4 = s1-z*d1;
        double a2 = m3/m4;
        System.out.println("a2:" + a2);

        double ans = a2/a1 * 100;
        System.out.println("ans:" + ans);
        return String.valueOf(ans);
    }
}
