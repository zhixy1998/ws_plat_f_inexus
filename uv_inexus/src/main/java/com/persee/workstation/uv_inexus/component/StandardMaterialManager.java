package com.persee.workstation.uv_inexus.component;

/*
 * *
 *  * 版权所有 (C) 2025 北京普析通用仪器有限责任公司。保留所有权利。
 *  * 本软件受商业机密法和版权法保护。
 *  * 本代码仅限与北京普析通用仪器有限责任公司签订有效协议的授权用户内部使用。
 *  * 任何未经明确许可的复制、使用、修改、分发或公开均被严格禁止。
 *
 */

import lombok.Data;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 标准物分类管理器 - 按类型存储任务的标准物ID
 */
@Component
public class StandardMaterialManager {

    /**
     * 任务标准物分组容器
     */
    private static final Map<String, StandardMaterialGroup> materialGroupMap = new ConcurrentHashMap<>();

    /**
     * 标准物分组类
     */
    @Data
    public static class StandardMaterialGroup {
        private Set<String> blackBlockIds = new HashSet<>();    // 黑挡块物质ID
        private Set<String> zeroIds = new HashSet<>();          // 校零物质ID
        private Set<String> measureIds = new HashSet<>();       // 测量物质ID
        private Set<String> baselineIds = new HashSet<>();       // 基线物质ID
        // 按动作类型存储样品池位置
        private Map<MaterialType, Integer> actionPositionMap = new HashMap<>();
    }

    /**
     * 添加基线物ID
     */
    public void addBaselineMaterial(String taskId, String standardId) {
        StandardMaterialGroup group = materialGroupMap.computeIfAbsent(taskId, k -> new StandardMaterialGroup());
        group.getBaselineIds().add(standardId);
    }
    /**
     * 添加黑挡块物质ID
     */
    public void addBlackBlockMaterial(String taskId, String standardId) {
        StandardMaterialGroup group = materialGroupMap.computeIfAbsent(taskId, k -> new StandardMaterialGroup());
        group.getBlackBlockIds().add(standardId);
    }

    /**
     * 添加校零物质ID
     */
    public void addZeroMaterial(String taskId, String standardId) {
        StandardMaterialGroup group = materialGroupMap.computeIfAbsent(taskId, k -> new StandardMaterialGroup());
        group.getZeroIds().add(standardId);
    }
    /**
     * 设置动作对应的样品池位置
     */
    public void setActionPosition(String taskId, MaterialType actionType, Integer position) {
        StandardMaterialGroup group = materialGroupMap.computeIfAbsent(taskId, k -> new StandardMaterialGroup());
        group.getActionPositionMap().put(actionType, position);
    }
    /**
     * 添加测量物质ID
     */
    public void addMeasureMaterial(String taskId, String standardId) {
        StandardMaterialGroup group = materialGroupMap.computeIfAbsent(taskId, k -> new StandardMaterialGroup());
        group.getMeasureIds().add(standardId);
    }

    /**
     * 获取指定类型的标准物ID集合
     */
    public Set<String> getStandardMaterials(String taskId, MaterialType type) {
        StandardMaterialGroup group = materialGroupMap.get(taskId);
        if (group != null) {
            switch (type) {
                case BLACK_BLOCK: return group.getBlackBlockIds();
                case ZERO: return group.getZeroIds();
                case MEASURE: return group.getMeasureIds();
                case BASELINE: return group.getBaselineIds();
                default: return Collections.emptySet();
            }
        }
        return Collections.emptySet();
    }

    /**
     * 标准物类型枚举
     */
    public enum MaterialType {
        BLACK_BLOCK, ZERO, MEASURE,BASELINE
    }
    /**
     * 获取指定动作类型的样品池位置
     */
    public Integer getActionPosition(String taskId, MaterialType actionType) {
        StandardMaterialGroup group = materialGroupMap.get(taskId);
        if (group != null) {
            return group.getActionPositionMap().get(actionType);
        }
        return null;
    }
}
