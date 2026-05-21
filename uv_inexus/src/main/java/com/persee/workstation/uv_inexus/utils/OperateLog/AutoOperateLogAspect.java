/*
 * *
 *  * 版权所有 (C) 2025 北京普析通用仪器有限责任公司。保留所有权利。
 *  * 本软件受商业机密法和版权法保护。
 *  * 本代码仅限与北京普析通用仪器有限责任公司签订有效协议的授权用户内部使用。
 *  * 任何未经明确许可的复制、使用、修改、分发或公开均被严格禁止。
 *
 */

package com.persee.workstation.uv_inexus.utils.OperateLog;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import com.persee.workstation.uv_inexus.vo.sysUserPermissionVo.LoginUserInfoVO;

import lombok.extern.slf4j.Slf4j;

/**
 * 自动操作日志切面：自动查询数据库并记录详细日志
 */
@Slf4j
@Aspect
@Component
public class AutoOperateLogAspect {

    @Autowired
    private ApplicationContext applicationContext;
    
    @Autowired
    private SqlSession sqlSession;
    
    @Autowired
    private SqlSessionFactory sqlSessionFactory;

    /**
     * ThreadLocal 用于存储旧数据
     */
    private static final ThreadLocal<Object> OLD_ENTITY_HOLDER = new ThreadLocal<>();

    /**
     * 操作类型映射
     */
    private static final Map<String, String> OPERATE_TYPE_MAP = new HashMap<>();
    static {
        OPERATE_TYPE_MAP.put("add", "新增");
        OPERATE_TYPE_MAP.put("edit", "修改");
        OPERATE_TYPE_MAP.put("delete", "删除");
    }

    /**
     * 实体类型映射
     */
    private static final Map<String, String> ENTITY_TYPE_MAP = new HashMap<>();
    static {
        ENTITY_TYPE_MAP.put("sysUser", "用户");
        ENTITY_TYPE_MAP.put("sysGroup", "小组");
        ENTITY_TYPE_MAP.put("device", "设备");
        ENTITY_TYPE_MAP.put("sample", "样品");
        ENTITY_TYPE_MAP.put("config", "配置");
    }

    @Before("@annotation(autoLog)")
    public void before(JoinPoint joinPoint, AutoOperateLog autoLog) {
        String operateType = autoLog.operateType();
        String entityType = autoLog.entityType();
        String idField = autoLog.idField();

        log.info("===== @Before 切面开始 =====");
        log.info("operateType={}, entityType={}, idField={}", operateType, entityType, idField);

        // 优先从 LogContext 获取旧数据（Controller 层已设置）
        Object oldDataFromContext = LogContext.getOldData();
        if (oldDataFromContext != null) {
            log.info("从 LogContext 获取到旧数据：{}", oldDataFromContext);
            OLD_ENTITY_HOLDER.set(oldDataFromContext);
            log.info("旧数据已存储到 ThreadLocal（来自 LogContext）");
            return;
        }

        // 只在修改和删除操作时查询旧数据
        if (("delete".equals(operateType) || "edit".equals(operateType))) {
            Object[] args = joinPoint.getArgs();
            String entityId = null;

            log.info("方法参数数量：{}", args.length);
            // 从参数中提取 ID
            for (int i = 0; i < args.length; i++) {
                Object arg = args[i];
                log.info("参数[{}] = {}, 类型 = {}", i, arg, arg != null ? arg.getClass().getName() : "null");

                if (arg != null && arg instanceof String) {
                    entityId = (String) arg;
                    log.info("从 String 参数提取 entityId: {}", entityId);
                    break;
                } else if (arg != null) {
                    String idValue = getFieldValue(arg, idField);
                    log.info("从对象参数提取 idField[{}] 的值: {}", idField, idValue);
                    if (idValue != null && !idValue.isEmpty()) {
                        entityId = idValue;
                        break;
                    }
                }
            }

            if (entityId != null) {
                log.info("===== @Before 切面：开始查询旧数据 =====");
                log.info("operateType={}, entityType={}, entityId={}", operateType, entityType, entityId);

                // 使用新的 SqlSession 查询旧数据
                Object oldEntity = queryOldDataBySql(entityType, entityId);
                log.info("@Before 查询结果：{}", oldEntity);

                // 存储到 ThreadLocal
                OLD_ENTITY_HOLDER.set(oldEntity);
                log.info("旧数据已存储到 ThreadLocal");
            } else {
                log.warn("无法从参数中提取 entityId");
            }
        } else {
            log.info("非修改/删除操作，跳过查询旧数据");
        }
    }

