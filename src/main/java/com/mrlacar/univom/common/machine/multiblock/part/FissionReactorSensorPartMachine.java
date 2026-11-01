package com.mrlacar.univom.common.machine.multiblock.part;

import com.gregtechceu.gtceu.api.gui.fancy.ConfiguratorPanel;
import com.gregtechceu.gtceu.api.gui.widget.IntInputWidget;
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.multiblock.part.TieredPartMachine;
import com.lowdragmc.lowdraglib.gui.widget.Widget;
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;
import com.lowdragmc.lowdraglib.syncdata.annotation.DescSynced;
import com.lowdragmc.lowdraglib.syncdata.annotation.Persisted;
import com.lowdragmc.lowdraglib.syncdata.field.ManagedFieldHolder;
import com.mrlacar.univom.api.machine.feature.IInvertibleRedstoneMachine;
import lombok.Getter;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.Direction;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;

import static com.gregtechceu.gtceu.api.GTValues.EV;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class FissionReactorSensorPartMachine extends TieredPartMachine implements IInvertibleRedstoneMachine {
    protected static final ManagedFieldHolder MANAGED_FIELD_HOLDER = new ManagedFieldHolder(
            FissionReactorSensorPartMachine.class, TieredPartMachine.MANAGED_FIELD_HOLDER);

    @Getter
    @Persisted
    @DescSynced
    private int thresholdTemperature = 2000;

    @Persisted
    private int signalStrength = 0;

    @DescSynced
    @Persisted
    private boolean inverted = false;

    @Persisted
    private int lastReactorTemp = 300;

    public FissionReactorSensorPartMachine(IMachineBlockEntity holder) {
        super(holder, EV);
    }

    @Override
    public ManagedFieldHolder getFieldHolder() {
        return MANAGED_FIELD_HOLDER;
    }

    @Override
    public boolean canShared() {
        return false;
    }

    public void setThresholdTemperature(int temperature) {
        this.thresholdTemperature = Math.max(300, Math.min(2500, temperature));
        refreshRedstone();
    }

    @Override
    public boolean isInverted() {
        return this.inverted;
    }

    @Override
    public void setInverted(boolean inverted) {
        if (this.inverted != inverted) {
            this.inverted = inverted;
            refreshRedstone();
        }
    }

    public void outputSignal(int reactorTemperature) {
        if (this.lastReactorTemp != reactorTemperature) {
            this.lastReactorTemp = reactorTemperature;
            refreshRedstone();
        }
    }

    private void refreshRedstone() {
        boolean shouldSignal;

        if (!this.isInverted()) {
            shouldSignal = this.lastReactorTemp >= this.thresholdTemperature;
        } else {
            shouldSignal = this.lastReactorTemp < this.thresholdTemperature;
        }

        int targetStrength = shouldSignal ? 15 : 0;

        if (this.signalStrength != targetStrength) {
            this.signalStrength = targetStrength;
            updateSignal();
        }
    }

    @Override
    public boolean canConnectRedstone(Direction side) {
        return side == getFrontFacing();
    }

    @Override
    public int getOutputSignal(@Nullable Direction side) {
        if (side == getFrontFacing().getOpposite()) {
            return this.signalStrength;
        }
        return 0;
    }

    @Override
    public void attachConfigurators(ConfiguratorPanel configuratorPanel) {
        this.attachInvertedConfigurator(configuratorPanel);
    }

    @Override
    public Widget createUIWidget() {
        var group = new WidgetGroup(0, 0, 100, 20);
        group.addWidget(new IntInputWidget(this::getThresholdTemperature, this::setThresholdTemperature)
                .setMin(300)
                .setMax(2500));
        return group;
    }
}