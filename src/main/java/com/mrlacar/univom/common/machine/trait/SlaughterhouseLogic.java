package com.mrlacar.univom.common.machine.trait;

import com.gregtechceu.gtceu.api.machine.feature.IRecipeLogicMachine;
import com.gregtechceu.gtceu.api.machine.trait.RecipeLogic;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.gregtechceu.gtceu.api.recipe.RecipeHelper;
import com.gregtechceu.gtceu.data.recipe.builder.GTRecipeBuilder;
import com.mrlacar.univom.api.data.EntityBlacklist;
import com.mrlacar.univom.common.item.MobContainerBehaviour;
import com.mrlacar.univom.common.machine.multiblock.electric.IndustrialSlaughterhouse;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.common.util.FakePlayerFactory;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import static com.gregtechceu.gtceu.api.GTValues.EV;

public class SlaughterhouseLogic extends RecipeLogic {

    private static final int MAX_PROGRESS = 100;
    private static final long FIXED_ENERGY = 480L;
    private static final int MAX_SIMULATIONS = 64;
    private static final int FAILED_SEARCH_COOLDOWN = 20;

    private static final List<EntityType<?>> POOL_FRIENDLY = new ArrayList<>();
    private static final List<EntityType<?>> POOL_HOSTILE = new ArrayList<>();
    private static boolean isPoolInitialized = false;

    @Nullable
    private GTRecipe cachedLootRecipe;

    @Nullable
    private GTRecipe cachedPowerProbe;

    private int cachedTier = Integer.MIN_VALUE;
    private boolean cachedHostileMode;
    @Nullable
    private String cachedContainerEntityId;

    private long nextSearchTick = Long.MIN_VALUE;

    public SlaughterhouseLogic(IRecipeLogicMachine machine) {
        super(machine);
        initEntityPools();
    }

    @Override
    public IndustrialSlaughterhouse getMachine() {
        return (IndustrialSlaughterhouse) super.getMachine();
    }

    private static synchronized void initEntityPools() {
        if (isPoolInitialized) return;

        for (EntityType<?> type : BuiltInRegistries.ENTITY_TYPE) {
            if (type.getCategory() == MobCategory.MISC) {
                continue;
            }
            if (EntityBlacklist.isBlacklisted(type)) {
                continue;
            }

            if (type.getCategory() == MobCategory.MONSTER) {
                POOL_HOSTILE.add(type);
            } else {
                POOL_FRIENDLY.add(type);
            }
        }
        isPoolInitialized = true;
    }

    @Override
    public void resetRecipeLogic() {
        super.resetRecipeLogic();
        invalidateLootCache();
        this.nextSearchTick = Long.MIN_VALUE;
    }

    private void invalidateLootCache() {
        this.cachedLootRecipe = null;
        this.cachedTier = Integer.MIN_VALUE;
        this.cachedContainerEntityId = null;
    }

    @Override
    public void findAndHandleRecipe() {
        boolean wasDirty = this.recipeDirty;
        this.lastFailedMatches = null;
        this.recipeDirty = false;

        this.lastRecipe = null;
        this.lastOriginRecipe = null;

        IndustrialSlaughterhouse machine = getMachine();
        Level level = machine.getLevel();
        if (!(level instanceof ServerLevel serverLevel)) return;
        if (!machine.isFormed() || machine.getTier() < EV) {
            invalidateLootCache();
            return;
        }

        long now = machine.getOffsetTimer();
        if (!wasDirty && now < this.nextSearchTick) return;

        this.failureReasonMap.clear();

        if (!RecipeHelper.matchContents(machine, getPowerProbe()).isSuccess()) {
            this.nextSearchTick = now + FAILED_SEARCH_COOLDOWN;
            return;
        }

        GTRecipe recipe = getOrRollLootRecipe(serverLevel, machine);
        if (recipe == null) {
            this.nextSearchTick = now + FAILED_SEARCH_COOLDOWN;
            return;
        }

        if (!RecipeHelper.matchContents(machine, recipe).isSuccess()) {
            this.nextSearchTick = now + FAILED_SEARCH_COOLDOWN;
            return;
        }

        setupRecipe(recipe);

        if (isWorking()) {
            invalidateLootCache();
            this.nextSearchTick = Long.MIN_VALUE;
        } else {
            this.nextSearchTick = now + FAILED_SEARCH_COOLDOWN;
        }
    }

    private GTRecipe getPowerProbe() {
        if (this.cachedPowerProbe == null) {
            this.cachedPowerProbe = GTRecipeBuilder.ofRaw()
                    .duration(MAX_PROGRESS)
                    .EUt(FIXED_ENERGY)
                    .buildRawRecipe();
        }
        return this.cachedPowerProbe;
    }

    @Nullable
    private String getContainerEntityId(IndustrialSlaughterhouse machine) {
        ItemStack containerStack = machine.getStorageSlot().getStackInSlot(0);
        if (!MobContainerBehaviour.hasStoredEntity(containerStack)) return null;
        String id = containerStack.getTag().getCompound("StoredEntity").getString("id");
        return id.isEmpty() ? null : id;
    }

