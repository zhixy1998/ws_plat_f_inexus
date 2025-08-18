package com.persee.workstation.uv_inexus.utils.serializer;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

public class NullToDashSerializer extends JsonSerializer<Object> {
    @Override
    public void serialize(Object value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        // 处理null
        if (value == null) {
            gen.writeString("-");
            return;
        }
        
        // 处理字符串类型（空字符串、纯空格）
        if (value instanceof String) {
            String str = (String) value;
            // 直接判断是否为空或纯空格
            if (str.isEmpty() || str.trim().length() == 0) {
                gen.writeString("-");
                return;
            }
        }
        
        // 其他类型正常输出
        gen.writeString(value.toString());
    }
}