package ecosim.model;

import java.util.Arrays;
import java.util.Optional;
import java.util.stream.IntStream;

/**
 * A grid of grass cells, each holding a growth level from 0 (bare) to 1 (lush).
 * Grass is not an Entity: there are thousands of cells and they never move,
 * so a flat double[] is far cheaper than thousands of objects.
 */
public final class GrassField {

    private final int cols, rows;
    private final double cellSize;
    private final double[] level;

    public GrassField(double width, double height, double cellSize, double initialLevel) {
        this.cellSize = cellSize;
        this.cols = (int) Math.ceil(width / cellSize);
        this.rows = (int) Math.ceil(height / cellSize);
        this.level = new double[cols * rows];
        Arrays.fill(level, initialLevel);
    }

    public int cols()          { return cols; }
    public int rows()          { return rows; }
    public double cellSize()   { return cellSize; }
    public double level(int c, int r) { return level[r * cols + c]; }

    public double levelAt(Vec2 p) {
        return level[index(p)];
    }

    /** Centre of the lushest cell within radius (only if it is worth walking to). */
    public Optional<Vec2> richestNear(Vec2 p, double radius) {
        int c0 = col(p.x() - radius), c1 = col(p.x() + radius);
        int r0 = row(p.y() - radius), r1 = row(p.y() + radius);
        double best = 0.25;
        Vec2 bestPos = null;
        double rSq = radius * radius;
        for (int r = r0; r <= r1; r++) {
            for (int c = c0; c <= c1; c++) {
                Vec2 centre = new Vec2((c + 0.5) * cellSize, (r + 0.5) * cellSize);
                double l = level[r * cols + c];
                if (l > best && centre.distSq(p) <= rSq) {
                    best = l;
                    bestPos = centre;
                }
            }
        }
        return Optional.ofNullable(bestPos);
    }

    /** Eat up to `bite` from the cell under p. Returns how much was eaten. Sequential phase only. */
    double graze(Vec2 p, double bite) {
        int i = index(p);
        double eaten = Math.min(bite, level[i]);
        level[i] -= eaten;
        return eaten;
    }

    /**
     * Logistic regrowth: bare ground recovers slowly, half-grown grass fastest.
     * Rows are independent (each thread writes a disjoint slice of the array),
     * so a parallel IntStream is safe here without any locking.
     */
    void regrow(double rate, boolean parallel) {
        IntStream rowsStream = IntStream.range(0, rows);
        if (parallel) rowsStream = rowsStream.parallel();
        rowsStream.forEach(r -> {
            int base = r * cols;
            for (int c = 0; c < cols; c++) {
                double l = level[base + c];
                level[base + c] = Math.min(1.0, l + rate * (l + 0.04) * (1 - l));
            }
        });
    }

    /** Fraction of maximum possible grass, 0..1. */
    public double coverage() {
        return Arrays.stream(level).sum() / level.length;
    }

    private int col(double x) { return Math.max(0, Math.min(cols - 1, (int) (x / cellSize))); }
    private int row(double y) { return Math.max(0, Math.min(rows - 1, (int) (y / cellSize))); }
    private int index(Vec2 p) { return row(p.y()) * cols + col(p.x()); }
}
