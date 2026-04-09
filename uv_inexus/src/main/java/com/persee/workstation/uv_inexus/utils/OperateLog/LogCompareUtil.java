package com.persee.workstation.uv_inexus.utils.OperateLog;

import java.lang.reflect.Field;
import java.util.Objects;

public class LogCompareUtil {

    public static String generateCompareContent(Object oldObj, Object newObj) {
        if (oldObj == null || newObj == null) return "";

        StringBuilder sb = new StringBuilder();
        Field[] fields = oldObj.getClass().getDeclaredFields();

        for (Field field : fields) {
            field.setAccessible(true);
            try {
                Object oldVal = field.get(oldObj);
                Object newVal = field.get(newObj);

                if (!Objects.equals(oldVal, newVal)) {
                    sb.append(field.getName())
                            .append(": [")
                            .append(oldVal)
                            .append("] → [")
                            .append(newVal)
                            .append("] ");
                }
            } catch (Exception e) {
            }
        }
        return sb.toString();
    }
}