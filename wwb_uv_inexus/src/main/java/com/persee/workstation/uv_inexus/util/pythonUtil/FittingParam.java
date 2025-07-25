package com.persee.workstation.uv_inexus.util.pythonUtil;

import lombok.Data;

@Data
public class FittingParam {
    /**
     * Degree，拟合的多项式次数：1：一次拟合，2:二次拟合，3：三次拟合，4：四次拟合
     */
    private long degree;
    /**
     * Force Zero，是否强制过零点，0=否，1=是
     */
    private Long force_zero;
    /**
     * X Data，自变量数据，例如：1,2,3,4,5
     */
    private double[] x_data;
    /**
     * Y Data，因变量数据，例如：2,4,6,8,10
     */
    private double[] y_data;
}
