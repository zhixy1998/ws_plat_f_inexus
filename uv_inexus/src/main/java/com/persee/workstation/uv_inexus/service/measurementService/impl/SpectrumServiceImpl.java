package com.persee.workstation.uv_inexus.service.measurementService.impl;

import com.persee.workstation.uv_inexus.dto.measurementDto.ExperimentMethodDto;
import com.persee.workstation.uv_inexus.po.measurementPo.TMethod;
import com.persee.workstation.uv_inexus.service.measurementService.SpectrumService;
import com.persee.workstation.uv_inexus.service.measurementService.TMethodService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 光谱扫描服务实现类
 */
@Service("spectrumService")
public class SpectrumServiceImpl implements SpectrumService {

//    @Autowired
//    private TMeasureMethodService  tMeasureMethodService;
//    @Autowired
//    private TMeasureProjectService tMeasureProjectService;
    @Autowired
    private TMethodService tMethodService;

    @Override
    // 重写父类方法
    public boolean newMethod(ExperimentMethodDto methodDto) {
        // 创建TMethod对象
        TMethod tMethod = new TMethod();
        // 将methodDto对象的属性值复制到tMethod对象中
        BeanUtils.copyProperties(methodDto, tMethod);
        tMethod.setTaskId("7562d781-4699-11f0-91da-40c2baa93bfc");
        boolean save = tMethodService.save(tMethod);
        return save;
    }


//    @Override
//    public boolean newMethod(ExperimentMethodDto methodDto) throws JsonProcessingException {
//        // 绑定默认项目
//        TMeasureProject defaultProject =tMeasureProjectService.defaultProject();
//        TMeasureMethod tMeasureMethod = new TMeasureMethod();
//        // 创建对应参数表的记录
//        TMeasureStandard tMeasureStandard = new TMeasureStandard();
//        ObjectMapper objectMapper = new ObjectMapper();
//        Json json = objectMapper.readValue(s, Json.class);
//        tMeasureStandard.setMeasurementParamInfo(json);
//
//        // 根据方法名称对参数表进行查询
//        // 创建任务
//        tMeasureMethod.setProjectItemKey(defaultProject.getId());
//        tMeasureMethod.setMethodName(methodDto.getMethodName());
//
//        return false;
//    }


}
