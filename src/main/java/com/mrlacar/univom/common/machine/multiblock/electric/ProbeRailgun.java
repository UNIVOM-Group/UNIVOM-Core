package com.mrlacar.univom.common.machine.multiblock.electric;

import com.gregtechceu.gtceu.api.gui.fancy.ConfiguratorPanel;
import com.gregtechceu.gtceu.api.gui.fancy.IFancyConfiguratorButton;
import com.gregtechceu.gtceu.api.gui.fancy.TabsWidget;
import com.gregtechceu.gtceu.api.machine.ConditionalSubscriptionHandler;
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.MetaMachine;
import com.gregtechceu.gtceu.api.machine.feature.IMachineLife;
import com.gregtechceu.gtceu.api.machine.multiblock.WorkableElectricMultiblockMachine;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.lowdragmc.lowdraglib.gui.texture.GuiTextureGroup;
import com.lowdragmc.lowdraglib.gui.texture.ResourceBorderTexture;
import com.lowdragmc.lowdraglib.gui.texture.TextTexture;
import com.lowdragmc.lowdraglib.gui.widget.ButtonWidget;
import com.lowdragmc.lowdraglib.gui.widget.Widget;
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;
import com.lowdragmc.lowdraglib.syncdata.annotation.DescSynced;
import com.lowdragmc.lowdraglib.syncdata.annotation.Persisted;
import com.lowdragmc.lowdraglib.syncdata.field.ManagedFieldHolder;
import com.mojang.blaze3d.MethodsReturnNonnullByDefault;
import com.mrlacar.univom.api.ModValues;
import com.mrlacar.univom.api.gui.ModGuiTextures;
import com.mrlacar.univom.api.machine.fancyconfigurator.SystemSelectionConfigurator;
import com.mrlacar.univom.api.machine.fancyconfigurator.TargetedPlanetConfigurator;
import com.mrlacar.univom.api.machine.feature.IMachineConnectorInteractable;
import com.mrlacar.univom.api.data.planet.TargetedPlanets;
import com.mrlacar.univom.common.data.ModItems;
import lombok.Getter;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;
import java.util.Objects;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class ProbeRailgun extends WorkableElectricMultiblockMachine implements IMachineConnectorInteractable, IMachineLife {
    protected static final ManagedFieldHolder MANAGED_FIELD_HOLDER = new ManagedFieldHolder(
            ProbeRailgun.class, WorkableElectricMultiblockMachine.MANAGED_FIELD_HOLDER);

    @Persisted
    @DescSynced
    private @Nullable BlockPos boundStationPos = null;

    @Getter
    @Persisted
    @DescSynced
    private String currentAUInput = "";

    @Getter
    @Persisted
    @DescSynced
    private String currentPlanetSetting = "";

    @Getter
    @Persisted
    @DescSynced
    private String currentSystemSetting = "";

    @Persisted
    @DescSynced
    @Getter
    private boolean deepSpaceMode;

    @Persisted
    private boolean hasWrittenCardForCurrentRun = false;

    @Persisted
    @DescSynced
    private boolean isLaunchTriggered = false;

    @Persisted
    private int launchTimeout = 0;

    private final ConditionalSubscriptionHandler completionCheckSubs;

    public ProbeRailgun(IMachineBlockEntity holder, Object... args) {
        super(holder, args);
        this.completionCheckSubs = new ConditionalSubscriptionHandler(this, this::checkCompletion, this::isFormed);
    }

    @Override
    public ManagedFieldHolder getFieldHolder() {
        return MANAGED_FIELD_HOLDER;
    }

    @Override
    public void onStructureFormed() {
        super.onStructureFormed();
        this.completionCheckSubs.initialize(this.getLevel());
    }

    @Override
    public void onMachineRemoved() {
        if (this.boundStationPos != null && getLevel() != null && !getLevel().isClientSide) {
            var stationMachine = MetaMachine.getMachine(getLevel(), this.boundStationPos);
            if (stationMachine instanceof AstronomicalAnalysisStation station) station.unbindRailgun();
        }
    }

    protected void checkCompletion() {
        boolean isWorking = this.getRecipeLogic().isWorking();

        if (isWorking) {
            this.isLaunchTriggered = true;
            this.launchTimeout = 0;

            int currentProgress = this.getRecipeLogic().getProgress();
            int maxProgress = this.getRecipeLogic().getMaxProgress();

            if (currentProgress <= 1) this.hasWrittenCardForCurrentRun = false;

            if (maxProgress > 0 && currentProgress >= maxProgress - 1 && !this.hasWrittenCardForCurrentRun) {
                onLaunchCompleted();
                this.hasWrittenCardForCurrentRun = true;
            }
        } else {
            if (this.isLaunchTriggered) {
                if (this.launchTimeout > 0) {
                    this.launchTimeout--;
                } else {
                    this.isLaunchTriggered = false;
                }
            }
        }
    }

    public void tryLaunch() {
        if (!this.getRecipeLogic().isWorking()) {
            this.isLaunchTriggered = true;
            this.launchTimeout = 20;

            if (this.getRecipeLogic() != null) {
                this.getRecipeLogic().markLastRecipeDirty();
                this.getRecipeLogic().updateTickSubscription();
            }
        }
    }

    private void onLaunchCompleted() {
        if (this.getLevel() == null || this.getLevel().isClientSide) return;

        AstronomicalAnalysisStation station = this.getBoundStation();
        if (station != null) {
            ItemStack cardStack = station.infoCardInventory.getStackInSlot(0);
            if (!cardStack.isEmpty()) {
                Item cardItem = cardStack.getItem();
                boolean needSync = false;

                if (this.isDeepSpaceMode() && cardItem == ModItems.SYSTEM_INFO_CARD.get()) {
                    cardStack.getOrCreateTag().putString("SystemID", this.currentSystemSetting);
                    needSync = true;
                }
                else if (!this.isDeepSpaceMode() && cardItem == ModItems.PLANET_INFO_CARD.get()) {
                    cardStack.getOrCreateTag().putString("PlanetID", this.currentPlanetSetting);
                    needSync = true;
                }

                if (needSync) {
                    station.infoCardInventory.setStackInSlot(0, cardStack);
                }
            }
        }
    }

    @Override
    public boolean beforeWorking(@Nullable GTRecipe recipe) {
        if (!this.isLaunchTriggered) return false;

        if (!this.isDeepSpaceMode() && "unknown".equals(this.currentPlanetSetting)) {
            this.getRecipeLogic().interruptRecipe();
            return false;
        }

        AstronomicalAnalysisStation station = this.getBoundStation();
        if (station == null || !station.getRecipeLogic().isWorking()) {
            this.getRecipeLogic().interruptRecipe();
            return false;
        }

        ItemStack cardStack = station.infoCardInventory.getStackInSlot(0);
        if (cardStack.isEmpty()) {
            this.getRecipeLogic().interruptRecipe();
            return false;
        }
        var tag = cardStack.getTag();
        if (tag != null && (tag.contains("SystemID") || tag.contains("PlanetID"))) {
            this.getRecipeLogic().interruptRecipe();
            return false;
        }

        Item cardItem = cardStack.getItem();
        if (this.isDeepSpaceMode() && cardItem != ModItems.SYSTEM_INFO_CARD.get()) {
            this.getRecipeLogic().interruptRecipe();
            return false;
        }
        if (!this.isDeepSpaceMode() && cardItem != ModItems.PLANET_INFO_CARD.get()) {
            this.getRecipeLogic().interruptRecipe();
            return false;
        }

        if (recipe != null) {
            if (this.isDeepSpaceMode()) {
                if (recipe.data.contains(ModValues.PLANET)) {
                    this.getRecipeLogic().interruptRecipe();
                    return false;
                }
                if (recipe.data.contains(ModValues.SYSTEM)) {
                    String requiredSystem = recipe.data.getString(ModValues.SYSTEM);
                    if (!Objects.equals(this.currentSystemSetting, requiredSystem)) {
                        this.getRecipeLogic().interruptRecipe();
                        return false;
                    }
                } else {
                    this.getRecipeLogic().interruptRecipe();
                    return false;
                }
            } else {
                if (recipe.data.contains(ModValues.SYSTEM)) {
                    this.getRecipeLogic().interruptRecipe();
                    return false;
                }
                if (recipe.data.contains(ModValues.PLANET)) {
                    String requiredPlanet = recipe.data.getString(ModValues.PLANET);
                    if (!Objects.equals(this.currentPlanetSetting, requiredPlanet)) {
                        this.getRecipeLogic().interruptRecipe();
                        return false;
                    }
                } else {
                    this.getRecipeLogic().interruptRecipe();
                    return false;
                }
            }
        }
        return super.beforeWorking(recipe);
    }

    @Override
    public InteractionResult onMachineConnectorUse(Player player, ItemStack machineConnector) {
        if (player.level().isClientSide) return InteractionResult.SUCCESS;

        if (machineConnector.getTag() != null && this.isFormed() && machineConnector.hasTag()
                && machineConnector.getTag().contains("astronomical_analysis_station_pos", Tag.TAG_LONG)
                && machineConnector.getTag().contains("astronomical_analysis_station_dim")) {

            String savedDim = machineConnector.getTag().getString("astronomical_analysis_station_dim");
            String currentDim = player.level().dimension().location().toString();

            if (!savedDim.equals(currentDim)) {
                player.displayClientMessage(Component.translatable("univom.message.bind.wrong_dimension"), true);
                return InteractionResult.FAIL;
            }
            if (this.boundStationPos != null) {
                player.displayClientMessage(Component.translatable("univom.message.bind.railgun_already_bound"), true);
                return InteractionResult.FAIL;
            }

            long stationPosLong = machineConnector.getTag().getLong("astronomical_analysis_station_pos");
            BlockPos stationPos = BlockPos.of(stationPosLong);

            if (this.getPos().equals(stationPos)) {
                player.displayClientMessage(Component.translatable("univom.message.bind.same_machine"), true);
                return InteractionResult.FAIL;
            }

            var targetMachine = MetaMachine.getMachine(Objects.requireNonNull(getLevel()), stationPos);
            if (targetMachine instanceof AstronomicalAnalysisStation station) {
                if (station.getBoundRailgunPos() != null) {
                    player.displayClientMessage(Component.translatable("univom.message.bind.astronomical_analysis_station_already_bound"), true);
                    return InteractionResult.FAIL;
                }
                this.boundStationPos = stationPos;
                station.bindRailgun(this.getPos());
                player.displayClientMessage(Component.translatable("univom.message.bind.success"), true);
                return InteractionResult.SUCCESS;
            } else {
                player.displayClientMessage(Component.translatable("univom.message.bind.astronomical_analysis_station_not_found"), true);
                return InteractionResult.FAIL;
            }
        }
        return InteractionResult.PASS;
    }

    public void unbindStation() { this.boundStationPos = null; }

    @Nullable public BlockPos getBoundStationPos() { return this.boundStationPos; }

    @Nullable
    public AstronomicalAnalysisStation getBoundStation() {
        if (this.boundStationPos == null || getLevel() == null) return null;

        if (getLevel().isLoaded(this.boundStationPos)) {
            var machine = MetaMachine.getMachine(getLevel(), this.boundStationPos);
            if (machine instanceof AstronomicalAnalysisStation station) {
                return station;
            } else {
                if (!getLevel().isClientSide) this.boundStationPos = null;
                return null;
            }
        }
        return null;
    }

    public void setDeepSpaceEnabled(boolean mode) {
        this.deepSpaceMode = mode;
        if (this.getRecipeLogic().isWorking()) this.getRecipeLogic().interruptRecipe();
    }

    public void setCurrentSystemSetting(String system) {
        this.currentSystemSetting = system;
        this.currentAUInput = "";
        this.currentPlanetSetting = "";
        if (this.getRecipeLogic().isWorking()) this.getRecipeLogic().interruptRecipe();
    }

    public void processAUInput(String input) {
        if (this.isDeepSpaceMode()) return;

        this.currentAUInput = input;

        if (input == null || input.trim().isEmpty()) {
            this.currentPlanetSetting = "";
            return;
        }

        String matchedPlanet = TargetedPlanets.getPlanetBySystemAndAU(this.currentSystemSetting, input);

        if (matchedPlanet.isEmpty()) {
            this.currentPlanetSetting = "unknown";
        } else {
            this.currentPlanetSetting = matchedPlanet;
        }
    }

    @Override
    public Widget createUIWidget() {
        Widget widget = super.createUIWidget();
        if (widget instanceof WidgetGroup group) {
            var size = group.getSize();
            if (this.isFormed()) {
                ButtonWidget launchButton = new ButtonWidget(size.width - 186, size.height - 24, 182, 20, new GuiTextureGroup(ResourceBorderTexture.BUTTON_COMMON, new TextTexture("univom.gui.button.launch_button")), null
                ) {
                    @Override
                    public boolean isActive() {
                        return !ProbeRailgun.this.isLaunchTriggered && !ProbeRailgun.this.getRecipeLogic().isWorking();
                    }
                };

                launchButton.setOnPressCallback(clickData -> {
                    if (!launchButton.isActive()) return;
                    if (!clickData.isRemote) this.tryLaunch();
                });
                group.addWidget(launchButton);
            }
        }
        return widget;
    }

    @Override
    public void attachSideTabs(TabsWidget sideTabs) {
        super.attachSideTabs(sideTabs);
        sideTabs.attachSubTab(new SystemSelectionConfigurator(this));
        sideTabs.attachSubTab(new TargetedPlanetConfigurator(this));
    }

    @Override
    public void attachConfigurators(ConfiguratorPanel configuratorPanel) {
        configuratorPanel.attachConfigurators(new IFancyConfiguratorButton.Toggle(
                ModGuiTextures.BUTTON_PROBE_RAILGUN_MODE.getSubTexture(0, 0, 1, 0.5),
                ModGuiTextures.BUTTON_PROBE_RAILGUN_MODE.getSubTexture(0, 0.5, 1, 0.5),
                this::isDeepSpaceMode, (cd, p) -> setDeepSpaceEnabled(p)).setTooltipsSupplier(p -> List.of(Component.translatable("behaviour.probe_railgun.scan_mode." + (p ? "deep_space" : "targeted")))));
    }

    @Override
    public void addDisplayText(List<Component> textList) {
        if (!isFormed()) {
            textList.add(Component.translatable("gtceu.multiblock.invalid_structure").withStyle(ChatFormatting.RED));
            return;
        }

        if (this.boundStationPos == null) {
            textList.add(Component.translatable("univom.multiblock.probe_railgun_not_bound"));
            return;
        }

        AstronomicalAnalysisStation boundStation = this.getBoundStation();

        if (boundStation == null) {
            textList.add(Component.translatable("univom.multiblock.probe_railgun_bound_out_of_range"));
            return;
        }

        if (!boundStation.isFormed()) {
            textList.add(Component.translatable("univom.multiblock.probe_railgun_not_bound"));
            return;
        }

        textList.add(Component.translatable("univom.multiblock.probe_railgun_bound_success"));
        textList.add(Component.empty());

        if (this.isDeepSpaceMode()) {
            textList.add(Component.translatable("univom.multiblock.probe_railgun.deep_space_mode"));
            if (this.currentSystemSetting != null && !this.currentSystemSetting.isEmpty()) {
                textList.add(Component.translatable("univom.multiblock.probe_railgun.current_system", Component.translatable("univom.gui.system_selection." + this.currentSystemSetting)));
            } else {
                textList.add(Component.translatable("univom.multiblock.probe_railgun.no_system"));
            }
        } else {
            textList.add(Component.translatable("univom.multiblock.probe_railgun.targeted_mode"));
            if (this.currentPlanetSetting != null && !this.currentPlanetSetting.isEmpty()) {
                if ("unknown".equals(this.currentPlanetSetting)) {
                    textList.add(Component.translatable("univom.multiblock.probe_railgun.current_target", Component.translatable("univom.multiblock.probe_railgun.unknown_target")));
                } else {
                    textList.add(Component.translatable("univom.multiblock.probe_railgun.current_target", Component.translatable("univom.gui.planet." + this.currentPlanetSetting)));
                }
            } else {
                textList.add(Component.translatable("univom.multiblock.probe_railgun.no_planet"));
            }
        }

        textList.add(Component.empty());

        if (recipeLogic.isWorking()) {
            double progressPercent = recipeLogic.getMaxProgress() > 0 ? (double) recipeLogic.getProgress() / recipeLogic.getMaxProgress() : 0.0;
            String phaseKey;
            if (progressPercent < 0.15) phaseKey = "univom.multiblock.probe_railgun.mission.phase.launch";
            else if (progressPercent < 0.35) phaseKey = "univom.multiblock.probe_railgun.mission.phase.transit";
            else if (progressPercent < 0.60) phaseKey = this.isDeepSpaceMode() ? "univom.multiblock.probe_railgun.mission.phase.scanning_deep" : "univom.multiblock.probe_railgun.mission.phase.scanning_target";
            else if (progressPercent < 0.85) phaseKey = "univom.multiblock.probe_railgun.mission.phase.compiling";
            else phaseKey = "univom.multiblock.probe_railgun.mission.phase.transmitting";

            textList.add(Component.translatable("univom.multiblock.probe_railgun.mission.phase.prefix", Component.translatable(phaseKey)));
            textList.add(Component.translatable("univom.multiblock.probe_railgun.mission.phase.progress", String.format("%.1f", progressPercent * 100)));
        } else {
            textList.add(Component.translatable("univom.multiblock.probe_railgun.mission.phase.prefix", Component.translatable("univom.multiblock.probe_railgun.mission.phase.standby")));
        }
    }
}