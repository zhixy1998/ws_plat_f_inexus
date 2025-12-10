/*
 * *
 *  * 版权所有 (C) 2025 北京普析通用仪器有限责任公司。保留所有权利。
 *  * 本软件受商业机密法和版权法保护。
 *  * 本代码仅限与北京普析通用仪器有限责任公司签订有效协议的授权用户内部使用。
 *  * 任何未经明确许可的复制、使用、修改、分发或公开均被严格禁止。
 *
 */

package com.persee.workstation.uv_inexus.service.concise.measurementService;

import com.persee.workstation.uv_inexus.model.R;
import com.persee.workstation.uv_inexus.po.concise.measurementPo.params.FRoundingVO;

import java.util.List;

public interface ConcisePublicUtilService {


    R<Boolean> changeSampleLocation(String taskId, List<Integer> usefulLocation);

    /**
     * 检查修约条件是否合法
     */
    R<Boolean> checkApproximateRange(FRoundingVO vo);

    /**
     * 解析修约条件字符串并填充到参数对象中
     *
     * @param condition    修约条件字符串 (例如 "1<x<=100")
     * @param paramVO      目标参数对象
     * @param variableName 未知量符号（如 "x", "result" 等），解析时将匹配该符号（忽略大小写）
     * @return 解析是否成功（若格式不正确或未知数不是指定的符号，返回false）
     */
    boolean parseRoundingCondition(String condition, com.persee.workstation.uv_inexus.po.concise.measurementPo.params.FRoundingParamVO paramVO, String variableName);
}