    @Around("@annotation(autoLog)")
    public Object around(ProceedingJoinPoint joinPoint, AutoOperateLog autoLog) throws Throwable {
        String module = autoLog.module();
        String operateType = autoLog.operateType();
        String entityType = autoLog.entityType();
        String idField = autoLog.idField();
        String nameField = autoLog.nameField();

        // 1. 从参数中提取实体对象或ID
        Object[] args = joinPoint.getArgs();
        Object entity = null;
        String entityId = null;

        log.info("方法参数数量：{}", args.length);
        for (int i = 0; i < args.length; i++) {
            Object arg = args[i];
            log.info("参数[{}] = {}, 类型 = {}", i, arg, arg != null ? arg.getClass().getName() : "null");
            if (arg != null) {
                // 如果是实体对象
                boolean isEntity = isEntityObject(arg);
                log.info("参数[{}] isEntityObject = {}", i, isEntity);
                if (isEntity) {
                    entity = arg;
                    entityId = getFieldValue(arg, idField);
                    log.info("提取到的 entityId = {}", entityId);
                    break;
                }
                // 如果是ID字符串
                if (arg instanceof String) {
                    entityId = (String) arg;
                }
            }
        }
        log.info("最终 entity = {}, entityId = {}", entity, entityId);

        // 2. 执行原方法（此时 Controller 会设置 LogContext 中的数据）
        Object result = joinPoint.proceed();

        // 3. 从 LogContext 获取旧数据和新数据（Controller 已设置）
        Object oldEntity = LogContext.getOldData();
        Object newEntityFromContext = LogContext.getNewData();
        log.info("从 LogContext 获取：oldEntity={}, newEntity={}", oldEntity, newEntityFromContext);
        
        // 如果 LogContext 中没有旧数据，再从 ThreadLocal 获取（@Before 切面查询的）
        if (oldEntity == null) {
            oldEntity = OLD_ENTITY_HOLDER.get();
            log.info("从 ThreadLocal 获取：oldEntity={}", oldEntity);
        }
        
        // 如果 LogContext 中有新数据，优先使用
        if (newEntityFromContext != null) {
            entity = newEntityFromContext;
            log.info("使用 LogContext 中的新数据");
        }
        
        log.info("operateType={}, oldEntity != null={}, entity != null={}", operateType, oldEntity != null, entity != null);

        // 4. 记录详细日志
        String operateDesc = OPERATE_TYPE_MAP.getOrDefault(operateType, operateType);
        String entityDesc = ENTITY_TYPE_MAP.getOrDefault(entityType, entityType);
        Object targetEntity = oldEntity != null ? oldEntity : entity;
        log.info("提取实体名称，targetEntity={}", targetEntity);
        String entityName = extractEntityName(targetEntity, nameField);
        log.info("提取到的实体名称：{}", entityName);

        // 构建 changeContent
        String changeContent = "";
        if ("edit".equals(operateType) && oldEntity != null && entity != null) {
            // 修改操作：使用 LogCompareUtil 自动生成对比内容（格式：字段名：从「旧值」改为「新值」）
            changeContent = LogCompareUtil.generateCompareContent(oldEntity, entity);
            log.info("修改操作：changeContent={}", changeContent);
            // 如果自动对比为空，使用默认格式
            if (changeContent == null || changeContent.isEmpty()) {
                String oldName = extractEntityName(oldEntity, nameField);
                String newName = extractEntityName(entity, nameField);
                changeContent = entityDesc + "名称：从「" + (oldName.isEmpty() ? "空" : oldName) + "」改为「" + (newName.isEmpty() ? "空" : newName) + "」";
            }
        } else if ("delete".equals(operateType) && oldEntity != null) {
            // 删除操作：显示名称（格式：删除用户小组：xxx）
            String name = extractEntityName(oldEntity, nameField);
            String idValue = getFieldValue(oldEntity, "id");
            if (idValue != null && !idValue.isEmpty() && name != null && !name.isEmpty()) {
                changeContent = operateDesc + "用户" + entityDesc + "：" + name;
            } else if (name != null && !name.isEmpty()) {
                changeContent = operateDesc + "用户" + entityDesc + "：" + name;
            } else if (idValue != null && !idValue.isEmpty()) {
                changeContent = operateDesc + entityDesc + "：编号[" + idValue + "]";
            } else {
                changeContent = operateDesc + entityDesc + "：ID[" + entityId + "]";
            }
        } else if ("add".equals(operateType) && entity != null) {
            // 新增操作：显示名称（格式：新增用户小组：xxx）
            String name = extractEntityName(entity, nameField);
            changeContent = operateDesc + "用户" + entityDesc + "：" + (name.isEmpty() ? "未知" : name);
        } else {
            changeContent = operateDesc + entityDesc + "：" + (entityName.isEmpty() ? "未知" : entityName);
        }
        log.info("最终变更内容：{}", changeContent);
        
        // 获取操作人信息
        String username = "anonymous";
        LoginUserInfoVO currentUser = UserContext.getCurrentUser();
        if (currentUser != null) {
            username = currentUser.getUsername();
        }

        // 记录日志（直接调用底层方法，支持不同操作类型）
        saveAutoOperateLog(module, operateDesc + entityDesc + "操作", changeContent, oldEntity, null, username, args);

        return result;
    }

