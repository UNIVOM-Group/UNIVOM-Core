package com.mrlacar.univom.api.chemistry;

import org.jetbrains.annotations.Nullable;

public final class Molecule {

    private static final MoleculeAtom[] NO_ATOMS = new MoleculeAtom[0];

    private final MoleculeAtom[] atoms;
    private final MoleculeAtom[] byIndex;
    private final MoleculeBond[] bonds;
    private final MoleculeParens[] parens;
    private final MoleculeCircle[] circles;
    private final MoleculeRing[] rings;

    public Molecule(MoleculeAtom[] atoms, MoleculeBond[] bonds, MoleculeParens[] parens, MoleculeCircle[] circles,
                    MoleculeRing[] rings) {
        this.atoms = atoms;
        this.bonds = bonds;
        this.parens = parens;
        this.circles = circles;
        this.rings = rings;

        int highest = -1;
        for (MoleculeAtom atom : atoms) {
            if (atom.index() > highest) {
                highest = atom.index();
            }
        }
        this.byIndex = highest < 0 ? NO_ATOMS : new MoleculeAtom[highest + 1];
        for (MoleculeAtom atom : atoms) {
            this.byIndex[atom.index()] = atom;
        }
    }

    public MoleculeAtom[] atoms() {
        return atoms;
    }

    public MoleculeBond[] bonds() {
        return bonds;
    }

    public MoleculeParens[] parens() {
        return parens;
    }

    public MoleculeCircle[] circles() {
        return circles;
    }

    public MoleculeRing[] rings() {
        return rings;
    }

    public boolean isEmpty() {
        return atoms.length == 0;
    }

    @Nullable
    public MoleculeAtom atom(int index) {
        return index >= 0 && index < byIndex.length ? byIndex[index] : null;
    }
}
