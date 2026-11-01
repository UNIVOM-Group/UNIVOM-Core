package com.mrlacar.univom.api.data.particle;

public record BeamPacket(String particle, double energy, long flux) {
    public static final BeamPacket EMPTY = new BeamPacket("", 0.0, 0L);

    public boolean isEmpty() {
        return this.particle == null || this.particle.isEmpty() || this.flux <= 0L;
    }
}
