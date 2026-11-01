package com.mrlacar.univom.common.machine.multiblock.part;

import com.gregtechceu.gtceu.api.capability.IOpticalComputationProvider;
import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.MetaMachine;
import com.gregtechceu.gtceu.api.machine.TickableSubscription;
import com.gregtechceu.gtceu.api.machine.feature.IRecipeLogicMachine;
import com.gregtechceu.gtceu.api.machine.feature.multiblock.IMultiController;
import com.gregtechceu.gtceu.api.machine.feature.multiblock.IMultiPart;
import com.gregtechceu.gtceu.api.machine.multiblock.part.MultiblockPartMachine;
import com.gregtechceu.gtceu.api.machine.trait.NotifiableComputationContainer;
import com.gregtechceu.gtceu.api.machine.trait.RecipeLogic;
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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class WirelessOpticalComputationHatchMachine extends MultiblockPartMachine {
    protected static final ManagedFieldHolder MANAGED_FIELD_HOLDER = new ManagedFieldHolder(
            WirelessOpticalComputationHatchMachine.class, MultiblockPartMachine.MANAGED_FIELD_HOLDER);

    public static final ConcurrentHashMap<ChannelKey, CopyOnWriteArrayList<WeakReference<WirelessOpticalComputationHatchMachine>>> TRANSMITTERS = new ConcurrentHashMap<>();
    public static final ConcurrentHashMap<ChannelKey, CopyOnWriteArrayList<WeakReference<WirelessOpticalComputationHatchMachine>>> RECEIVERS = new ConcurrentHashMap<>();

    @Getter
    private final boolean transmitter;
    @Getter
    @Persisted
    private String channel = "";
    @Getter
    protected NotifiableComputationContainer computationContainer;

    @Getter
    @Persisted
    @DescSynced
    private int connectionStatus = 0;

    private TickableSubscription tickSubscription;
    private int ticksSinceLastInput = -1;

    private transient ChannelKey registeredKey;

    public WirelessOpticalComputationHatchMachine(IMachineBlockEntity holder, boolean transmitter) {
        super(holder);
        this.transmitter = transmitter;
        this.computationContainer = createComputationContainer(transmitter);
    }

    protected NotifiableComputationContainer createComputationContainer(Object... args) {
        IO io = IO.IN;
        if (args.length > 1 && args[args.length - 2] instanceof IO newIo) {
            io = newIo;
        }
        if (args.length > 0 && args[args.length - 1] instanceof Boolean isTransmitter) {
            return new UnivomWirelessComputationTrait(this, io, isTransmitter);
        }
        throw new IllegalArgumentException();
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

    private ConcurrentHashMap<ChannelKey, CopyOnWriteArrayList<WeakReference<WirelessOpticalComputationHatchMachine>>> ownMap() {
        return isTransmitter() ? TRANSMITTERS : RECEIVERS;
    }

    @Nullable
    private static WirelessOpticalComputationHatchMachine getActiveMachine(
            ConcurrentHashMap<ChannelKey, CopyOnWriteArrayList<WeakReference<WirelessOpticalComputationHatchMachine>>> map,
            @Nullable ChannelKey key) {
        if (key == null) return null;
        CopyOnWriteArrayList<WeakReference<WirelessOpticalComputationHatchMachine>> list = map.get(key);
        if (list == null || list.isEmpty()) return null;

        WirelessOpticalComputationHatchMachine found = null;
        boolean needsCleanup = false;

        for (WeakReference<WirelessOpticalComputationHatchMachine> ref : list) {
            WirelessOpticalComputationHatchMachine m = ref.get();
            if (m == null || m.isInValid()) {
                needsCleanup = true;
                continue;
            }
            if (found == null) found = m;
        }

        if (needsCleanup) {
            list.removeIf(r -> {
                WirelessOpticalComputationHatchMachine m = r.get();
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
            WirelessOpticalComputationHatchMachine activeTx = getActiveMachine(TRANSMITTERS, key);
            if (activeTx != this) {
                this.connectionStatus = 6;
            } else {
                WirelessOpticalComputationHatchMachine activeRx = getActiveMachine(RECEIVERS, key);
                this.connectionStatus = (activeRx != null) ? 5 : 4;
            }
        } else {
            WirelessOpticalComputationHatchMachine activeRx = getActiveMachine(RECEIVERS, key);
            if (activeRx != this) {
                this.connectionStatus = 3;
            } else {
                WirelessOpticalComputationHatchMachine activeTx = getActiveMachine(TRANSMITTERS, key);
                this.connectionStatus = (activeTx != null) ? 2 : 1;
            }
        }
    }

    private void syncRegistration() {
        ChannelKey key = currentChannelKey();
        if (java.util.Objects.equals(this.registeredKey, key)) return;

        releaseRegistration();
        if (key == null) return;

        CopyOnWriteArrayList<WeakReference<WirelessOpticalComputationHatchMachine>> list =
                ownMap().computeIfAbsent(key, k -> new CopyOnWriteArrayList<>());

        for (WeakReference<WirelessOpticalComputationHatchMachine> ref : list) {
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

        ConcurrentHashMap<ChannelKey, CopyOnWriteArrayList<WeakReference<WirelessOpticalComputationHatchMachine>>> map = ownMap();
        CopyOnWriteArrayList<WeakReference<WirelessOpticalComputationHatchMachine>> list = map.get(this.registeredKey);
        if (list != null) {
            list.removeIf(ref -> {
                WirelessOpticalComputationHatchMachine m = ref.get();
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

    private class UnivomWirelessComputationTrait extends NotifiableComputationContainer {

        public UnivomWirelessComputationTrait(MetaMachine machine, IO io, boolean transmitter) {
            super(machine, io, transmitter);
        }

        @Nullable
        private NotifiableComputationContainer getLinkedContainer() {
            ChannelKey key = currentChannelKey();
            if (key == null) return null;
            if (isTransmitter()) return this;

            if (getActiveMachine(RECEIVERS, key) != WirelessOpticalComputationHatchMachine.this) {
                return null;
            }

            WirelessOpticalComputationHatchMachine source = getActiveMachine(TRANSMITTERS, key);
            if (source != null) {
                return source.getComputationContainer();
            }
            return null;
        }

        @Override
        public int getMaxCWUt(@NotNull Collection<IOpticalComputationProvider> seen) {
            if (isTransmitter()) return super.getMaxCWUt(seen);
            var linked = getLinkedContainer();
            return linked != null ? linked.getMaxCWUt(seen) : 0;
        }

        @Override
        public int requestCWUt(int cwut, boolean simulate, @NotNull Collection<IOpticalComputationProvider> seen) {
            if (isTransmitter()) return super.requestCWUt(cwut, simulate, seen);
            var linked = getLinkedContainer();
            return linked != null ? linked.requestCWUt(cwut, simulate, seen) : 0;
        }

        @Override
        public boolean canBridge(@NotNull Collection<IOpticalComputationProvider> seen) {
            if (isTransmitter()) return super.canBridge(seen);
            var linked = getLinkedContainer();
            return linked != null && linked.canBridge(seen);
        }

        private void modifyProgress(IRecipeLogicMachine rlm, int amountToAdd) {
            RecipeLogic logic = rlm.getRecipeLogic();
            if (logic != null) {
                logic.setProgress(logic.getProgress() + amountToAdd);
            }
        }

        @Nullable
        @Override
        public List<Integer> handleRecipeInner(IO io, GTRecipe recipe, List<Integer> left, boolean simulate) {
            if (isTransmitter()) {
                return super.handleRecipeInner(io, recipe, left, simulate);
            }

            NotifiableComputationContainer provider = getLinkedContainer();
            if (provider == null) return left;

            int sum = left.stream().mapToInt(Integer::intValue).sum();
            if (io == IO.IN) {
                int availableCWUt = provider.requestCWUt(Integer.MAX_VALUE, true, new ArrayList<>());

                if (availableCWUt >= sum) {
                    if (recipe.data.getBoolean("duration_is_total_cwu")) {
                        int drawn = provider.requestCWUt(availableCWUt, simulate, new ArrayList<>());
                        if (!simulate) {
                            if (getMachine() instanceof IRecipeLogicMachine rlm) {
                                modifyProgress(rlm, drawn - 1);
                            } else if (getMachine() instanceof IMultiPart multiPart) {
                                for (IMultiController controller : multiPart.getControllers()) {
                                    if (controller instanceof IRecipeLogicMachine rlm) {
                                        modifyProgress(rlm, drawn - 1);
                                    }
                                }
                            }
                        }
                        sum -= drawn;
                    } else {
                        sum -= provider.requestCWUt(sum, simulate, new ArrayList<>());
                    }
                }
            }
            return sum <= 0 ? null : Collections.singletonList(sum);
        }

        @Override
        public @NotNull List<Object> getContents() {
            if (isTransmitter()) return super.getContents();
            var linked = getLinkedContainer();
            return linked != null ? linked.getContents() : List.of(0);
        }

        @Override
        public double getTotalContentAmount() {
            if (isTransmitter()) return super.getTotalContentAmount();
            var linked = getLinkedContainer();
            return linked != null ? linked.getTotalContentAmount() : 0;
        }
    }
}
