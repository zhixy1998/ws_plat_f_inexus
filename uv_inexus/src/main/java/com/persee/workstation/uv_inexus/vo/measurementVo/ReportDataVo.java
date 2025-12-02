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

import java.util.List;

@Data
public class ReportDataVo {
    // 名称
    private String name;
    // 波峰
    private List<String> peak;
    // 波谷
    private List<String> valley;
    // 标记点
    private List<String> markPoint;
    // 参数数据
    private List<String> parameterData;
    // 数据类型
    private String dataType;


}
