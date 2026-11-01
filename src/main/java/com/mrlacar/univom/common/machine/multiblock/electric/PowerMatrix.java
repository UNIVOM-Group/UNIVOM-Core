package com.mrlacar.univom.common.machine.multiblock.electric;

import com.gregtechceu.gtceu.api.capability.IEnergyContainer;
import com.gregtechceu.gtceu.api.capability.IEnergyInfoProvider;
import com.gregtechceu.gtceu.api.capability.recipe.EURecipeCapability;
import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.gui.GuiTextures;
import com.gregtechceu.gtceu.api.gui.fancy.*;
import com.gregtechceu.gtceu.api.machine.ConditionalSubscriptionHandler;
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.feature.IFancyUIMachine;
import com.gregtechceu.gtceu.api.machine.feature.multiblock.IDisplayUIMachine;
import com.gregtechceu.gtceu.api.machine.feature.multiblock.IMaintenanceMachine;
import com.gregtechceu.gtceu.api.machine.feature.multiblock.IMultiPart;
import com.gregtechceu.gtceu.api.machine.multiblock.IBatteryData;
import com.gregtechceu.gtceu.api.machine.multiblock.WorkableMultiblockMachine;
import com.gregtechceu.gtceu.api.machine.trait.RecipeLogic;
import com.gregtechceu.gtceu.api.misc.EnergyContainerList;
import com.gregtechceu.gtceu.common.data.GTItems;
import com.gregtechceu.gtceu.config.ConfigHolder;
import com.gregtechceu.gtceu.utils.FormattingUtil;
import com.lowdragmc.lowdraglib.gui.modular.ModularUI;
import com.lowdragmc.lowdraglib.gui.widget.*;
import com.lowdragmc.lowdraglib.syncdata.annotation.DescSynced;
import com.lowdragmc.lowdraglib.syncdata.annotation.Persisted;
import com.lowdragmc.lowdraglib.syncdata.field.ManagedFieldHolder;
import com.mrlacar.univom.api.gui.ModGuiTextures;
import com.mrlacar.univom.api.machine.feature.IEnergyNetworkDisplay;
import com.mrlacar.univom.api.machine.feature.IWirelessNode;
import com.mrlacar.univom.network.wireless.WirelessEnergyNetwork;
import com.mrlacar.univom.utils.WirelessEnergyNetworkUtils;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMaps;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.math.BigInteger;
import java.time.Duration;
import java.util.*;

public class PowerMatrix extends WorkableMultiblockMachine implements IEnergyInfoProvider, IFancyUIMachine, IDisplayUIMachine, IWirelessNode, IEnergyNetworkDisplay {
    protected static final ManagedFieldHolder MANAGED_FIELD_HOLDER = new ManagedFieldHolder(
            PowerMatrix.class, WorkableMultiblockMachine.MANAGED_FIELD_HOLDER);

    public static final int MAX_BATTERY_LAYERS = 18;
    public static final int MIN_CASINGS = 14;

    public static final long PASSIVE_DRAIN_DIVISOR = 20 * 60 * 60 * 24 * 100;
    public static final long PASSIVE_DRAIN_MAX_PER_STORAGE = 100_000L;

    public static final String PMC_BATTERY_HEADER = "PSSBattery_";

    private static final BigInteger BIG_INTEGER_MAX_LONG = BigInteger.valueOf(Long.MAX_VALUE);

    @Nullable
    @Getter
    @Setter
    private WirelessEnergyNetwork wirelessNetworkCache;

    @Persisted
    @Getter
    private UUID boundPlayerUUID;

    @Persisted
    private UUID substationID;

    @Persisted
    @DescSynced
    @Getter
    @Setter
    private boolean wirelessModeEnabled = false;

    @Persisted
    @DescSynced
    @Getter
    @Setter
    private DisplayMode displayMode = DisplayMode.MIXED;

    @Persisted
    private String localStoredStr = "0";

    private BigInteger internalStoredCache = null;
    private BigInteger lastPersistedStored = null;
    private BigInteger localCapacity = BigInteger.ZERO;
    private long passiveDrain;

