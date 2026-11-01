package com.mrlacar.univom.common.pipelike.pressure;

public record PressurePipeProperties(int ratingBar, int volume, int safetyPercent) {

    public boolean isSafetyValve() {
        return safetyPercent > 0;
    }

    public PressurePipeProperties withSafetyPercent(int percent) {
        return new PressurePipeProperties(ratingBar, volume, percent);
    }
}
