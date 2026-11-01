package com.mrlacar.univom.client.tooltip;

import com.mrlacar.univom.api.chemistry.BondLine;
import com.mrlacar.univom.api.chemistry.Molecule;
import com.mrlacar.univom.api.chemistry.MoleculeAtom;
import com.mrlacar.univom.api.chemistry.MoleculeBond;
import com.mrlacar.univom.api.chemistry.MoleculeCircle;
import com.mrlacar.univom.api.chemistry.MoleculeParens;
import com.mrlacar.univom.api.chemistry.MoleculeRing;
import com.mrlacar.univom.api.chemistry.MoleculeSymbol;
import com.mrlacar.univom.config.ConfigHolder;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.Reference2IntMap;
import it.unimi.dsi.fastutil.objects.Reference2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.Reference2ObjectMap;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import org.joml.Matrix4f;

@OnlyIn(Dist.CLIENT)
public final class MoleculeLayout {

    private static final BondLine[] SINGLE_LINE = { BondLine.SOLID };
    private static final Reference2ObjectMap<Molecule, MoleculeLayout> CACHE = new Reference2ObjectOpenHashMap<>(64);

    private static int cachedScale = -1;
    private static boolean cachedAromaticCircle;

    private final int width;
    private final int height;
    private final String[] labelText;
    private final int[] labelX;
    private final int[] labelY;
    private final int[] labelPalette;
    private final SmoothShapes shapes;
    private final MoleculeSymbol[] palette;
    private final int[] paletteColors;

    private MoleculeLayout(int width, int height, String[] labelText, int[] labelX, int[] labelY, int[] labelPalette,
                           SmoothShapes shapes, MoleculeSymbol[] palette) {
        this.width = width;
        this.height = height;
        this.labelText = labelText;
        this.labelX = labelX;
        this.labelY = labelY;
        this.labelPalette = labelPalette;
        this.shapes = shapes;
        this.palette = palette;
        this.paletteColors = new int[palette.length];
    }

    public static void invalidate() {
        CACHE.clear();
        cachedScale = -1;
    }

    public static MoleculeLayout of(Molecule molecule) {
        ConfigHolder.MoleculeTooltipConfigs config = ConfigHolder.INSTANCE.moleculeTooltipSetting;
        if (cachedScale != config.moleculeScale || cachedAromaticCircle != config.aromaticCircle) {
            CACHE.clear();
            cachedScale = config.moleculeScale;
            cachedAromaticCircle = config.aromaticCircle;
        }
        MoleculeLayout layout = CACHE.get(molecule);
        if (layout == null) {
            layout = new Builder(molecule, cachedScale, cachedAromaticCircle).build();
            CACHE.put(molecule, layout);
        }
        return layout;
    }

    public int width() {
        return width;
    }

    public int height() {
        return height;
    }

    public void renderText(Font font, int x, int y, Matrix4f matrix, MultiBufferSource.BufferSource buffer) {
        resolvePalette();
        for (int i = 0; i < labelText.length; i++) {
            font.drawInBatch(labelText[i], x + labelX[i], y + labelY[i], paletteColors[labelPalette[i]], false,
                    matrix, buffer, Font.DisplayMode.NORMAL, 0, LightTexture.FULL_BRIGHT);
        }
    }

    public void renderImage(int x, int y, GuiGraphics graphics) {
        resolvePalette();
        shapes.render(x, y, graphics, paletteColors);
    }

    private void resolvePalette() {
        for (int i = 0; i < palette.length; i++) {
            paletteColors[i] = ChemColor.forSymbol(palette[i]);
        }
    }

    private static final class Builder {

        private static final int MAX_BOXES = 10;
        private static final float TINY = 1.0E-4F;

        private final Molecule molecule;
        private final int scale;
        private final boolean aromaticCircle;
        private final Font font;
        private final int lineHeight;
        private final float bondWidth;
        private final float bondGap;
        private final float wedgeHalf;
        private final float hashStep;