    private IMaintenanceMachine maintenance;
    private EnergyContainerList inputHatches;
    private EnergyContainerList outputHatches;

    private long netInLastSec;
    @Getter
    private long inputPerSec;
    private long netOutLastSec;
    @Getter
    private long outputPerSec;

    protected ConditionalSubscriptionHandler tickSubscription;

    public PowerMatrix(IMachineBlockEntity holder) {
        super(holder);
        this.tickSubscription = new ConditionalSubscriptionHandler(this, this::transferEnergyTick, this::isFormed);
    }

    public UUID getSubstationID() {
        if (substationID == null) {
            substationID = UUID.randomUUID();
        }
        return substationID;
    }

    private BigInteger getLocalStored() {
        if (internalStoredCache == null) {
            if (localStoredStr == null || localStoredStr.isEmpty()) {
                internalStoredCache = BigInteger.ZERO;
            } else {
                try {
                    internalStoredCache = new BigInteger(localStoredStr);
                } catch (NumberFormatException e) {
                    internalStoredCache = BigInteger.ZERO;
                }
            }
        }
        return internalStoredCache;
    }

    private void setLocalStored(BigInteger val) {
        this.internalStoredCache = val;
    }

    @Override
    public void onStructureFormed() {
        super.onStructureFormed();
        List<IEnergyContainer> inputs = new ArrayList<>();
        List<IEnergyContainer> outputs = new ArrayList<>();
        Long2ObjectMap<IO> ioMap = getMultiblockState().getMatchContext().getOrCreate("ioMap", Long2ObjectMaps::emptyMap);

        for (IMultiPart part : getParts()) {
            IO io = ioMap.getOrDefault(part.self().getPos().asLong(), IO.BOTH);
            if (io == IO.NONE) continue;
            if (part instanceof IMaintenanceMachine maintenanceMachine) {
                this.maintenance = maintenanceMachine;
            }
            var handlerLists = part.getRecipeHandlers();
            for (var handlerList : handlerLists) {
                if (!handlerList.isValid(io)) continue;

                var containers = handlerList.getCapability(EURecipeCapability.CAP).stream()
                        .filter(IEnergyContainer.class::isInstance)
                        .map(IEnergyContainer.class::cast)
                        .toList();

                if (handlerList.getHandlerIO().support(IO.IN)) {
                    inputs.addAll(containers);
                } else if (handlerList.getHandlerIO().support(IO.OUT)) {
                    outputs.addAll(containers);
                }

                traitSubscriptions.add(handlerList.subscribe(tickSubscription::updateSubscription, EURecipeCapability.CAP));
            }
        }
        this.inputHatches = new EnergyContainerList(inputs);
        this.outputHatches = new EnergyContainerList(outputs);

        List<IBatteryData> batteries = new ArrayList<>();
        for (Map.Entry<String, Object> battery : getMultiblockState().getMatchContext().entrySet()) {
            if (battery.getKey().startsWith(PMC_BATTERY_HEADER) &&
                    battery.getValue() instanceof PowerMatrix.BatteryMatchWrapper wrapper) {
                for (int i = 0; i < wrapper.amount; i++) {
                    batteries.add(wrapper.partType);
                }
            }
        }

        if (batteries.isEmpty()) {
            onStructureInvalid();
            return;
        }

        long[] maximums = new long[batteries.size()];
        for (int i = 0; i < batteries.size(); i++) maximums[i] = batteries.get(i).getCapacity();

        this.localCapacity = summarize(maximums);
        this.passiveDrain = calculatePassiveDrain(maximums, this.localCapacity);

        if (getWirelessNetwork() != null) {
            getWirelessNetwork().setSubstationCapacity(getSubstationID(), this.localCapacity);
        }
    }

    @Override
    public void onStructureInvalid() {
        if (getWirelessNetwork() != null) {
            getWirelessNetwork().setSubstationCapacity(getSubstationID(), BigInteger.ZERO);
        }

        inputHatches = null;
        outputHatches = null;
        passiveDrain = 0;
        netInLastSec = 0;
        inputPerSec = 0;
        netOutLastSec = 0;
        outputPerSec = 0;
        super.onStructureInvalid();
    }

