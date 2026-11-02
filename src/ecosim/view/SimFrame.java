package ecosim.view;

import ecosim.events.EventBus;
import ecosim.events.SimEvent;
import ecosim.model.Vec2;
import ecosim.model.World;
import ecosim.stats.StatsRecorder;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import java.awt.BorderLayout;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * The window: world on top, graphs underneath, driven by a Swing Timer.
 *
 * The Timer fires on the Swing event thread, and world.step() runs there too
 * (its parallel streams block until finished). So painting never overlaps a
 * step, and the UI never sees a half-updated world.
 */
public final class SimFrame extends JFrame {

    private final World world;
    private final StatsRecorder stats;
    private final WorldPanel worldPanel;
    private final GraphPanel graphPanel;
    private final Timer timer;
    private int stepsPerFrame = 1;
    private boolean paused;

    public SimFrame(World world, EventBus<SimEvent> bus) {
        super("Predator–Prey Ecosystem");
        this.world = world;
        this.stats = new StatsRecorder(bus, 600, 5);
        this.worldPanel = new WorldPanel(world, stats);
        this.graphPanel = new GraphPanel(stats, world.config().width());

        setLayout(new BorderLayout());
        add(worldPanel, BorderLayout.CENTER);
        add(graphPanel, BorderLayout.SOUTH);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);
        pack();
        setLocationRelativeTo(null);

        timer = new Timer(33, e -> tick());    // ~30 frames per second
        installControls();
        timer.start();
    }

    private void tick() {
        if (!paused) {
            try {
                for (int i = 0; i < stepsPerFrame; i++) world.step();
            } catch (RuntimeException ex) {
                // A bug in the model should not silently kill the window.
                // Pause, show it on screen, and print the full trace.
                paused = true;
                worldPanel.setStatus("ERROR: " + ex + " (paused)");
                ex.printStackTrace();
            }
        }
        worldPanel.repaint();
        graphPanel.repaint();
    }

    private void installControls() {
        worldPanel.addKeyListener(new KeyAdapter() {
            @Override public void keyPressed(KeyEvent e) {
                switch (e.getKeyCode()) {
                    case KeyEvent.VK_SPACE -> paused = !paused;
                    case KeyEvent.VK_P -> world.setParallel(!world.isParallel());
                    case KeyEvent.VK_V -> worldPanel.toggleVision();
                    case KeyEvent.VK_E -> worldPanel.toggleEnergy();
                    case KeyEvent.VK_L -> worldPanel.toggleLabels();
                    case KeyEvent.VK_UP -> stepsPerFrame = Math.min(20, stepsPerFrame + 1);
                    case KeyEvent.VK_DOWN -> stepsPerFrame = Math.max(1, stepsPerFrame - 1);
                    case KeyEvent.VK_R -> { world.reset(); stats.reset(); worldPanel.setStatus(""); paused = false; }
                    default -> { }
                }
                worldPanel.setStatus(paused ? "PAUSED" : (stepsPerFrame > 1 ? "x" + stepsPerFrame : ""));
            }
        });
        worldPanel.addMouseListener(new MouseAdapter() {
            @Override public void mousePressed(MouseEvent e) {
                Vec2 at = new Vec2(e.getX(), e.getY());
                if (SwingUtilities.isRightMouseButton(e)) world.addFoxes(at, 3);
                else world.addRabbits(at, 15);
                worldPanel.requestFocusInWindow();
            }
        });
        worldPanel.requestFocusInWindow();
    }
}
