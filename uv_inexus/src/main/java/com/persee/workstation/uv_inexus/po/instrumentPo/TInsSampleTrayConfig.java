package com.persee.workstation.uv_inexus.po.instrumentPo;

import com.persee.workstation.uv_inexus.model.BasePO;
import lombok.Data;

@Data
public class TInsSampleTrayConfig extends BasePO {


  private String instrumentSerial;

  private long lTraySetting;

  private long mTraySetting;

  private long rTraySetting;

  private long lRackSetting;

  private long mRackSetting;

  private long rRackSetting;

  private String lAreaInfo;

  private String mAreaInfo;

  private String rAreaInfo;

  private java.sql.Timestamp createTime;

  private java.sql.Timestamp updateTime;


}