    protected void transferEnergyTick() {
        if (!getLevel().isClientSide) {
            WirelessEnergyNetwork network = getWirelessNetwork();
            BigInteger currentLocal = getLocalStored();

            if (getOffsetTimer() % 20 == 0) {
                getRecipeLogic().setStatus((network != null && currentLocal.compareTo(BigInteger.ZERO) > 0) ?
                        RecipeLogic.Status.WORKING : RecipeLogic.Status.IDLE);
                inputPerSec = netInLastSec;
                outputPerSec = netOutLastSec;
                netInLastSec = 0;
                netOutLastSec = 0;

                if (this.internalStoredCache != null && !this.internalStoredCache.equals(this.lastPersistedStored)) {
                    this.localStoredStr = this.internalStoredCache.toString();
                    this.lastPersistedStored = this.internalStoredCache;
                }
            }

            if (isWorkingEnabled() && isFormed()) {
                long availableIn = inputHatches.getEnergyStored();
                if (availableIn > 0) {
                    BigInteger spaceLeft = localCapacity.subtract(currentLocal);
                    if (spaceLeft.compareTo(BigInteger.ZERO) > 0) {
                        long toInsert = Math.min(availableIn, WirelessEnergyNetworkUtils.getLongValue(spaceLeft));
                        currentLocal = currentLocal.add(BigInteger.valueOf(toInsert));
                        inputHatches.changeEnergy(-toInsert);
                        netInLastSec += toInsert;
                    }
                }

                long drain = getPassiveDrain();
                if (drain > 0) {
                    long actualDrain = Math.min(drain, WirelessEnergyNetworkUtils.getLongValue(currentLocal));
                    currentLocal = currentLocal.subtract(BigInteger.valueOf(actualDrain));
                    netOutLastSec += actualDrain;
                }

                long neededOut = outputHatches.getEnergyCapacity() - outputHatches.getEnergyStored();
                if (neededOut > 0) {
                    long toExtract = Math.min(neededOut, WirelessEnergyNetworkUtils.getLongValue(currentLocal));
                    if (toExtract > 0) {
                        currentLocal = currentLocal.subtract(BigInteger.valueOf(toExtract));
                        outputHatches.changeEnergy(toExtract);
                        netOutLastSec += toExtract;
                    }
                }

                int uploadInterval = com.mrlacar.univom.config.ConfigHolder.INSTANCE.WirelessEnergyNetworkConfigs.AutoBalancingInterval;
                if (uploadInterval < 20) uploadInterval = 20;

                if (this.wirelessModeEnabled && getOffsetTimer() % uploadInterval == 0 && network != null) {
                    BigInteger halfToUpload = currentLocal.divide(BigInteger.valueOf(2));
                    if (halfToUpload.compareTo(BigInteger.ZERO) > 0) {
                        BigInteger actualInserted = network.addEnergy(halfToUpload);
                        currentLocal = currentLocal.subtract(actualInserted);
                    }
                }

                setLocalStored(currentLocal);
            }
        }
    }

    @Override
    public void attachConfigurators(ConfiguratorPanel configuratorPanel) {
        IEnergyNetworkDisplay.attachConfigurators(configuratorPanel, this);

        configuratorPanel.attachConfigurators(new IFancyConfiguratorButton.Toggle(
                ModGuiTextures.BUTTON_NETWORK_UPLOAD.getSubTexture(0, 0, 1, 0.5),
                ModGuiTextures.BUTTON_NETWORK_UPLOAD.getSubTexture(0, 0.5, 1, 0.5),
                this::isWirelessModeEnabled,
                (cd, p) -> setWirelessModeEnabled(p)
        ).setTooltipsSupplier(p -> List.of(Component.translatable("univom.gui.energy_matrix.title"))));
    }

