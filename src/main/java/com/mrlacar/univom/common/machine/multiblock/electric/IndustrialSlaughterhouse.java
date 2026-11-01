package com.mrlacar.univom.common.machine.multiblock.electric;

import com.gregtechceu.gtceu.api.gui.fancy.ConfiguratorPanel;
import com.gregtechceu.gtceu.api.gui.fancy.IFancyConfiguratorButton;
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.trait.RecipeLogic;
import com.lowdragmc.lowdraglib.syncdata.annotation.Persisted;
import com.lowdragmc.lowdraglib.syncdata.field.ManagedFieldHolder;
import com.mrlacar.univom.api.gui.ModGuiTextures;
import com.mrlacar.univom.api.machine.multiblock.SlotWorkableElectricMultiblockMachine;
import com.mrlacar.univom.common.machine.trait.SlaughterhouseLogic;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

import static com.mrlacar.univom.common.data.ModItems.MOB_CONTAINER;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class IndustrialSlaughterhouse extends SlotWorkableElectricMultiblockMachine {
    protected static final ManagedFieldHolder MANAGED_FIELD_HOLDER = new ManagedFieldHolder(
            IndustrialSlaughterhouse.class, SlotWorkableElectricMultiblockMachine.MANAGED_FIELD_HOLDER);

    @Persisted
    private boolean hostileMode = false;

    public IndustrialSlaughterhouse(IMachineBlockEntity holder, Object... args) {
        super(holder, args);
        this.setSlotFilter(stack -> stack.getItem() == MOB_CONTAINER.get());
    }

    @Override
    public ManagedFieldHolder getFieldHolder() {
        return MANAGED_FIELD_HOLDER;
    }

    @Override
    protected RecipeLogic createRecipeLogic(Object... args) {
        return new SlaughterhouseLogic(this);
    }

    @NotNull
    @Override
    public SlaughterhouseLogic getRecipeLogic() {
        return (SlaughterhouseLogic) super.getRecipeLogic();
    }

    public boolean isHostileModeEnabled() {
        return this.hostileMode;
    }

    public void setHostileModeEnabled(boolean hostileMode) {
        this.hostileMode = hostileMode;
        if (getRecipeLogic() != null) {
            getRecipeLogic().markLastRecipeDirty();
        }
    }

    @Override
    public void attachConfigurators(ConfiguratorPanel configuratorPanel) {
        super.attachConfigurators(configuratorPanel);
        configuratorPanel.attachConfigurators(new IFancyConfiguratorButton.Toggle(
                ModGuiTextures.BUTTON_SLAUGHTERHOUSE_MODE.getSubTexture(0, 0, 1, 0.5),
                ModGuiTextures.BUTTON_SLAUGHTERHOUSE_MODE.getSubTexture(0, 0.5, 1, 0.5),
                this::isHostileModeEnabled, (clickData, isToggled) -> setHostileModeEnabled(isToggled)).setTooltipsSupplier(isToggled -> List.of(Component.translatable(this.hostileMode ? "behaviour.slaughterhouse.mode.hostile" : "behaviour.slaughterhouse.mode.friendly")
        )));
    }
}
