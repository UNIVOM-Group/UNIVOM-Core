package com.mrlacar.univom.common.pipelike.pressure;

public final class PressureProcessing {

    public static final int AIRLOCK_VOLUME = 50;
    private static final double ROUNDING = 1.0E-9D;

    private PressureProcessing() {}

    public static long airlockLoss(long amount, long volume) {
        return amount <= 0 || volume <= 0 ? 0 : AIRLOCK_VOLUME * amount / volume;
    }

    public static int maxParallel(int cells, int rating, long amount, long volume, int required, long consumption) {
        if (cells <= 0 || rating <= 0) {
            return 0;
        }
        long remaining = amount - airlockLoss(amount, volume);
        if (consumption > 0 && remaining < consumption) {
            return 0;
        }
        long parallel = 1;
        if (required > 0) {
            if (amount <= 0 || volume <= 0) {
                return 0;
            }
            double ratio = (double) amount / ((double) volume * required);
            parallel = (long) Math.floor(Math.min((double) rating * cells, cells * ratio * ratio + ROUNDING));
        }
        if (consumption > 0) {
            long excess = remaining - (long) Math.max(0, required) * Math.max(0, volume);
            parallel = Math.min(parallel, excess <= 0 ? 0 : excess / consumption);
        }
        return (int) Math.max(1, parallel);
    }

    public static long batchLoss(long amount, long volume, int parallels, long consumption) {
        long loss = airlockLoss(amount, volume) + (long) Math.max(0, parallels) * Math.max(0, consumption);
        return Math.min(Math.max(0, amount), loss);
    }
}
