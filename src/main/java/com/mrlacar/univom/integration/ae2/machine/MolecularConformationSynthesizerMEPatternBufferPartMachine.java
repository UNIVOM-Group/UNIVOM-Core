package com.mrlacar.univom.integration.ae2.machine;

import appeng.api.crafting.IPatternDetails;
import appeng.api.crafting.PatternDetailsHelper;
import appeng.api.implementations.blockentities.PatternContainerGroup;
import appeng.api.inventories.InternalInventory;
import appeng.api.networking.IGrid;
import appeng.api.networking.IGridNodeListener;
import appeng.api.networking.crafting.ICraftingProvider;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.AEKeyType;
import appeng.api.stacks.KeyCounter;
import appeng.api.storage.MEStorage;
import appeng.api.storage.StorageHelper;
import appeng.crafting.pattern.CraftingPatternItem;
import appeng.helpers.patternprovider.PatternContainer;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.gui.GuiTextures;
import com.gregtechceu.gtceu.api.gui.fancy.ConfiguratorPanel;
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.MultiblockMachineDefinition;
import com.gregtechceu.gtceu.api.machine.TickableSubscription;
import com.gregtechceu.gtceu.api.machine.fancyconfigurator.ButtonConfigurator;
import com.gregtechceu.gtceu.api.machine.feature.multiblock.IMultiController;
import com.gregtechceu.gtceu.api.machine.trait.RecipeHandlerList;
import com.gregtechceu.gtceu.api.recipe.ingredient.SizedIngredient;
import com.gregtechceu.gtceu.api.transfer.item.CustomItemStackHandler;
import com.gregtechceu.gtceu.common.data.machines.GTAEMachines;
import com.gregtechceu.gtceu.common.item.IntCircuitBehaviour;
import com.gregtechceu.gtceu.integration.ae2.gui.widget.AETextInputButtonWidget;
import com.gregtechceu.gtceu.integration.ae2.gui.widget.slot.AEPatternViewSlotWidget;
import com.gregtechceu.gtceu.integration.ae2.machine.MEBusPartMachine;
import com.gregtechceu.gtceu.utils.GTMath;
import com.gregtechceu.gtceu.utils.ItemStackHashStrategy;
import com.lowdragmc.lowdraglib.gui.texture.GuiTextureGroup;
import com.lowdragmc.lowdraglib.gui.util.ClickData;
import com.lowdragmc.lowdraglib.gui.widget.LabelWidget;
import com.lowdragmc.lowdraglib.gui.widget.Widget;
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;
import com.lowdragmc.lowdraglib.syncdata.IContentChangeAware;
import com.lowdragmc.lowdraglib.syncdata.ITagSerializable;
import com.lowdragmc.lowdraglib.syncdata.annotation.DescSynced;
import com.lowdragmc.lowdraglib.syncdata.annotation.Persisted;
import com.lowdragmc.lowdraglib.syncdata.field.ManagedFieldHolder;
import com.mrlacar.univom.api.machine.feature.IPatternOptimizableMachine;
import com.mrlacar.univom.common.machine.multiblock.electric.MolecularConformationSynthesizer;
import com.mrlacar.univom.integration.ae2.machine.trait.MolecularConformationSynthesizerInternalRecipeHandler;
import it.unimi.dsi.fastutil.objects.Object2LongOpenCustomHashMap;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.TickTask;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class MolecularConformationSynthesizerMEPatternBufferPartMachine extends MEBusPartMachine implements ICraftingProvider, PatternContainer, IPatternOptimizableMachine {
    protected static final ManagedFieldHolder MANAGED_FIELD_HOLDER = new ManagedFieldHolder(
            MolecularConformationSynthesizerMEPatternBufferPartMachine.class, MEBusPartMachine.MANAGED_FIELD_HOLDER);
    protected static final int MAX_PATTERN_COUNT = 81;

    private final InternalInventory internalPatternInventory = new InternalInventory() {
        @Override
        public int size() { return MAX_PATTERN_COUNT; }

        @Override
        public ItemStack getStackInSlot(int slotIndex) {
            return patternInventory.getStackInSlot(slotIndex);
        }

        @Override
        public void setItemDirect(int slotIndex, ItemStack stack) {
            patternInventory.setStackInSlot(slotIndex, stack);
            patternInventory.onContentsChanged(slotIndex);
            onPatternChange(slotIndex);
        }
    };

    @Getter
    @Persisted
    @DescSynced
    private final CustomItemStackHandler patternInventory = new CustomItemStackHandler(MAX_PATTERN_COUNT);

    @Getter
    @Persisted
    protected final InternalSlot[] internalInventory = new InternalSlot[MAX_PATTERN_COUNT];

    @Getter
    private final BiMap<IPatternDetails, InternalSlot> detailsSlotMap = HashBiMap.create(MAX_PATTERN_COUNT);

    @DescSynced
    @Persisted
    @Setter
    private String customName = "";

    @Persisted
    @DescSynced
    private boolean patternOptimizationAllowed = true;

    private boolean needPatternSync;

    private ListTag cachedJadeItemsTag = null;
    private boolean isJadeCacheDirty = true;

    @Getter
    protected final MolecularConformationSynthesizerInternalRecipeHandler internalRecipeHandler;

    @Nullable
    protected TickableSubscription updateSubs;

    @Getter
    private final BiMap<@NotNull IPatternDetails, Integer> patternSlotMap;

    public MolecularConformationSynthesizerMEPatternBufferPartMachine(IMachineBlockEntity holder, Object... args) {
        super(holder, IO.IN, args);
        this.patternInventory.setFilter(stack -> stack.getItem() instanceof CraftingPatternItem);
        for (int i = 0; i < this.internalInventory.length; i++) {
            this.internalInventory[i] = new InternalSlot();
        }
        patternSlotMap = HashBiMap.create();
        getMainNode().addService(ICraftingProvider.class, this);
        this.internalRecipeHandler = new MolecularConformationSynthesizerInternalRecipeHandler(this, internalInventory);
    }

    public void markJadeCacheDirty() {
        this.isJadeCacheDirty = true;
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (getLevel() instanceof ServerLevel serverLevel) {
            serverLevel.getServer().tell(new TickTask(1, () -> {
                for (int i = 0; i < patternInventory.getSlots(); i++) {
                    var pattern = patternInventory.getStackInSlot(i);
                    var patternDetails = PatternDetailsHelper.decodePattern(pattern, getLevel());
                    if (patternDetails != null) {
                        this.detailsSlotMap.put(patternDetails, this.internalInventory[i]);
                    }
                }
                needPatternSync = true;
            }));
        }
    }

    @Override
    public List<RecipeHandlerList> getRecipeHandlers() {
        return internalRecipeHandler.getSlotHandlers();
    }

    @Override
    public boolean isPatternOptimizationAllowed() {
        return this.patternOptimizationAllowed;
    }

    @Override
    public void setPatternOptimizationAllowed(boolean allowed) {
        if (this.patternOptimizationAllowed != allowed) {
            this.patternOptimizationAllowed = allowed;
            onChanged();
        }
    }

    @Override
    public boolean isWorkingEnabled() { return true; }

    @Override
    public void setWorkingEnabled(boolean ignored) {}

    @Override
    public boolean isDistinct() { return true; }

    @Override
    public void setDistinct(boolean ignored) {}

    @Override
    public void onMainNodeStateChanged(IGridNodeListener.State reason) {
        super.onMainNodeStateChanged(reason);
        this.updateSubscription();
    }

    protected void updateSubscription() {
        if (getMainNode().isOnline()) {
            updateSubs = subscribeServerTick(updateSubs, this::update);
        } else if (updateSubs != null) {
            updateSubs.unsubscribe();
            updateSubs = null;
        }
    }

    protected void update() {
        if (needPatternSync) {
            ICraftingProvider.requestUpdate(getMainNode());
            this.needPatternSync = false;
        }
    }

    private boolean isMachineWorking() {
        if (isFormed()) {
            var controllers = getControllers();
            if (controllers != null && !controllers.isEmpty()) {
                var controller = controllers.first();
                if (controller instanceof MolecularConformationSynthesizer machine) return machine.isActive();
            }
        }
        return false;
    }

    private void refundAll(ClickData clickData) {
        if (!clickData.isRemote) {
            for (InternalSlot internalSlot : internalInventory) {
                internalSlot.refund();
            }
        }
    }

    private void onPatternChange(int index) {
        if (isRemote()) return;
        var internalInv = internalInventory[index];
        var newPattern = patternInventory.getStackInSlot(index);
        var newPatternDetails = PatternDetailsHelper.decodePattern(newPattern, getLevel());
        var oldPatternDetails = detailsSlotMap.inverse().get(internalInv);

        if (oldPatternDetails != null) {
            detailsSlotMap.remove(oldPatternDetails);
        }
        if (newPatternDetails != null) {
            detailsSlotMap.forcePut(newPatternDetails, internalInv);
        }

        if (oldPatternDetails != null && !oldPatternDetails.equals(newPatternDetails)) {
            internalInv.refund();
        }
        needPatternSync = true;
    }

    @Override
    public void attachConfigurators(ConfiguratorPanel configuratorPanel) {
        IPatternOptimizableMachine.attachConfigurators(configuratorPanel, this);
        configuratorPanel.attachConfigurators(new ButtonConfigurator(
                new GuiTextureGroup(GuiTextures.BUTTON, GuiTextures.REFUND_OVERLAY), this::refundAll)
                .setTooltips(List.of(Component.translatable("gui.gtceu.refund_all.desc"))));
    }

    @Override
    public Widget createUIWidget() {
        int rowSize = 9;
        int colSize = 9;
        var group = new WidgetGroup(0, 0, 18 * rowSize + 16, 18 * colSize + 16);
        int index = 0;
        for (int y = 0; y < colSize; ++y) {
            for (int x = 0; x < rowSize; ++x) {
                int finalI = index;
                var slot = new AEPatternViewSlotWidget(patternInventory, index++, 8 + x * 18, 14 + y * 18)
                        .setOccupiedTexture(GuiTextures.SLOT)
                        .setItemHook(stack -> {
                            if (!stack.isEmpty() && stack.getItem() instanceof CraftingPatternItem iep) {
                                final ItemStack out = iep.getOutput(stack);
                                if (!out.isEmpty()) return out;
                            }
                            return stack;
                        })
                        .setChangeListener(() -> onPatternChange(finalI))
                        .setBackground(GuiTextures.SLOT, GuiTextures.PATTERN_OVERLAY);
                group.addWidget(slot);
            }
        }

        group.addWidget(new LabelWidget(
                8, 2, () -> this.isOnline ? "gtceu.gui.me_network.online" : "gtceu.gui.me_network.offline"));

        group.addWidget(new AETextInputButtonWidget(18 * rowSize + 8 - 70, 2, 70, 10)
                .setText(customName)
                .setOnConfirm(this::setCustomName)
                .setButtonTooltips(Component.translatable("gui.gtceu.rename.desc")));

        return group;
    }

    @Override
    public List<IPatternDetails> getAvailablePatterns() {
        return detailsSlotMap.keySet().stream().filter(Objects::nonNull).toList();
    }

    @Override
    public boolean pushPattern(IPatternDetails patternDetails, KeyCounter[] inputHolder) {
        if (!isFormed() || !getMainNode().isActive() || !detailsSlotMap.containsKey(patternDetails) ||
                !checkInput(inputHolder)) {
            return false;
        }

        var slot = detailsSlotMap.get(patternDetails);
        if (slot != null) {
            slot.pushPattern(patternDetails, inputHolder);
            return true;
        }
        return false;
    }

    @Override
    public boolean isBusy() { return false; }

    private boolean checkInput(KeyCounter[] inputHolder) {
        for (KeyCounter input : inputHolder) {
            var illegal = input.keySet().stream()
                    .map(AEKey::getType)
                    .map(AEKeyType::getId)
                    .anyMatch(id -> !id.equals(AEKeyType.items().getId()));
            if (illegal) return false;
        }
        return true;
    }

    @Override
    public ManagedFieldHolder getFieldHolder() { return MANAGED_FIELD_HOLDER; }

    @Override
    public @Nullable IGrid getGrid() { return getMainNode().getGrid(); }

    @Override
    public InternalInventory getTerminalPatternInventory() { return internalPatternInventory; }

    @Override
    public PatternContainerGroup getTerminalGroup() {
        if (isFormed()) {
            IMultiController controller = getControllers().first();
            MultiblockMachineDefinition controllerDefinition = controller.self().getDefinition();

            if (!customName.isEmpty()) {
                return new PatternContainerGroup(
                        AEItemKey.of(controllerDefinition.asStack()),
                        Component.literal(customName), Collections.emptyList());
            } else {
                ItemStack circuitStack = isHasCircuitSlot() ? circuitInventory.storage.getStackInSlot(0) : ItemStack.EMPTY;
                int circuitConfiguration = circuitStack.isEmpty() ? -1 : IntCircuitBehaviour.getCircuitConfiguration(circuitStack);

                Component groupName = circuitConfiguration != -1 ?
                        Component.translatable(controllerDefinition.getDescriptionId()).append(" - " + circuitConfiguration) :
                        Component.translatable(controllerDefinition.getDescriptionId());

                return new PatternContainerGroup(
                        AEItemKey.of(controllerDefinition.asStack()), groupName, Collections.emptyList());
            }
        } else {
            if (!customName.isEmpty()) {
                return new PatternContainerGroup(
                        AEItemKey.of(GTAEMachines.ME_PATTERN_BUFFER.getItem()),
                        Component.literal(customName), Collections.emptyList());
            } else {
                return new PatternContainerGroup(
                        AEItemKey.of(GTAEMachines.ME_PATTERN_BUFFER.getItem()),
                        GTAEMachines.ME_PATTERN_BUFFER.get().getDefinition().getItem().getDescription(), Collections.emptyList());
            }
        }
    }

    @Override
    public void onMachineRemoved() {
        clearInventory(patternInventory);
    }

    public void clearAllInternalSlots() {
        for (InternalSlot slot : internalInventory) {
            slot.itemInventory.clear();
            slot.onContentsChanged();
        }
    }

    public ListTag getJadeItemsTag(int limit) {
        if (!isJadeCacheDirty && cachedJadeItemsTag != null) {
            return cachedJadeItemsTag;
        }

        var items = new Object2LongOpenCustomHashMap<ItemStack>(ItemStackHashStrategy.comparingAllButCount());
        for (InternalSlot slot : internalInventory) {
            if (!slot.isItemEmpty()) {
                for (var entry : slot.itemInventory.object2LongEntrySet()) {
                    items.addTo(entry.getKey(), entry.getLongValue());
                    if (items.size() >= limit) break;
                }
            }
            if (items.size() >= limit) break;
        }

        ListTag itemsTag = new ListTag();
        int count = 0;
        for (var entry : items.object2LongEntrySet()) {
            if (count++ >= 20) break;
            var ct = entry.getKey().serializeNBT();
            ct.putLong("real", entry.getLongValue());
            itemsTag.add(ct);
        }

        this.cachedJadeItemsTag = itemsTag;
        this.isJadeCacheDirty = false;
        return itemsTag;
    }

    public class InternalSlot implements ITagSerializable<CompoundTag>, IContentChangeAware {

        @Getter
        @Setter
        private Runnable onContentsChanged = () -> {};

        private final Object2LongOpenCustomHashMap<ItemStack> itemInventory = new Object2LongOpenCustomHashMap<>(
                ItemStackHashStrategy.comparingAllButCount());
        private List<ItemStack> itemStacks = null;
        private long cachedTotalItems = 0;

        public InternalSlot() {}

        public boolean isItemEmpty() { return itemInventory.isEmpty(); }

        public void onContentsChanged() {
            itemStacks = null;
            long total = 0;
            for (var entry : itemInventory.object2LongEntrySet()) {
                total += entry.getLongValue();
            }
            this.cachedTotalItems = total;
            markJadeCacheDirty();
            onContentsChanged.run();
        }

        private void add(AEKey what, long amount) {
            if (amount <= 0L) return;
            if (what instanceof AEItemKey itemKey) {
                var stack = itemKey.toStack();
                itemInventory.addTo(stack, amount);
            }
        }

        public long getAvailableAmount(AEItemKey aeItem) {
            long available = 0;
            for (var entry : itemInventory.object2LongEntrySet()) {
                if (entry.getLongValue() > 0 && aeItem.matches(entry.getKey())) {
                    available += entry.getLongValue();
                }
            }
            return available;
        }

        public double getTotalContentAmount() {
            return this.cachedTotalItems;
        }

        public List<ItemStack> getItems() {
            if (itemStacks == null) {
                itemStacks = new ArrayList<>();
                for (var entry : itemInventory.object2LongEntrySet()) {
                    List<ItemStack> split = GTMath.splitStacks(entry.getKey(), entry.getLongValue());
                    itemStacks.addAll(split);
                }
            }
            return itemStacks;
        }

        public void refund() {
            var network = getMainNode().getGrid();
            if (network != null) {
                MEStorage networkInv = network.getStorageService().getInventory();
                var energy = network.getEnergyService();

                for (var it = itemInventory.object2LongEntrySet().iterator(); it.hasNext();) {
                    var entry = it.next();
                    var stack = entry.getKey();
                    var count = entry.getLongValue();
                    if (stack.isEmpty() || count <= 0) {
                        it.remove();
                        continue;
                    }

                    var key = AEItemKey.of(stack);
                    if (key == null) continue;

                    long inserted = StorageHelper.poweredInsert(energy, networkInv, key, count, actionSource);
                    if (inserted > 0) {
                        count -= inserted;
                        if (count <= 0) {
                            it.remove();
                        } else {
                            entry.setValue(count);
                        }
                    }
                }
                onContentsChanged();
            }
        }

        public void pushPattern(IPatternDetails patternDetails, KeyCounter[] inputHolder) {
            patternDetails.pushInputsToExternalInventory(inputHolder, this::add);
            onContentsChanged();
        }

        public @Nullable List<Ingredient> handleItemInternal(List<Ingredient> left, boolean simulate) {
            boolean changed = false;
            for (var it = left.listIterator(); it.hasNext();) {
                var ingredient = it.next();
                if (ingredient.isEmpty()) {
                    it.remove();
                    continue;
                }

                var items = ingredient.getItems();
                if (items.length == 0 || items[0].isEmpty()) {
                    it.remove();
                    continue;
                }

                int amount = items[0].getCount();
                for (var it2 = itemInventory.object2LongEntrySet().iterator(); it2.hasNext();) {
                    var entry = it2.next();
                    var stack = entry.getKey();
                    var count = entry.getLongValue();
                    if (stack.isEmpty() || count == 0) {
                        it2.remove();
                        continue;
                    }
                    if (!ingredient.test(stack)) continue;
                    int extracted = Math.min(GTMath.saturatedCast(count), amount);
                    if (!simulate && extracted > 0) {
                        changed = true;
                        count -= extracted;
                        if (count == 0) it2.remove();
                        else entry.setValue(count);
                    }
                    amount -= extracted;

                    if (amount <= 0) {
                        it.remove();
                        break;
                    }
                }

                if (amount > 0) {
                    ItemStack safeCopy = items[0].copy();
                    safeCopy.setCount(amount);
                    it.set(SizedIngredient.create(safeCopy));
                }
            }
            if (changed) onContentsChanged();
            return left.isEmpty() ? null : left;
        }

        @Override
        public CompoundTag serializeNBT() {
            CompoundTag tag = new CompoundTag();
            ListTag itemsTag = new ListTag();
            for (var entry : itemInventory.object2LongEntrySet()) {
                var ct = entry.getKey().serializeNBT();
                ct.putLong("real", entry.getLongValue());
                itemsTag.add(ct);
            }
            if (!itemsTag.isEmpty()) tag.put("inventory", itemsTag);
            return tag;
        }

        @Override
        public void deserializeNBT(CompoundTag tag) {
            ListTag items = tag.getList("inventory", Tag.TAG_COMPOUND);
            long total = 0;
            for (Tag t : items) {
                if (!(t instanceof CompoundTag ct)) continue;
                var stack = ItemStack.of(ct);
                var count = ct.getLong("real");
                if (!stack.isEmpty() && count > 0) {
                    itemInventory.put(stack, count);
                    total += count;
                }
            }
            this.cachedTotalItems = total;
            markJadeCacheDirty();
        }
    }
}