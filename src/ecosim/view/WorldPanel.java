package ecosim.view;

import ecosim.model.Animal;
import ecosim.model.DeathCause;
import ecosim.model.Fox;
import ecosim.model.GrassField;
import ecosim.model.Rabbit;
import ecosim.model.Warren;
import ecosim.model.World;
import ecosim.stats.StatsRecorder;
import ecosim.view.render.BodyRenderer;
import ecosim.view.render.EnergyBarDecorator;
import ecosim.view.render.HungerDecorator;
import ecosim.view.render.Renderer;
import ecosim.view.render.StateLabelDecorator;
import ecosim.view.render.VisionRingDecorator;

import javax.swing.JPanel;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;
import java.util.Comparator;
import java.util.List;

/** Draws the grass, the animals and a heads-up display. */
public final class WorldPanel extends JPanel {

    private final World world;
    private final StatsRecorder stats;
    private BufferedImage grassImage;

    private boolean showVision, showEnergy, showLabels, showHunger;
    private Renderer renderer = new BodyRenderer();
    private String status = "";

    public WorldPanel(World world, StatsRecorder stats) {
        this.world = world;
        this.stats = stats;
        setPreferredSize(new Dimension(world.config().width(), world.config().height()));
        setBackground(new Color(40, 34, 26));
        setFocusable(true);
    }

    // ---- overlay toggles: each rebuilds the decorator stack ---------------

    public void toggleVision() { showVision = !showVision; rebuildRenderer(); }
    public void toggleEnergy() { showEnergy = !showEnergy; rebuildRenderer(); }
    public void toggleLabels() { showLabels = !showLabels; rebuildRenderer(); }
    public void toggleHunger() { showHunger = !showHunger; rebuildRenderer(); }
    public void setStatus(String s) { status = s; }

    /**
     * Assemble the decorator stack. The order matters: decorators added later
     * are on the OUTSIDE and so get first say in draw().
     */
    private void rebuildRenderer() {
        Renderer r = new BodyRenderer();
        if (showEnergy) r = new EnergyBarDecorator(r);
        if (showLabels) r = new StateLabelDecorator(r);
        if (showHunger) r = new HungerDecorator(r);
        if (showVision) r = new VisionRingDecorator(r);
        renderer = r;
    }

    @Override
    protected void paintComponent(Graphics g0) {
        super.paintComponent(g0);
        Graphics2D g = (Graphics2D) g0;
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        drawGrass(g);
        drawWarrens(g);

        // Draw foxes last so they sit on top of rabbits.
        List<Animal> all = world.animals().stream()
                .sorted(Comparator.comparing(a -> a instanceof Fox))
                .toList();
        all.forEach(a -> renderer.draw(g, a));

        drawHud(g);
    }

    private void drawGrass(Graphics2D g) {
        GrassField f = world.grass();
        if (grassImage == null || grassImage.getWidth() != f.cols() || grassImage.getHeight() != f.rows()) {
            grassImage = new BufferedImage(f.cols(), f.rows(), BufferedImage.TYPE_INT_RGB);
        }
        double winter = Math.max(0, -world.seasonPhase());   // 0..1, paler grass in winter
        for (int r = 0; r < f.rows(); r++) {
            for (int c = 0; c < f.cols(); c++) {
                double l = f.level(c, r);
                int red   = (int) (58 + (45 - 58) * l + 30 * winter * l);
                int green = (int) (46 + (120 - 46) * l - 15 * winter * l);
                int blue  = (int) (32 + (40 - 32) * l + 20 * winter * l);
                grassImage.setRGB(c, r, (red << 16) | (green << 8) | blue);
            }
        }
        g.drawImage(grassImage, 0, 0, (int) (f.cols() * f.cellSize()), (int) (f.rows() * f.cellSize()), null);
    }

    private void drawWarrens(Graphics2D g) {
        for (Warren w : world.warrens()) {
            double r = w.radius(), x = w.centre().x() - r, y = w.centre().y() - r;
            g.setColor(new Color(30, 22, 14, 200));
            g.fill(new Ellipse2D.Double(x, y, r * 2, r * 2));
            g.setColor(new Color(150, 115, 70));
            g.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 1f, new float[]{4f, 4f}, 0f));
            g.draw(new Ellipse2D.Double(x, y, r * 2, r * 2));
        }
    }

    private void drawHud(Graphics2D g) {
        String[] lines = {
                String.format("tick %d   %s   season %s", world.tick(),
                        world.isParallel() ? "PARALLEL" : "SEQUENTIAL",
                        world.seasonPhase() >= 0 ? "summer" : "winter"),
                String.format("rabbits %d   foxes %d   grass %.0f%%",
                        world.count(Rabbit.class), world.count(Fox.class), world.grass().coverage() * 100),
                String.format("births %d   eaten %d   starved %d   old age %d",
                        stats.births(), stats.deaths(DeathCause.EATEN),
                        stats.deaths(DeathCause.STARVED), stats.deaths(DeathCause.OLD_AGE)),
                stats.latest() == null ? "" : String.format("step %.2f ms", stats.latest().stepMillis()),
                "[space] pause  [P] parallel  [V] vision  [E] energy  [L] labels  [H] hunger  [R] reset",
                "[up/down] speed  click: +rabbits  right-click: +foxes   " + status,
        };
        g.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        g.setColor(new Color(0, 0, 0, 150));
        g.fillRoundRect(6, 6, 560, lines.length * 15 + 10, 10, 10);
        g.setColor(new Color(235, 235, 225));
        for (int i = 0; i < lines.length; i++) {
            g.drawString(lines[i], 14, 22 + i * 15);
        }
    }
}
