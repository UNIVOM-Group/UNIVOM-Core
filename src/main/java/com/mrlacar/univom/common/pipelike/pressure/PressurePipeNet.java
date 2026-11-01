package com.mrlacar.univom.common.pipelike.pressure;

import com.gregtechceu.gtceu.api.pipenet.LevelPipeNet;
import com.gregtechceu.gtceu.api.pipenet.Node;
import com.gregtechceu.gtceu.api.pipenet.PipeNet;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntIterator;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

public class PressurePipeNet extends PipeNet<PressurePipeProperties> {

    private static final String GAS_TAG = "Gas";
    private static final String AMOUNT_TAG = "Amount";
    private static final String RATING_TAG = "Rating";
    private static final String VOLUME_TAG = "Volume";
    private static final String SAFETY_TAG = "Safety";
    private static final String ATTACHMENTS_TAG = "Attachments";
    private static final String ENDPOINT_TAG = "Endpoint";
    private static final String NODE_TAG = "Node";
    private static final String BURST_TAG = "Burst";

    private final Int2IntOpenHashMap ratings = new Int2IntOpenHashMap();
    private final Int2IntOpenHashMap safetyPercents = new Int2IntOpenHashMap();
    private final Map<BlockPos, Attachment> attachments = new HashMap<>();
    @Nullable
    private ResourceLocation gas;
    private long amount;
    private long volume;
    private int minRating;
    private int minSafetyPercent;
    @Nullable
    private final LevelPressurePipeNet owner;
    @Nullable
    private BlockPos weakestHint;

    public PressurePipeNet(LevelPipeNet<PressurePipeProperties, ? extends PipeNet<PressurePipeProperties>> world) {
        super(world);
        this.owner = world instanceof LevelPressurePipeNet level ? level : null;
    }

    public static boolean compatible(@Nullable ResourceLocation first, @Nullable ResourceLocation second) {
        return first == null || second == null || first.equals(second);
    }

    @Nullable
    public ResourceLocation getGas() {
        return gas;
    }

    public long getAmount() {
        return amount;
    }

    public long getVolume() {
        return volume;
    }

    public int getMinRating() {
        return minRating;
    }

    public int getMinSafetyPercent() {
        return minSafetyPercent;
    }

    public boolean accepts(@Nullable ResourceLocation candidate) {
        return compatible(gas, candidate);
    }

    public double getPressure() {
        return volume <= 0 ? 0.0D : (double) amount / volume;
    }

    public long capacityAt(long centibar) {
        return volume * centibar / 100L;
    }

    public long insert(ResourceLocation candidate, long maxAmount, long ceilingCentibar) {
        if (maxAmount <= 0 || volume <= 0 || !accepts(candidate)) {
            return 0;
        }
        long inserted = Math.min(maxAmount, capacityAt(ceilingCentibar) - amount);
        if (inserted <= 0) {
            return 0;
        }
        gas = candidate;
        amount += inserted;
        worldData.setDirty();
        enforceLimits();
        monitor();
        return inserted;
    }

    public long extract(long maxAmount) {
        long extracted = Math.min(maxAmount, amount);
        if (extracted <= 0) {
            return 0;
        }
        amount -= extracted;
        if (amount == 0) {
            gas = null;
        }
        worldData.setDirty();
        return extracted;
    }

    public void vent() {
        vent(null);
    }

    public void vent(@Nullable BlockPos at) {
        if (amount == 0 && gas == null) {
            return;
        }
        double load = minRating > 0 ? getPressure() / minRating : 0.0D;
        amount = 0;
        gas = null;
        worldData.setDirty();
        ServerLevel level = owner == null ? null : owner.getWorld();
        if (at != null && level != null) {
            PressureEffects.vent(level, at, load);
        }
    }

    public boolean isOverpressured() {
        return amount > 0 && minRating > 0 && amount >= volume * minRating;
    }

