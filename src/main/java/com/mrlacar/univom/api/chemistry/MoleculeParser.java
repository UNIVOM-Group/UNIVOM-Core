package com.mrlacar.univom.api.chemistry;

import com.gregtechceu.gtceu.api.GTCEuAPI;
import com.gregtechceu.gtceu.api.data.chemical.material.Material;

import net.minecraft.util.GsonHelper;
import net.minecraft.util.Mth;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import org.jetbrains.annotations.Nullable;

import it.unimi.dsi.fastutil.ints.IntArrayList;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class MoleculeParser {

    private static final float COS30 = (float) Math.cos(Math.PI / 6.0);
    private static final int[] NO_ATOMS = new int[0];
    private static final MoleculeParens[] NO_PARENS = new MoleculeParens[0];
    private static final MoleculeCircle[] NO_CIRCLES = new MoleculeCircle[0];
    private static final MoleculeRing[] NO_RINGS = new MoleculeRing[0];
    private static final BondLine[] SINGLE = { BondLine.SOLID };
    private static final int DECLARED_RING_BASE = 1 << 16;
    private static final ThreadLocal<Map<MoleculeSymbol, MoleculeSymbol>> INTERNED = ThreadLocal
            .withInitial(HashMap::new);

    private MoleculeParser() {}

    public static Molecule parse(JsonObject root) {
        INTERNED.get().clear();
        JsonArray contents = GsonHelper.getAsJsonArray(root, "contents");
        List<MoleculeAtom> atoms = new ArrayList<>(contents.size());
        List<MoleculeBond> bonds = new ArrayList<>(contents.size());
        List<MoleculeParens> parens = new ArrayList<>();
        List<MoleculeCircle> circles = new ArrayList<>();
        List<MoleculeRing> rings = new ArrayList<>();

        for (JsonElement entry : contents) {
            JsonObject obj = GsonHelper.convertToJsonObject(entry, "molecule element");
            String type = GsonHelper.getAsString(obj, "type");
            switch (type) {
                case "atom" -> atoms.add(parseAtom(obj));
                case "bond" -> bonds.add(parseBond(obj));
                case "parens" -> parens.add(parseParens(obj));
                case "circle" -> circles.add(parseCircle(obj));
                case "benzene" -> parseRing(obj, rings.size(), atoms, bonds, rings);
                default -> throw new JsonParseException("Unknown molecule element type: " + type);
            }
        }
        resolveDeclaredRings(atoms, bonds, rings);

        return new Molecule(
                atoms.toArray(new MoleculeAtom[0]),
                bonds.toArray(new MoleculeBond[0]),
                parens.isEmpty() ? NO_PARENS : parens.toArray(new MoleculeParens[0]),
                circles.isEmpty() ? NO_CIRCLES : circles.toArray(new MoleculeCircle[0]),
                rings.isEmpty() ? NO_RINGS : rings.toArray(new MoleculeRing[0]));
    }

    private static MoleculeAtom parseAtom(JsonObject obj) {
        int index = GsonHelper.getAsInt(obj, "index");
        MoleculeSymbol center = obj.has("element") ? parseSymbol(obj.get("element")) : MoleculeSymbol.INVISIBLE;
        MoleculeSymbol above = obj.has("above") ? parseSymbol(obj.get("above")) : null;
        MoleculeSymbol right = obj.has("right") ? parseSymbol(obj.get("right")) : null;
        MoleculeSymbol below = obj.has("below") ? parseSymbol(obj.get("below")) : null;
        MoleculeSymbol left = obj.has("left") ? parseSymbol(obj.get("left")) : null;

        float x;
        float y;
        if (obj.has("u") && obj.has("v")) {
            float u = GsonHelper.getAsFloat(obj, "u");
            float v = GsonHelper.getAsFloat(obj, "v");
            x = (u + v) * COS30;
            y = (u - v) * 0.5F;
        } else if (obj.has("x") && obj.has("y")) {
            x = GsonHelper.getAsFloat(obj, "x");
            y = GsonHelper.getAsFloat(obj, "y");
        } else {
            throw new JsonParseException("Atom " + index + " must define either u and v, or x and y");
        }

        return new MoleculeAtom(index, center, above, right, below, left, x, y);
    }

    private static MoleculeBond parseBond(JsonObject obj) {
        int a = GsonHelper.getAsInt(obj, "a");
        int b = GsonHelper.getAsInt(obj, "b");
        boolean centered = GsonHelper.getAsBoolean(obj, "centered", false);
        BondLine[] lines = SINGLE;
        if (obj.has("lines")) {
            JsonArray array = GsonHelper.getAsJsonArray(obj, "lines");
            lines = new BondLine[array.size()];
            for (int i = 0; i < lines.length; i++) {
                String name = array.get(i).getAsString();
                BondLine line = BondLine.byName(name);
                if (line == null) {
                    throw new JsonParseException("Unknown bond line type: " + name);
                }
                lines[i] = line;
            }
        }
        int ring = obj.has("ring") ? DECLARED_RING_BASE + GsonHelper.getAsInt(obj, "ring") : MoleculeBond.NO_RING;
        boolean aromatic = GsonHelper.getAsBoolean(obj, "aromatic", false);
        return new MoleculeBond(a, b, centered, lines, ring, aromatic);
    }

    private static void resolveDeclaredRings(List<MoleculeAtom> atoms, List<MoleculeBond> bonds,
                                             List<MoleculeRing> rings) {
        Map<Integer, IntArrayList> declared = new LinkedHashMap<>();
        for (MoleculeBond bond : bonds) {
            if (bond.ring() < DECLARED_RING_BASE) {
                continue;
            }
            IntArrayList members = declared.computeIfAbsent(bond.ring(), key -> new IntArrayList());
            if (!members.contains(bond.a())) {
                members.add(bond.a());
            }
            if (!members.contains(bond.b())) {
                members.add(bond.b());
            }
        }
        if (declared.isEmpty()) {
            return;
        }

        Map<Integer, Integer> remap = new LinkedHashMap<>();
        for (Map.Entry<Integer, IntArrayList> entry : declared.entrySet()) {
            int[] members = entry.getValue().toIntArray();
            remap.put(entry.getKey(), rings.size());
            rings.add(new MoleculeRing(members, ringRadius(atoms, members)));
        }
        for (int i = 0; i < bonds.size(); i++) {
            MoleculeBond bond = bonds.get(i);
            Integer mapped = remap.get(bond.ring());
            if (mapped != null) {
                bonds.set(i, new MoleculeBond(bond.a(), bond.b(), bond.centered(), bond.lines(), mapped,
                        bond.aromaticDouble()));
            }
        }
    }

    private static float ringRadius(List<MoleculeAtom> atoms, int[] members) {
        float centroidX = 0.0F;
        float centroidY = 0.0F;
        int counted = 0;
        for (int index : members) {
            MoleculeAtom atom = findAtom(atoms, index);
            if (atom != null) {
                centroidX += atom.x();
                centroidY += atom.y();
                counted++;
            }
        }
        if (counted == 0) {
            return 0.0F;
        }
        centroidX /= counted;
        centroidY /= counted;

        float total = 0.0F;
        for (int index : members) {
            MoleculeAtom atom = findAtom(atoms, index);
            if (atom != null) {
                float dx = atom.x() - centroidX;
                float dy = atom.y() - centroidY;
                total += Mth.sqrt(dx * dx + dy * dy);
            }
        }
        return total / counted * 2.0F / 3.0F;
    }

    @Nullable
    private static MoleculeAtom findAtom(List<MoleculeAtom> atoms, int index) {
        for (MoleculeAtom atom : atoms) {
            if (atom.index() == index) {
                return atom;
            }
        }
        return null;
    }

    private static MoleculeParens parseParens(JsonObject obj) {
        String sub = GsonHelper.getAsString(obj, "sub", "");
        String sup = GsonHelper.getAsString(obj, "sup", "");
        return new MoleculeParens(sub, sup, readIndices(obj));
    }

    private static MoleculeCircle parseCircle(JsonObject obj) {
        int[] atoms = readIndices(obj);
        if (obj.has("x") && obj.has("y")) {
            return MoleculeCircle.scaled(GsonHelper.getAsFloat(obj, "x"), GsonHelper.getAsFloat(obj, "y"), atoms);
        }
        return new MoleculeCircle(
                GsonHelper.getAsFloat(obj, "a00"),
                GsonHelper.getAsFloat(obj, "a01"),
                GsonHelper.getAsFloat(obj, "a10"),
                GsonHelper.getAsFloat(obj, "a11"),
                atoms);
    }

    private static void parseRing(JsonObject obj, int ringId, List<MoleculeAtom> atoms, List<MoleculeBond> bonds,
                                  List<MoleculeRing> rings) {
        int start = GsonHelper.getAsInt(obj, "start_index");
        boolean clockwise = GsonHelper.getAsBoolean(obj, "clockwise", true);

        float x0;
        float y0;
        float x1;
        float y1;
        if (obj.has("u0")) {
            float u0 = GsonHelper.getAsFloat(obj, "u0");
            float v0 = GsonHelper.getAsFloat(obj, "v0");
            float u1 = GsonHelper.getAsFloat(obj, "u1");
            float v1 = GsonHelper.getAsFloat(obj, "v1");
            x0 = (u0 + v0) * COS30;
            y0 = (u0 - v0) * 0.5F;
            x1 = (u1 + v1) * COS30;
            y1 = (u1 - v1) * 0.5F;
        } else {
            x0 = GsonHelper.getAsFloat(obj, "x0");
            y0 = GsonHelper.getAsFloat(obj, "y0");
            x1 = GsonHelper.getAsFloat(obj, "x1");
            y1 = GsonHelper.getAsFloat(obj, "y1");
        }

        float step = clockwise ? -Mth.TWO_PI / 6.0F : Mth.TWO_PI / 6.0F;
        float cos = Mth.cos(step);
        float sin = Mth.sin(step);
        float dx = x1 - x0;
        float dy = y1 - y0;
        float radius = Mth.sqrt(dx * dx + dy * dy) * 2.0F / 3.0F;

        int[] members = new int[6];
        float px = x0;
        float py = y0;
        for (int i = 0; i < 6; i++) {
            members[i] = start + i;
            atoms.add(new MoleculeAtom(start + i, MoleculeSymbol.INVISIBLE, null, null, null, null, px, py));
            if (i > 0) {
                float rx = dx * cos - dy * sin;
                dy = dx * sin + dy * cos;
                dx = rx;
            }
            px += dx;
            py += dy;
        }

        for (int i = 0; i < 6; i++) {
            boolean aromaticDouble = (i & 1) == 1;
            BondLine[] lines = aromaticDouble ? new BondLine[] { BondLine.SOLID, BondLine.SOLID } : SINGLE;
            bonds.add(new MoleculeBond(members[i], members[(i + 1) % 6], false, lines, ringId, aromaticDouble));
        }

        rings.add(new MoleculeRing(members, radius));
    }

    private static int[] readIndices(JsonObject obj) {
        if (!obj.has("atoms")) {
            return NO_ATOMS;
        }
        JsonArray array = GsonHelper.getAsJsonArray(obj, "atoms");
        int[] result = new int[array.size()];
        for (int i = 0; i < result.length; i++) {
            result[i] = array.get(i).getAsInt();
        }
        return result;
    }

    private static MoleculeSymbol parseSymbol(JsonElement element) {
        if (element.isJsonArray()) {
            JsonArray array = element.getAsJsonArray();
            MoleculeSymbol base = parseSymbol(array.get(0));
            return withCount(base, array.get(1).getAsInt());
        }
        if (element.isJsonPrimitive()) {
            return create(element.getAsString(), false, 0, null);
        }
        JsonObject obj = GsonHelper.convertToJsonObject(element, "element");
        String symbol = GsonHelper.getAsString(obj, "symbol", "");
        boolean invisible = GsonHelper.getAsBoolean(obj, "invisible", false);
        int color = obj.has("color") ? parseColor(obj.get("color")) : 0;
        Material material = null;
        if (obj.has("material")) {
            material = GTCEuAPI.materialManager.getMaterial(GsonHelper.getAsString(obj, "material"));
        }
        return create(symbol, invisible, color, material);
    }

    private static MoleculeSymbol create(String symbol, boolean invisible, int color, @Nullable Material material) {
        if (invisible || symbol.isEmpty()) {
            return color == 0 && material == null ? MoleculeSymbol.INVISIBLE :
                    intern(new MoleculeSymbol(symbol, true, color, material));
        }
        Material bound = material != null ? material : ChemSymbols.materialFor(baseSymbol(symbol));
        return intern(new MoleculeSymbol(symbol, false, color, bound));
    }

    private static String baseSymbol(String symbol) {
        int end = 0;
        while (end < symbol.length() && Character.isLetter(symbol.charAt(end))) {
            end++;
        }
        return end == symbol.length() ? symbol : symbol.substring(0, end);
    }

    private static MoleculeSymbol withCount(MoleculeSymbol base, int count) {
        if (count <= 1) {
            return base;
        }
        StringBuilder builder = new StringBuilder(base.text());
        String digits = Integer.toString(count);
        for (int i = 0; i < digits.length(); i++) {
            builder.append((char) (digits.charAt(i) - '0' + '₀'));
        }
        return intern(new MoleculeSymbol(builder.toString(), base.invisible(), base.explicitColor(),
                base.material()));
    }

    private static MoleculeSymbol intern(MoleculeSymbol symbol) {
        return INTERNED.get().computeIfAbsent(symbol, java.util.function.Function.identity());
    }

    private static int parseColor(JsonElement element) {
        JsonElement value = element.isJsonObject() ? element.getAsJsonObject().get("color") : element;
        if (value == null || value.isJsonNull()) {
            return 0;
        }
        JsonPrimitive primitive = value.getAsJsonPrimitive();
        if (primitive.isString()) {
            String text = primitive.getAsString();
            if (text.startsWith("#")) {
                text = text.substring(1);
            }
            return 0xFF000000 | (int) Long.parseLong(text, 16);
        }
        return 0xFF000000 | primitive.getAsInt();
    }
}