    @Override
    public InteractionResult onUse(BlockState state, Level world, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (isRemote()) return InteractionResult.PASS;
        ItemStack is = player.getItemInHand(hand);

        if (is.is(GTItems.TOOL_DATA_STICK.asItem())) {
            if (!player.getUUID().equals(this.boundPlayerUUID)) {
                if (isFormed() && getWirelessNetwork() != null) {
                    getWirelessNetwork().setSubstationCapacity(getSubstationID(), BigInteger.ZERO);
                }

                this.boundPlayerUUID = player.getUUID();
                setWirelessNetworkCache(null);

                if (isFormed() && getWirelessNetwork() != null) {
                    getWirelessNetwork().setSubstationCapacity(getSubstationID(), this.localCapacity);
                }

                player.displayClientMessage(Component.translatable("univom.message.bind.wireless_energy_hatch.bind", WirelessEnergyNetworkUtils.getPlayer(player)), true);
            }
            return InteractionResult.SUCCESS;
        }
        return super.onUse(state, world, pos, player, hand, hit);
    }

    @Override
    public boolean onLeftClick(Player player, Level world, InteractionHand hand, BlockPos pos, Direction direction) {
        if (isRemote()) return false;
        ItemStack is = player.getItemInHand(hand);

        if (is.is(GTItems.TOOL_DATA_STICK.asItem())) {
            if (this.boundPlayerUUID != null) {
                if (isFormed() && getWirelessNetwork() != null) {
                    getWirelessNetwork().setSubstationCapacity(getSubstationID(), BigInteger.ZERO);
                }

                this.boundPlayerUUID = null;
                setWirelessNetworkCache(null);

                player.displayClientMessage(Component.translatable("univom.message.bind.wireless_energy_hatch.unbind"), true);
            }
            return true;
        }
        return super.onLeftClick(player, world, hand, pos, direction);
    }

