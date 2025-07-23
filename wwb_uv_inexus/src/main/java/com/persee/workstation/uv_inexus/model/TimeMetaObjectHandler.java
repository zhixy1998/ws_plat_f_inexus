package com.persee.workstation.uv_inexus.model;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.baomidou.mybatisplus.core.toolkit.ObjectUtils;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@Configuration
public class TimeMetaObjectHandler implements MetaObjectHandler {

    @Override
    public void insertFill(MetaObject metaObject) {
        BasePO baseEntity = (BasePO) metaObject.getOriginalObject();
        LocalDateTime createTime = ObjectUtils.isNotNull(baseEntity.getCreated())
                ? baseEntity.getCreated() : LocalDateTime.now().withNano(0);
        LocalDateTime updateTime = ObjectUtils.isNotNull(baseEntity.getUpdated())
                ? baseEntity.getUpdated() : LocalDateTime.now().withNano(0);
        baseEntity.setCreated(createTime);
        baseEntity.setUpdated(updateTime);

    }

    /**
     * 修改时的操作
     * @param metaObject 元对象
     */
    @Override
    public void updateFill(MetaObject metaObject) {
        BasePO baseEntity = (BasePO) metaObject.getOriginalObject();
        LocalDateTime updateTime = ObjectUtils.isNotNull(baseEntity.getUpdated())
                ? baseEntity.getUpdated() : LocalDateTime.now().withNano(0);
        baseEntity.setUpdated(updateTime);
    }

}
