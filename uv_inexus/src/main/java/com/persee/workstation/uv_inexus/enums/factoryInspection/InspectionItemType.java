package com.persee.workstation.uv_inexus.enums.factoryInspection;
public enum InspectionItemType {
    WAVELENGTH_ERR_REP("波长示值误差与重复性校验", "wavelengthErrRep"),
    NOISE_TEST("噪声检验", "noiseTest"),
    TRANSMITTANCE_ERR_REP("透射比示值误差与重复性校验", "transmittanceErrRep"),
    STRAY_LIGHT("杂散光校验", "strayLight"),
    BASELINE_FLATNESS("基线平直度检验", "baselineFlatness"),
    BASELINE_DARK_NOISE("基线暗噪声检验", "baselineDarkNoise"),
    DRIFT_TEST("漂移检验", "driftTest"),
    WAVELENGTH_EDGE_NOISE("波长边缘噪声", "wavelengthEdgeNoise"),
    SPECTRAL_BANDWIDTH("光谱带宽与仪器最小光谱带宽", "spectralBandwidth");

    private final String displayName;
    private final String code;

    InspectionItemType(String displayName, String code) {
        this.displayName = displayName;
        this.code = code;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getCode() {
        return code;
    }
}
