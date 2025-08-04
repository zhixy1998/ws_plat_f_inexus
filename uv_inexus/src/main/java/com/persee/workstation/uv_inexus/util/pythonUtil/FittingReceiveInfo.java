package com.persee.workstation.uv_inexus.util.pythonUtil;

import lombok.Data;

import java.util.List;

@Data
public class FittingReceiveInfo {

    /**
     * Code，状态码
     */
    private Integer code;
    /**
     * Coefficients，所有系数列表，顺序为 k0, k1, ..., kn
     */
    private List<Double> coefficients;

    /**
     * Degree，拟合的多项式次数
     */
    private Integer degree;

    /**
     * K0，常数项
     */
    private Double k0;
    /**
     * K1，一次项
     */
    private Double k1;
    /**
     * K2，二次项
     */
    private Double k2;
    /**
     * K3，三次项
     */
    private Double k3;
    /**
     * K4，四次项
     */
    private Double k4;

    private Double minX;

    private Double maxX;

}
