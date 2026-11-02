package ecosim.view.render;

import ecosim.model.Animal;

import java.awt.Graphics2D;
import java.util.Objects;

/**
 * DECORATOR base class.
 *
 * It IS-A Renderer (so a decorated renderer can be used anywhere a plain one
 * can) and HAS-A Renderer (the thing it wraps). Implementing Renderer only
 * buys type compatibility; the wrapped reference does the real work.
 * Decorators can be stacked in any combination at runtime, which is how the
 * V / E / L keys switch overlays on and off without any if-statements in
 * the drawing code and without a subclass for every combination
 * (3 overlays would otherwise need 2^3 = 8 classes).
 */
public abstract class RendererDecorator implements Renderer {

    protected final Renderer inner;

    protected RendererDecorator(Renderer inner) {
        this.inner = Objects.requireNonNull(inner, "cannot decorate nothing");
    }

    @Override
    public void draw(Graphics2D g, Animal a) {
        inner.draw(g, a);
    }
}
