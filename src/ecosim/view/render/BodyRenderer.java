package ecosim.view.render;

import ecosim.model.Animal;
import ecosim.model.Fox;
import ecosim.model.Genome;
import ecosim.model.Vec2;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;

/**
 * The real component at the bottom of every decorator stack: draws the body.
 * Colour brightness follows the SPEED gene, so you can literally watch the
 * population get faster as evolution happens.
 */
public final class BodyRenderer implements Renderer {

    @Override
    public void draw(Graphics2D g, Animal a) {
        boolean fox = a instanceof Fox;
        double r = fox ? 5.5 : 3.6;
        Vec2 p = a.position();

        double speed01 = (a.genome().speed() - Genome.MIN_SPEED) / (Genome.MAX_SPEED - Genome.MIN_SPEED);
        float bright = (float) (0.45 + 0.55 * Math.min(1, speed01 * 1.8));
        Color body = fox
                ? Color.getHSBColor(0.045f, 0.85f, bright)     // orange-red
                : Color.getHSBColor(0.58f, 0.25f, bright);     // pale blue-grey

        g.setColor(body);
        g.fill(new Ellipse2D.Double(p.x() - r, p.y() - r, r * 2, r * 2));

        // a short line showing which way it is heading
        Vec2 tip = p.plus(a.velocity().normalised().times(r + 3));
        g.setStroke(new BasicStroke(fox ? 1.6f : 1.1f));
        g.draw(new Line2D.Double(p.x(), p.y(), tip.x(), tip.y()));
    }
}
