/*
 * *
 *  * 版权所有 (C) 2025 北京普析通用仪器有限责任公司。保留所有权利。
 *  * 本软件受商业机密法和版权法保护。
 *  * 本代码仅限与北京普析通用仪器有限责任公司签订有效协议的授权用户内部使用。
 *  * 任何未经明确许可的复制、使用、修改、分发或公开均被严格禁止。
 *
 */

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