    public boolean isWarning() {
        return amount > 0 && minRating > 0 && amount * 10L > volume * minRating * 9L;
    }

    @Nullable
    public Attachment getAttachment(BlockPos endpoint) {
        return attachments.isEmpty() ? null : attachments.get(endpoint);
    }

    public boolean isAttachedVia(BlockPos endpoint, BlockPos node) {
        Attachment attachment = getAttachment(endpoint);
        return attachment != null && attachment.node().equals(node);
    }

    public void attach(BlockPos endpoint, BlockPos node, int rating, int attachedVolume, float burstFactor) {
        if (!containsNode(node)) {
            return;
        }
        Attachment attachment = new Attachment(node.immutable(),
                new PressurePipeProperties(Math.max(1, rating), Math.max(1, attachedVolume), 0), burstFactor);
        Attachment previous = attachments.put(endpoint.immutable(), attachment);
        if (attachment.equals(previous)) {
            return;
        }
        if (previous != null) {
            account(previous.data(), -1);
        }
        account(attachment.data(), 1);
        worldData.setDirty();
        enforceLimits();
        monitor();
    }

    public void detach(BlockPos endpoint) {
        Attachment removed = attachments.isEmpty() ? null : attachments.get(endpoint);
        if (removed == null) {
            return;
        }
        vent(endpoint);
        attachments.remove(endpoint);
        account(removed.data(), -1);
        worldData.setDirty();
    }

    @Nullable
    public BlockPos findWeakest(Predicate<BlockPos> loaded) {
        BlockPos hint = weakestHint;
        if (hint != null && ratingAt(hint) == minRating && loaded.test(hint)) {
            return hint;
        }
        for (Map.Entry<BlockPos, Attachment> entry : attachments.entrySet()) {
            if (entry.getValue().data().ratingBar() == minRating && loaded.test(entry.getKey())) {
                weakestHint = entry.getKey();
                return weakestHint;
            }
        }
        for (Map.Entry<BlockPos, Node<PressurePipeProperties>> entry : getAllNodes().entrySet()) {
            if (entry.getValue().data.ratingBar() == minRating && loaded.test(entry.getKey())) {
                weakestHint = entry.getKey();
                return weakestHint;
            }
        }
        return null;
    }

    public boolean isJoined(BlockPos nodePos, Direction facing) {
        Node<PressurePipeProperties> node = getNodeAt(nodePos);
        Node<PressurePipeProperties> neighbour = getNodeAt(nodePos.relative(facing));
        return node != null && neighbour != null && !node.isBlocked(facing) &&
                !neighbour.isBlocked(facing.getOpposite());
    }

    @Override
    public void removeNode(BlockPos nodePos) {
        if (containsNode(nodePos)) {
            vent(nodePos);
            dropAttachments(nodePos);
        }
        super.removeNode(nodePos);
    }

    @Override
    public void updateBlockedConnections(BlockPos nodePos, Direction facing, boolean isBlocked) {
        if (isBlocked) {
            BlockPos endpoint = nodePos.relative(facing);
            if (isAttachedVia(endpoint, nodePos)) {
                detach(endpoint);
            } else if (isJoined(nodePos, facing)) {
                vent(nodePos);
            }
        }
        super.updateBlockedConnections(nodePos, facing, isBlocked);
        Node<PressurePipeProperties> node = getNodeAt(nodePos);
        if (isBlocked && node != null && !node.isBlocked(facing)) {
            node.openConnections &= ~(1 << facing.ordinal());
            worldData.setDirty();
        }
    }

    @Override
    public void updateNodeData(BlockPos nodePos, PressurePipeProperties data) {
        Node<PressurePipeProperties> node = getNodeAt(nodePos);
        if (node == null) {
            return;
        }
        account(node.data, -1);
        super.updateNodeData(nodePos, data);
        account(node.data, 1);
        enforceLimits();
    }

