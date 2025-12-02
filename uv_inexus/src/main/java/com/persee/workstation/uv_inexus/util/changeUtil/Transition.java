/*
 * *
 *  * 版权所有 (C) 2025 北京普析通用仪器有限责任公司。保留所有权利。
 *  * 本软件受商业机密法和版权法保护。
 *  * 本代码仅限与北京普析通用仪器有限责任公司签订有效协议的授权用户内部使用。
 *  * 任何未经明确许可的复制、使用、修改、分发或公开均被严格禁止。
 *
 */

package com.persee.workstation.uv_inexus.util.changeUtil;

import lombok.Data;

import java.util.List;

@Data
public class Transition {
    private String ID;
    private String Describe;
    private int Timeout;
    private String ACK;
    private transient byte[] TypeByte;
    private List<String> InputString;
    private List<String> OutputString;
    private String[] SendBytes;
    private String[] ReadBytes;
    private int ReplyNumber;
}
