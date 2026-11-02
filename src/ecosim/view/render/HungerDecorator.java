package ecosim.view.render;

import ecosim.model.Animal;
import ecosim.model.Fox;
import ecosim.model.Vec2;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Ellipse2D;

/** Red ring around starving animals (below 25% energy). Draws after inner, so it sits on top. */
public final class HungerDecorator extends RendererDecorator {

    private static final double STARVING = 0.25;   // below 25% energy counts as starving

    public HungerDecorator(Renderer inner) {
        super(inner);
    }

    @Override
    public void draw(Graphics2D g, Animal a) {
        super.draw(g, a);   // draw the body and other layers first

        if (a.energyFraction() < STARVING) {
            double r = (a instanceof Fox) ? 9 : 7;
            Vec2 p = a.position();
            g.setColor(Color.RED);
            g.setStroke(new BasicStroke(1.5f));
            g.draw(new Ellipse2D.Double(p.x() - r, p.y() - r, r * 2, r * 2));
        }
    }
}