package ecosim.behaviour;

import ecosim.model.Animal;
import ecosim.model.Vec2;
import ecosim.model.WorldView;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/** Small reusable movement calculations shared by the states. Pure functions. */
final class Steering {

    private static final double WALL_MARGIN = 30;

    private Steering() {}

    /** Drift roughly forward with a little random turning, at a relaxed pace. */
    static Vec2 wander(Animal self, WorldView view) {
        double turn = ThreadLocalRandom.current().nextGaussian() * 0.35;
        Vec2 dir = Vec2.fromAngle(self.velocity().angle() + turn);
        return dir.times(self.genome().speed() * 0.45).plus(avoidWalls(self, view));
    }

    /** Head straight for a point at full speed. */
    static Vec2 seek(Animal self, Vec2 target, WorldView view) {
        return target.minus(self.position()).normalised()
                .times(self.genome().speed())
                .plus(avoidWalls(self, view));
    }

    /** Run away from every threat, nearer threats weighted more heavily. */
    static Vec2 flee(Animal self, List<? extends Animal> threats, WorldView view) {
        Vec2 away = threats.stream()
                .map(t -> {
                    Vec2 d = self.position().minus(t.position());
                    return d.normalised().times(1.0 / Math.max(1, d.length()));
                })
                .reduce(Vec2.ZERO, Vec2::plus);
        return away.normalised().times(self.genome().speed())
                .plus(avoidWalls(self, view).times(2));
    }

    /** Gentle push back towards the middle when close to an edge. */
    static Vec2 avoidWalls(Animal self, WorldView view) {
        Vec2 p = self.position();
        double x = 0, y = 0;
        if (p.x() < WALL_MARGIN) x = 1;
        else if (p.x() > view.width() - WALL_MARGIN) x = -1;
        if (p.y() < WALL_MARGIN) y = 1;
        else if (p.y() > view.height() - WALL_MARGIN) y = -1;
        return new Vec2(x, y).times(self.genome().speed() * 0.6);
    }
}
