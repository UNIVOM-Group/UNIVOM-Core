package com.mrlacar.univom.integration.ae2.machine;

import appeng.api.crafting.IPatternDetails;
import appeng.api.crafting.PatternDetailsHelper;
import appeng.api.implementations.blockentities.PatternContainerGroup;
import appeng.api.inventories.InternalInventory;
import appeng.api.networking.IGrid;
import appeng.api.networking.IGridNodeListener;
import appeng.api.networking.crafting.ICraftingProvider;
import appeng.api.stacks.*;
import appeng.api.storage.MEStorage;
import appeng.api.storage.StorageHelper;
import appeng.crafting.pattern.EncodedPatternItem;
import appeng.crafting.pattern.ProcessingPatternItem;
import appeng.helpers.patternprovider.PatternContainer;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.gui.GuiTextures;
import com.gregtechceu.gtceu.api.gui.fancy.ConfiguratorPanel;
import com.gregtechceu.gtceu.api.gui.fancy.TabsWidget;
import com.gregtechceu.gtceu.api.gui.widget.SlotWidget;
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.MetaMachine;
import com.gregtechceu.gtceu.api.machine.MultiblockMachineDefinition;
import com.gregtechceu.gtceu.api.machine.TickableSubscription;
import com.gregtechceu.gtceu.api.machine.fancyconfigurator.ButtonConfigurator;
import com.gregtechceu.gtceu.api.machine.fancyconfigurator.FancyInvConfigurator;
import com.gregtechceu.gtceu.api.machine.fancyconfigurator.FancyTankConfigurator;
import com.gregtechceu.gtceu.api.machine.fancyconfigurator.MachineModeFancyConfigurator;
import com.gregtechceu.gtceu.api.machine.feature.IDataStickInteractable;
import com.gregtechceu.gtceu.api.machine.feature.IRecipeLogicMachine;
import com.gregtechceu.gtceu.api.machine.feature.multiblock.IMultiController;
import com.gregtechceu.gtceu.api.machine.trait.NotifiableFluidTank;
import com.gregtechceu.gtceu.api.machine.trait.NotifiableItemStackHandler;
import com.gregtechceu.gtceu.api.machine.trait.RecipeHandlerList;
import com.gregtechceu.gtceu.api.recipe.ingredient.FluidIngredient;
import com.gregtechceu.gtceu.api.recipe.ingredient.SizedIngredient;
import com.gregtechceu.gtceu.api.transfer.item.CustomItemStackHandler;
import com.gregtechceu.gtceu.common.data.machines.GTAEMachines;
import com.gregtechceu.gtceu.common.item.IntCircuitBehaviour;
import com.gregtechceu.gtceu.config.ConfigHolder;
import com.gregtechceu.gtceu.integration.ae2.gui.widget.AETextInputButtonWidget;
import com.gregtechceu.gtceu.integration.ae2.gui.widget.slot.AEPatternViewSlotWidget;
import com.gregtechceu.gtceu.utils.GTMath;
import com.gregtechceu.gtceu.utils.ItemStackHashStrategy;
import com.lowdragmc.lowdraglib.gui.texture.GuiTextureGroup;
import com.lowdragmc.lowdraglib.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib.gui.texture.ItemStackTexture;
import com.lowdragmc.lowdraglib.gui.texture.TextTexture;
import com.lowdragmc.lowdraglib.gui.util.ClickData;
import com.lowdragmc.lowdraglib.gui.widget.*;
import com.lowdragmc.lowdraglib.syncdata.IContentChangeAware;
import com.lowdragmc.lowdraglib.syncdata.ITagSerializable;
import com.lowdragmc.lowdraglib.syncdata.annotation.DescSynced;
import com.lowdragmc.lowdraglib.syncdata.annotation.Persisted;
import com.lowdragmc.lowdraglib.syncdata.field.ManagedFieldHolder;
import com.mrlacar.univom.api.machine.fancyconfigurator.PatternModifierConfigurator;
import com.mrlacar.univom.api.machine.feature.IPatternOptimizableMachine;
import com.mrlacar.univom.integration.ae2.machine.trait.WirelessPatternInternalSlotRecipeHandler;
import it.unimi.dsi.fastutil.objects.*;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.TickTask;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidType;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnmodifiableView;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.*;
import java.util.function.Consumer;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class WirelessMEPatternBufferPartMachine extends WirelessMEBusPartMachine implements ICraftingProvider, PatternContainer, IDataStickInteractable, IPatternOptimizableMachine {
    protected static final ManagedFieldHolder MANAGED_FIELD_HOLDER = new ManagedFieldHolder(
            WirelessMEPatternBufferPartMachine.class, WirelessMEBusPartMachine.MANAGED_FIELD_HOLDER);

    @Getter
    protected final int maxPatternCount;

    private final InternalInventory internalPatternInventory;

    @Getter
    @Persisted
    @DescSynced
    private final CustomItemStackHandler patternInventory;

    @Getter
    @Persisted
    protected final NotifiableItemStackHandler shareInventory;

    @Getter
    @Persisted
    protected final NotifiableFluidTank shareTank;

    @Getter
    @Persisted
    protected final InternalSlot[] internalInventory;

    private final BiMap<IPatternDetails, InternalSlot> detailsSlotMap;

    @DescSynced
    @Persisted
    @Setter
    private String customName = "";

    @Persisted
    @DescSynced
    private boolean patternOptimizationAllowed = true;

    private boolean needPatternSync;

    @Persisted
    private final Set<BlockPos> proxies = new ObjectOpenHashSet<>();
    private final Set<WirelessMEPatternBufferProxyPartMachine> proxyMachines = new ReferenceOpenHashSet<>();

    @Getter
    protected final WirelessPatternInternalSlotRecipeHandler internalRecipeHandler;

    @Nullable
    protected TickableSubscription updateSubs;

    public WirelessMEPatternBufferPartMachine(IMachineBlockEntity holder, int maxPatternCount, Object... args) {
        super(holder, IO.IN, args);
        this.maxPatternCount = maxPatternCount;

        this.patternInventory = new CustomItemStackHandler(maxPatternCount);
        this.patternInventory.setFilter(stack -> stack.getItem() instanceof ProcessingPatternItem);

        this.internalInventory = new InternalSlot[maxPatternCount];
        for (int i = 0; i < this.internalInventory.length; i++) {
            this.internalInventory[i] = new InternalSlot(this);
        }

        this.detailsSlotMap = HashBiMap.create(maxPatternCount);

        this.internalPatternInventory = new InternalInventory() {

            @Override
            public int size() {
                return maxPatternCount;
            }

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

        getMainNode().addService(ICraftingProvider.class, this);

        this.shareInventory = new NotifiableItemStackHandler(this, 9, IO.IN, IO.NONE);
        this.shareTank = new NotifiableFluidTank(this, 9, 8 * FluidType.BUCKET_VOLUME, IO.IN, IO.NONE);

        this.internalRecipeHandler = new WirelessPatternInternalSlotRecipeHandler(this, internalInventory);
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
    public boolean isWorkingEnabled() {
        return true;
    }

    @Override
    public void setWorkingEnabled(boolean ignored) {}

    @Override
    public boolean isDistinct() {
        return true;
    }

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

    public void addProxy(WirelessMEPatternBufferProxyPartMachine proxy) {
        proxies.add(proxy.getPos());
        proxyMachines.add(proxy);
    }

    public void removeProxy(WirelessMEPatternBufferProxyPartMachine proxy) {
        proxies.remove(proxy.getPos());
        proxyMachines.remove(proxy);
    }

    @UnmodifiableView
    public Set<WirelessMEPatternBufferProxyPartMachine> getProxies() {
        if (proxyMachines.size() != proxies.size()) {
            proxyMachines.clear();
            for (var pos : proxies) {
                if (MetaMachine.getMachine(getLevel(), pos) instanceof WirelessMEPatternBufferProxyPartMachine proxy) {
                    proxyMachines.add(proxy);
                }
            }
        }
        return Collections.unmodifiableSet(proxyMachines);
    }

    private void refundAll(ClickData clickData) {
        if (!clickData.isRemote) {
            for (InternalSlot internalSlot : internalInventory) {
                internalSlot.refund();
            }
        }
    }

    public void modifyPatterns(int factor, boolean divide) {
        if (isRemote()) return;

        if (factor <= 1) return;

        boolean changedAny = false;

        for (int i = 0; i < patternInventory.getSlots(); i++) {
            if (modifyPatternAtSlot(i, factor, divide)) {
                changedAny = true;
            }
        }

        if (changedAny) {
            needPatternSync = true;
            if (getMainNode().isOnline()) {
                ICraftingProvider.requestUpdate(getMainNode());
            }
        }
    }

    public void modifySinglePattern(int slotIndex, int factor, boolean divide) {
        if (isRemote()) return;
        if (slotIndex < 0 || slotIndex >= patternInventory.getSlots()) return;

        if (factor <= 1) return;

        if (modifyPatternAtSlot(slotIndex, factor, divide)) {
            needPatternSync = true;
            if (getMainNode().isOnline()) {
                ICraftingProvider.requestUpdate(getMainNode());
            }
        }
    }

    private boolean modifyPatternAtSlot(int slotIndex, int factor, boolean divide) {
        ItemStack pattern = patternInventory.getStackInSlot(slotIndex);
        if (pattern.isEmpty() || !(pattern.getItem() instanceof ProcessingPatternItem)) return false;

        CompoundTag tag = pattern.getOrCreateTag().copy();

        if (!canModifyPattern(tag, factor, divide)) {
            return false;
        }

        applyPatternModification(tag, factor, divide);
        pattern.setTag(tag);
        patternInventory.setStackInSlot(slotIndex, pattern);
        onPatternChange(slotIndex);
        return true;
    }

    private boolean canModifyPattern(CompoundTag tag, int factor, boolean divide) {
        return !(tag.contains("in", Tag.TAG_LIST) && checkTagList(tag.getList("in", Tag.TAG_COMPOUND), factor, divide)) && !(tag.contains("out", Tag.TAG_LIST) && checkTagList(tag.getList("out", Tag.TAG_COMPOUND), factor, divide));
    }

    private boolean checkTagList(ListTag list, int factor, boolean divide) {
        for (Tag t : list) {
            if (t instanceof CompoundTag ct) {
                if (!ct.contains("#", Tag.TAG_LONG)) continue;
                long v = ct.getLong("#");

                if (divide) {
                    if (v % factor != 0) return true;
                } else {
                    try {
                        long mul = Math.multiplyExact(v, factor);
                        if (mul > Integer.MAX_VALUE) return true;
                    } catch (ArithmeticException e) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private void applyPatternModification(CompoundTag tag, int factor, boolean divide) {
        if (tag.contains("in", Tag.TAG_LIST)) {
            applyTagListModification(tag.getList("in", Tag.TAG_COMPOUND), factor, divide);
        }
        if (tag.contains("out", Tag.TAG_LIST)) {
            applyTagListModification(tag.getList("out", Tag.TAG_COMPOUND), factor, divide);
        }
    }

    private void applyTagListModification(ListTag list, int factor, boolean divide) {
        for (Tag t : list) {
            if (t instanceof CompoundTag ct) {
                if (!ct.contains("#", Tag.TAG_LONG)) continue;
                long v = ct.getLong("#");
                if (divide) {
                    ct.putLong("#", v / factor);
                } else {
                    ct.putLong("#", v * factor);
                }
            }
        }
    }

    public void uploadPatterns(Player player) {
        if (isRemote()) return;
        var inventory = player.getInventory();
        List<Integer> emptySlots = new ArrayList<>();

        for (int i = 0; i < patternInventory.getSlots(); i++) {
            if (patternInventory.getStackInSlot(i).isEmpty()) {
                emptySlots.add(i);
            }
        }

        if (emptySlots.isEmpty()) return;

        boolean changedAny = false;
        for (int slot = 0; slot < inventory.getContainerSize() && !emptySlots.isEmpty(); slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (stack.isEmpty() || !(stack.getItem() instanceof ProcessingPatternItem)) continue;

            while (!stack.isEmpty() && !emptySlots.isEmpty()) {
                ItemStack itemStack = stack.copy();
                itemStack.setCount(1);

                int target = emptySlots.remove(0);
                patternInventory.setStackInSlot(target, itemStack);
                onPatternChange(target);

                stack.shrink(1);
                changedAny = true;
            }
            if (stack.getCount() <= 0) inventory.setItem(slot, ItemStack.EMPTY);
        }

        if (changedAny) {
            needPatternSync = true;
            if (getMainNode().isOnline()) {
                ICraftingProvider.requestUpdate(getMainNode());
            }
        }
    }

    public void downloadPatterns(Player player) {
        if (isRemote()) return;
        var playerInv = player.getInventory();
        boolean changedAny = false;

        for (int i = 0; i < patternInventory.getSlots(); i++) {
            ItemStack stackInMachine = patternInventory.getStackInSlot(i);
            if (stackInMachine.isEmpty() || !(stackInMachine.getItem() instanceof ProcessingPatternItem)) continue;

            ItemStack toMove = stackInMachine.copy();
            toMove.setCount(1);

            boolean inserted = false;

            for (int slot = 0; slot < playerInv.getContainerSize(); slot++) {
                ItemStack slotStack = playerInv.getItem(slot);
                if (!slotStack.isEmpty() &&
                        ItemStack.isSameItemSameTags(slotStack, toMove) &&
                        slotStack.getCount() < slotStack.getMaxStackSize()) {
                    slotStack.grow(1);
                    inserted = true;
                    break;
                }
            }

            if (!inserted) {
                int empty = playerInv.getFreeSlot();
                if (empty != -1) {
                    playerInv.setItem(empty, toMove);
                    inserted = true;
                }
            }

            if (inserted) {
                stackInMachine.shrink(1);
                if (stackInMachine.getCount() <= 0) {
                    patternInventory.setStackInSlot(i, ItemStack.EMPTY);
                }
                onPatternChange(i);
                changedAny = true;
            }
        }

        if (changedAny) {
            needPatternSync = true;
            if (getMainNode().isOnline()) {
                ICraftingProvider.requestUpdate(getMainNode());
            }
        }
    }

    private void onPatternChange(int index) {
        if (isRemote()) return;

        var internalInv = internalInventory[index];
        var newPattern = patternInventory.getStackInSlot(index);
        var newPatternDetails = PatternDetailsHelper.decodePattern(newPattern, getLevel());
        var oldPatternDetails = detailsSlotMap.inverse().get(internalInv);
        detailsSlotMap.forcePut(newPatternDetails, internalInv);
        if (oldPatternDetails != null && !oldPatternDetails.equals(newPatternDetails)) {
            internalInv.refund();
        }
        needPatternSync = true;
    }

    @Override
    public void attachSideTabs(TabsWidget sideTabs) {
        super.attachSideTabs(sideTabs);
        if (isFormed() && !getControllers().isEmpty()) {
            IMultiController controller = getControllers().first();
            if (controller instanceof IRecipeLogicMachine rLMachine && rLMachine.getRecipeTypes().length > 1) {
                sideTabs.attachSubTab(new MachineModeFancyConfigurator(rLMachine));
            }
        }
    }

    @Override
    public void attachConfigurators(ConfiguratorPanel configuratorPanel) {
        IPatternOptimizableMachine.attachConfigurators(configuratorPanel, this);
        configuratorPanel.attachConfigurators(new PatternModifierConfigurator(this));
        configuratorPanel.attachConfigurators(new ButtonConfigurator(
                new GuiTextureGroup(GuiTextures.BUTTON, GuiTextures.REFUND_OVERLAY), this::refundAll)
                .setTooltips(List.of(Component.translatable("gui.gtceu.refund_all.desc"))));
        configuratorPanel.attachConfigurators(new FancyInvConfigurator(
                shareInventory.storage, Component.translatable("gui.gtceu.share_inventory.title"))
                .setTooltips(List.of(
                        Component.translatable("gui.gtceu.share_inventory.desc.0"),
                        Component.translatable("gui.gtceu.share_inventory.desc.1"))));
        configuratorPanel.attachConfigurators(new FancyTankConfigurator(
                shareTank.getStorages(), Component.translatable("gui.gtceu.share_tank.title"))
                .setTooltips(List.of(
                        Component.translatable("gui.gtceu.share_tank.desc.0"),
                        Component.translatable("gui.gtceu.share_inventory.desc.1"))));
    }

    @Override
    public Widget createUIWidget() {
        int rowSize = 9;
        int colSize = maxPatternCount / rowSize;
        if (maxPatternCount % rowSize != 0) colSize++;

        int bgWidth = 18 * rowSize + 16;
        int bgHeight = 18 * colSize + 16;

        int rootHeight = Math.max(bgHeight, 132);

        var group = new PatternBufferUIGroup(0, 0, bgWidth, rootHeight, bgWidth, rootHeight);

        WidgetGroup mainView = new WidgetGroup(0, 0, bgWidth, bgHeight);
        group.mainView = mainView;

        int index = 0;
        for (int y = 0; y < colSize; ++y) {
            for (int x = 0; x < rowSize; ++x) {
                if (index >= maxPatternCount) break;
                int finalI = index++;
                var slot = new PatternSlotWidget(patternInventory, finalI, 8 + x * 18, 14 + y * 18, group);
                slot.setOccupiedTexture(GuiTextures.SLOT)
                        .setItemHook(stack -> {
                            if (!stack.isEmpty() && stack.getItem() instanceof EncodedPatternItem iep) {
                                final ItemStack out = iep.getOutput(stack);
                                if (!out.isEmpty()) return out;
                            }
                            return stack;
                        })
                        .setChangeListener(() -> onPatternChange(finalI))
                        .setBackground(GuiTextures.SLOT, GuiTextures.PATTERN_OVERLAY);
                mainView.addWidget(slot);
            }
        }
        mainView.addWidget(new LabelWidget(
                8, 2, () -> this.isOnline ? "gtceu.gui.me_network.online" : "gtceu.gui.me_network.offline"));
        mainView.addWidget(new AETextInputButtonWidget(bgWidth - 8 - 70, 2, 70, 10)
                .setText(customName).setOnConfirm(this::setCustomName)
                .setButtonTooltips(Component.translatable("gui.gtceu.rename.desc")));

        group.addWidget(mainView);

        return group;
    }

    private static final int SET_SLOT_CIRCUIT_VALUE = 200;
    private static final int OPEN_DIALOG = 201;
    private static final int CLOSE_DIALOG = 202;

    private class PatternBufferUIGroup extends WidgetGroup {
        public WidgetGroup mainView;
        public WidgetGroup activeDialogPanel;

        private final int bgWidth;
        private final int rootHeight;

        public PatternBufferUIGroup(int x, int y, int width, int height, int bgWidth, int rootHeight) {
            super(x, y, width, height);
            this.bgWidth = bgWidth;
            this.rootHeight = rootHeight;
        }

        @Override
        @Nullable
        public Object getXEIIngredientOverMouse(double mouseX, double mouseY) {
            if (activeDialogPanel != null && activeDialogPanel.isVisible()) {
                if (activeDialogPanel.isMouseOverElement(mouseX, mouseY)) {
                    return activeDialogPanel.getXEIIngredientOverMouse(mouseX, mouseY);
                }
                return null;
            }
            return super.getXEIIngredientOverMouse(mouseX, mouseY);
        }

        public void sendClientAction(int id, Consumer<FriendlyByteBuf> dataWriter) {
            this.writeClientAction(id, dataWriter);
        }

        public void openDialogLocal(int slotIndex) {
            if (mainView != null) {
                mainView.setVisible(false);
                mainView.setActive(false);
                mainView.setSelfPosition(-10000, -10000);
            }
            if (activeDialogPanel != null) {
                this.removeWidget(activeDialogPanel);
            }
            activeDialogPanel = createCircuitDialogPanel(slotIndex, this, bgWidth, rootHeight);
            this.addWidget(activeDialogPanel);
        }

        public void closeDialogLocal() {
            if (activeDialogPanel != null) {
                this.removeWidget(activeDialogPanel);
                activeDialogPanel = null;
            }
            if (mainView != null) {
                mainView.setVisible(true);
                mainView.setActive(true);
                mainView.setSelfPosition(0, 0);
            }
        }

        @Override
        public void handleClientAction(int id, FriendlyByteBuf buffer) {
            if (id == SET_SLOT_CIRCUIT_VALUE) {
                int slotIndex = buffer.readVarInt();
                int value = buffer.readVarInt();
                if (slotIndex >= 0 && slotIndex < internalInventory.length) {
                    if (value < -1 || value > IntCircuitBehaviour.CIRCUIT_MAX) return;

                    ItemStackHandler handler = internalInventory[slotIndex].getCircuitInventory().storage;
                    if (value == -1) {
                        handler.setStackInSlot(0, ItemStack.EMPTY);
                    } else {
                        handler.setStackInSlot(0, IntCircuitBehaviour.stack(value));
                    }
                }
            } else if (id == OPEN_DIALOG) {
                int slotIndex = buffer.readVarInt();
                openDialogLocal(slotIndex);
            } else if (id == CLOSE_DIALOG) {
                closeDialogLocal();
            } else if (id == 780) {
                int slotIndex = buffer.readVarInt();
                int factor = buffer.readInt();
                boolean divide = buffer.readBoolean();
                modifySinglePattern(slotIndex, factor, divide);
            } else {
                super.handleClientAction(id, buffer);
            }
        }
    }

    private class PatternSlotWidget extends AEPatternViewSlotWidget {
        private final int slotIndex;
        private final PatternBufferUIGroup group;

        public PatternSlotWidget(ItemStackHandler inventory, int index, int xPosition, int yPosition, PatternBufferUIGroup group) {
            super(inventory, index, xPosition, yPosition);
            this.slotIndex = index;
            this.group = group;
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            if (button == 2 && isMouseOverElement(mouseX, mouseY)) {
                group.openDialogLocal(slotIndex);

                if (group.getGui() != null && group.getGui().holder.isRemote()) {
                    group.sendClientAction(OPEN_DIALOG, buf -> buf.writeVarInt(slotIndex));
                }
                return true;
            }
            return super.mouseClicked(mouseX, mouseY, button);
        }
    }

    private WidgetGroup createCircuitDialogPanel(int slotIndex, PatternBufferUIGroup group, int bgWidth, int rootHeight) {
        int dialogWidth = bgWidth - 2;
        int dialogHeight = rootHeight - 3;
        int posX = 1;
        int posY = 1;

        WidgetGroup circuitPanel = new WidgetGroup(posX, posY, dialogWidth, dialogHeight) {
            @Override
            @Nullable
            public Widget getHoverElement(double mouseX, double mouseY) {
                if (isMouseOverElement(mouseX, mouseY)) {
                    Widget hover = super.getHoverElement(mouseX, mouseY);
                    return hover != null ? hover : this;
                }
                return null;
            }

            @Override
            public boolean mouseClicked(double mouseX, double mouseY, int button) {
                super.mouseClicked(mouseX, mouseY, button);
                return isMouseOverElement(mouseX, mouseY);
            }

            @Override
            public boolean mouseWheelMove(double mouseX, double mouseY, double wheelDelta) {
                super.mouseWheelMove(mouseX, mouseY, wheelDelta);
                return isMouseOverElement(mouseX, mouseY);
            }
        };
        circuitPanel.setBackground(GuiTextures.BACKGROUND);

        DraggableScrollableWidgetGroup scrollGroup = new DraggableScrollableWidgetGroup(4, 20, dialogWidth - 8, dialogHeight - 24);
        scrollGroup.setYScrollBarWidth(4).setYBarStyle(GuiTextures.BACKGROUND_INVERSE, GuiTextures.BUTTON);

        int currentY = 2;
        InternalSlot slotData = internalInventory[slotIndex];

        scrollGroup.addWidget(new LabelWidget(4, currentY, Component.translatable("gtceu.gui.circuit.title")));
        currentY += 12;

        ItemStackHandler circuitHandler = slotData.getCircuitInventory().storage;
        int circuitSlotX = (scrollGroup.getSize().width - 18) / 2;

        SlotWidget displaySlot = new SlotWidget(
                slotData.getCircuitInventory().storage, 0, circuitSlotX, currentY,
                !ConfigHolder.INSTANCE.machines.ghostCircuit, !ConfigHolder.INSTANCE.machines.ghostCircuit);
        displaySlot.setBackground(new GuiTextureGroup(GuiTextures.SLOT, GuiTextures.INT_CIRCUIT_OVERLAY));
        scrollGroup.addWidget(displaySlot);

        CircuitSlotButton dialogSlotButton = new CircuitSlotButton(circuitSlotX, currentY, 18, 18, circuitHandler, slotIndex, group);
        scrollGroup.addWidget(dialogSlotButton);

        currentY += 22;

        int idx = 0;
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                int finalIdx = idx;
                ButtonWidget btn = new ButtonWidget(4 + (18 * col), currentY + (18 * row), 18, 18,
                        new GuiTextureGroup(GuiTextures.SLOT, new ItemStackTexture(IntCircuitBehaviour.stack(finalIdx)).scale(16f / 18)),
                        clickData -> setCircuitValue(circuitHandler, finalIdx, slotIndex, group));
                scrollGroup.addWidget(btn);
                idx++;
            }
        }
        currentY += 3 * 18;

        int lastRowStartX = (scrollGroup.getSize().width - (6 * 18)) / 2;
        for (int col = 0; col < 6; col++) {
            int finalIdx = idx;
            ButtonWidget btn = new ButtonWidget(lastRowStartX + (18 * col), currentY, 18, 18,
                    new GuiTextureGroup(GuiTextures.SLOT, new ItemStackTexture(IntCircuitBehaviour.stack(finalIdx)).scale(16f / 18)),
                    clickData -> setCircuitValue(circuitHandler, finalIdx, slotIndex, group));
            scrollGroup.addWidget(btn);
            idx++;
        }
        currentY += 18 + 12;

        scrollGroup.addWidget(new LabelWidget(4, currentY, Component.translatable("univom.gui.pattern_modifier.title")));
        currentY += 12;

        int modifierStartX = (scrollGroup.getSize().width - 78) / 2;
        int[] factors = {2, 4, 8, 16};
        for (int i = 0; i < 4; i++) {
            int factor = factors[i];

            ButtonWidget mulBtn = new ButtonWidget(modifierStartX + i * 20, currentY, 18, 18,
                    new GuiTextureGroup(GuiTextures.BUTTON, new TextTexture("x" + factor)), clickData -> {
                if (group.getGui() != null && group.getGui().holder.isRemote()) {
                    group.sendClientAction(780, buf -> {
                        buf.writeVarInt(slotIndex);
                        buf.writeInt(factor);
                        buf.writeBoolean(false);
                    });
                }
            });
            mulBtn.setHoverTooltips(Component.translatable("univom.gui.pattern_modifier.multiply", factor));
            scrollGroup.addWidget(mulBtn);

            ButtonWidget divBtn = new ButtonWidget(modifierStartX + i * 20, currentY + 20, 18, 18,
                    new GuiTextureGroup(GuiTextures.BUTTON, new TextTexture("÷" + factor)), clickData -> {
                if (group.getGui() != null && group.getGui().holder.isRemote()) {
                    group.sendClientAction(780, buf -> {
                        buf.writeVarInt(slotIndex);
                        buf.writeInt(factor);
                        buf.writeBoolean(true);
                    });
                }
            });
            divBtn.setHoverTooltips(Component.translatable("univom.gui.pattern_modifier.divide", factor));
            scrollGroup.addWidget(divBtn);
        }

        circuitPanel.addWidget(scrollGroup);

        circuitPanel.addWidget(new LabelWidget(8, 6, Component.translatable("univom.gui.slot").append(String.valueOf((slotIndex + 1)))));

        ButtonWidget closeButton = new ButtonWidget(dialogWidth - 16 - 4, 4, 16, 16, GuiTextures.CLOSE_ICON, clickData -> {
            group.closeDialogLocal();
            if (group.getGui() != null && group.getGui().holder.isRemote()) {
                group.sendClientAction(CLOSE_DIALOG, buf -> {});
            }
        });
        circuitPanel.addWidget(closeButton);

        return circuitPanel;
    }

    private void setCircuitValue(ItemStackHandler handler, int value, int slotIndex, PatternBufferUIGroup group) {
        handler.setStackInSlot(0, IntCircuitBehaviour.stack(value));
        group.sendClientAction(SET_SLOT_CIRCUIT_VALUE, buf -> {
            buf.writeVarInt(slotIndex);
            buf.writeVarInt(value);
        });
    }

    private void incrementCircuitValue(ItemStackHandler handler, int slotIndex, PatternBufferUIGroup group) {
        ItemStack current = handler.getStackInSlot(0);
        int newValue;
        if (current.isEmpty()) {
            newValue = 0;
            handler.setStackInSlot(0, IntCircuitBehaviour.stack(newValue));
        } else if (IntCircuitBehaviour.isIntegratedCircuit(current)) {
            int cur = IntCircuitBehaviour.getCircuitConfiguration(current);
            int next = (cur + 1) % (IntCircuitBehaviour.CIRCUIT_MAX + 1);
            if (next == 0 && !ConfigHolder.INSTANCE.machines.ghostCircuit) {
                handler.setStackInSlot(0, ItemStack.EMPTY);
                newValue = -1;
            } else {
                newValue = next;
                handler.setStackInSlot(0, IntCircuitBehaviour.stack(newValue));
            }
        } else {
            newValue = 0;
            handler.setStackInSlot(0, IntCircuitBehaviour.stack(newValue));
        }
        if (group.getGui() != null && group.getGui().holder.isRemote()) {
            group.sendClientAction(SET_SLOT_CIRCUIT_VALUE, buf -> {
                buf.writeVarInt(slotIndex);
                buf.writeVarInt(newValue);
            });
        }
    }

    private void decrementCircuitValue(ItemStackHandler handler, int slotIndex, PatternBufferUIGroup group) {
        ItemStack current = handler.getStackInSlot(0);
        int newValue;
        if (current.isEmpty()) {
            if (ConfigHolder.INSTANCE.machines.ghostCircuit) {
                newValue = IntCircuitBehaviour.CIRCUIT_MAX;
                handler.setStackInSlot(0, IntCircuitBehaviour.stack(newValue));
            } else {
                newValue = 0;
                handler.setStackInSlot(0, IntCircuitBehaviour.stack(newValue));
            }
        } else if (IntCircuitBehaviour.isIntegratedCircuit(current)) {
            int cur = IntCircuitBehaviour.getCircuitConfiguration(current);
            if (cur == 0) {
                if (ConfigHolder.INSTANCE.machines.ghostCircuit) {
                    newValue = IntCircuitBehaviour.CIRCUIT_MAX;
                    handler.setStackInSlot(0, IntCircuitBehaviour.stack(newValue));
                } else {
                    handler.setStackInSlot(0, ItemStack.EMPTY);
                    newValue = -1;
                }
            } else {
                newValue = cur - 1;
                handler.setStackInSlot(0, IntCircuitBehaviour.stack(newValue));
            }
        } else {
            newValue = 0;
            handler.setStackInSlot(0, IntCircuitBehaviour.stack(newValue));
        }
        if (group.getGui() != null && group.getGui().holder.isRemote()) {
            group.sendClientAction(SET_SLOT_CIRCUIT_VALUE, buf -> {
                buf.writeVarInt(slotIndex);
                buf.writeVarInt(newValue);
            });
        }
    }

    private void handleRightClick(ItemStackHandler handler, int slotIndex, PatternBufferUIGroup group) {
        int newValue;
        if (ConfigHolder.INSTANCE.machines.ghostCircuit) {
            handler.setStackInSlot(0, ItemStack.EMPTY);
            newValue = -1;
        } else {
            handler.setStackInSlot(0, IntCircuitBehaviour.stack(0));
            newValue = 0;
        }
        if (group.getGui() != null && group.getGui().holder.isRemote()) {
            group.sendClientAction(SET_SLOT_CIRCUIT_VALUE, buf -> {
                buf.writeVarInt(slotIndex);
                buf.writeVarInt(newValue);
            });
        }
    }

    private class CircuitSlotButton extends ButtonWidget {
        private final ItemStackHandler handler;
        private final int slotIndex;
        private final PatternBufferUIGroup group;

        public CircuitSlotButton(int x, int y, int width, int height, ItemStackHandler handler, int slotIndex, PatternBufferUIGroup group) {
            super(x, y, width, height, IGuiTexture.EMPTY, null);
            this.handler = handler;
            this.slotIndex = slotIndex;
            this.group = group;
        }

        @Override
        public boolean mouseWheelMove(double mouseX, double mouseY, double wheelDelta) {
            if (!isMouseOverElement(mouseX, mouseY)) return false;
            if (wheelDelta > 0) {
                incrementCircuitValue(handler, slotIndex, group);
            } else {
                decrementCircuitValue(handler, slotIndex, group);
            }
            return true;
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            if (!isMouseOverElement(mouseX, mouseY)) return false;
            if (button == 0) {
                incrementCircuitValue(handler, slotIndex, group);
                return true;
            } else if (button == 1) {
                handleRightClick(handler, slotIndex, group);
                return true;
            }
            return false;
        }
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
    public boolean isBusy() {
        return false;
    }

    private boolean checkInput(KeyCounter[] inputHolder) {
        for (KeyCounter input : inputHolder) {
            var illegal = input.keySet().stream()
                    .map(AEKey::getType)
                    .map(AEKeyType::getId)
                    .anyMatch(id -> !id.equals(AEKeyType.items().getId()) && !id.equals(AEKeyType.fluids().getId()));
            if (illegal) return false;
        }
        return true;
    }

    @Override
    public ManagedFieldHolder getFieldHolder() {
        return MANAGED_FIELD_HOLDER;
    }

    @Override
    public @Nullable IGrid getGrid() {
        return getMainNode().getGrid();
    }

    @Override
    public InternalInventory getTerminalPatternInventory() {
        return internalPatternInventory;
    }

    @Override
    public PatternContainerGroup getTerminalGroup() {
        if (isFormed()) {
            IMultiController controller = getControllers().first();
            MultiblockMachineDefinition controllerDefinition = controller.self().getDefinition();
            if (!customName.isEmpty()) {
                return new PatternContainerGroup(
                        AEItemKey.of(controllerDefinition.asStack()),
                        Component.literal(customName),
                        Collections.emptyList());
            } else {
                Component groupName = Component.translatable(controllerDefinition.getDescriptionId());
                return new PatternContainerGroup(
                        AEItemKey.of(controllerDefinition.asStack()), groupName, Collections.emptyList());
            }
        } else {
            if (!customName.isEmpty()) {
                return new PatternContainerGroup(
                        AEItemKey.of(GTAEMachines.ME_PATTERN_BUFFER.getItem()),
                        Component.literal(customName),
                        Collections.emptyList());
            } else {
                return new PatternContainerGroup(
                        AEItemKey.of(GTAEMachines.ME_PATTERN_BUFFER.getItem()),
                        GTAEMachines.ME_PATTERN_BUFFER.get().getDefinition().getItem().getDescription(),
                        Collections.emptyList());
            }
        }
    }

    @Override
    public void onMachineRemoved() {
        clearInventory(patternInventory);
        clearInventory(shareInventory.storage);
        super.onMachineRemoved();
    }

    @Override
    public InteractionResult onDataStickShiftUse(Player player, ItemStack dataStick) {
        dataStick.getOrCreateTag().putIntArray("pos", new int[] { getPos().getX(), getPos().getY(), getPos().getZ() });
        return InteractionResult.SUCCESS;
    }

    public record BufferData(Object2LongMap<ItemStack> items, Object2LongMap<FluidStack> fluids) {}

    public BufferData mergeInternalSlots() {
        var items = new Object2LongOpenCustomHashMap<>(ItemStackHashStrategy.comparingAllButCount());
        var fluids = new Object2LongOpenHashMap<FluidStack>();
        for (InternalSlot slot : internalInventory) {
            slot.itemInventory.object2LongEntrySet().fastForEach(e -> items.addTo(e.getKey(), e.getLongValue()));
            slot.fluidInventory.object2LongEntrySet().fastForEach(e -> fluids.addTo(e.getKey(), e.getLongValue()));
        }
        return new BufferData(items, fluids);
    }

    public class InternalSlot implements ITagSerializable<CompoundTag>, IContentChangeAware {

        @Getter
        @Setter
        private Runnable onContentsChanged = () -> {};

        private final Object2LongOpenCustomHashMap<ItemStack> itemInventory = new Object2LongOpenCustomHashMap<>(
                ItemStackHashStrategy.comparingAllButCount());
        private final Object2LongOpenHashMap<FluidStack> fluidInventory = new Object2LongOpenHashMap<>();
        private List<ItemStack> itemStacks = null;
        private List<FluidStack> fluidStacks = null;

        @Getter
        private final NotifiableItemStackHandler circuitInventory;

        public InternalSlot(WirelessMEPatternBufferPartMachine machine) {
            this.circuitInventory = new NotifiableItemStackHandler(machine, 1, IO.IN, IO.NONE) {
                @Override
                public void onContentsChanged() {
                    InternalSlot.this.onContentsChanged();
                }
            };
            this.circuitInventory.setFilter(IntCircuitBehaviour::isIntegratedCircuit);
        }

        public boolean isItemEmpty() {
            return itemInventory.isEmpty();
        }

        public boolean isFluidEmpty() {
            return fluidInventory.isEmpty();
        }

        public void onContentsChanged() {
            itemStacks = null;
            fluidStacks = null;
            onContentsChanged.run();
        }

        private void add(AEKey what, long amount) {
            if (amount <= 0L) return;
            if (what instanceof AEItemKey itemKey) {
                var stack = itemKey.toStack();
                itemInventory.addTo(stack, amount);
            } else if (what instanceof AEFluidKey fluidKey) {
                var stack = fluidKey.toStack(1);
                fluidInventory.addTo(stack, amount);
            }
        }

        public List<ItemStack> getItems() {
            if (itemStacks == null) {
                itemStacks = new ArrayList<>();
                itemInventory.object2LongEntrySet().stream()
                        .map(e -> GTMath.splitStacks(e.getKey(), e.getLongValue()))
                        .forEach(itemStacks::addAll);
            }
            return itemStacks;
        }

        public List<FluidStack> getFluids() {
            if (fluidStacks == null) {
                fluidStacks = new ArrayList<>();
                fluidInventory.object2LongEntrySet().stream()
                        .map(e -> GTMath.splitFluidStacks(e.getKey(), e.getLongValue()))
                        .forEach(fluidStacks::addAll);
            }
            return fluidStacks;
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
                    if (stack.isEmpty() || count == 0) {
                        it.remove();
                        continue;
                    }

                    var key = AEItemKey.of(stack);
                    if (key == null) continue;

                    long inserted = StorageHelper.poweredInsert(energy, networkInv, key, count, actionSource);
                    if (inserted > 0) {
                        count -= inserted;
                        if (count == 0) it.remove();
                        else entry.setValue(count);
                    }
                }

                for (var it = fluidInventory.object2LongEntrySet().iterator(); it.hasNext();) {
                    var entry = it.next();
                    var stack = entry.getKey();
                    var amount = entry.getLongValue();
                    if (stack.isEmpty() || amount == 0) {
                        it.remove();
                        continue;
                    }

                    var key = AEFluidKey.of(stack);
                    if (key == null) continue;

                    long inserted = StorageHelper.poweredInsert(energy, networkInv, key, amount, actionSource);
                    if (inserted > 0) {
                        amount -= inserted;
                        if (amount == 0) it.remove();
                        else entry.setValue(amount);
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
                    if (ingredient instanceof SizedIngredient si) {
                        si.setAmount(amount);
                    } else {
                        items[0].setCount(amount);
                    }
                }
            }
            if (changed) onContentsChanged();
            return left.isEmpty() ? null : left;
        }

        public @Nullable List<FluidIngredient> handleFluidInternal(List<FluidIngredient> left, boolean simulate) {
            boolean changed = false;
            for (var it = left.listIterator(); it.hasNext();) {
                var ingredient = it.next();
                if (ingredient.isEmpty()) {
                    it.remove();
                    continue;
                }

                var fluids = ingredient.getStacks();
                if (fluids.length == 0 || fluids[0].isEmpty()) {
                    it.remove();
                    continue;
                }

                int amount = fluids[0].getAmount();
                for (var it2 = fluidInventory.object2LongEntrySet().iterator(); it2.hasNext();) {
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
                    ingredient.setAmount(amount);
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

            ListTag fluidsTag = new ListTag();
            for (var entry : fluidInventory.object2LongEntrySet()) {
                var ct = entry.getKey().writeToNBT(new CompoundTag());
                ct.putLong("real", entry.getLongValue());
                fluidsTag.add(ct);
            }
            if (!fluidsTag.isEmpty()) tag.put("fluidInventory", fluidsTag);

            if (circuitInventory != null && circuitInventory.storage != null) {
                tag.put("circuitInventory", circuitInventory.storage.serializeNBT());
            }

            return tag;
        }

        @Override
        public void deserializeNBT(CompoundTag tag) {
            ListTag items = tag.getList("inventory", Tag.TAG_COMPOUND);
            for (Tag t : items) {
                if (!(t instanceof CompoundTag ct)) continue;
                var stack = ItemStack.of(ct);
                var count = ct.getLong("real");
                if (!stack.isEmpty() && count > 0) {
                    itemInventory.put(stack, count);
                }
            }

            ListTag fluids = tag.getList("fluidInventory", Tag.TAG_COMPOUND);
            for (Tag t : fluids) {
                if (!(t instanceof CompoundTag ct)) continue;
                var stack = FluidStack.loadFluidStackFromNBT(ct);
                var amount = ct.getLong("real");
                if (!stack.isEmpty() && amount > 0) {
                    fluidInventory.put(stack, amount);
                }
            }

            if (tag.contains("circuitInventory") && circuitInventory != null && circuitInventory.storage != null) {
                circuitInventory.storage.deserializeNBT(tag.getCompound("circuitInventory"));
            }
        }
    }
}