        private final Reference2IntMap<MoleculeSymbol> widths = new Reference2IntOpenHashMap<>(64);
        private final Reference2IntMap<MoleculeSymbol> paletteIndex = new Reference2IntOpenHashMap<>(64);
        private final ObjectArrayList<MoleculeSymbol> palette = new ObjectArrayList<>();
        private final ObjectArrayList<String> labelText = new ObjectArrayList<>();
        private final IntArrayList labelX = new IntArrayList();
        private final IntArrayList labelY = new IntArrayList();
        private final IntArrayList labelPalette = new IntArrayList();
        private final SmoothShapes.Collector shapes = new SmoothShapes.Collector();
        private final float[] boxes = new float[MAX_BOXES * 4];

        private int boxCount;
        private float trimStart;
        private float trimEnd;
        private float startX;
        private float startY;
        private int padLeft;
        private int padTop;
        private float[] screenX;
        private float[] screenY;

        Builder(Molecule molecule, int scale, boolean aromaticCircle) {
            this.molecule = molecule;
            this.scale = scale;
            this.aromaticCircle = aromaticCircle;
            this.font = Minecraft.getInstance().font;
            this.lineHeight = font.lineHeight;
            this.bondWidth = Math.max(1.0F, scale / 16.0F);
            this.bondGap = Math.max(bondWidth * 2.0F, scale / 10.0F);
            this.wedgeHalf = Math.max(bondWidth * 1.5F, scale * 0.075F);
            this.hashStep = Math.max(bondWidth * 3.0F, scale * 0.15F);
            this.paletteIndex.defaultReturnValue(-1);
            this.widths.defaultReturnValue(0);
        }

        MoleculeLayout build() {
            MoleculeAtom[] atoms = molecule.atoms();
            measureWidths(atoms);

            float minX = Float.MAX_VALUE;
            float maxX = -Float.MAX_VALUE;
            float minY = Float.MAX_VALUE;
            float maxY = -Float.MAX_VALUE;
            for (MoleculeAtom atom : atoms) {
                minX = Math.min(minX, atom.x());
                maxX = Math.max(maxX, atom.x());
                minY = Math.min(minY, atom.y());
                maxY = Math.max(maxY, atom.y());
            }

            boolean atomAtTop = false;
            boolean atomAtTopTop = false;
            boolean atomAtBotBot = false;
            boolean atomAtLefLef = false;
            for (MoleculeAtom atom : atoms) {
                if (atom.isInvisible()) {
                    continue;
                }
                if (Math.abs(maxY - atom.y()) < 0.1F) {
                    atomAtTop = true;
                    if (atom.above() != null) {
                        atomAtTopTop = true;
                    }
                }
                if (Math.abs(minY - atom.y()) < 0.1F && atom.below() != null) {
                    atomAtBotBot = true;
                }
                if (Math.abs(minX - atom.x()) < 0.1F && atom.left() != null) {
                    atomAtLefLef = true;
                }
            }
            boolean parenAtLeft = hasParensAtLeft(atoms, minX);

            this.startX = minX;
            this.startY = maxY;
            this.padLeft = 8 + (atomAtLefLef ? 12 : 0) + (parenAtLeft ? 6 : 0);
            this.padTop = atomAtTopTop ? lineHeight * 3 / 2 : atomAtTop ? lineHeight / 2 : 3;

            int sizeX = Mth.ceil((maxX - minX) * scale);
            int sizeY = Mth.ceil((maxY - minY) * scale);
            int width = sizeX + 32 + (atomAtLefLef ? 12 : 0) + (parenAtLeft ? 6 : 0);
            int height = sizeY + 20 + (atomAtBotBot ? 10 : 0) + (atomAtTopTop ? 10 : 0);

            this.screenX = new float[atoms.length];
            this.screenY = new float[atoms.length];
            for (int i = 0; i < atoms.length; i++) {
                screenX[i] = toScreenX(atoms[i].x());
                screenY[i] = toScreenY(atoms[i].y());
            }

            emitLabels(atoms);
            emitBonds(atoms);
            emitCircles();
            emitParens(atoms);

            return new MoleculeLayout(width, height,
                    labelText.toArray(new String[0]),
                    labelX.toIntArray(), labelY.toIntArray(), labelPalette.toIntArray(),
                    shapes.build(), palette.toArray(new MoleculeSymbol[0]));
        }

        private void measureWidths(MoleculeAtom[] atoms) {
            for (MoleculeAtom atom : atoms) {
                measure(atom.center());
                measure(atom.above());
                measure(atom.right());
                measure(atom.below());
                measure(atom.left());
            }
        }

