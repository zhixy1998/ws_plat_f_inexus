package com.persee.workstation.uv_inexus.service.instrumentService;

import com.persee.workstation.uv_inexus.po.instrumentPo.InitRecordPO;

import java.util.List;

public interface InitRecordService {

    void saveOrUpdateInitTask(InitRecordPO po);

    void initAllTask(String deviceSerialNumber, String ip);

    List<InitRecordPO> searchInitStatus(String deviceSerialNumber);


}
