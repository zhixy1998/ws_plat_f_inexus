package com.persee.workstation.uv_inexus.vo.concise.timesweep;

import com.persee.workstation.uv_inexus.po.concise.measurementPo.params.FInputParamMapTableHead;
import com.persee.workstation.uv_inexus.vo.concise.photometricAndRationVo.FHeadVO;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class FTimeSweepReportDataVo {
    // 头信息
    private List<FHeadVO> headList;

    //数据，按行来，一行是一个map
    private List<Map<String, Object>> dataList;

    private List<FInputParamMapTableHead> mapRelation;
}
