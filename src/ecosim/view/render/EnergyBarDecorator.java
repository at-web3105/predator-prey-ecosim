package ecosim.view.render;

import ecosim.model.Animal;
import ecosim.model.Vec2;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;

/** Small bar above the animal: green when well fed, red when starving. Draws AFTER delegating. */
public final class EnergyBarDecorator extends RendererDecorator {

    public EnergyBarDecorator(Renderer inner) {
        super(inner);
    }

    @Override
    public void draw(Graphics2D g, Animal a) {
        super.draw(g, a);
        double f = a.energyFraction();
        Vec2 p = a.position();
        double w = 12, x = p.x() - w / 2, y = p.y() - 11;
        g.setColor(new Color(0, 0, 0, 120));
        g.fill(new Rectangle2D.Double(x, y, w, 2.5));
        g.setColor(Color.getHSBColor((float) (0.33 * f), 0.9f, 0.95f));
        g.fill(new Rectangle2D.Double(x, y, w * f, 2.5));
    }
}
