package ecosim.model;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Buckets entities into square cells so "who is near me?" checks only a few
 * cells instead of every entity: O(n) per tick instead of O(n^2).
 *
 * GENERICS: the grid is parameterised on T extends Entity. The bound is what
 * lets the grid call position() on its contents; the parameter is what lets a
 * SpatialGrid<Animal> hand back Animals with no casts. A raw List<Object> grid
 * would compile but push ClassCastExceptions to runtime.
 *
 * Thread-safety contract: rebuilt (written) once per tick in the sequential
 * phase, then only READ during the parallel think phase.
 */
public final class SpatialGrid<T extends Entity> {

    private final double cellSize;
    private final int cols, rows;
    private final List<List<T>> cells;

    public SpatialGrid(double width, double height, double cellSize) {
        if (cellSize <= 0) throw new IllegalArgumentException("cellSize must be > 0");
        this.cellSize = cellSize;
        this.cols = Math.max(1, (int) Math.ceil(width / cellSize));
        this.rows = Math.max(1, (int) Math.ceil(height / cellSize));
        this.cells = new ArrayList<>(cols * rows);
        for (int i = 0; i < cols * rows; i++) cells.add(new ArrayList<>());
    }

    public void rebuild(Iterable<? extends T> items) {    // "producer extends"
        for (List<T> cell : cells) cell.clear();
        for (T item : items) insert(item);
    }

    public void insert(T item) {
        Vec2 p = item.position();
        cells.get(row(p.y()) * cols + col(p.x())).add(item);
    }

    /** Visit every item within radius of centre. */
    public void forEachWithin(Vec2 centre, double radius, Consumer<? super T> visitor) {  // "consumer super"
        double rSq = radius * radius;
        int c0 = col(centre.x() - radius), c1 = col(centre.x() + radius);
        int r0 = row(centre.y() - radius), r1 = row(centre.y() + radius);
        for (int r = r0; r <= r1; r++)
            for (int c = c0; c <= c1; c++)
                for (T item : cells.get(r * cols + c))
                    if (item.position().distSq(centre) <= rSq) visitor.accept(item);
    }

    /**
     * Items within radius that are instances of `type`. The Class<S> token is
     * needed because generics are erased at runtime: we cannot write
     * `instanceof S`, but we can call type.isInstance and type.cast.
     */
    public <S extends T> List<S> within(Vec2 centre, double radius, Class<S> type) {
        List<S> out = new ArrayList<>();
        forEachWithin(centre, radius, item -> {
            if (type.isInstance(item)) out.add(type.cast(item));
        });
        return out;
    }

    private int col(double x) { return Math.max(0, Math.min(cols - 1, (int) (x / cellSize))); }
    private int row(double y) { return Math.max(0, Math.min(rows - 1, (int) (y / cellSize))); }
}
