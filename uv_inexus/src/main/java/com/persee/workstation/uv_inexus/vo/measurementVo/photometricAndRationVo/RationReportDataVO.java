/*
 * *
 *  * 版权所有 (C) 2025 北京普析通用仪器有限责任公司。保留所有权利。
 *  * 本软件受商业机密法和版权法保护。
 *  * 本代码仅限与北京普析通用仪器有限责任公司签订有效协议的授权用户内部使用。
 *  * 任何未经明确许可的复制、使用、修改、分发或公开均被严格禁止。
 *
 */

package com.persee.workstation.uv_inexus.vo.measurementVo.photometricAndRationVo;

import com.persee.workstation.uv_inexus.po.guide.measurementPo.model.InputParamMapTableHead;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class RationReportDataVO {
    /**
     * 标准表头
     */
    private List<HeadVO> standHeadList;
    /**
     * 未知表头
     */
    private List<HeadVO> unknowHeadList;
    /**
     * 标准数据
     */
    private List<Map<String, Object>> standDataList;
    /**
     * 未知数据
     */
    private List<Map<String, Object>> unknowDataList;

    /**
     * 映射关系
     */
    private List<InputParamMapTableHead> mapRelation;
    /**
     * 曲线信息
     */
    private RationFitCurveVO curveInfo;


}