        private void measure(MoleculeSymbol symbol) {
            if (symbol == null || widths.containsKey(symbol)) {
                return;
            }
            widths.put(symbol, symbol.invisible() ? 0 : font.width(symbol.text()));
        }

        private boolean hasParensAtLeft(MoleculeAtom[] atoms, float minX) {
            MoleculeParens[] groups = molecule.parens();
            if (groups.length == 0) {
                return false;
            }
            for (MoleculeAtom atom : atoms) {
                if (Math.abs(minX - atom.x()) > 0.1F) {
                    continue;
                }
                for (MoleculeParens group : groups) {
                    if (contains(group.atoms(), atom.index())) {
                        return true;
                    }
                }
            }
            return false;
        }

        private float toScreenX(float value) {
            return (value - startX) * scale + padLeft;
        }

        private float toScreenY(float value) {
            return -((value - startY) * scale) + padTop;
        }

        private int paletteOf(MoleculeSymbol symbol) {
            int existing = paletteIndex.getInt(symbol);
            if (existing >= 0) {
                return existing;
            }
            int index = palette.size();
            palette.add(symbol);
            paletteIndex.put(symbol, index);
            return index;
        }

        private void emitLabels(MoleculeAtom[] atoms) {
            for (int i = 0; i < atoms.length; i++) {
                MoleculeAtom atom = atoms[i];
                int centerWidth = widths.getInt(atom.center());
                int sx = Mth.floor(screenX[i]);
                int sy = Mth.floor(screenY[i]);

                addLabel(atom.center(), sx + Mth.floor(-centerWidth / 2.0F) + 1, sy + 1);
                if (atom.right() != null) {
                    addLabel(atom.right(), sx + Mth.floor(centerWidth / 2.0F) + 1, sy + 1);
                }
                if (atom.left() != null) {
                    addLabel(atom.left(), sx - widths.getInt(atom.left()) + Mth.floor(-centerWidth / 2.0F), sy + 1);
                }
                if (atom.above() != null) {
                    addLabel(atom.above(), sx + Mth.floor(-widths.getInt(atom.above()) / 2.0F) + 1,
                            sy - lineHeight + 1);
                }
                if (atom.below() != null) {
                    addLabel(atom.below(), sx + Mth.floor(-widths.getInt(atom.below()) / 2.0F) + 1,
                            sy + lineHeight + 1);
                }
            }
        }

        private void addLabel(MoleculeSymbol symbol, int x, int y) {
            if (symbol.invisible() || symbol.text().isEmpty()) {
                return;
            }
            labelText.add(symbol.text());
            labelX.add(x);
            labelY.add(y);
            labelPalette.add(paletteOf(symbol));
        }

        private void emitBonds(MoleculeAtom[] atoms) {
            int[] slotOf = slotIndex(atoms);
            for (MoleculeBond bond : molecule.bonds()) {
                MoleculeAtom atomA = molecule.atom(bond.a());
                MoleculeAtom atomB = molecule.atom(bond.b());
                if (atomA == null || atomB == null) {
                    continue;
                }
                int slotA = slotOf[bond.a()];
                int slotB = slotOf[bond.b()];
                BondLine[] lines = bond.lines();
                if (aromaticCircle && bond.ring() != MoleculeBond.NO_RING && bond.aromaticDouble()) {
                    lines = SINGLE_LINE;
                }
                drawBond(atomA, atomB, screenX[slotA] + 0.5F, screenY[slotA] + lineHeight / 2.0F + 0.5F,
                        screenX[slotB] + 0.5F, screenY[slotB] + lineHeight / 2.0F + 0.5F, bond.centered(), lines);
            }
        }

        private int[] slotIndex(MoleculeAtom[] atoms) {
            int highest = 0;
            for (MoleculeAtom atom : atoms) {
                highest = Math.max(highest, atom.index());
            }
            int[] slots = new int[highest + 1];
            for (int i = 0; i < atoms.length; i++) {
                slots[atoms[i].index()] = i;
            }
            return slots;
        }

