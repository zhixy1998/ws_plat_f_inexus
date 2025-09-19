package com.persee.workstation.uv_inexus.service.guide.instrumentService;

import com.persee.workstation.uv_inexus.po.guide.instrumentPo.InitRecordPO;

import java.util.List;

public interface InitRecordService {

    void saveOrUpdateInitTask(InitRecordPO po);

    void initAllTask(String deviceSerialNumber, String ip);

    List<InitRecordPO> searchInitStatus(String deviceSerialNumber, String deviceIp);


}
