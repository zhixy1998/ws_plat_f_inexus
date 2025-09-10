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
}
