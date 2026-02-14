package com.persee.workstation.uv_inexus.component;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
@Component
public abstract class IAlgorithm {
    public List<BigDecimal> calcResult(List<BigDecimal> X, List<BigDecimal> Y, List<BigDecimal> featurePoints) {
        return null;
    }
}