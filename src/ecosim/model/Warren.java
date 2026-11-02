package ecosim.model;

/**
 * A rabbit burrow. Rabbits inside one cannot be seen by foxes.
 * Refuges like this are what stop real predator-prey systems from crashing
 * to extinction: there is always a reserve of prey left to rebuild from.
 */
public record Warren(Vec2 centre, double radius) {

    public boolean contains(Vec2 p) {
        return centre.distSq(p) <= radius * radius;
    }
}
