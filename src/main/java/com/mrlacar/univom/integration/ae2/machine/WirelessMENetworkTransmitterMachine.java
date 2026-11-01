package com.mrlacar.univom.integration.ae2.machine;

import appeng.api.networking.IGridNodeListener;
import appeng.api.networking.IManagedGridNode;
import appeng.api.networking.security.IActionSource;
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.MetaMachine;
import com.gregtechceu.gtceu.api.machine.TickableSubscription;
import com.gregtechceu.gtceu.api.machine.feature.IFancyUIMachine;
import com.gregtechceu.gtceu.api.machine.feature.IMachineLife;
import com.gregtechceu.gtceu.integration.ae2.machine.feature.IGridConnectedMachine;
import com.gregtechceu.gtceu.integration.ae2.machine.trait.GridNodeHolder;
import com.lowdragmc.lowdraglib.gui.widget.LabelWidget;
import com.lowdragmc.lowdraglib.gui.widget.TextFieldWidget;
import com.lowdragmc.lowdraglib.gui.widget.Widget;
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;
import com.lowdragmc.lowdraglib.syncdata.annotation.DescSynced;
import com.lowdragmc.lowdraglib.syncdata.annotation.Persisted;
import com.lowdragmc.lowdraglib.syncdata.field.ManagedFieldHolder;
import com.mrlacar.univom.api.data.ChannelKey;
import com.mrlacar.univom.data.player.AEWirelessChannelSavedData;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.EnumSet;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class WirelessMENetworkTransmitterMachine extends MetaMachine implements IFancyUIMachine, IMachineLife, IGridConnectedMachine {
    protected static final ManagedFieldHolder MANAGED_FIELD_HOLDER = new ManagedFieldHolder(
            WirelessMENetworkTransmitterMachine.class, MetaMachine.MANAGED_FIELD_HOLDER);

    @Persisted
    protected final GridNodeHolder nodeHolder;

    @DescSynced
    @Getter
    @Setter
    protected boolean isOnline;

    protected final IActionSource actionSource;

    @Persisted
    @DescSynced
    @Getter
    private String channel = "";

    @Persisted
    @DescSynced
    private int connectedReceivers = 0;

    @DescSynced
    private boolean channelConflict = false;

    private TickableSubscription tickSubscription;

    private transient GlobalPos cachedPos;
    private transient ChannelKey registeredKey;

    public WirelessMENetworkTransmitterMachine(IMachineBlockEntity holder) {
        super(holder);
        this.nodeHolder = new GridNodeHolder(this);
        this.actionSource = IActionSource.ofMachine(nodeHolder.getMainNode()::getNode);
    }

    @Override
    public ManagedFieldHolder getFieldHolder() {
        return MANAGED_FIELD_HOLDER;
    }

    @Override
    public IManagedGridNode getMainNode() {
        return nodeHolder.getMainNode();
    }

    @Nullable
    private AEWirelessChannelSavedData getChannelData() {
        if (!(getLevel() instanceof ServerLevel serverLevel)) return null;
        return AEWirelessChannelSavedData.get(serverLevel.getServer());
    }

    @Nullable
    private ChannelKey currentChannelKey() {
        if (this.channel.isEmpty()) return null;
        return ChannelKey.of(getOwnerUUID(), this.channel);
    }

    private void releaseRegistration() {
        if (this.registeredKey == null) return;
        AEWirelessChannelSavedData manager = getChannelData();
        if (manager != null) {
            manager.unregisterChannel(this.registeredKey, this.cachedPos);
        }
        this.registeredKey = null;
    }

    private void syncRegistration() {
        if (isRemote() || this.cachedPos == null) return;

        AEWirelessChannelSavedData manager = getChannelData();
        if (manager == null) return;

        ChannelKey key = currentChannelKey();

        if (key == null || !isOnline()) {
            if (this.registeredKey != null) {
                manager.unregisterChannel(this.registeredKey, this.cachedPos);
                this.registeredKey = null;
            }
            setChannelConflict(false);
            return;
        }

        if (this.registeredKey != null && !this.registeredKey.equals(key)) {
            manager.unregisterChannel(this.registeredKey, this.cachedPos);
            this.registeredKey = null;
        }

        boolean claimed = manager.registerChannel(key, this.cachedPos);
        this.registeredKey = claimed ? key : null;
        setChannelConflict(!claimed);
    }

    private void setChannelConflict(boolean conflict) {
        if (this.channelConflict != conflict) {
            this.channelConflict = conflict;
            onChanged();
        }
    }

    public void setChannel(String newChannel) {
        String trimmed = newChannel.trim();
        if (this.channel.equals(trimmed)) return;

        releaseRegistration();

        this.channel = trimmed;
        onChanged();

        syncRegistration();
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (!isRemote()) {
            this.cachedPos = GlobalPos.of(getLevel().dimension(), getPos());
            syncRegistration();
            tickSubscription = subscribeServerTick(this::serverUpdate);
        }
    }

    private void serverUpdate() {
        if (getOffsetTimer() % 20 != 0) return;

        syncRegistration();

        int count = 0;
        if (this.registeredKey != null) {
            AEWirelessChannelSavedData manager = getChannelData();
            if (manager != null) {
                count = manager.getReceiverCount(this.registeredKey);
            }
        }

        if (this.connectedReceivers != count) {
            this.connectedReceivers = count;
            onChanged();
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

    @Override
    public void onMachineRemoved() {
        IMachineLife.super.onMachineRemoved();
        if (!isRemote()) {
            releaseRegistration();
        }
    }

    @Override
    public void onMainNodeStateChanged(IGridNodeListener.State reason) {
        IGridConnectedMachine.super.onMainNodeStateChanged(reason);
        syncRegistration();
    }

    @Override
    public void onRotated(Direction oldFacing, Direction newFacing) {
        super.onRotated(oldFacing, newFacing);
        getMainNode().setExposedOnSides(EnumSet.of(newFacing));
    }

    @Override
    public Widget createUIWidget() {
        WidgetGroup group = new WidgetGroup(0, 0, 176, 69);

        group.addWidget(new LabelWidget(7, 8, () ->
                Component.translatable("univom.multiblock.wireless_optical_hatch.channel").getString() +
                        " - " + (channel.isEmpty() ? "..." : channel)
        ));

        group.addWidget(new TextFieldWidget(9, 22, 152, 16, () -> channel, this::setChannel));

        group.addWidget(new LabelWidget(7, 44, () -> this.isOnline ?
                "gtceu.gui.me_network.online" :
                "gtceu.gui.me_network.offline"));

        group.addWidget(new LabelWidget(7, 56, () -> {
            String prefix = Component.translatable("univom.multiblock.wireless_optical_hatch.status_prefix").getString();
            if (channel.isEmpty()) {
                return prefix + Component.translatable("univom.multiblock.wireless_optical_hatch.unconfigured").getString();
            } else if (channelConflict) {
                return prefix + Component.translatable("univom.multiblock.wireless_optical_hatch.conflict_source").getString();
            } else if (connectedReceivers > 0) {
                return prefix + Component.translatable("univom.multiblock.wireless_optical_hatch.linked_target").getString() +
                        " : " + connectedReceivers;
            } else {
                return prefix + Component.translatable("univom.multiblock.wireless_optical_hatch.waiting_target").getString();
            }
        }));

        return group;
    }
}
