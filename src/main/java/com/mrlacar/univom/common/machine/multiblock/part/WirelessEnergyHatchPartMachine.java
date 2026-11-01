package com.mrlacar.univom.common.machine.multiblock.part;

import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.TickableSubscription;
import com.gregtechceu.gtceu.api.machine.feature.IInteractedMachine;
import com.gregtechceu.gtceu.api.machine.feature.IMachineLife;
import com.gregtechceu.gtceu.api.machine.multiblock.part.TieredIOPartMachine;
import com.gregtechceu.gtceu.api.machine.trait.NotifiableEnergyContainer;
import com.gregtechceu.gtceu.common.data.GTItems;
import com.lowdragmc.lowdraglib.syncdata.annotation.Persisted;
import com.lowdragmc.lowdraglib.syncdata.field.ManagedFieldHolder;
import com.mrlacar.univom.api.machine.feature.IWirelessNode;
import com.mrlacar.univom.network.wireless.WirelessEnergyNetwork;
import com.mrlacar.univom.utils.WirelessEnergyNetworkUtils;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.UUID;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class WirelessEnergyHatchPartMachine extends TieredIOPartMachine implements IInteractedMachine, IMachineLife, IWirelessNode {
    protected static final ManagedFieldHolder MANAGED_FIELD_HOLDER = new ManagedFieldHolder(
            WirelessEnergyHatchPartMachine.class, TieredIOPartMachine.MANAGED_FIELD_HOLDER);

    @Nullable
    @Getter
    @Setter
    private WirelessEnergyNetwork wirelessNetworkCache;

    @Persisted
    @Getter
    private UUID boundPlayerUUID;

    @Persisted
    public final NotifiableEnergyContainer energyContainer;

    protected TickableSubscription updEnergySubs;

    @Getter
    protected int amperage;

    public WirelessEnergyHatchPartMachine(IMachineBlockEntity holder, int tier, IO io, int amperage, Object... args) {
        super(holder, tier, io);
        this.amperage = amperage;
        this.energyContainer = createEnergyContainer();
    }

    @Override
    public ManagedFieldHolder getFieldHolder() {
        return MANAGED_FIELD_HOLDER;
    }

    protected NotifiableEnergyContainer createEnergyContainer() {
        if (io == IO.OUT) {
            return NotifiableEnergyContainer.emitterContainer(this, GTValues.V[tier] * 64L * amperage,
                    GTValues.VEX[tier], amperage);
        } else {
            return NotifiableEnergyContainer.receiverContainer(this, GTValues.V[tier] * 16L * amperage,
                    GTValues.VEX[tier], amperage);
        }
    }

    @Override
    public void onLoad() {
        super.onLoad();
        updateEnergySubscription();
    }

    @Override
    public void onUnload() {
        super.onUnload();
        if (updEnergySubs != null) {
            updEnergySubs.unsubscribe();
            updEnergySubs = null;
        }
    }

    @Override
    public void onMachinePlaced(@Nullable LivingEntity player, ItemStack stack) {
        IMachineLife.super.onMachinePlaced(player, stack);
        if (player != null) {
            this.boundPlayerUUID = player.getUUID();
            updateEnergySubscription();
        }
    }

    private void updateEnergySubscription() {
        if (this.boundPlayerUUID != null) {
            updEnergySubs = subscribeServerTick(updEnergySubs, this::updateEnergy);
        } else if (updEnergySubs != null) {
            updEnergySubs.unsubscribe();
            updEnergySubs = null;
        }
    }

    private void updateEnergy() {
        if (this.boundPlayerUUID == null) return;
        if (io == IO.IN) {
            useEnergy();
        } else {
            addEnergy();
        }
    }

    private void useEnergy() {
        var currentStored = energyContainer.getEnergyStored();
        var maxCapacity = energyContainer.getEnergyCapacity();
        long maxExtract = Math.min(maxCapacity - currentStored, energyContainer.getInputVoltage() * energyContainer.getInputAmperage());
        if (maxExtract <= 0) return;

        WirelessEnergyNetwork network = getWirelessNetwork();
        if (network == null) return;

        long extracted = network.removeEnergy(maxExtract);
        if (extracted > 0) energyContainer.setEnergyStored(currentStored + extracted);
    }

    private void addEnergy() {
        var currentStored = energyContainer.getEnergyStored();
        if (currentStored <= 0) return;

        long maxInsert = Math.min(energyContainer.getOutputVoltage() * energyContainer.getOutputAmperage(), currentStored);
        WirelessEnergyNetwork network = getWirelessNetwork();
        if (network == null) return;

        long inserted = network.addEnergy(maxInsert);
        if (inserted > 0) energyContainer.setEnergyStored(currentStored - inserted);
    }

    @Override
    public boolean shouldOpenUI(Player player, InteractionHand hand, BlockHitResult hit) {
        return false;
    }

    @Override
    public InteractionResult onUse(BlockState state, Level world, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (isRemote()) return InteractionResult.PASS;
        ItemStack is = player.getItemInHand(hand);

        if (is.is(GTItems.TOOL_DATA_STICK.asItem())) {
            if (!player.getUUID().equals(this.boundPlayerUUID)) {
                this.boundPlayerUUID = player.getUUID();
                setWirelessNetworkCache(null);
                player.displayClientMessage(Component.translatable("univom.message.bind.wireless_energy_hatch.bind", WirelessEnergyNetworkUtils.getPlayer(player)), true);
                updateEnergySubscription();
            }
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }

    @Override
    public boolean onLeftClick(Player player, Level world, InteractionHand hand, BlockPos pos, Direction direction) {
        if (isRemote()) return false;
        ItemStack is = player.getItemInHand(hand);

        if (is.is(GTItems.TOOL_DATA_STICK.asItem())) {
            if (this.boundPlayerUUID != null) {
                this.boundPlayerUUID = null;
                setWirelessNetworkCache(null);
                player.displayClientMessage(Component.translatable("univom.message.bind.wireless_energy_hatch.unbind"), true);
                updateEnergySubscription();
            }
            return true;
        }
        return false;
    }

    @Override
    public int tintColor(int index) {
        if (index == 2) {
            return GTValues.VC[getTier()];
        }
        return super.tintColor(index);
    }
}
