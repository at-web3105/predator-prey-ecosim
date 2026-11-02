package ecosim.view.render;

import ecosim.model.Animal;
import ecosim.model.Vec2;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;

/** Writes the current behaviour state (wander / graze / hunt / flee / mate) next to the animal. */
public final class StateLabelDecorator extends RendererDecorator {

    private static final Font FONT = new Font(Font.SANS_SERIF, Font.PLAIN, 9);

    public StateLabelDecorator(Renderer inner) {
        super(inner);
    }

    @Override
    public void draw(Graphics2D g, Animal a) {
        super.draw(g, a);
        Vec2 p = a.position();
        g.setFont(FONT);
        g.setColor(new Color(255, 255, 255, 190));
        g.drawString(a.state().label(), (float) p.x() + 7, (float) p.y() + 3);
    }
}
