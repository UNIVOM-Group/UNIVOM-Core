package com.mrlacar.univom.common.machine.multiblock.part;

import com.gregtechceu.gtceu.api.capability.IDataAccessHatch;
import com.gregtechceu.gtceu.api.capability.IOpticalDataAccessHatch;
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.TickableSubscription;
import com.gregtechceu.gtceu.api.machine.feature.multiblock.IMultiController;
import com.gregtechceu.gtceu.api.machine.feature.multiblock.IWorkableMultiController;
import com.gregtechceu.gtceu.api.machine.multiblock.PartAbility;
import com.gregtechceu.gtceu.api.machine.multiblock.part.MultiblockPartMachine;
import com.gregtechceu.gtceu.api.pattern.MultiblockWorldSavedData;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.lowdragmc.lowdraglib.gui.widget.LabelWidget;
import com.lowdragmc.lowdraglib.gui.widget.TextFieldWidget;
import com.lowdragmc.lowdraglib.gui.widget.Widget;
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;
import com.lowdragmc.lowdraglib.syncdata.annotation.DescSynced;
import com.lowdragmc.lowdraglib.syncdata.annotation.Persisted;
import com.lowdragmc.lowdraglib.syncdata.field.ManagedFieldHolder;
import com.mrlacar.univom.api.data.ChannelKey;
import lombok.Getter;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class WirelessOpticalDataHatchMachine extends MultiblockPartMachine implements IOpticalDataAccessHatch {
    protected static final ManagedFieldHolder MANAGED_FIELD_HOLDER = new ManagedFieldHolder(
            WirelessOpticalDataHatchMachine.class, MultiblockPartMachine.MANAGED_FIELD_HOLDER);

    public static final ConcurrentHashMap<ChannelKey, CopyOnWriteArrayList<WeakReference<WirelessOpticalDataHatchMachine>>> TRANSMITTERS = new ConcurrentHashMap<>();
    public static final ConcurrentHashMap<ChannelKey, CopyOnWriteArrayList<WeakReference<WirelessOpticalDataHatchMachine>>> RECEIVERS = new ConcurrentHashMap<>();

    @Getter
    private final boolean transmitter;
    @Getter
    @Persisted
    private String channel = "";

    @Getter
    @Persisted
    @DescSynced
    private int connectionStatus = 0;

    private TickableSubscription tickSubscription;
    private int ticksSinceLastInput = -1;

    private transient ChannelKey registeredKey;

    public WirelessOpticalDataHatchMachine(IMachineBlockEntity holder, boolean transmitter) {
        super(holder);
        this.transmitter = transmitter;
    }

    @Override
    public ManagedFieldHolder getFieldHolder() {
        return MANAGED_FIELD_HOLDER;
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (!isRemote()) {
            syncRegistration();
            tickSubscription = subscribeServerTick(this::serverUpdate);
        }
    }

    @Override
    public void onUnload() {
        super.onUnload();
        if (!isRemote()) {
            releaseRegistration();
            if (tickSubscription != null) {
                tickSubscription.unsubscribe();
                tickSubscription = null;
            }
        }
    }

    private void serverUpdate() {
        if (getOffsetTimer() % 20 == 0) {
            syncRegistration();
            updateConnectionStatus();
        }

        if (ticksSinceLastInput >= 0) {
            ticksSinceLastInput++;
            if (ticksSinceLastInput >= 20) {
                rebuildMultiblock();
                ticksSinceLastInput = -1;
            }
        }
    }

    @Nullable
    private ChannelKey currentChannelKey() {
        if (this.channel.isEmpty()) return null;
        return ChannelKey.of(getOwnerUUID(), this.channel);
    }

    private ConcurrentHashMap<ChannelKey, CopyOnWriteArrayList<WeakReference<WirelessOpticalDataHatchMachine>>> ownMap() {
        return isTransmitter() ? TRANSMITTERS : RECEIVERS;
    }

    @Nullable
    private static WirelessOpticalDataHatchMachine getActiveMachine(
            ConcurrentHashMap<ChannelKey, CopyOnWriteArrayList<WeakReference<WirelessOpticalDataHatchMachine>>> map,
            @Nullable ChannelKey key) {
        if (key == null) return null;
        CopyOnWriteArrayList<WeakReference<WirelessOpticalDataHatchMachine>> list = map.get(key);
        if (list == null || list.isEmpty()) return null;

        WirelessOpticalDataHatchMachine found = null;
        boolean needsCleanup = false;

        for (WeakReference<WirelessOpticalDataHatchMachine> ref : list) {
            WirelessOpticalDataHatchMachine m = ref.get();
            if (m == null || m.isInValid()) {
                needsCleanup = true;
                continue;
            }
            if (found == null) found = m;
        }

        if (needsCleanup) {
            list.removeIf(r -> {
                WirelessOpticalDataHatchMachine m = r.get();
                return m == null || m.isInValid();
            });
            if (list.isEmpty()) map.remove(key, list);
        }
        return found;
    }

    private void updateConnectionStatus() {
        ChannelKey key = currentChannelKey();
        if (key == null) {
            this.connectionStatus = 0;
            return;
        }

        if (isTransmitter()) {
            WirelessOpticalDataHatchMachine activeTx = getActiveMachine(TRANSMITTERS, key);
            if (activeTx != this) {
                this.connectionStatus = 6;
            } else {
                WirelessOpticalDataHatchMachine activeRx = getActiveMachine(RECEIVERS, key);
                this.connectionStatus = (activeRx != null) ? 5 : 4;
            }
        } else {
            WirelessOpticalDataHatchMachine activeRx = getActiveMachine(RECEIVERS, key);
            if (activeRx != this) {
                this.connectionStatus = 3;
            } else {
                WirelessOpticalDataHatchMachine activeTx = getActiveMachine(TRANSMITTERS, key);
                this.connectionStatus = (activeTx != null) ? 2 : 1;
            }
        }
    }

    private void syncRegistration() {
        ChannelKey key = currentChannelKey();
        if (Objects.equals(this.registeredKey, key)) return;

        releaseRegistration();
        if (key == null) return;

        CopyOnWriteArrayList<WeakReference<WirelessOpticalDataHatchMachine>> list =
                ownMap().computeIfAbsent(key, k -> new CopyOnWriteArrayList<>());

        for (WeakReference<WirelessOpticalDataHatchMachine> ref : list) {
            if (ref.get() == this) {
                this.registeredKey = key;
                return;
            }
        }
        list.add(new WeakReference<>(this));
        this.registeredKey = key;
    }

    private void releaseRegistration() {
        if (this.registeredKey == null) return;

        ConcurrentHashMap<ChannelKey, CopyOnWriteArrayList<WeakReference<WirelessOpticalDataHatchMachine>>> map = ownMap();
        CopyOnWriteArrayList<WeakReference<WirelessOpticalDataHatchMachine>> list = map.get(this.registeredKey);
        if (list != null) {
            list.removeIf(ref -> {
                WirelessOpticalDataHatchMachine m = ref.get();
                return m == null || m == this;
            });
            if (list.isEmpty()) map.remove(this.registeredKey, list);
        }
        this.registeredKey = null;
    }

    private void setChannel(String newChannel) {
        String trimmed = newChannel.trim();
        if (this.channel.equals(trimmed)) return;

        releaseRegistration();
        this.channel = trimmed;
        syncRegistration();

        this.ticksSinceLastInput = 0;
    }

    private Component getLinkStatusComponent() {
        return switch (this.connectionStatus) {
            case 1 -> Component.translatable("univom.multiblock.wireless_optical_hatch.offline");
            case 2 -> Component.translatable("univom.multiblock.wireless_optical_hatch.linked_source");
            case 3 -> Component.translatable("univom.multiblock.wireless_optical_hatch.conflict_target");
            case 4 -> Component.translatable("univom.multiblock.wireless_optical_hatch.waiting_target");
            case 5 -> Component.translatable("univom.multiblock.wireless_optical_hatch.linked_target");
            case 6 -> Component.translatable("univom.multiblock.wireless_optical_hatch.conflict_source");
            default -> Component.translatable("univom.multiblock.wireless_optical_hatch.unconfigured");
        };
    }

    private void rebuildMultiblock() {
        if (getLevel() instanceof ServerLevel serverLevel) {
            serverLevel.getServer().execute(() -> {
                for (var c : getControllers()) {
                    if (c.isFormed()) {
                        c.getPatternLock().lock();
                        try {
                            c.onStructureInvalid();
                            var data = MultiblockWorldSavedData.getOrCreate(serverLevel);
                            data.removeMapping(c.getMultiblockState());
                            data.addAsyncLogic(c);
                        } finally {
                            c.getPatternLock().unlock();
                        }
                    }
                }
            });
        }
    }

    @Override
    public boolean canShared() {
        return false;
    }

    @Override
    public Widget createUIWidget() {
        WidgetGroup group = new WidgetGroup(0, 0, 176, 69);

        group.addWidget(new LabelWidget(7, 8, () -> Component.translatable("univom.multiblock.wireless_optical_hatch.channel").getString())
                .setHoverTooltips(Component.translatable("univom.multiblock.wireless_optical_hatch.channel_hover")));

        group.addWidget(new TextFieldWidget(9, 22, 152, 16, () -> channel, this::setChannel));

        group.addWidget(new LabelWidget(7, 46, () -> isTransmitter() ?
                Component.translatable("univom.multiblock.wireless_optical_hatch.mode.source").getString() :
                Component.translatable("univom.multiblock.wireless_optical_hatch.mode.target").getString()));

        group.addWidget(new LabelWidget(7, 62, () ->
                Component.translatable("univom.multiblock.wireless_optical_hatch.status_prefix")
                        .append(getLinkStatusComponent()).getString()));

        return group;
    }

    @Override
    public boolean isRecipeAvailable(@NotNull GTRecipe recipe, @NotNull Collection<IDataAccessHatch> seen) {
        seen.add(this);

        if (isTransmitter()) {
            if (!isFormed()) return false;

            IMultiController controller = getControllers().first();
            if (!(controller instanceof IWorkableMultiController workable) || !workable.getRecipeLogic().isWorking())
                return false;

            List<IDataAccessHatch> dataAccesses = new ArrayList<>();
            List<IDataAccessHatch> transmitters = new ArrayList<>();
            for (var part : controller.getParts()) {
                Block block = part.self().getBlockState().getBlock();
                if (part instanceof IDataAccessHatch hatch && PartAbility.DATA_ACCESS.isApplicable(block)) {
                    dataAccesses.add(hatch);
                }
                if (part instanceof IDataAccessHatch hatch &&
                        PartAbility.OPTICAL_DATA_RECEPTION.isApplicable(block)) {
                    transmitters.add(hatch);
                }
            }

            return checkRecipeAvailable(dataAccesses, seen, recipe) ||
                    checkRecipeAvailable(transmitters, seen, recipe);
        } else {
            ChannelKey key = currentChannelKey();
            if (key == null) return false;

            if (getActiveMachine(RECEIVERS, key) != this) {
                return false;
            }

            WirelessOpticalDataHatchMachine source = getActiveMachine(TRANSMITTERS, key);
            if (source != null && source.isFormed()) {
                return source.isRecipeAvailable(recipe, seen);
            }
        }
        return false;
    }

    private static boolean checkRecipeAvailable(@NotNull Iterable<? extends IDataAccessHatch> hatches,
                                                @NotNull Collection<IDataAccessHatch> seen,
                                                @NotNull GTRecipe recipe) {
        for (IDataAccessHatch hatch : hatches) {
            if (seen.contains(hatch)) continue;
            if (hatch.isRecipeAvailable(recipe, seen)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public GTRecipe modifyRecipe(GTRecipe recipe) {
        return IOpticalDataAccessHatch.super.modifyRecipe(recipe);
    }

    @Override
    public boolean isCreative() {
        return false;
    }
}
