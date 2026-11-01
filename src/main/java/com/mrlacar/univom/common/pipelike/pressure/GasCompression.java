package com.mrlacar.univom.common.pipelike.pressure;

import com.gregtechceu.gtceu.api.GTValues;
import net.minecraft.ChatFormatting;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;

public final class GasCompression {

    private static final int[] TARGET_CAPS = { 0, 10, 25, 40, 120, 240 };
    private static final int[] FLOWS = { 0, 25, 50, 100, 300, 800 };
    private static final double BASE_LOAD = 0.25D;

    private GasCompression() {}

    public static int targetCap(int tier) {
        return TARGET_CAPS[Math.max(0, Math.min(tier, TARGET_CAPS.length - 1))];
    }

    public static int flow(int tier) {
        return FLOWS[Math.max(0, Math.min(tier, FLOWS.length - 1))];
    }

    public static long energyCost(int tier, double pressure, long amount) {
        int flow = flow(tier);
        int cap = targetCap(tier);
        if (amount <= 0 || flow <= 0 || cap <= 0) {
            return 0;
        }
        double load = Math.min(1.0D, Math.log1p(Math.max(0.0D, pressure)) / Math.log1p(cap));
        double factor = BASE_LOAD + (1.0D - BASE_LOAD) * load;
        return (long) Math.ceil(GTValues.V[tier] * factor * Math.min(amount, flow) / flow);
    }

    public static Result compress(@Nullable PressurePipeNet net, @Nullable ResourceLocation gas, long available,
                                  long energy, int tier, int targetBar) {
        if (net == null) {
            return Result.of(Status.NO_NETWORK);
        }
        if (gas == null || available <= 0) {
            return Result.of(Status.NO_GAS);
        }
        if (!net.accepts(gas)) {
            return Result.of(Status.INCOMPATIBLE_GAS);
        }
        long ceiling = Math.max(0, Math.min(targetBar, targetCap(tier))) * 100L;
        long room = net.capacityAt(ceiling) - net.getAmount();
        if (room <= 0) {
            return Result.of(Status.TARGET_REACHED);
        }
        long planned = Math.min(Math.min(flow(tier), available), room);
        double pressure = net.getPressure();
        if (energy < energyCost(tier, pressure, planned)) {
            return Result.of(Status.NO_ENERGY);
        }
        long inserted = net.insert(gas, planned, ceiling);
        if (inserted <= 0) {
            return Result.of(Status.TARGET_REACHED);
        }
        return new Result(Status.WORKING, inserted, energyCost(tier, pressure, inserted));
    }

    public enum Status {

        WORKING(ChatFormatting.GREEN),
        TARGET_REACHED(ChatFormatting.YELLOW),
        NO_NETWORK(ChatFormatting.RED),
        INCOMPATIBLE_GAS(ChatFormatting.RED),
        NO_ENERGY(ChatFormatting.RED),
        NO_GAS(ChatFormatting.GRAY),
        DISABLED(ChatFormatting.GRAY);

        private static final Status[] VALUES = values();

        private final ChatFormatting color;
        private final String translationKey;

        Status(ChatFormatting color) {
            this.color = color;
            this.translationKey = "univom.machine.gas_compressor.status." + name().toLowerCase(Locale.ROOT);
        }

        public ChatFormatting getColor() {
            return color;
        }

        public String getTranslationKey() {
            return translationKey;
        }

        public static Status byOrdinal(int ordinal) {
            return VALUES[Math.max(0, Math.min(ordinal, VALUES.length - 1))];
        }
    }

    public record Result(Status status, long inserted, long energy) {

        private static final Result[] IDLE = new Result[Status.VALUES.length];

        static {
            for (Status status : Status.VALUES) {
                IDLE[status.ordinal()] = new Result(status, 0, 0);
            }
        }

        static Result of(Status status) {
            return IDLE[status.ordinal()];
        }
    }
}