    @Override
    public void addDisplayText(List<Component> textList) {
        IDisplayUIMachine.super.addDisplayText(textList);
        if (isFormed()) {
            WirelessEnergyNetwork network = getWirelessNetwork();

            if (!isWorkingEnabled()) {
                textList.add(Component.translatable("gtceu.multiblock.work_paused"));
            } else if (isActive()) {
                textList.add(Component.translatable("gtceu.multiblock.running"));
            } else {
                textList.add(Component.translatable("gtceu.multiblock.idling"));
            }

            var STYLE_GOLD = Style.EMPTY.withColor(ChatFormatting.GOLD);
            var STYLE_AQUA = Style.EMPTY.withColor(ChatFormatting.AQUA);
            var STYLE_DARK_RED = Style.EMPTY.withColor(ChatFormatting.DARK_RED);
            var STYLE_GREEN = Style.EMPTY.withColor(ChatFormatting.GREEN);
            var STYLE_RED = Style.EMPTY.withColor(ChatFormatting.RED);

            boolean showLocal = (this.displayMode == DisplayMode.LOCAL || this.displayMode == DisplayMode.MIXED);
            boolean showNetwork = (this.displayMode == DisplayMode.NETWORK || this.displayMode == DisplayMode.MIXED);

            if (showLocal) {
                var storedComponent = Component.literal(FormattingUtil.formatNumbers(getLocalStored()));
                textList.add(Component.translatable("gtceu.multiblock.power_substation.stored",
                        storedComponent.setStyle(STYLE_GOLD)));

                var capacityComponent = Component.literal(FormattingUtil.formatNumbers(this.localCapacity));
                textList.add(Component.translatable("gtceu.multiblock.power_substation.capacity",
                        capacityComponent.setStyle(STYLE_GOLD)));

                var passiveDrainComponent = Component.literal(FormattingUtil.formatNumbers(getPassiveDrain()));
                textList.add(Component.translatable("gtceu.multiblock.power_substation.passive_drain",
                        passiveDrainComponent.setStyle(STYLE_DARK_RED)));

                var avgInComponent = Component.literal(FormattingUtil.formatNumbers(inputPerSec / 20));
                textList.add(Component.translatable("gtceu.multiblock.power_substation.average_in",
                                avgInComponent.setStyle(STYLE_GREEN))
                        .withStyle(Style.EMPTY.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                                Component.translatable("gtceu.multiblock.power_substation.average_in_hover")))));

                var avgOutComponent = Component.literal(FormattingUtil.formatNumbers(Math.abs(outputPerSec / 20)));
                textList.add(Component.translatable("gtceu.multiblock.power_substation.average_out",
                                avgOutComponent.setStyle(STYLE_RED))
                        .withStyle(Style.EMPTY.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                                Component.translatable("gtceu.multiblock.power_substation.average_out_hover")))));

                if (inputPerSec > outputPerSec) {
                    BigInteger timeToFillSeconds = this.localCapacity.subtract(getLocalStored())
                            .divide(BigInteger.valueOf(Math.max(1, inputPerSec - outputPerSec)));
                    textList.add(Component.translatable("gtceu.multiblock.power_substation.time_to_fill",
                            getTimeToFillDrainText(timeToFillSeconds).setStyle(STYLE_GREEN)));
                } else if (inputPerSec < outputPerSec) {
                    BigInteger timeToDrainSeconds = getLocalStored()
                            .divide(BigInteger.valueOf(Math.max(1, outputPerSec - inputPerSec)));
                    textList.add(Component.translatable("gtceu.multiblock.power_substation.time_to_drain",
                            getTimeToFillDrainText(timeToDrainSeconds).setStyle(STYLE_RED)));
                }
            }

            if (showLocal && showNetwork) {
                textList.add(Component.literal(" "));
            }

            if (showNetwork) {
                if (network != null) {
                    var statusKey = this.wirelessModeEnabled ? "univom.multiblock.energy_matrix.network_status.enabled" : "univom.multiblock.energy_matrix.network_status.disabled";
                    var statusComp = Component.translatable(statusKey).setStyle(this.wirelessModeEnabled ? STYLE_GREEN : STYLE_RED);
                    textList.add(Component.translatable("univom.multiblock.energy_matrix.network_upload_status", statusComp));

                    var netStoredComp = Component.literal(FormattingUtil.formatNumbers(network.getStorage())).setStyle(STYLE_AQUA);
                    var netCapComp = Component.literal(FormattingUtil.formatNumbers(network.getCapacity())).setStyle(STYLE_AQUA);
                    textList.add(Component.translatable("univom.multiblock.energy_matrix.network_stored", netStoredComp));
                    textList.add(Component.translatable("univom.multiblock.energy_matrix.network_capacity", netCapComp));

                    long netAvgIn = network.getLastSecAdded() / 20;
                    long netAvgOut = network.getLastSecRemoved() / 20;

                    var netAvgInComp = Component.literal(FormattingUtil.formatNumbers(netAvgIn)).setStyle(STYLE_GREEN);
                    textList.add(Component.translatable("univom.multiblock.energy_matrix.network_avg_in", netAvgInComp));

                    var netAvgOutComp = Component.literal(FormattingUtil.formatNumbers(netAvgOut)).setStyle(STYLE_RED);
                    textList.add(Component.translatable("univom.multiblock.energy_matrix.network_avg_out", netAvgOutComp));
                } else {
                    textList.add(Component.translatable("univom.multiblock.energy_matrix.network_not_bound").setStyle(STYLE_RED));
                }
            }
        }
        getDefinition().getAdditionalDisplay().accept(this, textList);
    }

    private static MutableComponent getTimeToFillDrainText(BigInteger timeToFillSeconds) {
        if (timeToFillSeconds.compareTo(BIG_INTEGER_MAX_LONG) > 0) timeToFillSeconds = BIG_INTEGER_MAX_LONG;
        Duration duration = Duration.ofSeconds(timeToFillSeconds.longValue());
        String key;
        long fillTime;
        if (duration.getSeconds() <= 180) { fillTime = duration.getSeconds(); key = "gtceu.multiblock.power_substation.time_seconds"; }
        else if (duration.toMinutes() <= 180) { fillTime = duration.toMinutes(); key = "gtceu.multiblock.power_substation.time_minutes"; }
        else if (duration.toHours() <= 72) { fillTime = duration.toHours(); key = "gtceu.multiblock.power_substation.time_hours"; }
        else if (duration.toDays() <= 730) { fillTime = duration.toDays(); key = "gtceu.multiblock.power_substation.time_days"; }
        else if (duration.toDays() / 365 < 1_000_000) { fillTime = duration.toDays() / 365; key = "gtceu.multiblock.power_substation.time_years"; }
        else return Component.translatable("gtceu.multiblock.power_substation.time_forever");

        return Component.translatable(key, FormattingUtil.formatNumbers(fillTime));
    }

    public long getPassiveDrain() {
        if (ConfigHolder.INSTANCE.machines.enableMaintenance) {
            if (maintenance == null) {
                for (IMultiPart part : getParts()) {
                    if (part instanceof IMaintenanceMachine maintenanceMachine) {
                        this.maintenance = maintenanceMachine;
                        break;
                    }
                }
            }
            int multiplier = 1 + (maintenance != null ? maintenance.getNumMaintenanceProblems() : 0);
            double modifier = maintenance != null ? maintenance.getDurationMultiplier() : 1.0;
            return (long) (passiveDrain * multiplier * modifier);
        }
        return passiveDrain;
    }

    @Override
    public EnergyInfo getEnergyInfo() {
        return new EnergyInfo(this.localCapacity, getLocalStored());
    }

    @Override
    public boolean supportsBigIntEnergyValues() {
        return true;
    }

    @Override
    public @NotNull ManagedFieldHolder getFieldHolder() {
        return MANAGED_FIELD_HOLDER;
    }

    @Override
    public Widget createUIWidget() {
        var group = new WidgetGroup(0, 0, 182 + 8, 117 + 8);
        group.addWidget(new DraggableScrollableWidgetGroup(4, 4, 182, 117).setBackground(getScreenTexture())
                .addWidget(new LabelWidget(4, 5, self().getBlockState().getBlock().getDescriptionId()))
                .addWidget(new ComponentPanelWidget(4, 17, this::addDisplayText)
                        .textSupplier(isRemote() ? null : this::addDisplayText)
                        .setMaxWidthLimit(150)
                        .clickHandler(this::handleDisplayClick)));
        group.setBackground(GuiTextures.BACKGROUND_INVERSE);
        return group;
    }

    @Override
    public ModularUI createUI(Player entityPlayer) {
        return new ModularUI(198, 208, this, entityPlayer).widget(new FancyMachineUIWidget(this, 198, 208));
    }

    @Override
    public List<IFancyUIProvider> getSubTabs() {
        return getParts().stream()
                .filter(IFancyUIProvider.class::isInstance)
                .map(IFancyUIProvider.class::cast)
                .toList();
    }

    @Override
    public void attachTooltips(TooltipsPanel tooltipsPanel) {
        for (IMultiPart part : getParts()) {
            part.attachFancyTooltipsToController(this, tooltipsPanel);
        }
    }

    private static BigInteger summarize(long[] values) {
        BigInteger retVal = BigInteger.ZERO;
        long currentSum = 0;
        for (long value : values) {
            if (currentSum != 0 && value > Long.MAX_VALUE - currentSum) {
                retVal = retVal.add(BigInteger.valueOf(currentSum));
                currentSum = 0;
            }
            currentSum += value;
        }
        if (currentSum != 0) {
            retVal = retVal.add(BigInteger.valueOf(currentSum));
        }
        return retVal;
    }

    private static long calculatePassiveDrain(long[] maximums, BigInteger capacity) {
        long[] maximumsExcl = new long[maximums.length];
        int index = 0;
        int numExcl = 0;
        for (long maximum : maximums) {
            if (maximum / PASSIVE_DRAIN_DIVISOR >= PASSIVE_DRAIN_MAX_PER_STORAGE) {
                numExcl++;
            } else {
                maximumsExcl[index++] = maximum;
            }
        }
        maximumsExcl = Arrays.copyOf(maximumsExcl, index);
        BigInteger capacityExcl = summarize(maximumsExcl);

        return capacityExcl.divide(BigInteger.valueOf(PASSIVE_DRAIN_DIVISOR))
                .add(BigInteger.valueOf(PASSIVE_DRAIN_MAX_PER_STORAGE * numExcl))
                .longValue();
    }

    @Getter
    public static class BatteryMatchWrapper {

        private final IBatteryData partType;
        private int amount;

        public BatteryMatchWrapper(IBatteryData partType) {
            this.partType = partType;
        }

        public PowerMatrix.BatteryMatchWrapper increment() {
            amount++;
            return this;
        }
    }
}