    @Override
    protected void addNodeSilently(BlockPos nodePos, Node<PressurePipeProperties> node) {
        Node<PressurePipeProperties> previous = getNodeAt(nodePos);
        super.addNodeSilently(nodePos, node);
        if (previous != null) {
            account(previous.data, -1);
        }
        account(node.data, 1);
    }

    @Override
    protected Node<PressurePipeProperties> removeNodeWithoutRebuilding(BlockPos nodePos) {
        Node<PressurePipeProperties> removed = super.removeNodeWithoutRebuilding(nodePos);
        if (removed != null) {
            account(removed.data, -1);
        }
        return removed;
    }

    @Override
    protected void transferNodeData(Map<BlockPos, Node<PressurePipeProperties>> transferredNodes,
                                    PipeNet<PressurePipeProperties> parentNet) {
        PressurePipeNet parent = parentNet instanceof PressurePipeNet net ? net : null;
        if (parent != null) {
            parent.moveAttachments(transferredNodes.keySet(), this);
        }
        super.transferNodeData(transferredNodes, parentNet);
        if (parent == null) {
            return;
        }
        if (parent.isValid()) {
            parent.vent();
            vent();
            return;
        }
        if (parent.amount > 0) {
            if (amount > 0 && !compatible(gas, parent.gas)) {
                vent();
            } else {
                gas = parent.gas;
                amount += parent.amount;
            }
            parent.amount = 0;
            parent.gas = null;
            worldData.setDirty();
        }
        enforceLimits();
        monitor();
    }

    @Override
    protected boolean areNodesCustomContactable(PressurePipeProperties first, PressurePipeProperties second,
                                                PipeNet<PressurePipeProperties> secondNodePipeNet) {
        return !(secondNodePipeNet instanceof PressurePipeNet other) || other == this ||
                compatible(gas, other.gas);
    }

    @Override
    protected void onNodeConnectionsUpdate() {
        super.onNodeConnectionsUpdate();
        enforceLimits();
        monitor();
    }

    @Override
    protected void writeNodeData(PressurePipeProperties nodeData, CompoundTag tagCompound) {
        tagCompound.putInt(RATING_TAG, nodeData.ratingBar());
        tagCompound.putInt(VOLUME_TAG, nodeData.volume());
        if (nodeData.isSafetyValve()) {
            tagCompound.putInt(SAFETY_TAG, nodeData.safetyPercent());
        }
    }

    @Override
    protected PressurePipeProperties readNodeData(CompoundTag tagCompound) {
        return new PressurePipeProperties(Math.max(1, tagCompound.getInt(RATING_TAG)),
                Math.max(1, tagCompound.getInt(VOLUME_TAG)), Math.max(0, tagCompound.getInt(SAFETY_TAG)));
    }

    @Override
    public CompoundTag serializeNBT() {
        CompoundTag tag = super.serializeNBT();
        if (gas != null && amount > 0) {
            tag.putString(GAS_TAG, gas.toString());
            tag.putLong(AMOUNT_TAG, amount);
        }
        if (!attachments.isEmpty()) {
            ListTag list = new ListTag();
            for (Map.Entry<BlockPos, Attachment> entry : attachments.entrySet()) {
                Attachment attachment = entry.getValue();
                CompoundTag attachmentTag = new CompoundTag();
                attachmentTag.putLong(ENDPOINT_TAG, entry.getKey().asLong());
                attachmentTag.putLong(NODE_TAG, attachment.node().asLong());
                attachmentTag.putInt(RATING_TAG, attachment.data().ratingBar());
                attachmentTag.putInt(VOLUME_TAG, attachment.data().volume());
                attachmentTag.putFloat(BURST_TAG, attachment.burstFactor());
                list.add(attachmentTag);
            }
            tag.put(ATTACHMENTS_TAG, list);
        }
        return tag;
    }

