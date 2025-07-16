package com.persee.workstation.uv_inexus.util;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class DataCountUtil {

    public static String changeTPercentOrAbs(String abs, String tPercent) {

        if (abs != null) {
            double i = Double.parseDouble(abs);

            log.info("吸光度不为空，计算透光率");
            double v = Math.pow(10, -i) * 100;
            return String.format("%.2f", v);
        }
        if (tPercent != null) {
            double d = Double.parseDouble(tPercent);
            log.info("透光率不为空，计算吸光度");
            double re = -Math.log10(d / 100.0);
            return String.format("%.2f", re);
        }
        return null;
    }
}
