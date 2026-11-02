package ecosim.model;

/**
 * Immutable 2D vector.
 *
 * A record is used because a vector is pure data: two vectors with the same
 * x and y are interchangeable. Immutability also matters for parallelism:
 * a Vec2 can be shared between threads freely because nobody can change it.
 */
public record Vec2(double x, double y) {

    public static final Vec2 ZERO = new Vec2(0, 0);

    public static Vec2 fromAngle(double radians) {
        return new Vec2(Math.cos(radians), Math.sin(radians));
    }

    public Vec2 plus(Vec2 o)        { return new Vec2(x + o.x, y + o.y); }
    public Vec2 minus(Vec2 o)       { return new Vec2(x - o.x, y - o.y); }
    public Vec2 times(double k)     { return new Vec2(x * k, y * k); }
    public double lengthSq()        { return x * x + y * y; }
    public double length()          { return Math.sqrt(lengthSq()); }
    public double angle()           { return Math.atan2(y, x); }
    public double distSq(Vec2 o)    { double dx = x - o.x, dy = y - o.y; return dx * dx + dy * dy; }
    public double dist(Vec2 o)      { return Math.sqrt(distSq(o)); }

    /** Unit vector in the same direction, or ZERO for a (near) zero vector. */
    public Vec2 normalised() {
        double len = length();
        return len < 1e-9 ? ZERO : new Vec2(x / len, y / len);
    }

    /** Same direction, length capped at max. */
    public Vec2 limit(double max) {
        double len = length();
        return len <= max ? this : times(max / len);
    }

    /** Linear interpolation: t = 0 gives this, t = 1 gives target. */
    public Vec2 lerp(Vec2 target, double t) {
        return new Vec2(x + (target.x - x) * t, y + (target.y - y) * t);
    }
}
