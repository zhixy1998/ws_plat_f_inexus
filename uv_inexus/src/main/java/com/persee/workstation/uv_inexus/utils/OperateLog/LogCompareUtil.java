package com.persee.workstation.uv_inexus.utils.OperateLog;

import java.lang.reflect.Field;
import java.util.Objects;

import io.swagger.annotations.ApiModelProperty;

public class LogCompareUtil {

    // 排除字段列表（不记录这些字段的变更）
    private static final String[] EXCLUDE_FIELDS = {"id", "roleId", "groupId", "userId"};

    public static String generateCompareContent(Object oldObj, Object newObj) {
        if (oldObj == null || newObj == null) return "";

        StringBuilder sb = new StringBuilder();
        // 用 新对象的字段 遍历（DTO），不要用旧对象（PO）
        Field[] fields = newObj.getClass().getDeclaredFields();

        for (Field field : fields) {
            field.setAccessible(true);
            String fieldName = field.getName();

            // 跳过排除列表中的字段
            if (isExcluded(fieldName)) {
                continue;
            }

            try {
                Object oldVal = getFieldValue(oldObj, field.getName());
                Object newVal = field.get(newObj);

                if (!Objects.equals(oldVal, newVal)) {
                    // 优先从字段的@ApiModelProperty注解获取中文名称
                    String cnName = getFieldChineseName(field, newObj.getClass(), oldObj.getClass());
                    sb.append(cnName)
                            .append("：从「").append(oldVal).append("」改为「").append(newVal).append("」<br/>");
                }
            } catch (Exception e) {}
        }
        return sb.toString();
    }

    // 判断字段是否在排除列表中
    private static boolean isExcluded(String fieldName) {
        for (String excluded : EXCLUDE_FIELDS) {
            if (excluded.equals(fieldName)) {
                return true;
            }
        }
        return false;
    }

    // 新增：根据字段名取值（兼容不同类）
    private static Object getFieldValue(Object obj, String fieldName) throws Exception {
        Field field = obj.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        return field.get(obj);
    }

    /**
     * 获取字段的中文名称
     * 优先从字段的@ApiModelProperty注解获取，其次从类的字段查找，最后使用全局映射
     * @param field 当前遍历的字段（来自newObj）
     * @param newObjClass newObj的类（通常是DTO）
     * @param oldObjClass oldObj的类（通常是PO）
     * @return 字段的中文名称
     */
    private static String getFieldChineseName(Field field, Class<?> newObjClass, Class<?> oldObjClass) {
        String fieldName = field.getName();

        // 1. 首先尝试从当前字段（DTO字段）的@ApiModelProperty注解获取
        ApiModelProperty apiModelProperty = field.getAnnotation(ApiModelProperty.class);
        if (apiModelProperty != null && !apiModelProperty.value().isEmpty()) {
            return apiModelProperty.value();
        }

        // 2. 尝试从DTO类中查找对应字段的@ApiModelProperty注解
        try {
            Field dtoField = newObjClass.getDeclaredField(fieldName);
            ApiModelProperty dtoAnnotation = dtoField.getAnnotation(ApiModelProperty.class);
            if (dtoAnnotation != null && !dtoAnnotation.value().isEmpty()) {
                return dtoAnnotation.value();
            }
        } catch (NoSuchFieldException e) {
            // 忽略，继续下一步
        }

        // 3. 尝试从PO类（oldObj）中查找对应字段的@ApiModelProperty注解
        if (oldObjClass != null) {
            try {
                Field poField = oldObjClass.getDeclaredField(fieldName);
                ApiModelProperty poAnnotation = poField.getAnnotation(ApiModelProperty.class);
                if (poAnnotation != null && !poAnnotation.value().isEmpty()) {
                    return poAnnotation.value();
                }
            } catch (NoSuchFieldException e) {
                // 忽略，继续下一步
            }
        }

        // 4. 最后使用全局映射作为兜底
        return getGlobalFieldChineseName(fieldName);
    }

    // ============================ 全局字段中文名映射（兜底使用） ============================
    private static String getGlobalFieldChineseName(String field) {
        switch (field) {
            // 通用
            case "id": return "ID";
            case "name": return "名称";
            case "userName": return "用户名";
            case "realName": return "真实姓名";
            case "phone": return "手机号";
            case "status": return "状态";
            case "description": return "描述";
            case "remark": return "备注";
            case "remarks": return "备注";

            // 小组表
            case "groupName": return "小组名称";
            case "roleId": return "角色ID";

            // 用户表
            case "account": return "账号";
            case "password": return "密码";
            case "email": return "邮箱";

            // 设备表
            case "deviceName": return "设备名称";
            case "ip": return "IP地址";
            case "instrumentName": return "仪器名称";

            // 样品
            case "sampleName": return "样品名称";
            case "sampleNumber": return "样品编号";
            // 格式
            // case "xxx": return "XXX";

            default: return field;
        }
    }
}