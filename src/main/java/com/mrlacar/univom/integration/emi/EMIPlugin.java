package com.mrlacar.univom.integration.emi;

import appeng.menu.me.items.PatternEncodingTermMenu;
import com.mrlacar.univom.integration.emi.multipage.MultiblockInfoEmiCategory;
import com.mrlacar.univom.integration.emi.recipe.Ae2PatternTerminalHandler;
import dev.emi.emi.api.EmiEntrypoint;
import dev.emi.emi.api.EmiPlugin;
import dev.emi.emi.api.EmiRegistry;

@EmiEntrypoint
public class EMIPlugin implements EmiPlugin {
    @Override
    public void register(EmiRegistry registry) {
        registry.addCategory(MultiblockInfoEmiCategory.CATEGORY);
        registry.addRecipeHandler(PatternEncodingTermMenu.TYPE, new Ae2PatternTerminalHandler<>());
        MultiblockInfoEmiCategory.registerDisplays(registry);
    }
}
