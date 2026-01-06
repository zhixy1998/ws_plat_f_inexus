package com.persee.workstation.uv_inexus.util.deviceUtil;

import java.util.*;

public class SpectrumPointStore {
    // 按顺序保存扫描点
    private final List<SpectrumPoint> pointList = Collections.synchronizedList(new ArrayList<>());

    // 添加新的扫描点
    public void addPoint(SpectrumPoint point) {
        pointList.add(point);
    }

    // 获取全部扫描点
    public List<SpectrumPoint> getAllPoints() {
        return pointList;
    }

    // 清空
    public void pointListClear() {
        pointList.clear();
    }

    /** key = wavelength（建议统一保留 1 位或 2 位小数） */
    private final Map<Double, SpectrumPoint> pointMap = new LinkedHashMap<>();

    public SpectrumPoint getOrCreate(double wavelength) {
        return pointMap.computeIfAbsent(wavelength, SpectrumPoint::new);
    }

    public SpectrumPoint get(double wavelength) {
        return pointMap.get(wavelength);
    }

    public Collection<SpectrumPoint> allPoints() {
        return pointMap.values();
    }

    public void clear() {
        pointMap.clear();
    }
}
