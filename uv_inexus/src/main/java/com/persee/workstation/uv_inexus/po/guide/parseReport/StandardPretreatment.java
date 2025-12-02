/*
 * *
 *  * 版权所有 (C) 2025 北京普析通用仪器有限责任公司。保留所有权利。
 *  * 本软件受商业机密法和版权法保护。
 *  * 本代码仅限与北京普析通用仪器有限责任公司签订有效协议的授权用户内部使用。
 *  * 任何未经明确许可的复制、使用、修改、分发或公开均被严格禁止。
 *
 */

package com.persee.workstation.uv_inexus.po.guide.parseReport;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;

// import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.persee.workstation.uv_inexus.model.BasePO;

import lombok.Data;
import lombok.EqualsAndHashCode;
// import java.time.LocalDateTime;
@Entity
@EqualsAndHashCode(callSuper = true)
@Data
@TableName("t_report_standard_pretreatment")
public class StandardPretreatment extends BasePO {
    @Id
    @Column(name = "detection_id")
    private String detectionId;

    @Column(name = "pretreatment_description")
    private String pretreatmentDescription;

}