package com.persee.workstation.uv_inexus.util.deviceUtil;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

public class SpectrumPointStore {

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
