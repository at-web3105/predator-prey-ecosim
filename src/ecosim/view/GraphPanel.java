package ecosim.view;

import ecosim.stats.RingBuffer;
import ecosim.stats.StatsRecorder;
import ecosim.stats.StatsRecorder.Sample;

import javax.swing.JPanel;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Path2D;
import java.util.function.ToDoubleFunction;

/**
 * Two live line charts under the world:
 *   left  - population of rabbits and foxes (the predator-prey cycle)
 *   right - average speed gene of each species (evolution, the arms race)
 *
 * Each line is described by a ToDoubleFunction<Sample> (a lambda that pulls
 * one number out of a sample), so one drawSeries method draws every line.
 */
public final class GraphPanel extends JPanel {

    private static final Color RABBIT = new Color(170, 200, 255);
    private static final Color FOX = new Color(255, 120, 50);
    private static final Color GRASS = new Color(90, 150, 60);

    private final StatsRecorder stats;

    public GraphPanel(StatsRecorder stats, int width) {
        this.stats = stats;
        setPreferredSize(new Dimension(width, 190));
        setBackground(new Color(22, 22, 26));
    }

    @Override
    protected void paintComponent(Graphics g0) {
        super.paintComponent(g0);
        Graphics2D g = (Graphics2D) g0;
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        RingBuffer<Sample> h = stats.history();
        int w = getWidth(), half = w / 2, top = 24, height = getHeight() - top - 12;

        g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        g.setColor(Color.LIGHT_GRAY);
        g.drawString("Population", 12, 16);
        g.drawString("Mean speed gene", half + 12, 16);
        if (h.size() < 2) return;

        // Left chart: grass (0..1), then rabbits and foxes each scaled to their own max
        drawSeries(g, h, s -> s.grass(), 0, 1, 10, top, half - 20, height, GRASS);
        double maxR = h.stream().mapToDouble(s -> s.rabbits().count()).max().orElse(1);
        double maxF = h.stream().mapToDouble(s -> s.foxes().count()).max().orElse(1);
        drawSeries(g, h, s -> s.rabbits().count(), 0, Math.max(1, maxR), 10, top, half - 20, height, RABBIT);
        drawSeries(g, h, s -> s.foxes().count(), 0, Math.max(1, maxF), 10, top, half - 20, height, FOX);

        Sample now = h.newest();
        legend(g, 100, 16, String.format("rabbits %d (peak %.0f)", now.rabbits().count(), maxR), RABBIT);
        legend(g, 250, 16, String.format("foxes %d (peak %.0f)", now.foxes().count(), maxF), FOX);
        legend(g, 380, 16, String.format("grass %.0f%%", now.grass() * 100), GRASS);

        // Right chart: speed genes on a shared axis so they can be compared directly
        double lo = h.stream().mapToDouble(s -> Math.min(nz(s.rabbits().meanSpeed()), nz(s.foxes().meanSpeed()))).min().orElse(0);
        double hi = h.stream().mapToDouble(s -> Math.max(s.rabbits().meanSpeed(), s.foxes().meanSpeed())).max().orElse(4);
        lo = Math.max(0, lo - 0.1);
        hi = hi + 0.1;
        drawSeries(g, h, s -> s.rabbits().meanSpeed(), lo, hi, half + 10, top, half - 20, height, RABBIT);
        drawSeries(g, h, s -> s.foxes().meanSpeed(), lo, hi, half + 10, top, half - 20, height, FOX);
        g.setColor(Color.GRAY);
        g.drawString(String.format("%.2f", hi), w - 40, top + 10);
        g.drawString(String.format("%.2f", lo), w - 40, top + height);
        legend(g, half + 130, 16, String.format("rabbit %.2f", now.rabbits().meanSpeed()), RABBIT);
        legend(g, half + 230, 16, String.format("fox %.2f", now.foxes().meanSpeed()), FOX);
    }

    private static double nz(double v) { return v == 0 ? Double.MAX_VALUE : v; }   // ignore extinct species

    private void drawSeries(Graphics2D g, RingBuffer<Sample> h, ToDoubleFunction<Sample> value,
                            double lo, double hi, int x0, int y0, int w, int ht, Color c) {
        Path2D.Double path = new Path2D.Double();
        int n = h.capacity();
        int i = 0;
        for (Sample s : h) {    // uses RingBuffer's custom iterator
            double v = value.applyAsDouble(s);
            double x = x0 + (double) i / (n - 1) * w;
            double y = y0 + ht - (v - lo) / (hi - lo) * ht;
            if (i == 0) path.moveTo(x, y); else path.lineTo(x, y);
            i++;
        }
        g.setColor(c);
        g.setStroke(new BasicStroke(1.8f));
        g.draw(path);
    }

    private void legend(Graphics2D g, int x, int y, String text, Color c) {
        g.setColor(c);
        g.fillRect(x, y - 8, 8, 8);
        g.setColor(Color.LIGHT_GRAY);
        g.drawString(text, x + 12, y);
    }
}
