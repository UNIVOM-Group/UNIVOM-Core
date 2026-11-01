package com.mrlacar.univom.integration.ae2.machine;

import appeng.api.networking.GridHelper;
import appeng.api.networking.IGridConnection;
import appeng.api.networking.IGridNode;
import com.gregtechceu.gtceu.GTCEu;
import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.gui.fancy.TabsWidget;
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.TickableSubscription;
import com.gregtechceu.gtceu.integration.ae2.machine.MEHatchPartMachine;
import com.lowdragmc.lowdraglib.syncdata.annotation.DescSynced;
import com.lowdragmc.lowdraglib.syncdata.annotation.Persisted;
import com.lowdragmc.lowdraglib.syncdata.field.ManagedFieldHolder;
import com.mrlacar.univom.api.data.ChannelKey;
import com.mrlacar.univom.api.machine.fancyconfigurator.WirelessNetworkSelectionConfigurator;
import com.mrlacar.univom.api.machine.feature.IAEWirelessMachine;
import com.mrlacar.univom.data.player.AEWirelessChannelSavedData;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public abstract class WirelessMEHatchPartMachine extends MEHatchPartMachine implements IAEWirelessMachine {
    protected static final ManagedFieldHolder MANAGED_FIELD_HOLDER = new ManagedFieldHolder(WirelessMEHatchPartMachine.class,
            MEHatchPartMachine.MANAGED_FIELD_HOLDER);

    private static final int CONNECTION_RETRY_COOLDOWN = 100;

    @Persisted
    @DescSynced
    protected String targetChannel = "";

    protected IGridConnection virtualConnection;
    protected TickableSubscription connectionSubs;

    private transient GlobalPos cachedPos;
    private transient GlobalPos connectedTransmitterPos;
    private transient ChannelKey registeredKey;
    private transient int connectionCooldown = 0;

    public WirelessMEHatchPartMachine(IMachineBlockEntity holder, IO io, Object... args) {
        super(holder, io, args);
    }

    @Override
    public ManagedFieldHolder getFieldHolder() {
        return MANAGED_FIELD_HOLDER;
    }

    @Override
    public String getTargetChannel() {
        return targetChannel;
    }

    @Override
    public void setTargetChannel(String channel) {
        String trimmed = channel.trim();
        if (this.targetChannel.equals(trimmed)) return;

        this.targetChannel = trimmed;
        onChanged();
        destroyConnection();
        this.connectionCooldown = 0;
    }

    @Nullable
    protected ChannelKey currentChannelKey() {
        if (this.targetChannel.isEmpty()) return null;
        return ChannelKey.of(getOwnerUUID(), this.targetChannel);
    }

    @Nullable
    private AEWirelessChannelSavedData getChannelData() {
        if (!(getLevel() instanceof ServerLevel serverLevel)) return null;
        return AEWirelessChannelSavedData.get(serverLevel.getServer());
    }

    protected void destroyConnection() {
        if (virtualConnection != null) {
            virtualConnection.destroy();
            virtualConnection = null;
        }
        this.connectedTransmitterPos = null;

        if (this.registeredKey != null && this.cachedPos != null) {
            AEWirelessChannelSavedData manager = getChannelData();
            if (manager != null) {
                manager.unregisterReceiver(this.registeredKey, this.cachedPos);
            }
        }
        this.registeredKey = null;
    }

    private void checkAndConnect() {
        if (getOffsetTimer() % 20 != 0 || !(getLevel() instanceof ServerLevel serverLevel)) return;

        ChannelKey key = currentChannelKey();
        if (key == null || getMainNode().getNode() == null) {
            destroyConnection();
            return;
        }

        AEWirelessChannelSavedData manager = AEWirelessChannelSavedData.get(serverLevel.getServer());
        if (manager == null) return;

        if (this.registeredKey != null && !this.registeredKey.equals(key)) {
            destroyConnection();
        }

        GlobalPos targetPos = manager.getChannelPos(key);

        if (virtualConnection != null) {
            if (virtualConnection.a() == null || virtualConnection.b() == null ||
                    !virtualConnection.a().isActive() || !virtualConnection.b().isActive() ||
                    targetPos == null || !targetPos.equals(connectedTransmitterPos)) {
                destroyConnection();
            } else {
                return;
            }
        }

        if (connectionCooldown > 0) {
            connectionCooldown -= 20;
            return;
        }

        if (targetPos == null) return;

        ServerLevel targetLevel = serverLevel.getServer().getLevel(targetPos.dimension());
        if (targetLevel != null && targetLevel.isLoaded(targetPos.pos())) {
            BlockEntity be = targetLevel.getBlockEntity(targetPos.pos());
            if (be instanceof IMachineBlockEntity mbe &&
                    mbe.getMetaMachine() instanceof WirelessMENetworkTransmitterMachine transmitter) {
                IGridNode transmitterNode = transmitter.getMainNode().getNode();
                IGridNode myNode = this.getMainNode().getNode();

                if (transmitterNode != null && myNode != null && this.cachedPos != null) {
                    try {
                        this.virtualConnection = GridHelper.createConnection(myNode, transmitterNode);
                        this.connectedTransmitterPos = targetPos;
                        this.registeredKey = key;
                        manager.registerReceiver(key, this.cachedPos);
                        this.connectionCooldown = 0;
                    } catch (Exception e) {
                        GTCEu.LOGGER.debug("Failed to open wireless ME connection at {}", getPos(), e);
                        this.connectionCooldown = CONNECTION_RETRY_COOLDOWN;
                    }
                }
            }
        }
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (!isRemote()) {
            this.cachedPos = GlobalPos.of(getLevel().dimension(), getPos());
            connectionSubs = subscribeServerTick(connectionSubs, this::checkAndConnect);
        }
    }

    @Override
    public void onUnload() {
        super.onUnload();
        destroyConnection();
        if (connectionSubs != null) {
            connectionSubs.unsubscribe();
            connectionSubs = null;
        }
    }

    @Override
    public void onMachineRemoved() {
        super.onMachineRemoved();
        destroyConnection();
        if (connectionSubs != null) {
            connectionSubs.unsubscribe();
            connectionSubs = null;
        }
    }

    @Override
    public void attachSideTabs(TabsWidget sideTabs) {
        super.attachSideTabs(sideTabs);
        sideTabs.attachSubTab(new WirelessNetworkSelectionConfigurator(this));
    }
}
