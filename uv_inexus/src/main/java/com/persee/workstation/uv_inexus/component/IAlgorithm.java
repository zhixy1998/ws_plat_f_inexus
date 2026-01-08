package com.persee.workstation.uv_inexus.component;

import org.springframework.stereotype.Component;

import java.util.List;
@Component
public abstract class IAlgorithm {
    public List<Double> calcResult(List<Double> X, List<Double> Y, List<Double> featurePoints) {
        return null;
    }
}