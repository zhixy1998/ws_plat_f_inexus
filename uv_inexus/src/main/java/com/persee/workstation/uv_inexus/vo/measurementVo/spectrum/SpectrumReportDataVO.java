package com.persee.workstation.uv_inexus.vo.measurementVo.spectrum;

import com.persee.workstation.uv_inexus.po.guide.measurementPo.model.InputParamMapTableHead;
import com.persee.workstation.uv_inexus.vo.measurementVo.photometricAndRationVo.HeadVO;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class SpectrumReportDataVO {

    // 头信息
    private List<HeadVO> headList;

    //数据，按行来，一行是一个map
    private List<Map<String, Object>> dataList;

    private List<InputParamMapTableHead> mapRelation;
}