        private void drawBond(MoleculeAtom atomA, MoleculeAtom atomB, float ax, float ay, float bx, float by,
                              boolean centered, BondLine[] lines) {
            float dx = bx - ax;
            float dy = by - ay;
            float length = Mth.sqrt(dx * dx + dy * dy);
            if (length < TINY || lines.length == 0) {
                return;
            }
            float normalX = -dy / length;
            float normalY = dx / length;
            int paletteA = paletteOf(atomA.center());
            int paletteB = paletteOf(atomB.center());
            collectBoxes(atomA, ax, ay, atomB, bx, by);

            int thickness = 0;
            for (BondLine line : lines) {
                thickness += line.thickness;
            }
            float starting = centered ? (thickness - 1) / 2.0F : (float) ((thickness - 1) / 2);
            int done = lines[0].thickness > 1 ? 1 : 0;

            for (int i = 0; i < lines.length; done += lines[i].thickness > 1 ? 3 : 1, i++) {
                float shift = (done - starting) * bondGap;
                float startPx = ax + normalX * shift;
                float startPy = ay + normalY * shift;
                float endPx = bx + normalX * shift;
                float endPy = by + normalY * shift;
                if (!trim(startPx, startPy, endPx, endPy)) {
                    continue;
                }
                float half = bondWidth * 0.5F;
                switch (lines[i]) {
                    case SOLID -> taper(startPx, startPy, endPx, endPy, half, half, paletteA, paletteB);
                    case THICK -> taper(startPx, startPy, endPx, endPy, wedgeHalf, wedgeHalf, paletteA, paletteB);
                    case OUTWARD -> taper(startPx, startPy, endPx, endPy, half, wedgeHalf, paletteA, paletteB);
                    case DOTTED -> dotted(startPx, startPy, endPx, endPy, half, paletteA, paletteB);
                    case INWARD -> hashes(startPx, startPy, endPx, endPy, half, paletteA, paletteB);
                }
            }
        }

        private void taper(float ax, float ay, float bx, float by, float halfA, float halfB, int paletteA,
                           int paletteB) {
            float length = Mth.sqrt((bx - ax) * (bx - ax) + (by - ay) * (by - ay));
            if (length < TINY) {
                return;
            }
            float from = trimStart <= TINY ? -halfA / length : trimStart;
            float to = trimEnd >= 1.0F - TINY ? 1.0F + halfB / length : trimEnd;
            if (paletteA == paletteB) {
                segment(ax, ay, bx, by, from, to, halfA, halfB, paletteA);
                return;
            }
            float mid = Mth.clamp(0.5F, from, to);
            if (mid > from) {
                segment(ax, ay, bx, by, from, mid, halfA, halfB, paletteA);
            }
            if (to > mid) {
                segment(ax, ay, bx, by, mid, to, halfA, halfB, paletteB);
            }
        }

        private void segment(float ax, float ay, float bx, float by, float from, float to, float halfA, float halfB,
                             int paletteId) {
            shapes.stroke(Mth.lerp(from, ax, bx), Mth.lerp(from, ay, by), Mth.lerp(to, ax, bx), Mth.lerp(to, ay, by),
                    Mth.lerp(from, halfA, halfB), Mth.lerp(to, halfA, halfB), paletteId);
        }

        private void dotted(float ax, float ay, float bx, float by, float half, int paletteA, int paletteB) {
            float length = Mth.sqrt((bx - ax) * (bx - ax) + (by - ay) * (by - ay));
            float span = (trimEnd - trimStart) * length;
            if (span < TINY) {
                return;
            }
            int count = Math.max(1, Mth.floor(span / hashStep));
            float period = (trimEnd - trimStart) / count;
            for (int i = 0; i < count; i++) {
                float from = trimStart + period * i;
                float to = from + period * 0.5F;
                segment(ax, ay, bx, by, from, to, half, half, from + to < 1.0F ? paletteA : paletteB);
            }
        }

        private void hashes(float ax, float ay, float bx, float by, float half, int paletteA, int paletteB) {
            float dx = bx - ax;
            float dy = by - ay;
            float length = Mth.sqrt(dx * dx + dy * dy);
            if (length < TINY) {
                return;
            }
            float span = (trimEnd - trimStart) * length;
            int count = Math.max(2, Mth.floor(span / hashStep) + 1);
            float normalX = -dy / length;
            float normalY = dx / length;
            for (int i = 0; i < count; i++) {
                float at = trimStart + (trimEnd - trimStart) * i / (count - 1);
                float reach = Mth.lerp(at, half, wedgeHalf);
                float px = ax + dx * at;
                float py = ay + dy * at;
                shapes.stroke(px - normalX * reach, py - normalY * reach, px + normalX * reach, py + normalY * reach,
                        half, half, at < 0.5F ? paletteA : paletteB);
            }
        }

