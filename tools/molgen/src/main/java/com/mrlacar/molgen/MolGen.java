package com.mrlacar.molgen;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.openscience.cdk.aromaticity.Aromaticity;
import org.openscience.cdk.aromaticity.ElectronDonation;
import org.openscience.cdk.aromaticity.Kekulization;
import org.openscience.cdk.graph.Cycles;
import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IBond;
import org.openscience.cdk.interfaces.IRingSet;
import org.openscience.cdk.layout.StructureDiagramGenerator;
import org.openscience.cdk.silent.SilentChemObjectBuilder;
import org.openscience.cdk.smiles.SmilesParser;
import org.openscience.cdk.tools.manipulator.AtomContainerManipulator;

import javax.vecmath.Point2d;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public final class MolGen {

    private static final Aromaticity AROMATICITY = new Aromaticity(ElectronDonation.daylight(),
            Cycles.or(Cycles.all(), Cycles.all(6)));
    private static final char[] SUPERSCRIPTS = { '⁰', '¹', '²', '³', '⁴', '⁵',
            '⁶', '⁷', '⁸', '⁹' };

    public static void main(String[] args) throws IOException {
        Path input = Path.of(args.length > 0 ? args[0] : "materials.tsv");
        Path outputRoot = Path.of(args.length > 1 ? args[1] : "out");

        if (!Files.exists(input)) {
            System.err.println("Input file not found: " + input.toAbsolutePath());
            System.exit(1);
        }

        SmilesParser parser = new SmilesParser(SilentChemObjectBuilder.getInstance());
        var gson = new GsonBuilder().setPrettyPrinting().create();

        int written = 0;
        int failed = 0;
        for (String raw : Files.readAllLines(input, StandardCharsets.UTF_8)) {
            String line = raw.trim();
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }
            String[] parts = line.split("\\s+", 3);
            if (parts.length < 2) {
                System.err.println("Skipping malformed line: " + line);
                failed++;
                continue;
            }
            String id = parts[0];
            String smiles = parts[1].trim();
            String repeat = parts.length > 2 ? parts[2].trim() : "";
            try {
                JsonObject molecule = convert(parser, smiles, repeat);
                Path target = resolve(outputRoot, id);
                Files.createDirectories(target.getParent());
                Files.writeString(target, gson.toJson(molecule) + System.lineSeparator(), StandardCharsets.UTF_8);
                written++;
            } catch (Exception e) {
                System.err.println("Failed on " + id + " (" + smiles + "): " + e.getMessage());
                failed++;
            }
        }

        System.out.println("Wrote " + written + " molecule(s) to " + outputRoot.toAbsolutePath()
                + (failed > 0 ? ", " + failed + " failure(s)" : ""));
    }

    private static Path resolve(Path root, String id) {
        int colon = id.indexOf(':');
        String namespace = colon < 0 ? "gtceu" : id.substring(0, colon);
        String name = colon < 0 ? id : id.substring(colon + 1);
        return root.resolve("assets").resolve(namespace).resolve("molecules").resolve(name + ".json");
    }

    private static JsonObject convert(SmilesParser parser, String smiles, String repeat) throws Exception {
        IAtomContainer molecule = parser.parseSmiles(smiles);
        AtomContainerManipulator.percieveAtomTypesAndConfigureAtoms(molecule);
        AROMATICITY.apply(molecule);
        Kekulization.kekulize(molecule);

        StructureDiagramGenerator generator = new StructureDiagramGenerator();
        generator.setMolecule(molecule, false);
        generator.generateCoordinates();
        IAtomContainer laidOut = generator.getMolecule();

        double unit = medianBondLength(laidOut);
        double centerX = 0.0;
        double centerY = 0.0;
        int count = laidOut.getAtomCount();
        for (int i = 0; i < count; i++) {
            Point2d point = laidOut.getAtom(i).getPoint2d();
            centerX += point.x;
            centerY += point.y;
        }
        centerX /= count;
        centerY /= count;

        JsonArray contents = new JsonArray();
        for (int i = 0; i < count; i++) {
            IAtom atom = laidOut.getAtom(i);
            Point2d point = atom.getPoint2d();
            JsonObject node = new JsonObject();
            node.addProperty("type", "atom");
            node.addProperty("index", i);

            String symbol = atom.getSymbol();
            boolean skeletal = "C".equals(symbol) && charge(atom) == 0 && atom.getMassNumber() == null;
            if (!skeletal) {
                node.addProperty("element", symbol + chargeSuffix(charge(atom)));
                int hydrogens = implicitHydrogens(atom);
                if (hydrogens > 0) {
                    String side = hydrogenSide(laidOut, atom);
                    if (hydrogens == 1) {
                        node.addProperty(side, "H");
                    } else {
                        JsonArray counted = new JsonArray();
                        counted.add("H");
                        counted.add(hydrogens);
                        node.add(side, counted);
                    }
                }
            }

            node.addProperty("x", round((point.x - centerX) / unit));
            node.addProperty("y", round((point.y - centerY) / unit));
            contents.add(node);
        }

        IRingSet ringSet = Cycles.mcb(laidOut).toRingSet();
        List<IAtomContainer> aromaticRings = new ArrayList<>();
        for (IAtomContainer ring : ringSet.atomContainers()) {
            if (isAromatic(ring)) {
                aromaticRings.add(ring);
            }
        }

        for (IBond bond : laidOut.bonds()) {
            int from = laidOut.indexOf(bond.getBegin());
            int to = laidOut.indexOf(bond.getEnd());
            int lines = switch (bond.getOrder()) {
                case DOUBLE -> 2;
                case TRIPLE -> 3;
                case QUADRUPLE -> 4;
                default -> 1;
            };

            IAtomContainer ring = lines == 2 ? smallestRing(ringSet, bond) : null;
            if (ring != null && !pointsInward(bond, ring)) {
                int swap = from;
                from = to;
                to = swap;
            }

            JsonObject node = new JsonObject();
            node.addProperty("type", "bond");
            node.addProperty("a", from);
            node.addProperty("b", to);
            JsonArray styles = new JsonArray();
            for (int i = 0; i < lines; i++) {
                styles.add("solid");
            }
            node.add("lines", styles);
            if (lines == 2 && ring == null) {
                node.addProperty("centered", true);
            }

            int aromaticIndex = indexOfRing(aromaticRings, bond);
            if (aromaticIndex >= 0) {
                node.addProperty("ring", aromaticIndex);
                if (lines == 2) {
                    node.addProperty("aromatic", true);
                }
            }
            contents.add(node);
        }

        if (!repeat.isEmpty()) {
            JsonObject node = new JsonObject();
            node.addProperty("type", "parens");
            node.addProperty("sub", repeat);
            JsonArray members = new JsonArray();
            for (int i = 0; i < count; i++) {
                members.add(i);
            }
            node.add("atoms", members);
            contents.add(node);
        }

        JsonObject root = new JsonObject();
        root.add("contents", contents);
        return root;
    }

    private static boolean isAromatic(IAtomContainer ring) {
        for (IBond bond : ring.bonds()) {
            if (!bond.isAromatic()) {
                return false;
            }
        }
        return ring.getBondCount() > 0;
    }

    private static int indexOfRing(List<IAtomContainer> rings, IBond bond) {
        for (int i = 0; i < rings.size(); i++) {
            if (rings.get(i).contains(bond)) {
                return i;
            }
        }
        return -1;
    }

    private static IAtomContainer smallestRing(IRingSet rings, IBond bond) {
        IAtomContainer best = null;
        for (IAtomContainer ring : rings.atomContainers()) {
            if (!ring.contains(bond)) {
                continue;
            }
            if (best == null || ring.getAtomCount() < best.getAtomCount()) {
                best = ring;
            }
        }
        return best;
    }

    private static boolean pointsInward(IBond bond, IAtomContainer ring) {
        Point2d begin = bond.getBegin().getPoint2d();
        Point2d end = bond.getEnd().getPoint2d();
        double dx = end.x - begin.x;
        double dy = end.y - begin.y;

        double centroidX = 0.0;
        double centroidY = 0.0;
        int count = ring.getAtomCount();
        for (int i = 0; i < count; i++) {
            Point2d point = ring.getAtom(i).getPoint2d();
            centroidX += point.x;
            centroidY += point.y;
        }
        centroidX = centroidX / count - (begin.x + end.x) / 2.0;
        centroidY = centroidY / count - (begin.y + end.y) / 2.0;

        return -dy * centroidX + dx * centroidY > 0.0;
    }

    private static String chargeSuffix(int charge) {
        if (charge == 0) {
            return "";
        }
        int magnitude = Math.abs(charge);
        StringBuilder builder = new StringBuilder();
        if (magnitude > 1) {
            for (char digit : Integer.toString(magnitude).toCharArray()) {
                builder.append(SUPERSCRIPTS[digit - '0']);
            }
        }
        builder.append(charge > 0 ? '⁺' : '⁻');
        return builder.toString();
    }

    private static int charge(IAtom atom) {
        Integer value = atom.getFormalCharge();
        return value == null ? 0 : value;
    }

    private static int implicitHydrogens(IAtom atom) {
        Integer value = atom.getImplicitHydrogenCount();
        return value == null ? 0 : value;
    }

    private static String hydrogenSide(IAtomContainer molecule, IAtom atom) {
        List<IAtom> neighbours = molecule.getConnectedAtomsList(atom);
        if (neighbours.isEmpty()) {
            return "right";
        }
        double sum = 0.0;
        for (IAtom neighbour : neighbours) {
            sum += neighbour.getPoint2d().x - atom.getPoint2d().x;
        }
        return sum > 0.0 ? "left" : "right";
    }

    private static double medianBondLength(IAtomContainer molecule) {
        List<Double> lengths = new ArrayList<>();
        for (IBond bond : molecule.bonds()) {
            Point2d a = bond.getBegin().getPoint2d();
            Point2d b = bond.getEnd().getPoint2d();
            lengths.add(a.distance(b));
        }
        if (lengths.isEmpty()) {
            return 1.0;
        }
        Double[] sorted = lengths.toArray(new Double[0]);
        Arrays.sort(sorted);
        double median = sorted[sorted.length / 2];
        return median > 1.0E-6 ? median : 1.0;
    }

    private static double round(double value) {
        return Double.parseDouble(String.format(Locale.ROOT, "%.5f", value));
    }
}