    @After("@annotation(autoLog)")
    public void after(JoinPoint joinPoint, AutoOperateLog autoLog) {
        // 清理 ThreadLocal，避免内存泄漏
        OLD_ENTITY_HOLDER.remove();
        LogContext.clear();  // 清理 LogContext
        log.info("ThreadLocal 和 LogContext 已清理");
    }

    /**
     * 通用日志保存方法
     */
    private void saveAutoOperateLog(String module, String description, String changeContent, 
                                    Object oldObj, Object newObj, String username, Object params) {
        try {
            if (!org.springframework.util.StringUtils.hasText(changeContent)) return;

            String realUser = org.springframework.util.StringUtils.hasText(username) ? username : getDefaultUsername();

            com.persee.workstation.uv_inexus.po.OperateLog.OperateLogPO log = 
                new com.persee.workstation.uv_inexus.po.OperateLog.OperateLogPO();
            log.setModule(module);
            log.setDescription(description);
            log.setChangeContent(changeContent);
            log.setBeforeData(com.alibaba.fastjson.JSON.toJSONString(oldObj));
            log.setAfterData(com.alibaba.fastjson.JSON.toJSONString(newObj));
            log.setUsername(realUser);
            log.setCreateTime(new java.util.Date());
            log.setStatus(1);
            log.setOperateTime(System.currentTimeMillis() / 1000);
            log.setIp(getIp());
            log.setMethod(getMethod());
            log.setParams(safeStr(com.alibaba.fastjson.JSON.toJSONString(params)));
            log.setResult("");
            
            // 获取 logMapper
            com.persee.workstation.uv_inexus.dao.OperateLog.OperateLogMapper logMapper = 
                applicationContext.getBean(com.persee.workstation.uv_inexus.dao.OperateLog.OperateLogMapper.class);
            logMapper.insert(log);
        } catch (Exception e) {
            log.error("记录自动操作日志失败: {}", e.getMessage());
        }
    }

