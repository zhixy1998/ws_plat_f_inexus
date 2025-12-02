/*
 * *
 *  * 版权所有 (C) 2025 北京普析通用仪器有限责任公司。保留所有权利。
 *  * 本软件受商业机密法和版权法保护。
 *  * 本代码仅限与北京普析通用仪器有限责任公司签订有效协议的授权用户内部使用。
 *  * 任何未经明确许可的复制、使用、修改、分发或公开均被严格禁止。
 *
 */

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