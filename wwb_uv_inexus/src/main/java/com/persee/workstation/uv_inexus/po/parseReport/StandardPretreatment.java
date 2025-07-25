package com.persee.workstation.uv_inexus.po.parseReport;

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