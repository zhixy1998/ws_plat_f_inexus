package com.persee.workstation.uv_inexus.utils.OperateLog;

import java.lang.reflect.Field;
import java.util.Objects;

public class LogCompareUtil {

    public static String generateCompareContent(Object oldObj, Object newObj) {
        if (oldObj == null || newObj == null) return "";

        StringBuilder sb = new StringBuilder();
        // 🔥 用 新对象的字段 遍历（DTO），不要用旧对象（PO）
        Field[] fields = newObj.getClass().getDeclaredFields();

        for (Field field : fields) {
            field.setAccessible(true);
            try {
                Object oldVal = getFieldValue(oldObj, field.getName());
                Object newVal = field.get(newObj);

                if (!Objects.equals(oldVal, newVal)) {
                    String cnName = getFieldChineseName(field.getName());
                    sb.append(cnName)
                            .append("：从「").append(oldVal).append("」改为「").append(newVal).append("」<br/>");
                }
            } catch (Exception e) {}
        }
        return sb.toString();
    }

    // 新增：根据字段名取值（兼容不同类）
    private static Object getFieldValue(Object obj, String fieldName) throws Exception {
        Field field = obj.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        return field.get(obj);
    }

    // ============================ 全局字段中文名映射 ============================
    private static String getFieldChineseName(String field) {
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