        private void collectBoxes(MoleculeAtom atomA, float ax, float ay, MoleculeAtom atomB, float bx, float by) {
            boxCount = 0;
            if (atomA.isInvisible() && atomB.isInvisible()) {
                return;
            }
            addBoxes(atomA, ax, ay);
            addBoxes(atomB, bx, by);
        }

        private void addBoxes(MoleculeAtom atom, float cx, float cy) {
            float band = lineHeight * 2.0F / 3.0F;
            float centerWidth = widths.getInt(atom.center());
            if (!atom.isInvisible()) {
                addBox(cx, cy, centerWidth * 2.0F / 3.0F, band);
            }
            if (atom.above() != null) {
                addBox(cx, cy - lineHeight - 1, widths.getInt(atom.above()) * 2.0F / 3.0F, band);
            }
            if (atom.below() != null) {
                addBox(cx, cy + lineHeight + 1, widths.getInt(atom.below()) * 2.0F / 3.0F, band);
            }
            if (atom.right() != null) {
                float right = widths.getInt(atom.right());
                addBox(cx + (centerWidth + right) / 2.0F + 1, cy, right * 2.0F / 3.0F, band);
            }
            if (atom.left() != null) {
                float left = widths.getInt(atom.left());
                addBox(cx - (centerWidth + left) / 2.0F - 1, cy, left * 2.0F / 3.0F, band);
            }
        }

        private void addBox(float cx, float cy, float halfWidth, float halfHeight) {
            if (halfWidth <= 0.0F || halfHeight <= 0.0F || boxCount == MAX_BOXES) {
                return;
            }
            int base = boxCount++ * 4;
            boxes[base] = cx - halfWidth;
            boxes[base + 1] = cy - halfHeight;
            boxes[base + 2] = cx + halfWidth;
            boxes[base + 3] = cy + halfHeight;
        }

        private boolean trim(float ax, float ay, float bx, float by) {
            trimStart = 0.0F;
            trimEnd = 1.0F;
            float dx = bx - ax;
            float dy = by - ay;
            for (int pass = 0; pass < 2; pass++) {
                for (int i = 0; i < boxCount; i++) {
                    int base = i * 4;
                    float enter = 0.0F;
                    float exit = 1.0F;
                    if (Math.abs(dx) < TINY) {
                        if (ax < boxes[base] || ax > boxes[base + 2]) {
                            continue;
                        }
                    } else {
                        float low = (boxes[base] - ax) / dx;
                        float high = (boxes[base + 2] - ax) / dx;
                        enter = Math.max(enter, Math.min(low, high));
                        exit = Math.min(exit, Math.max(low, high));
                    }
                    if (Math.abs(dy) < TINY) {
                        if (ay < boxes[base + 1] || ay > boxes[base + 3]) {
                            continue;
                        }
                    } else {
                        float low = (boxes[base + 1] - ay) / dy;
                        float high = (boxes[base + 3] - ay) / dy;
                        enter = Math.max(enter, Math.min(low, high));
                        exit = Math.min(exit, Math.max(low, high));
                    }
                    if (exit <= enter) {
                        continue;
                    }
                    if (enter <= trimStart && exit > trimStart) {
                        trimStart = exit;
                    }
                    if (exit >= trimEnd && enter < trimEnd) {
                        trimEnd = enter;
                    }
                }
            }
            return trimEnd - trimStart > 1.0E-3F;
        }

        private void emitCircles() {
            for (MoleculeCircle circle : molecule.circles()) {
                emitCircle(circle.a00(), circle.a01(), circle.a10(), circle.a11(), circle.atoms());
            }
            if (!aromaticCircle) {
                return;
            }
            for (MoleculeRing ring : molecule.rings()) {
                emitCircle(ring.radius(), 0.0F, 0.0F, ring.radius(), ring.atoms());
            }
        }

