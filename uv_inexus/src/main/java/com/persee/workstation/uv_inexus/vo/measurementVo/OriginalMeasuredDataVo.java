/*
 * *
 *  * 版权所有 (C) 2025 北京普析通用仪器有限责任公司。保留所有权利。
 *  * 本软件受商业机密法和版权法保护。
 *  * 本代码仅限与北京普析通用仪器有限责任公司签订有效协议的授权用户内部使用。
 *  * 任何未经明确许可的复制、使用、修改、分发或公开均被严格禁止。
 *
 */

package com.persee.workstation.uv_inexus.vo.measurementVo;

import lombok.Data;

@Data
public class OriginalMeasuredDataVo {
    private int id;           // 编号
    private String x;
    private String y;
    private String feature;   // 特征描述，如“峰值”或“无”
    private String grain;     // 谷值
    private String tag;       // 标记，如“未处理”
}
