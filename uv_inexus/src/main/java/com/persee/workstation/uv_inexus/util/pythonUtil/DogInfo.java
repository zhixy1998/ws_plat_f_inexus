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
public class DogInfo {

    /**
     * Bid，绑定ID，如果没有找到狗或绑定ID为new_dog，则返回空字符串
     */
    private String bid;
    /**
     * Has Dog，是否找到狗
     */
    private Boolean hasDog;
    /**
     * Message，返回消息，成功时为空字符串，失败时包含错误信息
     */
    private String message;
    /**
     * Status Code，状态码，200表示成功，400表示失败
     */
    private long statusCode;
}
