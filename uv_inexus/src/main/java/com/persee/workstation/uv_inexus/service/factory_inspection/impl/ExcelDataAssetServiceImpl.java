package com.persee.workstation.uv_inexus.service.factory_inspection.impl;


import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.persee.workstation.uv_inexus.dao.factory_inspection.ExcelDataAssetMapper;
import com.persee.workstation.uv_inexus.po.factory_inspection.ExcelDataAsset;
import com.persee.workstation.uv_inexus.service.factory_inspection.ExcelDataAssetService;
import org.springframework.stereotype.Service;

@Service
public class ExcelDataAssetServiceImpl extends ServiceImpl<ExcelDataAssetMapper, ExcelDataAsset> implements ExcelDataAssetService {
}