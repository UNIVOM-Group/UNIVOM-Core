package com.mrlacar.univom.common.machine.trait;

import com.gregtechceu.gtceu.api.machine.feature.IRecipeLogicMachine;
import com.gregtechceu.gtceu.api.machine.feature.multiblock.IMultiPart;
import com.gregtechceu.gtceu.api.machine.trait.RecipeLogic;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.gregtechceu.gtceu.api.recipe.RecipeHelper;
import com.gregtechceu.gtceu.common.machine.multiblock.part.ParallelHatchPartMachine;
import com.gregtechceu.gtceu.data.recipe.builder.GTRecipeBuilder;
import com.mrlacar.univom.common.machine.multiblock.electric.FishingPort;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

import static com.gregtechceu.gtceu.api.GTValues.LV;
import static com.gregtechceu.gtceu.api.GTValues.VA;

public class FishingPortLogic extends RecipeLogic {
    private static final int MAX_PROGRESS = 100;
    private static final int FAILED_SEARCH_COOLDOWN = 20;
    private static final int MODE_COUNT = 3;

    private static final Item[] POOL_FISH = { Items.COD, Items.SALMON, Items.PUFFERFISH, Items.TROPICAL_FISH };
    private static final Item[] POOL_JUNK = { Items.BONE, Items.BOWL, Items.LEATHER_BOOTS, Items.LEATHER, Items.ROTTEN_FLESH, Items.KELP, Items.TRIPWIRE_HOOK, Items.GLASS_BOTTLE, Items.LILY_PAD, Items.INK_SAC };
    private static final Item[] POOL_TREASURE = { Items.BOOK, Items.BOW, Items.FISHING_ROD, Items.NAME_TAG, Items.SADDLE, Items.NAUTILUS_SHELL };

    private final GTRecipe[] cachedProbes = new GTRecipe[MODE_COUNT + 1];
    private int probeTotalParallels = -1;

    @Nullable
    private GTRecipe cachedLootRecipe;
    private int cachedLootMode = -1;
    private int cachedLootBaseParallels = -1;
    private int cachedLootHatchParallels = -1;

    private long nextSearchTick = Long.MIN_VALUE;

    public FishingPortLogic(IRecipeLogicMachine machine) {
        super(machine);
    }

    @Override
    public FishingPort getMachine() {
        return (FishingPort) super.getMachine();
    }

    @Override
    public void resetRecipeLogic() {
        super.resetRecipeLogic();
        invalidateLootCache();
        this.nextSearchTick = Long.MIN_VALUE;
    }

    private void invalidateLootCache() {
        this.cachedLootRecipe = null;
        this.cachedLootMode = -1;
        this.cachedLootBaseParallels = -1;
        this.cachedLootHatchParallels = -1;
    }

    @Override
    public void findAndHandleRecipe() {
        boolean wasDirty = this.recipeDirty;
        this.lastFailedMatches = null;
        this.recipeDirty = false;

        this.lastRecipe = null;
        this.lastOriginRecipe = null;

        FishingPort machine = getMachine();
        Level level = machine.getLevel();
        if (!(level instanceof ServerLevel serverLevel)) return;
        if (!machine.isFormed()) {
            invalidateLootCache();
            return;
        }

        long now = machine.getOffsetTimer();
        if (!wasDirty && now < this.nextSearchTick) return;

        this.failureReasonMap.clear();

        int baseParallels = (int) Math.max(1, Math.pow(machine.getTier(), 2));
        int hatchParallels = 1;
        for (IMultiPart part : machine.getParts()) {
            if (part instanceof ParallelHatchPartMachine parallelHatch) {
                hatchParallels = parallelHatch.getCurrentParallel();
                break;
            }
        }

        int totalParallels = baseParallels * hatchParallels;
        long requiredEnergy = (long) VA[LV] * totalParallels;

        for (int mode = 1; mode <= MODE_COUNT; mode++) {
            GTRecipe probe = getProbe(mode, totalParallels, requiredEnergy);
            if (!RecipeHelper.matchContents(machine, probe).isSuccess()) continue;

            GTRecipe realRecipe = getOrRollLootRecipe(serverLevel, mode, baseParallels, hatchParallels,
                    totalParallels, requiredEnergy);
            if (realRecipe == null) continue;

            if (!RecipeHelper.matchContents(machine, realRecipe).isSuccess()) continue;

            setupRecipe(realRecipe);
            if (isWorking()) {
                invalidateLootCache();
                this.nextSearchTick = Long.MIN_VALUE;
                return;
            }
        }

        this.nextSearchTick = now + FAILED_SEARCH_COOLDOWN;
    }

