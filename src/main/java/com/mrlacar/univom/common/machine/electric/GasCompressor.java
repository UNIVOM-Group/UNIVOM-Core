package com.mrlacar.univom.common.machine.electric;

import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.capability.IControllable;
import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.gui.GuiTextures;
import com.gregtechceu.gtceu.api.gui.widget.IntInputWidget;
import com.gregtechceu.gtceu.api.gui.widget.TankWidget;
import com.gregtechceu.gtceu.api.item.tool.GTToolType;
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.TickableSubscription;
import com.gregtechceu.gtceu.api.machine.TieredEnergyMachine;
import com.gregtechceu.gtceu.api.machine.feature.IFancyUIMachine;
import com.gregtechceu.gtceu.api.machine.property.GTMachineModelProperties;
import com.gregtechceu.gtceu.api.machine.trait.NotifiableFluidTank;
import com.gregtechceu.gtceu.api.machine.trait.RecipeLogic;
import com.gregtechceu.gtceu.api.transfer.fluid.IFluidHandlerModifiable;
import com.gregtechceu.gtceu.client.model.machine.MachineRenderState;
import com.gregtechceu.gtceu.common.data.machines.GTMachineUtils;
import com.gregtechceu.gtceu.utils.FormattingUtil;
import com.lowdragmc.lowdraglib.gui.texture.ResourceTexture;
import com.lowdragmc.lowdraglib.gui.widget.ComponentPanelWidget;
import com.lowdragmc.lowdraglib.gui.widget.LabelWidget;
import com.lowdragmc.lowdraglib.gui.widget.Widget;
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;
import com.lowdragmc.lowdraglib.syncdata.ISubscription;
import com.lowdragmc.lowdraglib.syncdata.annotation.DescSynced;
import com.lowdragmc.lowdraglib.syncdata.annotation.Persisted;
import com.lowdragmc.lowdraglib.syncdata.field.ManagedFieldHolder;
import com.mrlacar.univom.api.machine.feature.IPressureEndpoint;
import com.mrlacar.univom.common.blockentity.PressurePipeBlockEntity;
import com.mrlacar.univom.common.pipelike.pressure.GasCompression;
import com.mrlacar.univom.common.pipelike.pressure.PressurePipeNet;
import lombok.Getter;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.common.Tags;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.lang.ref.WeakReference;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class GasCompressor extends TieredEnergyMachine implements IPressureEndpoint, IFancyUIMachine,
                                     IControllable {

    protected static final ManagedFieldHolder MANAGED_FIELD_HOLDER = new ManagedFieldHolder(
            GasCompressor.class, TieredEnergyMachine.MANAGED_FIELD_HOLDER);

    private static final int IDLE_INTERVAL = 10;
    private static final int ACTIVE_HOLD = 40;

    @Persisted
    public final NotifiableFluidTank tank;
    @Getter
    @Persisted
    @DescSynced
    private int targetPressure;
    @Getter
    @Persisted
    @DescSynced
    private boolean workingEnabled = true;
    @Getter
    private GasCompression.Status status = GasCompression.Status.NO_GAS;
    @Getter
    private long lastFlow;
    private int activeTicks;
    private boolean recheck;
    @Nullable
    private TickableSubscription workSubs;
    @Nullable
    private ISubscription tankSubs;
    @Nullable
    private Fluid cachedFluid;
    @Nullable
    private ResourceLocation cachedGas;
    private WeakReference<PressurePipeBlockEntity> outlet = new WeakReference<>(null);

    public GasCompressor(IMachineBlockEntity holder, int tier, Object... args) {
        super(holder, tier, args);
        this.targetPressure = GasCompression.targetCap(tier);
        this.tank = new NotifiableFluidTank(this, 1, GTMachineUtils.defaultTankSizeFunction.applyAsInt(tier), IO.IN)
                .setFilter(GasCompressor::isGas);
    }

    @Override
    public ManagedFieldHolder getFieldHolder() {
        return MANAGED_FIELD_HOLDER;
    }

    public static Component[] tooltips(int tier) {
        long voltage = GTValues.V[tier];
        int flow = GasCompression.flow(tier);
        return new Component[] {
                Component.translatable("univom.machine.gas_compressor.tooltip.0"),
                Component.translatable("univom.machine.gas_compressor.tooltip.1", GasCompression.targetCap(tier)),
                Component.translatable("univom.machine.gas_compressor.tooltip.2", flow),
                Component.translatable("univom.machine.gas_compressor.tooltip.3",
                        FormattingUtil.formatNumbers(GasCompression.energyCost(tier, 0.0D, flow)),
                        FormattingUtil.formatNumbers(voltage)),
                Component.translatable("gtceu.universal.tooltip.voltage_in", FormattingUtil.formatNumbers(voltage),
                        GTValues.VNF[tier]),
                Component.translatable("gtceu.universal.tooltip.energy_storage_capacity",
                        FormattingUtil.formatNumbers(voltage * 64L)),
                Component.translatable("gtceu.universal.tooltip.fluid_storage_capacity",
                        FormattingUtil.formatNumbers(GTMachineUtils.defaultTankSizeFunction.applyAsInt(tier)))
        };
    }

    public static boolean isGas(FluidStack stack) {
        Fluid fluid = stack.getFluid();
        return !stack.isEmpty() && (fluid.is(Tags.Fluids.GASEOUS) || fluid.getFluidType().isLighterThanAir());
    }

    public Direction getOutletFacing() {
        return getFrontFacing().getOpposite();
    }

    @Override
    public boolean canConnectPressure(Direction side) {
        return side == getOutletFacing();
    }

    public void setTargetPressure(int targetPressure) {
        int clamped = Mth.clamp(targetPressure, 1, GasCompression.targetCap(getTier()));
        if (this.targetPressure != clamped) {
            this.targetPressure = clamped;
            recheck = true;
            markDirty();
        }
    }

    @Override
    public void setWorkingEnabled(boolean workingEnabled) {
        if (this.workingEnabled != workingEnabled) {
            this.workingEnabled = workingEnabled;
            markDirty();
            if (!isRemote()) {
                updateSubscription();
            }
        }
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (!isRemote()) {
            tankSubs = tank.addChangedListener(this::updateSubscription);
            updateSubscription();
        }
    }

    @Override
    public void onUnload() {
        super.onUnload();
        if (tankSubs != null) {
            tankSubs.unsubscribe();
            tankSubs = null;
        }
        workSubs = null;
    }

    @Override
    public @Nullable IFluidHandlerModifiable getFluidHandlerCap(@Nullable Direction side, boolean useCoverCapability) {
        return side != null && side == getOutletFacing() ? null : super.getFluidHandlerCap(side, useCoverCapability);
    }

    @Override
    protected InteractionResult onWrenchClick(Player playerIn, InteractionHand hand, Direction gridSide,
                                              BlockHitResult hitResult) {
        if (playerIn.isShiftKeyDown()) {
            return super.onWrenchClick(playerIn, hand, gridSide, hitResult);
        }
        Direction front = gridSide.getOpposite();
        if (gridSide == getOutletFacing() || !isFacingValid(front)) {
            return InteractionResult.FAIL;
        }
        setFrontFacing(front);
        recheck = true;
        return InteractionResult.sidedSuccess(isRemote());
    }

    @Override
    public @Nullable ResourceTexture sideTips(Player player, BlockPos pos, BlockState state, Set<GTToolType> toolTypes,
                                              Direction side) {
        if (toolTypes.contains(GTToolType.WRENCH)) {
            if (!player.isShiftKeyDown()) {
                return side == getOutletFacing() ? null : GuiTextures.TOOL_IO_FACING_ROTATION;
            }
            return isFacingValid(side) ? GuiTextures.TOOL_FRONT_FACING_ROTATION : null;
        }
        return super.sideTips(player, pos, state, toolTypes, side);
    }

    @Override
    public Widget createUIWidget() {
        var group = new WidgetGroup(0, 0, 168, 88);
        var screen = new WidgetGroup(4, 4, 136, 44);
        screen.setBackground(GuiTextures.DISPLAY);
        screen.addWidget(new ComponentPanelWidget(4, 4, this::addDisplayText)
                .textSupplier(isRemote() ? null : this::addDisplayText)
                .setMaxWidthLimit(128));
        group.addWidget(screen);
        group.addWidget(new TankWidget(tank.getStorages()[0], 146, 17, true, true)
                .setBackground(GuiTextures.FLUID_SLOT));
        group.addWidget(new LabelWidget(4, 54, "univom.machine.gas_compressor.target"));
        group.addWidget(new IntInputWidget(4, 64, 100, 20, this::getTargetPressure, this::setTargetPressure)
                .setMin(1)
                .setMax(GasCompression.targetCap(getTier())));
        return group;
    }

    private void addDisplayText(List<Component> textList) {
        textList.add(Component.translatable(status.getTranslationKey()).withStyle(status.getColor()));
        PressurePipeNet net = outletNet();
        if (net != null) {
            textList.add(Component.translatable("univom.machine.gas_compressor.pressure",
                    String.format(Locale.ROOT, "%.1f", net.getPressure()), targetPressure));
        }
        textList.add(Component.translatable("univom.machine.gas_compressor.flow", lastFlow,
                GasCompression.flow(getTier())));
    }

    private void updateSubscription() {
        if (workingEnabled && !tank.isEmpty()) {
            if (workSubs == null) {
                recheck = true;
            }
            workSubs = subscribeServerTick(workSubs, this::update);
            return;
        }
        unsubscribe(workSubs);
        workSubs = null;
        status = workingEnabled ? GasCompression.Status.NO_GAS : GasCompression.Status.DISABLED;
        lastFlow = 0;
        activeTicks = 0;
        updateRenderStatus();
    }

    private void update() {
        if (!recheck && status != GasCompression.Status.WORKING && getOffsetTimer() % IDLE_INTERVAL != 0) {
            coolDown();
            return;
        }
        recheck = false;
        FluidStack gas = tank.getFluidInTank(0);
        GasCompression.Result result = GasCompression.compress(outletNet(), gasId(gas), gas.getAmount(),
                energyContainer.getEnergyStored(), getTier(), targetPressure);
        status = result.status();
        lastFlow = result.inserted();
        if (result.inserted() <= 0) {
            if (activeTicks > 0) {
                activeTicks--;
            }
            updateRenderStatus();
            return;
        }
        energyContainer.removeEnergy(result.energy());
        activeTicks = ACTIVE_HOLD;
        updateRenderStatus();
        tank.drainInternal((int) result.inserted(), IFluidHandler.FluidAction.EXECUTE);
    }

    private void coolDown() {
        if (activeTicks > 0 && --activeTicks == 0) {
            updateRenderStatus();
        }
    }

    private void updateRenderStatus() {
        RecipeLogic.Status target;
        if (!workingEnabled) {
            target = RecipeLogic.Status.SUSPEND;
        } else if (activeTicks > 0) {
            target = RecipeLogic.Status.WORKING;
        } else {
            target = RecipeLogic.Status.IDLE;
        }
        MachineRenderState state = getRenderState();
        if (state.hasProperty(GTMachineModelProperties.RECIPE_LOGIC_STATUS) &&
                state.getValue(GTMachineModelProperties.RECIPE_LOGIC_STATUS) != target) {
            setRenderState(state.setValue(GTMachineModelProperties.RECIPE_LOGIC_STATUS, target));
        }
    }

    @Nullable
    private ResourceLocation gasId(FluidStack stack) {
        if (stack.isEmpty()) {
            return null;
        }
        Fluid fluid = stack.getFluid();
        if (fluid != cachedFluid) {
            cachedFluid = fluid;
            cachedGas = ForgeRegistries.FLUIDS.getKey(fluid);
        }
        return cachedGas;
    }

    @Nullable
    private PressurePipeNet outletNet() {
        Level level = getLevel();
        if (level == null) {
            return null;
        }
        Direction side = getOutletFacing();
        BlockPos pos = getPos().relative(side);
        PressurePipeBlockEntity pipe = outlet.get();
        if (pipe == null || pipe.isRemoved() || !pipe.getBlockPos().equals(pos)) {
            if (!level.isLoaded(pos) || !(level.getBlockEntity(pos) instanceof PressurePipeBlockEntity found)) {
                return null;
            }
            pipe = found;
            outlet = new WeakReference<>(found);
        }
        return pipe.isConnected(side.getOpposite()) ? pipe.getPressureNet() : null;
    }
}