    private String getDefaultUsername() {
        try {
            LoginUserInfoVO user = UserContext.getCurrentUser();
            if (user != null && org.springframework.util.StringUtils.hasText(user.getUsername())) {
                return user.getUsername();
            }
        } catch (Exception e) {
        }
        return "系统操作";
    }

    private String getMethod() {
        try {
            org.springframework.web.context.request.ServletRequestAttributes attrs = 
                (org.springframework.web.context.request.ServletRequestAttributes) 
                org.springframework.web.context.request.RequestContextHolder.getRequestAttributes();
            if (attrs == null) return "";
            javax.servlet.http.HttpServletRequest request = attrs.getRequest();
            org.springframework.web.method.HandlerMethod handler = (org.springframework.web.method.HandlerMethod) 
                request.getAttribute(org.springframework.web.servlet.HandlerMapping.BEST_MATCHING_HANDLER_ATTRIBUTE);
            return handler.getBeanType().getName() + "." + handler.getMethod().getName();
        } catch (Exception e) {
            return "";
        }
    }

    private String getIp() {
        try {
            org.springframework.web.context.request.ServletRequestAttributes attrs = 
                (org.springframework.web.context.request.ServletRequestAttributes) 
                org.springframework.web.context.request.RequestContextHolder.getRequestAttributes();
            if (attrs == null) return "";
            javax.servlet.http.HttpServletRequest request = attrs.getRequest();
            String ip = request.getHeader("X-Forwarded-For");
            if (!org.springframework.util.StringUtils.hasText(ip)) {
                ip = request.getHeader("Proxy-Client-IP");
            }
            if (!org.springframework.util.StringUtils.hasText(ip)) {
                ip = request.getHeader("WL-Proxy-Client-IP");
            }
            if (!org.springframework.util.StringUtils.hasText(ip)) {
                ip = request.getHeader("X-Real-IP");
            }
            if (!org.springframework.util.StringUtils.hasText(ip)) {
                ip = request.getRemoteAddr();
            }
            return ip.contains(",") ? ip.split(",")[0].trim() : ip;
        } catch (Exception e) {
            return "";
        }
    }

    private String safeStr(String str) {
        return str != null && str.length() > 2000 ? str.substring(0, 2000) : str;
    }

    /**
     * 判断是否为实体对象（不是基本类型或字符串）
     */
    private boolean isEntityObject(Object arg) {
        Class<?> clazz = arg.getClass();
        return !clazz.isPrimitive() 
                && !clazz.getName().startsWith("java.lang")
                && !clazz.isArray();
    }

    /**
     * 获取对象指定字段的值（支持实体对象和 Map）
     */
    private String getFieldValue(Object obj, String fieldName) {
        if (obj == null || fieldName == null) {
            return null;
        }
        
        // 如果是 Map 类型，直接通过 key 获取值
        if (obj instanceof Map) {
            Map<?, ?> map = (Map<?, ?>) obj;
            Object value = map.get(fieldName);
            if (value != null) {
                return value.toString();
            }
            return null;
        }
        
        // 实体对象：尝试通过反射获取字段值
        try {
            Field field = obj.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            Object value = field.get(obj);
            return value != null ? value.toString() : null;
        } catch (Exception e) {
            try {
                // 尝试通过 getter 方法获取
                String getterName = "get" + capitalize(fieldName);
                Method getter = obj.getClass().getMethod(getterName);
                Object value = getter.invoke(obj);
                return value != null ? value.toString() : null;
            } catch (Exception ex) {
                log.warn("获取字段 {} 的值失败: {}", fieldName, ex.getMessage());
                return null;
            }
        }
    }

