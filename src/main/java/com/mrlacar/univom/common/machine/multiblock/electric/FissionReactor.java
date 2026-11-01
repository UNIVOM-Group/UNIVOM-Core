package com.mrlacar.univom.common.machine.multiblock.electric;

import com.gregtechceu.gtceu.api.capability.recipe.FluidRecipeCapability;
import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.capability.recipe.IRecipeHandler;
import com.gregtechceu.gtceu.api.gui.fancy.ConfiguratorPanel;
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.TickableSubscription;
import com.gregtechceu.gtceu.api.machine.feature.IExplosionMachine;
import com.gregtechceu.gtceu.api.machine.feature.multiblock.IMultiPart;
import com.gregtechceu.gtceu.api.machine.multiblock.WorkableElectricMultiblockMachine;
import com.gregtechceu.gtceu.api.machine.trait.NotifiableFluidTank;
import com.gregtechceu.gtceu.common.data.GTMaterials;
import com.lowdragmc.lowdraglib.syncdata.annotation.Persisted;
import com.lowdragmc.lowdraglib.syncdata.field.ManagedFieldHolder;
import com.mojang.blaze3d.MethodsReturnNonnullByDefault;
import com.mrlacar.univom.api.machine.fancyconfigurator.FissionReactorCoolantConfigurator;
import com.mrlacar.univom.common.machine.multiblock.part.ControlRodBusPartMachine;
import com.mrlacar.univom.common.machine.multiblock.part.FissionReactorSensorPartMachine;
import com.mrlacar.univom.common.machine.multiblock.part.FuelRodBusPartMachine;
import lombok.Getter;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Marker;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;

