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
public class PhotometricReportDataVO {

    // 头信息
    private List<HeadVO> headList;

    //数据，按行来，一行是一个map
    private List<Map<String, Object>> dataList;

    private List<InputParamMapTableHead> mapRelation;
}
