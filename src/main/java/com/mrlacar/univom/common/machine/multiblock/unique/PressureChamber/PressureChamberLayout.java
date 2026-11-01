package com.mrlacar.univom.common.machine.multiblock.unique.PressureChamber;

import com.gregtechceu.gtceu.api.pattern.util.RelativeDirection;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

import java.util.function.Predicate;

public final class PressureChamberLayout {

    public static final int MAX_INTERIOR = 5;
    public static final int COUNT = MAX_INTERIOR * MAX_INTERIOR * MAX_INTERIOR;
    public static final int DEFAULT = 0;
    public static final int INVALID = -1;
    public static final char CONTROLLER = 'C';
    public static final char SHELL = 'X';
    public static final char INTERIOR = '#';
    private static final int[] RATINGS = { 50, 100, 150, 225, 300 };

    private PressureChamberLayout() {}

    public static int rating(int size) {
        return RATINGS[Math.max(1, Math.min(size, MAX_INTERIOR)) - 1];
    }

    public static int encode(int size, int column, int row) {
        return ((size - 1) * MAX_INTERIOR + column - 1) * MAX_INTERIOR + row - 1;
    }

    public static int size(int layout) {
        return layout / (MAX_INTERIOR * MAX_INTERIOR) + 1;
    }

    public static int column(int layout) {
        return layout / MAX_INTERIOR % MAX_INTERIOR + 1;
    }

    public static int row(int layout) {
        return layout % MAX_INTERIOR + 1;
    }

    public static int scan(BlockPos controller, Direction front, Predicate<BlockPos> interior) {
        if (front.getAxis() == Direction.Axis.Y) {
            return INVALID;
        }
        Direction back = front.getOpposite();
        Direction left = RelativeDirection.LEFT.getActualDirection(front);
        BlockPos origin = controller.relative(back);
        if (!interior.test(origin)) {
            return INVALID;
        }
        int toLeft = depth(origin, left, interior);
        int toRight = depth(origin, left.getOpposite(), interior);
        int size = toLeft + toRight + 1;
        if (size > MAX_INTERIOR) {
            return INVALID;
        }
        int up = depth(origin, Direction.UP, interior);
        int down = depth(origin, Direction.DOWN, interior);
        if (up + down + 1 != size || depth(origin, back, interior) + 1 != size) {
            return INVALID;
        }
        return encode(size, toRight + 1, down + 1);
    }

    public static String[][] aisles(int layout) {
        int size = size(layout);
        int column = column(layout);
        int row = row(layout);
        int edge = size + 2;
        String[][] aisles = new String[edge][edge];
        for (int aisle = 0; aisle < edge; aisle++) {
            for (int y = 0; y < edge; y++) {
                char[] line = new char[edge];
                for (int x = 0; x < edge; x++) {
                    boolean shell = aisle == 0 || aisle == edge - 1 || y == 0 || y == edge - 1 || x == 0 ||
                            x == edge - 1;
                    line[x] = aisle == edge - 1 && y == row && x == column ? CONTROLLER : shell ? SHELL : INTERIOR;
                }
                aisles[aisle][y] = new String(line);
            }
        }
        return aisles;
    }

    public static int casings(int layout) {
        int size = size(layout);
        int edge = size + 2;
        return (edge * edge * edge - size * size * size - 1) / 2;
    }

    private static int depth(BlockPos origin, Direction direction, Predicate<BlockPos> interior) {
        BlockPos.MutableBlockPos pos = origin.mutable();
        for (int i = 0; i < MAX_INTERIOR; i++) {
            if (!interior.test(pos.move(direction))) {
                return i;
            }
        }
        return MAX_INTERIOR;
    }
}