    @Nullable
    private GTRecipe getOrRollLootRecipe(ServerLevel serverLevel, IndustrialSlaughterhouse machine) {
        int tier = machine.getTier();
        boolean hostile = machine.isHostileModeEnabled();
        String containerId = getContainerEntityId(machine);

        boolean stillValid = this.cachedLootRecipe != null &&
                this.cachedTier == tier &&
                this.cachedHostileMode == hostile &&
                Objects.equals(this.cachedContainerEntityId, containerId);

        if (stillValid) {
            return this.cachedLootRecipe;
        }

        long totalRolls = Math.min((long) Math.pow(4, tier - 4), 16384L);
        List<ItemStack> loots = generateDynamicLoots(serverLevel, machine, containerId, hostile, totalRolls);
        if (loots.isEmpty()) {
            invalidateLootCache();
            return null;
        }

        GTRecipeBuilder builder = GTRecipeBuilder.ofRaw()
                .duration(MAX_PROGRESS)
                .EUt(FIXED_ENERGY);
        for (ItemStack loot : loots) {
            builder.outputItems(loot);
        }

        this.cachedLootRecipe = builder.buildRawRecipe();
        this.cachedTier = tier;
        this.cachedHostileMode = hostile;
        this.cachedContainerEntityId = containerId;
        return this.cachedLootRecipe;
    }

    private List<ItemStack> generateDynamicLoots(ServerLevel serverLevel,
                                                 IndustrialSlaughterhouse machine,
                                                 @Nullable String containerEntityId,
                                                 boolean hostileMode,
                                                 long totalRolls) {
        List<ItemStack> results = new ArrayList<>();
        if (totalRolls <= 0) return results;

        List<EntityType<?>> activePool = null;

        if (containerEntityId != null) {
            Optional<EntityType<?>> optionalType = EntityType.byString(containerEntityId);
            if (optionalType.isPresent()) {
                activePool = List.of(optionalType.get());
            }
        }

        if (activePool == null) {
            activePool = hostileMode ? POOL_HOSTILE : POOL_FRIENDLY;
        }
        if (activePool.isEmpty()) return results;

        long maxSimulations = Math.min(totalRolls, MAX_SIMULATIONS);
        long scaleFactor = totalRolls / maxSimulations;
        long remainder = totalRolls % maxSimulations;

        RandomSource random = serverLevel.getRandom();
        FakePlayer fakePlayer = FakePlayerFactory.getMinecraft(serverLevel);

        DamageSource damageSource = serverLevel.damageSources().playerAttack(fakePlayer);
        Vec3 origin = Vec3.atCenterOf(machine.getPos());

        List<ItemStack> batchResults = new ArrayList<>();

        Map<EntityType<?>, Entity> dummyCache = new HashMap<>();
        Map<EntityType<?>, LootTable> lootTableCache = new HashMap<>();

        for (int i = 0; i < maxSimulations; i++) {
            EntityType<?> randomMob = activePool.get(random.nextInt(activePool.size()));

            Entity dummyEntity = dummyCache.computeIfAbsent(randomMob, type -> type.create(serverLevel));
            if (dummyEntity == null) continue;

            LootTable lootTable = lootTableCache.computeIfAbsent(randomMob,
                    type -> serverLevel.getServer().getLootData().getLootTable(type.getDefaultLootTable()));

            LootParams lootParams = new LootParams.Builder(serverLevel)
                    .withParameter(LootContextParams.ORIGIN, origin)
                    .withParameter(LootContextParams.DAMAGE_SOURCE, damageSource)
                    .withParameter(LootContextParams.KILLER_ENTITY, fakePlayer)
                    .withParameter(LootContextParams.LAST_DAMAGE_PLAYER, fakePlayer)
                    .withParameter(LootContextParams.THIS_ENTITY, dummyEntity)
                    .create(LootContextParamSets.ENTITY);

            List<ItemStack> singleLoot = lootTable.getRandomItems(lootParams);
            for (ItemStack stack : singleLoot) {
                mergeItemStack(batchResults, stack);
            }
        }

        dummyCache.clear();
        lootTableCache.clear();

        for (ItemStack batchItem : batchResults) {
            long finalCount = (long) batchItem.getCount() * scaleFactor;

            if (remainder > 0 && random.nextFloat() < ((float) remainder / maxSimulations)) {
                finalCount += batchItem.getCount();
            }

            if (finalCount > 0) {
                ItemStack finalSlice = batchItem.copy();
                finalSlice.setCount((int) Math.min(Integer.MAX_VALUE, finalCount));
                mergeItemStack(results, finalSlice);
            }
        }

        return results;
    }

    private void mergeItemStack(List<ItemStack> list, ItemStack stack) {
        if (stack.isEmpty()) return;
        for (ItemStack existing : list) {
            if (ItemStack.isSameItemSameTags(existing, stack)) {
                long newCount = (long) existing.getCount() + stack.getCount();
                existing.setCount((int) Math.min(Integer.MAX_VALUE, newCount));
                return;
            }
        }
        list.add(stack);
    }
}
