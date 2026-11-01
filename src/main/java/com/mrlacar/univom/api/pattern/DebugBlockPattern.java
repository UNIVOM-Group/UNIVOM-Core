package com.mrlacar.univom.api.pattern;

import com.gregtechceu.gtceu.api.pattern.util.RelativeDirection;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import lombok.Getter;

import java.util.*;

public class DebugBlockPattern {
    private RelativeDirection[] structureDir;
    @Getter
    private String[][] pattern;
    private int[][] aisleRepetitions;
    private final Map<Character, Set<String>> symbolMap = new HashMap<>();

    public DebugBlockPattern() {
        this.structureDir = new RelativeDirection[] {
                RelativeDirection.LEFT, RelativeDirection.UP, RelativeDirection.FRONT
        };
    }

    public DebugBlockPattern(Level world, int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
        this();

        int sizeX = 1 + maxX - minX;
        int sizeY = 1 + maxY - minY;
        this.pattern = new String[sizeX][sizeY];
        this.aisleRepetitions = initAisleRepetitions(sizeX);

        Map<BlockState, Character> blockCharMap = new HashMap<>();
        blockCharMap.put(Blocks.AIR.defaultBlockState(), ' ');

        char symbol = 'A';
        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                StringBuilder builder = new StringBuilder();
                for (int z = minZ; z <= maxZ; z++) {
                    BlockState state = world.getBlockState(new BlockPos(x, y, z));
                    if (!blockCharMap.containsKey(state)) {
                        blockCharMap.put(state, symbol);
                        symbolMap.computeIfAbsent(symbol, k -> new HashSet<>()).add(String.valueOf(symbol));
                        symbol++;
                    }
                    builder.append(blockCharMap.get(state));
                }
                pattern[x - minX][y - minY] = builder.toString();
            }
        }

        changeDir(getDir(Direction.NORTH));
    }

    public static RelativeDirection[] getDir(Direction facing) {
        return switch (facing) {
            case WEST -> new RelativeDirection[] { RelativeDirection.LEFT, RelativeDirection.UP,
                    RelativeDirection.BACK };
            case EAST -> new RelativeDirection[] { RelativeDirection.RIGHT, RelativeDirection.UP,
                    RelativeDirection.FRONT };
            case NORTH -> new RelativeDirection[] { RelativeDirection.BACK, RelativeDirection.UP,
                    RelativeDirection.RIGHT };
            case SOUTH -> new RelativeDirection[] { RelativeDirection.FRONT, RelativeDirection.UP,
                    RelativeDirection.LEFT };
            case DOWN -> new RelativeDirection[] { RelativeDirection.RIGHT, RelativeDirection.BACK,
                    RelativeDirection.UP };
            default -> new RelativeDirection[] { RelativeDirection.LEFT, RelativeDirection.FRONT,
                    RelativeDirection.UP };
        };
    }

    public void changeDir(RelativeDirection... dirs) {
        RelativeDirection charDir = dirs[0];
        RelativeDirection stringDir = dirs[1];
        RelativeDirection aisleDir = dirs[2];

        if (charDir.isSameAxis(stringDir) || stringDir.isSameAxis(aisleDir) || aisleDir.isSameAxis(charDir)) return;

        char[][][] newPattern = allocateNewPattern(charDir, stringDir, aisleDir);

        for (int i = 0; i < pattern.length; i++) {
            for (int j = 0; j < pattern[0].length; j++) {
                for (int k = 0; k < pattern[0][0].length(); k++) {
                    char c = pattern[i][j].charAt(k);
                    int[] coords = transformCoordinates(i, j, k, charDir, stringDir, aisleDir);
                    newPattern[coords[0]][coords[1]][coords[2]] = c;
                }
            }
        }

        rebuildPattern(newPattern);
        this.aisleRepetitions = initAisleRepetitions(pattern.length);
        this.structureDir = new RelativeDirection[] { charDir, stringDir, aisleDir };
    }

    private char[][][] allocateNewPattern(RelativeDirection charDir, RelativeDirection stringDir,
                                          RelativeDirection aisleDir) {
        int xSize = getDimensionLength(aisleDir);
        int ySize = getDimensionLength(stringDir);
        int zSize = getDimensionLength(charDir);
        return new char[xSize][ySize][zSize];
    }

    private int getDimensionLength(RelativeDirection dir) {
        if (structureDir[0].isSameAxis(dir)) return pattern[0][0].length();
        if (structureDir[1].isSameAxis(dir)) return pattern[0].length;
        return pattern.length;
    }

    private int[] transformCoordinates(int i, int j, int k,
                                       RelativeDirection charDir,
                                       RelativeDirection stringDir,
                                       RelativeDirection aisleDir) {
        int x = 0, y = 0, z = 0;
        x = mapCoordinate(i, structureDir[2], aisleDir, stringDir, charDir, pattern.length, x, y, z)[0];
        y = mapCoordinate(i, structureDir[2], aisleDir, stringDir, charDir, pattern.length, x, y, z)[1];
        z = mapCoordinate(i, structureDir[2], aisleDir, stringDir, charDir, pattern.length, x, y, z)[2];
        x = mapCoordinate(j, structureDir[1], aisleDir, stringDir, charDir, pattern[0].length, x, y, z)[0];
        y = mapCoordinate(j, structureDir[1], aisleDir, stringDir, charDir, pattern[0].length, x, y, z)[1];
        z = mapCoordinate(j, structureDir[1], aisleDir, stringDir, charDir, pattern[0].length, x, y, z)[2];
        x = mapCoordinate(k, structureDir[0], aisleDir, stringDir, charDir, pattern[0][0].length(), x, y, z)[0];
        y = mapCoordinate(k, structureDir[0], aisleDir, stringDir, charDir, pattern[0][0].length(), x, y, z)[1];
        z = mapCoordinate(k, structureDir[0], aisleDir, stringDir, charDir, pattern[0][0].length(), x, y, z)[2];
        return new int[] { x, y, z };
    }

    private int[] mapCoordinate(int index, RelativeDirection oldDir,
                                RelativeDirection aisleDir, RelativeDirection stringDir, RelativeDirection charDir,
                                int length, int x, int y, int z) {
        if (oldDir.isSameAxis(aisleDir)) {
            x = (oldDir == aisleDir) ? index : length - index - 1;
        } else if (oldDir.isSameAxis(stringDir)) {
            y = (oldDir == stringDir) ? index : length - index - 1;
        } else if (oldDir.isSameAxis(charDir)) {
            z = (oldDir == charDir) ? index : length - index - 1;
        }
        return new int[] { x, y, z };
    }

    private void rebuildPattern(char[][][] newPattern) {
        this.pattern = new String[newPattern.length][newPattern[0].length];
        for (int i = 0; i < newPattern.length; i++) {
            for (int j = 0; j < newPattern[0].length; j++) {
                this.pattern[i][j] = new String(newPattern[i][j]);
            }
        }
    }

    private int[][] initAisleRepetitions(int length) {
        int[][] reps = new int[length][2];
        for (int[] rep : reps) Arrays.fill(rep, 1);
        return reps;
    }

    public DebugBlockPattern copy() {
        DebugBlockPattern newPattern = new DebugBlockPattern();
        newPattern.structureDir = Arrays.copyOf(this.structureDir, this.structureDir.length);
        newPattern.pattern = new String[this.pattern.length][this.pattern[0].length];
        for (int i = 0; i < this.pattern.length; i++) {
            newPattern.pattern[i] = Arrays.copyOf(this.pattern[i], this.pattern[i].length);
        }
        newPattern.aisleRepetitions = new int[this.aisleRepetitions.length][2];
        for (int i = 0; i < this.aisleRepetitions.length; i++) {
            newPattern.aisleRepetitions[i] = Arrays.copyOf(this.aisleRepetitions[i], 2);
        }
        this.symbolMap.forEach((k, v) -> newPattern.symbolMap.put(k, new HashSet<>(v)));
        return newPattern;
    }
}
