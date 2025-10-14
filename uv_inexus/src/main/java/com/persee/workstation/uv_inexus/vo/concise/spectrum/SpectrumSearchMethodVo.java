/*
 * *****************************************************************************************************
 * Module name:    **.**
 *
 * Copyright (C) 2025 北京普析通用仪器有限责任公司 All Rights Reserved.
 *                此源代码仅限在公司内部使用。未经公司事先书面许可，任何单位和个人不得以任何方式复制、传播本代码的任何部分。
 * Description:
 *
 *
 * *************************************************************************************
 */

package com.persee.workstation.uv_inexus.vo.concise.spectrum;

import lombok.Data;

import java.util.List;

/**
 * 光谱查找方法VO类
 */
@Data
public class SpectrumSearchMethodVo {
    // 总条数
    protected long total;
    // 总页数
    protected long size;
    // 当前页
    protected long current;
    protected List<SpectrumNearestMethodVo> records;
}