    private GTRecipe getProbe(int mode, int totalParallels, long requiredEnergy) {
        if (this.probeTotalParallels != totalParallels) {
            this.probeTotalParallels = totalParallels;
            for (int i = 1; i <= MODE_COUNT; i++) {
                this.cachedProbes[i] = null;
            }
        }
        if (this.cachedProbes[mode] == null) {
            this.cachedProbes[mode] = GTRecipeBuilder.ofRaw()
                    .duration(MAX_PROGRESS)
                    .EUt(requiredEnergy)
                    .inputItems(new ItemStack(Items.STRING, totalParallels))
                    .circuitMeta(mode)
                    .buildRawRecipe();
        }
        return this.cachedProbes[mode];
    }

    @Nullable
    private GTRecipe getOrRollLootRecipe(ServerLevel serverLevel, int mode, int baseParallels, int hatchParallels,
                                         int totalParallels, long requiredEnergy) {
        if (this.cachedLootRecipe != null &&
                this.cachedLootMode == mode &&
                this.cachedLootBaseParallels == baseParallels &&
                this.cachedLootHatchParallels == hatchParallels) {
            return this.cachedLootRecipe;
        }

        List<ItemStack> loots = generateLoots(serverLevel, mode, baseParallels, hatchParallels);
        if (loots.isEmpty()) {
            invalidateLootCache();
            return null;
        }

        GTRecipeBuilder builder = GTRecipeBuilder.ofRaw()
                .duration(MAX_PROGRESS)
                .EUt(requiredEnergy)
                .inputItems(new ItemStack(Items.STRING, totalParallels))
                .circuitMeta(mode);

        for (ItemStack loot : loots) {
            builder.outputItems(loot);
        }

        this.cachedLootRecipe = builder.buildRawRecipe();
        this.cachedLootMode = mode;
        this.cachedLootBaseParallels = baseParallels;
        this.cachedLootHatchParallels = hatchParallels;
        return this.cachedLootRecipe;
    }

    private List<ItemStack> generateLoots(ServerLevel serverLevel, int mode, int baseParallels, int hatchParallels) {
        List<ItemStack> results = new ArrayList<>();
        Item[] activePool;

        switch (mode) {
            case 1: activePool = POOL_FISH; break;
            case 2: activePool = POOL_JUNK; break;
            case 3: activePool = POOL_TREASURE; break;
            default: return results;
        }

        RandomSource random = serverLevel.getRandom();

        for (int i = 0; i < baseParallels; i++) {
            ItemStack drawnItem = new ItemStack(activePool[random.nextInt(activePool.length)]);

            if (mode == 3) {
                if (drawnItem.is(Items.BOOK)) {
                    if (random.nextFloat() < 0.5f) {
                        int enchantLevel = 15 + random.nextInt(16);
                        drawnItem = EnchantmentHelper.enchantItem(random, drawnItem, enchantLevel, true);
                    }
                } else if (drawnItem.is(Items.BOW) || drawnItem.is(Items.FISHING_ROD)) {
                    if (random.nextFloat() < 0.5f) {
                        int enchantLevel = 10 + random.nextInt(16);
                        drawnItem = EnchantmentHelper.enchantItem(random, drawnItem, enchantLevel, true);
                    }
                }
            }

            if (drawnItem.isDamageableItem()) {
                int maxDamage = drawnItem.getMaxDamage();
                int randomDamage = (int) (maxDamage * (0.1f + random.nextFloat() * 0.8f));
                drawnItem.setDamageValue(randomDamage);
            }

            drawnItem.setCount(hatchParallels);
            boolean merged = false;

            for (ItemStack resultItem : results) {
                if (ItemStack.isSameItemSameTags(drawnItem, resultItem)) {
                    resultItem.grow(drawnItem.getCount());
                    merged = true;
                    break;
                }
            }

            if (!merged) {
                results.add(drawnItem);
            }
        }

        return results;
    }
}
