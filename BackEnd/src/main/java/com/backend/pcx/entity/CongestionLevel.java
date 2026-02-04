package com.backend.pcx.entity;

public enum CongestionLevel {
    FREE("畅通"),
    FLOWING("缓行"),
    CONGESTED("拥堵");

    private final String description;

    CongestionLevel(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    public static CongestionLevel fromSpeed(Double speed, Double freeThreshold, Double flowingThreshold) {
        if (speed == null) {
            return CONGESTED;
        }
        if (speed >= freeThreshold) {
            return FREE;
        } else if (speed >= flowingThreshold) {
            return FLOWING;
        } else {
            return CONGESTED;
        }
    }
}
