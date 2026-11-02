package ecosim.view.render;

import ecosim.model.Animal;
import ecosim.model.Fox;
import ecosim.model.Vec2;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Ellipse2D;

/** Draws how far the animal can see. Draws BEFORE delegating so the ring sits underneath the body. */
public final class VisionRingDecorator extends RendererDecorator {

    private static final Color FOX_RING = new Color(255, 120, 60, 55);
    private static final Color RABBIT_RING = new Color(200, 220, 255, 35);

    public VisionRingDecorator(Renderer inner) {
        super(inner);
    }

    @Override
    public void draw(Graphics2D g, Animal a) {
        double v = a.genome().vision();
        Vec2 p = a.position();
        g.setColor(a instanceof Fox ? FOX_RING : RABBIT_RING);
        g.setStroke(new BasicStroke(1f));
        g.draw(new Ellipse2D.Double(p.x() - v, p.y() - v, v * 2, v * 2));
        super.draw(g, a);
    }
}
