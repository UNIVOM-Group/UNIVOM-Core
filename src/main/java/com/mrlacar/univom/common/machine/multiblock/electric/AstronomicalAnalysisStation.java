package com.mrlacar.univom.common.machine.multiblock.electric;

import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.gui.fancy.ConfiguratorPanel;
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.MetaMachine;
import com.gregtechceu.gtceu.api.machine.feature.IMachineLife;
import com.gregtechceu.gtceu.api.machine.feature.IRecipeLogicMachine;
import com.gregtechceu.gtceu.api.machine.multiblock.WorkableElectricMultiblockMachine;
import com.gregtechceu.gtceu.api.machine.trait.NotifiableItemStackHandler;
import com.gregtechceu.gtceu.api.machine.trait.RecipeLogic;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.gregtechceu.gtceu.api.recipe.RecipeHelper;
import com.gregtechceu.gtceu.data.recipe.builder.GTRecipeBuilder;
import com.lowdragmc.lowdraglib.syncdata.annotation.DescSynced;
import com.lowdragmc.lowdraglib.syncdata.annotation.Persisted;
import com.lowdragmc.lowdraglib.syncdata.field.ManagedFieldHolder;
import com.mojang.blaze3d.MethodsReturnNonnullByDefault;
import com.mrlacar.univom.api.machine.fancyconfigurator.AstronomicalAnalysisStationInvConfigurator;
import com.mrlacar.univom.api.machine.feature.IMachineConnectorInteractable;
import com.mrlacar.univom.common.data.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class AstronomicalAnalysisStation extends WorkableElectricMultiblockMachine implements IMachineConnectorInteractable, IMachineLife {
    protected static final ManagedFieldHolder MANAGED_FIELD_HOLDER = new ManagedFieldHolder(
            AstronomicalAnalysisStation.class, WorkableElectricMultiblockMachine.MANAGED_FIELD_HOLDER);

    @Persisted
    @DescSynced
    private @Nullable BlockPos boundRailgunPos = null;

    @DescSynced
    @Persisted
    public final NotifiableItemStackHandler infoCardInventory;

    public AstronomicalAnalysisStation(IMachineBlockEntity holder, Object... args) {
        super(holder, args);

        this.infoCardInventory = new NotifiableItemStackHandler(this, 1, IO.NONE, IO.BOTH) {
            @NotNull
            @Override
            public ItemStack extractItem(int slot, int amount, boolean simulate) {
                if (!isLocked()) return super.extractItem(slot, amount, simulate);
                return ItemStack.EMPTY;
            }

            @NotNull
            @Override
            public ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
                if (!isLocked()) return super.insertItem(slot, stack, simulate);
                return stack;
            }
        };

        this.infoCardInventory.setFilter(stack -> {
            Item item = stack.getItem();
            return item == ModItems.PLANET_INFO_CARD.get() || item == ModItems.SYSTEM_INFO_CARD.get();
        });
    }

    @Override
    public ManagedFieldHolder getFieldHolder() {
        return MANAGED_FIELD_HOLDER;
    }

    @Override
    public void onMachineRemoved() {
        if (this.boundRailgunPos != null && getLevel() != null && !getLevel().isClientSide) {
            var railgunMachine = MetaMachine.getMachine(getLevel(), this.boundRailgunPos);
            if (railgunMachine instanceof ProbeRailgun railgun) railgun.unbindStation();
        }
        clearInventory(infoCardInventory.storage);
    }

    public boolean isLocked() {
        ProbeRailgun railgun = getBoundRailgun();
        return railgun != null && railgun.getRecipeLogic().isWorking();
    }

    @Override
    public InteractionResult onMachineConnectorShiftUse(Player player, ItemStack machineConnector) {
        if (this.isFormed()) {
            machineConnector.getOrCreateTag().putLong("astronomical_analysis_station_pos", getPos().asLong());
            machineConnector.getOrCreateTag().putString("astronomical_analysis_station_dim", player.level().dimension().location().toString());

            if (!player.level().isClientSide) player.displayClientMessage(Component.translatable("univom.message.bind.astronomical_analysis_station_saved"), true);
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }

    @Nullable
    public BlockPos getBoundRailgunPos() {
        return this.boundRailgunPos;
    }

    public void bindRailgun(BlockPos railgunPos) {
        if (getLevel() == null || getLevel().isClientSide) return;
        this.boundRailgunPos = railgunPos;
    }

    public void unbindRailgun() {
        this.boundRailgunPos = null;
    }

    @Nullable
    public ProbeRailgun getBoundRailgun() {
        if (this.boundRailgunPos == null || getLevel() == null) return null;

        if (getLevel().isLoaded(this.boundRailgunPos)) {
            var machine = MetaMachine.getMachine(getLevel(), this.boundRailgunPos);
            if (machine instanceof ProbeRailgun railgun) {
                return railgun;
            } else {
                if (!getLevel().isClientSide) this.boundRailgunPos = null;
                return null;
            }
        }
        return null;
    }

    @Override
    public boolean beforeWorking(@Nullable GTRecipe recipe) {
        ProbeRailgun boundRailgun = this.getBoundRailgun();

        if (boundRailgun == null || !boundRailgun.isFormed()) {
            this.getRecipeLogic().interruptRecipe();
            return false;
        }
        return super.beforeWorking(recipe);
    }

    @Override
    protected RecipeLogic createRecipeLogic(Object... args) {
        return new runningLogic(this);
    }

    public static class runningLogic extends RecipeLogic {

        private GTRecipe recipe = null;

        public runningLogic(IRecipeLogicMachine machine) {
            super(machine);
        }

        @Override
        public AstronomicalAnalysisStation getMachine() {
            return (AstronomicalAnalysisStation) super.getMachine();
        }

        @Override
        public void findAndHandleRecipe() {
            this.lastFailedMatches = null;
            this.recipeDirty = false;
            this.lastRecipe = null;
            this.lastOriginRecipe = null;

            if (getMachine().getLevel() == null || getMachine().getLevel().isClientSide) return;
            if (!getMachine().isFormed()) return;

            if (recipe == null) {
                recipe = GTRecipeBuilder.ofRaw()
                        .duration(1200)
                        .EUt(GTValues.VA[GTValues.MV])
                        .buildRawRecipe();
            }

            if (RecipeHelper.matchContents(getMachine(), recipe).isSuccess()) {
                setupRecipe(recipe);
            }
        }
    }

    @Override
    public void attachConfigurators(ConfiguratorPanel configuratorPanel) {
        configuratorPanel.attachConfigurators(new AstronomicalAnalysisStationInvConfigurator(this.infoCardInventory.storage, Component.translatable("univom.gui.astronomical_analysis_station_inventory.title"), this));
        super.attachConfigurators(configuratorPanel);
    }

    @Override
    public void addDisplayText(List<Component> textList) {
        if (!isFormed()) {
            textList.add(Component.translatable("gtceu.multiblock.invalid_structure").withStyle(ChatFormatting.RED));
            return;
        }

        if (this.boundRailgunPos == null) {
            textList.add(Component.translatable("univom.multiblock.astronomical_analysis_station_not_bound"));
            return;
        }

        ProbeRailgun boundRailgun = this.getBoundRailgun();

        if (boundRailgun == null) {
            textList.add(Component.translatable("univom.multiblock.astronomical_analysis_station_bound_out_of_range"));
            return;
        }

        if (!boundRailgun.isFormed()) {
            textList.add(Component.translatable("univom.multiblock.astronomical_analysis_station_not_bound"));
            return;
        }

        textList.add(Component.translatable("univom.multiblock.astronomical_analysis_station_bound_success"));
        textList.add(Component.empty());

        if (boundRailgun.isDeepSpaceMode()) {
            textList.add(Component.translatable("univom.multiblock.probe_railgun.deep_space_mode"));
            String sys = boundRailgun.getCurrentSystemSetting();
            if (sys != null && !sys.isEmpty()) {
                textList.add(Component.translatable("univom.multiblock.probe_railgun.current_system", Component.translatable("univom.gui.system_selection." + sys)));
            } else {
                textList.add(Component.translatable("univom.multiblock.probe_railgun.no_system"));
            }
        } else {
            textList.add(Component.translatable("univom.multiblock.probe_railgun.targeted_mode"));
            String planet = boundRailgun.getCurrentPlanetSetting();
            if (planet != null && !planet.isEmpty()) {
                if ("unknown".equals(planet)) {
                    textList.add(Component.translatable("univom.multiblock.probe_railgun.current_target", Component.translatable("univom.multiblock.probe_railgun.unknown_target")));
                } else {
                    textList.add(Component.translatable("univom.multiblock.probe_railgun.current_target", Component.translatable("univom.gui.planet." + planet)));
                }
            } else {
                textList.add(Component.translatable("univom.multiblock.probe_railgun.no_planet"));
            }
        }

        textList.add(Component.empty());

        var railgunLogic = boundRailgun.getRecipeLogic();

        if (railgunLogic.isWorking()) {
            double progressPercent = 0.0;
            if (railgunLogic.getMaxProgress() > 0) progressPercent = (double) railgunLogic.getProgress() / railgunLogic.getMaxProgress();

            String phaseKey;
            if (progressPercent < 0.15) phaseKey = "univom.multiblock.probe_railgun.mission.phase.launch";
            else if (progressPercent < 0.35) phaseKey = "univom.multiblock.probe_railgun.mission.phase.transit";
            else if (progressPercent < 0.60) phaseKey = boundRailgun.isDeepSpaceMode() ? "univom.multiblock.probe_railgun.mission.phase.scanning_deep" : "univom.multiblock.probe_railgun.mission.phase.scanning_target";
            else if (progressPercent < 0.85) phaseKey = "univom.multiblock.probe_railgun.mission.phase.compiling";
            else phaseKey = "univom.multiblock.probe_railgun.mission.phase.transmitting";

            textList.add(Component.translatable("univom.multiblock.probe_railgun.mission.phase.prefix", Component.translatable(phaseKey)));
            textList.add(Component.translatable("univom.multiblock.probe_railgun.mission.phase.progress", String.format("%.1f", progressPercent * 100)));

        } else {
            textList.add(Component.translatable("univom.multiblock.probe_railgun.mission.phase.prefix", Component.translatable("univom.multiblock.probe_railgun.mission.phase.standby")));
        }
    }
}