    /**
     * 深拷贝实体对象
     */
    private Object deepCopyEntity(Object entity) {
        if (entity == null) {
            return null;
        }
        try {
            Class<?> clazz = entity.getClass();
            Object copy = clazz.newInstance();
            
            // 复制所有字段
            for (Field field : clazz.getDeclaredFields()) {
                field.setAccessible(true);
                Object value = field.get(entity);
                if (value != null) {
                    field.set(copy, value);
                }
            }
            
            // 复制父类字段
            Class<?> superClass = clazz.getSuperclass();
            while (superClass != null && !superClass.equals(Object.class)) {
                for (Field field : superClass.getDeclaredFields()) {
                    field.setAccessible(true);
                    Object value = field.get(entity);
                    if (value != null) {
                        field.set(copy, value);
                    }
                }
                superClass = superClass.getSuperclass();
            }
            
            return copy;
        } catch (Exception e) {
            log.warn("深拷贝对象失败: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 清除 MyBatis 一级缓存
     */
    private void clearMybatisCache() {
        try {
            // 清除当前 SqlSession 的一级缓存
            sqlSession.clearCache();
            log.info("MyBatis 一级缓存已清除");
        } catch (Exception e) {
            log.warn("清除 MyBatis 缓存失败: {}", e.getMessage());
        }
    }

    /**
     * 使用原生 SQL 查询旧数据（完全绕过缓存和当前事务）
     */
    private Object queryOldDataBySql(String entityType, String entityId) {
        SqlSession newSession = null;
        try {
            // 获取表名和实体类映射
            String tableName = getTableName(entityType);
            String entityClassName = getEntityClassName(entityType);
            
            if (tableName == null || entityClassName == null) {
                log.warn("无法获取表名或实体类名，entityType={}", entityType);
                return null;
            }
            
            log.info("使用原生 SQL 查询，tableName={}, entityClassName={}, entityId={}", tableName, entityClassName, entityId);
            
            // 创建新的 SqlSession，绕过当前事务和缓存
            newSession = sqlSessionFactory.openSession();
            
            // 使用原生 SQL 查询
            String sql = "SELECT * FROM " + tableName + " WHERE id = ?";
            
            // 使用新的 SqlSession 查询（支持 String 和 Long 类型的 ID）
            Object result = null;
            try {
                // 尝试 Long 类型
                result = newSession.selectOne(sql, Long.parseLong(entityId));
            } catch (NumberFormatException e) {
                // 如果转换失败，尝试 String 类型
                log.info("ID 不是数字，尝试使用 String 类型查询");
                result = newSession.selectOne(sql, entityId);
            }
            
            log.info("原生 SQL 查询结果: {}", result);
            
            return result;
        } catch (Exception e) {
            log.warn("原生 SQL 查询失败: {}", e.getMessage());
            return null;
        } finally {
            if (newSession != null) {
                try {
                    newSession.close();
                } catch (Exception e) {
                    log.warn("关闭 SqlSession 失败: {}", e.getMessage());
                }
            }
        }
    }

    /**
     * 获取实体对应的表名
     */
    private String getTableName(String entityType) {
        Map<String, String> tableNameMap = new HashMap<>();
        tableNameMap.put("sysGroup", "t_sys_group");
        tableNameMap.put("sysUser", "t_sys_user");
        tableNameMap.put("device", "device_info");
        tableNameMap.put("sample", "f_measure_sample");
        tableNameMap.put("config", "sys_config");
        return tableNameMap.get(entityType);
    }

    /**
     * 获取实体类的全限定名
     */
    private String getEntityClassName(String entityType) {
        Map<String, String> classNameMap = new HashMap<>();
        classNameMap.put("sysGroup", "com.persee.workstation.uv_inexus.po.systemUserPermissionPo.SysGroupPO");
        classNameMap.put("sysUser", "com.persee.workstation.uv_inexus.po.systemUserPermissionPo.SysUserPO");
        classNameMap.put("device", "com.persee.workstation.uv_inexus.po.devicePo.DeviceInfoPO");
        classNameMap.put("sample", "com.persee.workstation.uv_inexus.po.measurementPo.FMeasureSample");
        classNameMap.put("config", "com.persee.workstation.uv_inexus.po.sysConfigPo.SysConfigPO");
        return classNameMap.get(entityType);
    }

    /**
     * 查询实体
     */
    private Object queryEntity(String entityType, String entityId) {
        log.info("queryEntity 被调用，entityType={}, entityId={}", entityType, entityId);
        // 尝试多种可能的 bean 名称
        String[] possibleBeanNames = {
            entityType + "Service",
            capitalize(entityType) + "Service",
            capitalize(entityType) + "ServiceImpl",
            entityType + "ServiceImpl"
        };
        
        for (String beanName : possibleBeanNames) {
            try {
                if (applicationContext.containsBean(beanName)) {
                    log.info("找到 bean: {}", beanName);
                    Object service = applicationContext.getBean(beanName);
                    // 尝试多种参数类型
                    Object result = tryInvokeGetById(service, entityId);
                    if (result != null) {
                        log.info("查询成功，result={}", result);
                        return result;
                    } else {
                        log.warn("bean {} 的 getById 返回 null", beanName);
                    }
                } else {
                    log.info("未找到 bean: {}", beanName);
                }
            } catch (Exception e) {
                log.warn("尝试 bean {} 查询失败: {}", beanName, e.getMessage());
            }
        }
        log.warn("所有 Service 尝试都失败，返回 null");
        return null;
    }
    
    /**
     * 通过 Mapper 直接查询实体
     */
    private Object queryEntityByMapper(String entityType, String entityId) {
        try {
            // 构建 Mapper bean 名称
            String mapperBeanName = entityType + "Mapper";
            if (applicationContext.containsBean(mapperBeanName)) {
                Object mapper = applicationContext.getBean(mapperBeanName);
                log.info("找到 Mapper: {}", mapperBeanName);
                
                // 尝试调用 selectById 方法
                Method selectByIdMethod = findMethod(mapper.getClass(), "selectById", String.class);
                if (selectByIdMethod != null) {
                    Object result = selectByIdMethod.invoke(mapper, entityId);
                    log.info("Mapper selectById 结果: {}", result);
                    return result;
                }
                
                // 尝试 Long 类型
                Method selectByIdLongMethod = findMethod(mapper.getClass(), "selectById", Long.class);
                if (selectByIdLongMethod != null) {
                    Object result = selectByIdLongMethod.invoke(mapper, Long.parseLong(entityId));
                    log.info("Mapper selectById(Long) 结果: {}", result);
                    return result;
                }
            }
        } catch (Exception e) {
            log.warn("通过 Mapper 查询失败: {}", e.getMessage());
        }
        return null;
    }
    
    /**
     * 尝试调用 getById 方法，支持多种参数类型
     */
    private Object tryInvokeGetById(Object service, String entityId) {
        try {
            log.info("tryInvokeGetById: service={}, entityId={}, entityId类型={}", 
                     service.getClass().getName(), entityId, entityId.getClass().getName());
            log.info("service的父类: {}", service.getClass().getSuperclass().getName());
            
            // 尝试 String 类型参数
            Method getByIdMethod = findMethod(service.getClass(), "getById", String.class);
            log.info("getById(String) 方法是否存在: {}", getByIdMethod != null);
            if (getByIdMethod != null) {
                Object result = getByIdMethod.invoke(service, entityId);
                log.info("getById(String) 调用结果: {}", result);
                if (result != null) return result;
            }
            
            // 尝试 Long 类型参数
            Method getByIdLongMethod = findMethod(service.getClass(), "getById", Long.class);
            log.info("getById(Long) 方法是否存在: {}", getByIdLongMethod != null);
            if (getByIdLongMethod != null) {
                Object result = getByIdLongMethod.invoke(service, Long.parseLong(entityId));
                log.info("getById(Long) 调用结果: {}", result);
                if (result != null) return result;
            }
            
            // 尝试 Integer 类型参数
            Method getByIdIntMethod = findMethod(service.getClass(), "getById", Integer.class);
            log.info("getById(Integer) 方法是否存在: {}", getByIdIntMethod != null);
            if (getByIdIntMethod != null) {
                Object result = getByIdIntMethod.invoke(service, Integer.parseInt(entityId));
                log.info("getById(Integer) 调用结果: {}", result);
                if (result != null) return result;
            }
            
            // 尝试不指定参数类型查找（处理泛型方法）
            Method genericGetById = findGenericMethod(service.getClass(), "getById");
            log.info("generic getById 方法是否存在: {}", genericGetById != null);
            if (genericGetById != null) {
                try {
                    Object result = genericGetById.invoke(service, entityId);
                    log.info("generic getById(String) 调用结果: {}", result);
                    if (result != null) return result;
                    
                    result = genericGetById.invoke(service, Long.parseLong(entityId));
                    log.info("generic getById(Long) 调用结果: {}", result);
                    if (result != null) return result;
                } catch (Exception e) {
                    log.warn("调用 generic getById 失败: {}", e.getMessage());
                }
            }
            
            // 尝试 findById 方法
            Method findByIdMethod = findMethod(service.getClass(), "findById", String.class);
            log.info("findById(String) 方法是否存在: {}", findByIdMethod != null);
            if (findByIdMethod != null) {
                Object result = findByIdMethod.invoke(service, entityId);
                log.info("findById(String) 调用结果: {}", result);
                if (result != null) return result;
            }
            
            log.warn("所有方法都尝试过了，返回 null");
        } catch (Exception e) {
            log.warn("调用 getById 失败: {}", e.getMessage());
        }
        return null;
    }

    /**
     * 查找方法（包括父类）
     */
    private Method findMethod(Class<?> clazz, String methodName, Class<?>... paramTypes) {
        try {
            return clazz.getMethod(methodName, paramTypes);
        } catch (NoSuchMethodException e) {
            Class<?> superClass = clazz.getSuperclass();
            if (superClass != null) {
                return findMethod(superClass, methodName, paramTypes);
            }
            return null;
        }
    }

    /**
     * 查找泛型方法（不指定参数类型）
     */
    private Method findGenericMethod(Class<?> clazz, String methodName) {
        try {
            for (Method method : clazz.getMethods()) {
                if (method.getName().equals(methodName) && method.getParameterCount() == 1) {
                    return method;
                }
            }
            Class<?> superClass = clazz.getSuperclass();
            if (superClass != null) {
                return findGenericMethod(superClass, methodName);
            }
        } catch (Exception e) {
            log.warn("查找泛型方法失败: {}", e.getMessage());
        }
        return null;
    }

    /**
     * 提取实体名称
     */
    private String extractEntityName(Object entity, String customNameField) {
        log.info("extractEntityName 被调用，entity={}, customNameField={}", entity, customNameField);
        if (entity == null) {
            log.warn("entity 为 null，返回空字符串");
            return "";
        }

        // 优先使用自定义名称字段
        if (customNameField != null && !customNameField.isEmpty()) {
            log.info("尝试使用自定义字段：{}", customNameField);
            String value = getFieldValue(entity, customNameField);
            log.info("自定义字段值：{}", value);
            if (value != null && !value.isEmpty()) {
                return value;
            }
        }

        // 尝试常见的名称字段（支持 Map 和实体对象）
        String[] nameFields = {"username", "realName", "name", "sampleName", "deviceName", "groupName"};
        // 同时尝试数据库字段名（下划线格式）
        String[] dbNameFields = {"user_name", "real_name", "sample_name", "device_name", "group_name"};
        
        for (int i = 0; i < nameFields.length; i++) {
            String value = getFieldValue(entity, nameFields[i]);
            if (value != null && !value.isEmpty()) {
                log.info("找到字段 {} 的值：{}", nameFields[i], value);
                return value;
            }
            // 如果是 Map 类型，尝试数据库字段名
            if (entity instanceof Map && i < dbNameFields.length) {
                value = getFieldValue(entity, dbNameFields[i]);
                if (value != null && !value.isEmpty()) {
                    log.info("找到数据库字段 {} 的值：{}", dbNameFields[i], value);
                    return value;
                }
            }
        }

        log.warn("所有字段都未找到值，返回空字符串");
        return "";
    }

    /**
     * 首字母大写
     */
    private String capitalize(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }
}