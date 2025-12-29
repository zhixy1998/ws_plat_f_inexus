package com.persee.workstation.uv_inexus.dto.factory_inspection;

import lombok.Data;

/*
 * *
 *  * 版权所有 (C) 2025 北京普析通用仪器有限责任公司。保留所有权利。
 *  * 本软件受商业机密法和版权法保护。
 *  * 本代码仅限与北京普析通用仪器有限责任公司签订有效协议的授权用户内部使用。
 *  * 任何未经明确许可的复制、使用、修改、分发或公开均被严格禁止。
 *
 */

/**
 * 设置单元格值
 * 比如有三个列，position就是0,1,2
 */
@Data
public class CellUpdDto {
    //位于哪个位置
    private Integer position;
    //单元格值
    private String cellValue;
    //文件名称
    private String fileName;
    //sheet名称
    private String sheetName;
    //列名称
    private String valueType;
    //行名称
    private String projectName;
    //特征值
    private String featureValue;
    //是否拥有多个特征值
    private Boolean isMultiple=false;
}
