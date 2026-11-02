package ecosim.view.render;

import ecosim.model.Animal;

import java.awt.Graphics2D;

/**
 * Draws one animal. This is the "Component" role in the DECORATOR pattern:
 * BodyRenderer is the concrete component and every RendererDecorator wraps
 * another Renderer to add something extra on screen.
 */
@FunctionalInterface
public interface Renderer {
    void draw(Graphics2D g, Animal a);
}