    @Override
    public void deserializeNBT(CompoundTag nbt) {
        ratings.clear();
        safetyPercents.clear();
        attachments.clear();
        volume = 0;
        minRating = 0;
        minSafetyPercent = 0;
        super.deserializeNBT(nbt);
        ResourceLocation stored = nbt.contains(GAS_TAG, Tag.TAG_STRING) ?
                ResourceLocation.tryParse(nbt.getString(GAS_TAG)) : null;
        long storedAmount = nbt.getLong(AMOUNT_TAG);
        if (stored != null && storedAmount > 0) {
            gas = stored;
            amount = storedAmount;
        } else {
            gas = null;
            amount = 0;
        }
        ListTag list = nbt.getList(ATTACHMENTS_TAG, Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag attachmentTag = list.getCompound(i);
            BlockPos node = BlockPos.of(attachmentTag.getLong(NODE_TAG));
            if (containsNode(node)) {
                Attachment attachment = new Attachment(node, new PressurePipeProperties(
                        Math.max(1, attachmentTag.getInt(RATING_TAG)), Math.max(1, attachmentTag.getInt(VOLUME_TAG)),
                        0), attachmentTag.getFloat(BURST_TAG));
                attachments.put(BlockPos.of(attachmentTag.getLong(ENDPOINT_TAG)), attachment);
                account(attachment.data(), 1);
            }
        }
    }

    private int ratingAt(BlockPos pos) {
        Node<PressurePipeProperties> node = getNodeAt(pos);
        if (node != null) {
            return node.data.ratingBar();
        }
        Attachment attachment = getAttachment(pos);
        return attachment == null ? -1 : attachment.data().ratingBar();
    }

    private void moveAttachments(Set<BlockPos> nodes, PressurePipeNet target) {
        if (attachments.isEmpty()) {
            return;
        }
        Iterator<Map.Entry<BlockPos, Attachment>> iterator = attachments.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<BlockPos, Attachment> entry = iterator.next();
            Attachment attachment = entry.getValue();
            if (nodes.contains(attachment.node())) {
                iterator.remove();
                account(attachment.data(), -1);
                target.attachments.put(entry.getKey(), attachment);
                target.account(attachment.data(), 1);
            }
        }
    }

    private void dropAttachments(BlockPos nodePos) {
        if (attachments.isEmpty()) {
            return;
        }
        Iterator<Attachment> iterator = attachments.values().iterator();
        while (iterator.hasNext()) {
            Attachment attachment = iterator.next();
            if (attachment.node().equals(nodePos)) {
                iterator.remove();
                account(attachment.data(), -1);
            }
        }
    }

    private void monitor() {
        if (amount > 0 && owner != null) {
            owner.watch(this);
        }
    }

    private void enforceLimits() {
        if (amount <= 0 || minSafetyPercent <= 0 || minRating <= 0) {
            return;
        }
        long limit = volume * minRating * minSafetyPercent / 100L;
        if (amount > limit) {
            amount = Math.max(0, limit);
            if (amount == 0) {
                gas = null;
            }
            worldData.setDirty();
        }
    }

    private void account(@Nullable PressurePipeProperties data, int sign) {
        if (data == null) {
            return;
        }
        volume += (long) sign * data.volume();
        adjust(ratings, data.ratingBar(), sign);
        minRating = lowest(ratings);
        if (data.isSafetyValve()) {
            adjust(safetyPercents, data.safetyPercent(), sign);
            minSafetyPercent = lowest(safetyPercents);
        }
    }

    private static void adjust(Int2IntOpenHashMap counts, int key, int sign) {
        if (counts.addTo(key, sign) + sign <= 0) {
            counts.remove(key);
        }
    }

    private static int lowest(Int2IntOpenHashMap counts) {
        int lowest = 0;
        for (IntIterator iterator = counts.keySet().iterator(); iterator.hasNext();) {
            int key = iterator.nextInt();
            if (lowest == 0 || key < lowest) {
                lowest = key;
            }
        }
        return lowest;
    }

    public record Attachment(BlockPos node, PressurePipeProperties data, float burstFactor) {}
}