import static net.minecraft.world.entity.EntityType.*;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class FissionReactor extends WorkableElectricMultiblockMachine implements IExplosionMachine {
    protected static final ManagedFieldHolder MANAGED_FIELD_HOLDER = new ManagedFieldHolder(FissionReactor.class,
            WorkableElectricMultiblockMachine.MANAGED_FIELD_HOLDER);

    private static final double MASS = 50.0;
    private static final double MELTDOWN_TEMPERATURE = 2500.0;
    private static final double AMBIENT_TEMPERATURE = 300.0;

    private static final double[] COOLANT_FACTORS = { 10.0, 40.0, 200.0 };

    private final List<FissionReactorSensorPartMachine> sensors = new ArrayList<>();

    private final List<NotifiableFluidTank> inputTanks = new ArrayList<>();
    private final List<NotifiableFluidTank> outputTanks = new ArrayList<>();
    private boolean tanksDirty = true;

    @Nullable
    private Fluid[] coolantFluids;
    @Nullable
    private Fluid[] coolantOutputs;
    @Nullable
    private FluidStack[] drainProbes;
    @Nullable
    private FluidStack[] fillProbes;

    @Nullable
    private TickableSubscription serverTickSub;
    @Nullable
    private FuelRodBusPartMachine fuelBus;
    @Nullable
    private ControlRodBusPartMachine controlRodBus;

    private double internalTemperature = AMBIENT_TEMPERATURE;

    @Getter
    @Persisted
    private double coreTemperature = AMBIENT_TEMPERATURE;

    @Getter
    @Persisted
    private int activeFuelCount = 0;

    @Getter
    @Persisted
    private int maxCoolantFlowRate = 200;

    public FissionReactor(IMachineBlockEntity holder) {
        super(holder);
    }

    @Override
    public ManagedFieldHolder getFieldHolder() {
        return MANAGED_FIELD_HOLDER;
    }

    public void setMaxCoolantFlowRate(int rate) {
        this.maxCoolantFlowRate = Math.max(0, rate);
    }

    @Override
    public void onStructureFormed() {
        super.onStructureFormed();
        this.fuelBus = null;
        this.controlRodBus = null;
        this.sensors.clear();

        for (IMultiPart part : getParts()) {
            if (part instanceof FuelRodBusPartMachine machine) {
                this.fuelBus = machine;
            } else if (part instanceof ControlRodBusPartMachine rodBus) {
                this.controlRodBus = rodBus;
            } else if (part instanceof FissionReactorSensorPartMachine sensor) {
                this.sensors.add(sensor);
            }
        }

        this.tanksDirty = true;

        this.internalTemperature = this.coreTemperature;
        serverTickSub = subscribeServerTick(serverTickSub, this::reactorTick);
    }

    @Override
    public void onStructureInvalid() {
        super.onStructureInvalid();
        if (serverTickSub != null) {
            serverTickSub.unsubscribe();
            serverTickSub = null;
        }
        this.fuelBus = null;
        this.controlRodBus = null;
        this.sensors.clear();
        clearTankCache();
    }

    @Override
    public void onPartUnload() {
        super.onPartUnload();
        clearTankCache();
    }

    @Override
    public void onUnload() {
        super.onUnload();
        if (serverTickSub != null) {
            serverTickSub.unsubscribe();
            serverTickSub = null;
        }
        this.fuelBus = null;
        this.controlRodBus = null;
        this.sensors.clear();
        clearTankCache();
    }

    private void clearTankCache() {
        this.inputTanks.clear();
        this.outputTanks.clear();
        this.tanksDirty = true;
    }

    private void rebuildTankCacheIfNeeded() {
        if (!this.tanksDirty) return;
        this.tanksDirty = false;
        this.inputTanks.clear();
        this.outputTanks.clear();

        for (var entry : getCapabilitiesFlat().entrySet()) {
            IO io = entry.getKey();
            if (io == IO.NONE) continue;

            List<IRecipeHandler<?>> handlers = entry.getValue().get(FluidRecipeCapability.CAP);
            if (handlers == null) continue;

            boolean isInput = io.support(IO.IN);
            boolean isOutput = io.support(IO.OUT);

            for (IRecipeHandler<?> handler : handlers) {
                if (!(handler instanceof NotifiableFluidTank tank)) continue;
                if (isInput) this.inputTanks.add(tank);
                if (isOutput) this.outputTanks.add(tank);
            }
        }
    }

    private void initCoolantsIfNeeded() {
        if (this.coolantFluids != null) return;

        this.coolantFluids = new Fluid[] {
                GTMaterials.DistilledWater.getFluid(),
                GTMaterials.PCBCoolant.getFluid(),
                GTMaterials.SodiumPotassium.getFluid()
        };
        this.coolantOutputs = new Fluid[] {
                GTMaterials.Steam.getFluid(),
                GTMaterials.Lava.getFluid(),
                GTMaterials.Milk.getFluid()
        };
        this.drainProbes = new FluidStack[this.coolantFluids.length];
        this.fillProbes = new FluidStack[this.coolantFluids.length];
        for (int i = 0; i < this.coolantFluids.length; i++) {
            if (this.coolantFluids[i] != null) {
                this.drainProbes[i] = new FluidStack(this.coolantFluids[i], 1);
            }
            if (this.coolantOutputs[i] != null) {
                this.fillProbes[i] = new FluidStack(this.coolantOutputs[i], 1);
            }
        }
    }

    private boolean tryConsumeCoolant(int coolantIndex, int amount) {
        if (amount <= 0 || this.drainProbes == null) return false;
        FluidStack probe = this.drainProbes[coolantIndex];
        if (probe == null || this.inputTanks.isEmpty()) return false;

        int available = 0;
        for (NotifiableFluidTank tank : this.inputTanks) {
            probe.setAmount(amount - available);
            available += tank.drainInternal(probe, IFluidHandler.FluidAction.SIMULATE).getAmount();
            if (available >= amount) break;
        }
        if (available < amount) return false;

        int drained = 0;
        for (NotifiableFluidTank tank : this.inputTanks) {
            probe.setAmount(amount - drained);
            drained += tank.drainInternal(probe, IFluidHandler.FluidAction.EXECUTE).getAmount();
            if (drained >= amount) break;
        }
        return true;
    }

    private void emitCoolantOutput(int coolantIndex, int amount) {
        if (amount <= 0 || this.fillProbes == null) return;
        FluidStack probe = this.fillProbes[coolantIndex];
        if (probe == null || this.outputTanks.isEmpty()) return;

        int remaining = amount;
        for (NotifiableFluidTank tank : this.outputTanks) {
            probe.setAmount(remaining);
            remaining -= tank.fillInternal(probe, IFluidHandler.FluidAction.EXECUTE);
            if (remaining <= 0) break;
        }
    }

    private void reactorTick() {
        if (isRemote() || getLevel() == null) return;

        rebuildTankCacheIfNeeded();
        initCoolantsIfNeeded();

        double currentInsertion = this.getControlRodInsertion();
        boolean isMissingRod = this.controlRodBus != null && this.controlRodBus.noControlRod();
        double effectiveInsertion = isWorkingEnabled() ? currentInsertion : (isMissingRod ? 0.0 : 1.0);

        double totalBaseHeat = 0.0;
        int currentActiveCount = 0;

        if (this.fuelBus != null) {
            var heatResult = this.fuelBus.processFuelAndGetHeat(effectiveInsertion);
            totalBaseHeat = heatResult.totalHeat();
            currentActiveCount = heatResult.activeCount();
        }

        if (this.activeFuelCount != currentActiveCount) {
            this.activeFuelCount = currentActiveCount;
        }

        double currentEfficiency = this.controlRodBus != null ? this.controlRodBus.getControlRodEfficiency() : 0.8;
        double q_gen = Math.max(0.0, totalBaseHeat * (1.0 - (effectiveInsertion * currentEfficiency)));

        double availableHeat = 0.0;
        if (internalTemperature > 400.0) {
            availableHeat = q_gen + (internalTemperature - 400.0) * MASS;
        }

        double coolingFactor = 0.0;
        int usedCoolant = -1;
        int actualConsumed = 0;

        if (availableHeat > 0 && this.maxCoolantFlowRate > 0 && !this.inputTanks.isEmpty()) {
            for (int i = 0; i < COOLANT_FACTORS.length; i++) {
                int target = Math.min(this.maxCoolantFlowRate, (int) Math.ceil(availableHeat / COOLANT_FACTORS[i]));
                if (target <= 0) continue;
                if (tryConsumeCoolant(i, target)) {
                    coolingFactor = COOLANT_FACTORS[i];
                    usedCoolant = i;
                    actualConsumed = target;
                    break;
                }
            }
        }

        double ambientCooling = 0.0;
        if (internalTemperature > AMBIENT_TEMPERATURE) {
            ambientCooling = (internalTemperature - AMBIENT_TEMPERATURE) * 0.5;
        }

        double q_transfer = (actualConsumed * coolingFactor) + ambientCooling;
        double q_net = q_gen - q_transfer;
        internalTemperature = Math.max(AMBIENT_TEMPERATURE, internalTemperature + (q_net / MASS));

        if (actualConsumed > 0 && usedCoolant >= 0) {
            emitCoolantOutput(usedCoolant, actualConsumed);
        }

        if (getOffsetTimer() % 20 == 0 || Math.abs(this.coreTemperature - this.internalTemperature) > 10.0) {
            this.coreTemperature = this.internalTemperature;
        }

        if (!this.sensors.isEmpty()) {
            int tempForSensors = (int) this.coreTemperature;
            for (FissionReactorSensorPartMachine sensor : this.sensors) {
                sensor.outputSignal(tempForSensors);
            }
        }

        if (internalTemperature >= MELTDOWN_TEMPERATURE) {
            float dynamicPower = 15.0F + (float) (totalBaseHeat / 200.0);
            triggerMeltdown(Math.min(dynamicPower, 80.0F));
        }
    }

    public double getControlRodInsertion() {
        if (this.controlRodBus != null) {
            if (this.controlRodBus.noControlRod()) {
                return 0.0;
            }
            if (this.controlRodBus.isRedstonePowered()) {
                return this.controlRodBus.getControlRodThresholdValue() / 100.0;
            }
            return this.controlRodBus.getControlRodInsertion();
        }
        return 1.0;
    }

    public int getControlRodPercent() {
        return (int) (this.getControlRodInsertion() * 100);
    }

    public boolean isMissingControlRod() {
        return this.controlRodBus != null && this.controlRodBus.noControlRod();
    }

    public int getDecayHeatPercent() {
        double currentEfficiency = this.controlRodBus != null ? this.controlRodBus.getControlRodEfficiency() : 0.8;
        return (int) Math.round((1.0 - currentEfficiency) * 100);
    }

    private void triggerMeltdown(float explosionPower) {
        if (getLevel() != null && !isRemote()) {
            this.internalTemperature = AMBIENT_TEMPERATURE;
            this.coreTemperature = AMBIENT_TEMPERATURE;
            this.setWorkingEnabled(false);

            Marker fallout = new Marker(MARKER, getLevel());
            fallout.setPos(getPos().getX() + 0.5, getPos().getY() + 0.5, getPos().getZ() + 0.5);
            fallout.addTag("univom_fallout");

            fallout.getPersistentData().putLong("expiry_time", getLevel().getGameTime() + 12000);
            getLevel().addFreshEntity(fallout);

            doExplosion(explosionPower);
        }
    }

    @Override
    public boolean shouldWeatherOrTerrainExplosion() {
        return false;
    }

    @Override
    public void attachConfigurators(ConfiguratorPanel configuratorPanel) {
        super.attachConfigurators(configuratorPanel);
        configuratorPanel.attachConfigurators(new FissionReactorCoolantConfigurator(this));
    }

    @Override
    public void addDisplayText(List<Component> textList) {
        if (isFormed()) {
            ChatFormatting tempColor = ChatFormatting.GREEN;
            if (this.coreTemperature >= 2000.0) {
                tempColor = ChatFormatting.RED;
            } else if (this.coreTemperature >= 1000.0) {
                tempColor = ChatFormatting.YELLOW;
            }

            textList.add(Component.translatable("univom.multiblock.fission_reactor.temperature")
                    .append(Component.literal(String.format("%.1f K", this.coreTemperature)).withStyle(tempColor))
                    .append(Component.literal(" / 2500.0 K").withStyle(ChatFormatting.GRAY)));

            boolean isMissingRod = this.controlRodBus != null && this.controlRodBus.noControlRod();
            int displayInsertion = isWorkingEnabled() ? this.getControlRodPercent() : 100;

            if (isMissingRod) {
                textList.add(Component.translatable("univom.multiblock.fission_reactor.missing_control_rod").withStyle(ChatFormatting.RED));
                displayInsertion = 0;
            } else {
                textList.add(Component.translatable("univom.multiblock.fission_reactor.insertion_rate", displayInsertion).withStyle(ChatFormatting.AQUA));
            }

            textList.add(Component.translatable("univom.multiblock.fission_reactor.active_fuel", this.activeFuelCount).withStyle(ChatFormatting.LIGHT_PURPLE));
            textList.add(Component.translatable("univom.multiblock.fission_reactor.coolant_flow_setting", this.maxCoolantFlowRate).withStyle(ChatFormatting.BLUE));

            if (!isWorkingEnabled() || displayInsertion >= 100) {
                if (!isMissingRod) {
                    double currentEfficiency = this.controlRodBus != null ? this.controlRodBus.getControlRodEfficiency() : 0.8;
                    int decayHeatPercent = (int) Math.round((1.0 - currentEfficiency) * 100);

                    if (decayHeatPercent > 0) {
                        textList.add(Component.translatable("univom.multiblock.fission_reactor.scramming", decayHeatPercent).withStyle(ChatFormatting.RED));
                    } else {
                        textList.add(Component.translatable("univom.multiblock.fission_reactor.cold_shutdown").withStyle(ChatFormatting.GREEN));
                    }
                } else {
                    textList.add(Component.translatable("univom.multiblock.fission_reactor.scram_failed").withStyle(ChatFormatting.DARK_RED));
                }
            } else if (this.activeFuelCount == 0) {
                if (this.coreTemperature > 305.0) {
                    textList.add(Component.translatable("univom.multiblock.fission_reactor.status.no_fuel_cooling").withStyle(ChatFormatting.GOLD));
                } else {
                    textList.add(Component.translatable("univom.multiblock.fission_reactor.status.no_fuel_standby").withStyle(ChatFormatting.GOLD));
                }
            } else {
                textList.add(Component.translatable("univom.multiblock.fission_reactor.status.running").withStyle(ChatFormatting.GREEN));
            }
        } else {
            textList.add(Component.translatable("gtceu.multiblock.invalid_structure").withStyle(ChatFormatting.RED));
        }
    }
}
