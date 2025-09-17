package com.persee.workstation.uv_inexus.vo.concise.ration;

import com.persee.workstation.uv_inexus.po.concise.measurementPo.params.FInputParamMapTableHead;
import com.persee.workstation.uv_inexus.vo.concise.photometricAndRationVo.FHeadVO;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class FRationReportDataVO {
    /**
     * 标准表头
     */
    private List<FHeadVO> standHeadList;
    /**
     * 未知表头
     */
    private List<FHeadVO> unknowHeadList;
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
    private List<FInputParamMapTableHead> mapRelation;
}