        private void emitCircle(float a00, float a01, float a10, float a11, int[] members) {
            float centroidX = 0.0F;
            float centroidY = 0.0F;
            int counted = 0;
            int paletteId = -1;
            for (int index : members) {
                MoleculeAtom atom = molecule.atom(index);
                if (atom == null) {
                    continue;
                }
                centroidX += atom.x();
                centroidY += atom.y();
                counted++;
                if (paletteId < 0) {
                    paletteId = paletteOf(atom.center());
                }
            }
            if (counted == 0) {
                return;
            }
            centroidX /= counted;
            centroidY /= counted;

            float m00 = a00 * scale;
            float m10 = a10 * scale;
            float m01 = -a01 * scale;
            float m11 = -a11 * scale;
            if (Math.abs(m00 * m11 - m10 * m01) < TINY) {
                return;
            }
            shapes.ellipse(toScreenX(centroidX) + 0.5F, toScreenY(centroidY) + lineHeight / 2.0F + 0.5F,
                    m00, m01, m10, m11, bondWidth * 0.5F, paletteId);
        }

        private void emitParens(MoleculeAtom[] atoms) {
            MoleculeParens[] groups = molecule.parens();
            if (groups.length == 0) {
                return;
            }
            int defaultPalette = paletteOf(MoleculeSymbol.INVISIBLE);
            for (MoleculeParens group : groups) {
                int minX = Integer.MAX_VALUE;
                int minY = Integer.MAX_VALUE;
                int maxX = Integer.MIN_VALUE;
                int maxY = Integer.MIN_VALUE;
                boolean found = false;
                for (int slot = 0; slot < atoms.length; slot++) {
                    MoleculeAtom atom = atoms[slot];
                    if (!contains(group.atoms(), atom.index())) {
                        continue;
                    }
                    found = true;
                    int centerWidth = widths.getInt(atom.center());
                    int extentLeft = centerWidth / 2;
                    int extentRight = centerWidth / 2;
                    int extentUp = 1;
                    int extentDown = lineHeight + 1;
                    if (atom.right() != null) {
                        extentRight += 1 + widths.getInt(atom.right());
                    }
                    if (atom.left() != null) {
                        extentLeft += 1 + widths.getInt(atom.left());
                    }
                    if (atom.above() != null) {
                        extentUp += 1 + lineHeight;
                        int half = widths.getInt(atom.above()) / 2;
                        extentLeft = Math.max(extentLeft, half);
                        extentRight = Math.max(extentRight, half);
                    }
                    if (atom.below() != null) {
                        extentDown += 1 + lineHeight;
                        int half = widths.getInt(atom.below()) / 2;
                        extentLeft = Math.max(extentLeft, half);
                        extentRight = Math.max(extentRight, half);
                    }
                    int anchorX = Mth.floor(screenX[slot]);
                    int anchorY = Mth.floor(screenY[slot]);
                    minX = Math.min(minX, anchorX - extentLeft);
                    maxX = Math.max(maxX, anchorX + extentRight);
                    minY = Math.min(minY, anchorY - extentUp);
                    maxY = Math.max(maxY, anchorY + extentDown);
                }
                if (!found) {
                    continue;
                }
                minX -= 2;
                minY -= 1;
                maxX += 2;
                maxY += 1;

                shapes.rect(minX - 2, minY, minX + 3, minY + 1, defaultPalette);
                shapes.rect(minX - 2, maxY, minX + 3, maxY + 1, defaultPalette);
                shapes.rect(maxX - 2, minY, maxX + 3, minY + 1, defaultPalette);
                shapes.rect(maxX - 2, maxY, maxX + 3, maxY + 1, defaultPalette);
                shapes.rect(minX - 2, minY, minX - 1, maxY + 1, defaultPalette);
                shapes.rect(maxX + 2, minY, maxX + 3, maxY + 1, defaultPalette);

                if (!group.sub().isEmpty()) {
                    labelText.add(group.sub());
                    labelX.add(maxX + 7);
                    labelY.add(maxY - 2);
                    labelPalette.add(defaultPalette);
                }
                if (!group.sup().isEmpty()) {
                    labelText.add(group.sup());
                    labelX.add(maxX + 7);
                    labelY.add(minY - 4);
                    labelPalette.add(defaultPalette);
                }
            }
        }

        private static boolean contains(int[] values, int target) {
            for (int value : values) {
                if (value == target) {
                    return true;
                }
            }
            return false;
        }
    }
}
