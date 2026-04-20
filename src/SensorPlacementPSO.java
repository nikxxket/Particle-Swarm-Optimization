import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

public class SensorPlacementPSO extends JPanel implements ActionListener {
    // Размеры поля
    private static final int WIDTH = 900;
    private static final int HEIGHT = 700;
    private static final int SENSOR_RADIUS = 100;
    private static final int SENSOR_RADIUS_SQ = SENSOR_RADIUS * SENSOR_RADIUS;

    // PSO параметры
    private static final int NUM_PARTICLES = 40;
    private static final int NUM_SENSORS = 12;
    private static final int NUM_ITERATIONS = 300;
    private static final double W = 0.7;
    private static final double C1 = 1.5;
    private static final double C2 = 1.5;

    // Границы для ЦЕНТРОВ сенсоров (шире, чтобы круги могли покрывать края)
    private static final double MIN_X = -SENSOR_RADIUS - 20;
    private static final double MAX_X = WIDTH + SENSOR_RADIUS + 20;
    private static final double MIN_Y = -SENSOR_RADIUS - 20;
    private static final double MAX_Y = HEIGHT + SENSOR_RADIUS + 20;

    private PSOEngine engine;
    private javax.swing.Timer timer;
    private int currentIteration = 0;
    private BufferedImage coverageHeatmap;
    private List<Double> fitnessHistory;
    private List<double[]> bestPositionHistory;
    private float pulsePhase = 0;

    public SensorPlacementPSO() {
        engine = new PSOEngine();
        fitnessHistory = new ArrayList<>();
        bestPositionHistory = new ArrayList<>();
        coverageHeatmap = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_ARGB);
        updateHeatmap();

        timer = new javax.swing.Timer(30, this);
        timer.start();

        JFrame frame = new JFrame("PSO - Расстановка сенсоров (покрытие углов)");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(WIDTH + 20, HEIGHT + 50);
        frame.setResizable(false);
        frame.add(this);
        frame.setVisible(true);
    }

    private void updateHeatmap() {
        Graphics2D g = coverageHeatmap.createGraphics();
        g.setComposite(AlphaComposite.Clear);
        g.fillRect(0, 0, WIDTH, HEIGHT);
        g.setComposite(AlphaComposite.SrcOver);

        int[][] overlap = new int[WIDTH][HEIGHT];
        double[] bestPos = engine.getGlobalBestPosition();
        for (int s = 0; s < NUM_SENSORS; s++) {
            int cx = (int)bestPos[s * 2];
            int cy = (int)bestPos[s * 2 + 1];
            int r = SENSOR_RADIUS;
            for (int dx = -r; dx <= r; dx++) {
                int x = cx + dx;
                if (x < 0 || x >= WIDTH) continue;
                int dyMax = (int)Math.sqrt(r*r - dx*dx);
                for (int dy = -dyMax; dy <= dyMax; dy++) {
                    int y = cy + dy;
                    if (y >= 0 && y < HEIGHT) {
                        overlap[x][y]++;
                    }
                }
            }
        }

        for (int x = 0; x < WIDTH; x++) {
            for (int y = 0; y < HEIGHT; y++) {
                int intensity = overlap[x][y];
                Color c;
                if (intensity == 0) {
                    c = new Color(40, 40, 50, 0);
                } else if (intensity == 1) {
                    c = new Color(0, 200, 0, 100);
                } else if (intensity == 2) {
                    c = new Color(100, 230, 0, 140);
                } else {
                    c = new Color(255, 200, 0, 180);
                }
                coverageHeatmap.setRGB(x, y, c.getRGB());
            }
        }
        g.dispose();
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (currentIteration < NUM_ITERATIONS) {
            engine.updatePSO();
            currentIteration++;

            if (currentIteration % 5 == 0) {
                updateHeatmap();
                fitnessHistory.add(engine.getGlobalBestFitness());
                if (fitnessHistory.size() > NUM_ITERATIONS) fitnessHistory.remove(0);
                bestPositionHistory.add(engine.getGlobalBestPosition().clone());
                if (bestPositionHistory.size() > 20) bestPositionHistory.remove(0);
            }

            pulsePhase += 0.1f;
            repaint();

            if (currentIteration % 50 == 0) {
                System.out.printf("Итерация %d, покрытие: %.2f%%\n", currentIteration, engine.getGlobalBestFitness());
            }
            if (currentIteration == NUM_ITERATIONS) {
                timer.stop();
                System.out.printf("Оптимизация завершена! Покрытие: %.2f%%\n", engine.getGlobalBestFitness());
            }
        }
    }

    private void drawConvergenceGraph(Graphics2D g2d, int x, int y, int width, int height) {
        g2d.setColor(new Color(0, 0, 0, 180));
        g2d.fillRect(x, y, width, height);
        g2d.setColor(Color.WHITE);
        g2d.drawRect(x, y, width, height);
        g2d.setFont(new Font("Arial", Font.BOLD, 12));
        g2d.drawString("Прогресс покрытия (%)", x + 5, y + 15);

        if (fitnessHistory.size() < 2) return;
        int steps = fitnessHistory.size();
        double stepX = (double)width / (steps - 1);
        GeneralPath path = new GeneralPath();
        for (int i = 0; i < steps; i++) {
            double fit = fitnessHistory.get(i);
            int px = x + (int)(i * stepX);
            int py = y + height - (int)(fit / 100.0 * height);
            if (i == 0) path.moveTo(px, py);
            else path.lineTo(px, py);
        }
        g2d.setColor(new Color(0, 255, 100, 220));
        g2d.setStroke(new BasicStroke(2));
        g2d.draw(path);

        double current = fitnessHistory.get(fitnessHistory.size() - 1);
        g2d.setColor(Color.YELLOW);
        g2d.drawString(String.format("%.1f%%", current), x + width - 45, y + height - 5);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        g2d.setColor(new Color(30, 30, 40));
        g2d.fillRect(0, 0, WIDTH, HEIGHT);

        g2d.setColor(new Color(70, 70, 90));
        for (int i = 0; i < WIDTH; i += 50) {
            g2d.drawLine(i, 0, i, HEIGHT);
            g2d.drawLine(0, i, WIDTH, i);
        }

        g2d.drawImage(coverageHeatmap, 0, 0, null);

        for (double[] histPos : bestPositionHistory) {
            g2d.setColor(new Color(255, 100, 100, 25));
            for (int s = 0; s < NUM_SENSORS; s++) {
                int x = (int)histPos[s * 2];
                int y = (int)histPos[s * 2 + 1];
                g2d.fillOval(x - SENSOR_RADIUS, y - SENSOR_RADIUS, SENSOR_RADIUS*2, SENSOR_RADIUS*2);
            }
        }

        g2d.setColor(new Color(80, 150, 255, 80));
        for (Particle p : engine.getParticles()) {
            for (int s = 0; s < NUM_SENSORS; s++) {
                int x = (int)p.position[s * 2];
                int y = (int)p.position[s * 2 + 1];
                g2d.fillOval(x - 3, y - 3, 6, 6);
            }
        }

        double[] bestPos = engine.getGlobalBestPosition();
        for (int s = 0; s < NUM_SENSORS; s++) {
            int x = (int)bestPos[s * 2];
            int y = (int)bestPos[s * 2 + 1];

            RadialGradientPaint grad = new RadialGradientPaint(
                    x, y, SENSOR_RADIUS + 5,
                    new float[]{0f, 0.7f, 1f},
                    new Color[]{new Color(255, 80, 80, 80), new Color(255, 0, 0, 40), new Color(255, 0, 0, 0)}
            );
            g2d.setPaint(grad);
            g2d.fillOval(x - SENSOR_RADIUS - 5, y - SENSOR_RADIUS - 5,
                    (SENSOR_RADIUS + 5)*2, (SENSOR_RADIUS + 5)*2);

            g2d.setColor(Color.RED);
            g2d.setStroke(new BasicStroke(2.5f));
            g2d.drawOval(x - SENSOR_RADIUS, y - SENSOR_RADIUS, SENSOR_RADIUS*2, SENSOR_RADIUS*2);

            g2d.setColor(Color.WHITE);
            g2d.fillOval(x - 6, y - 6, 12, 12);
            g2d.setColor(Color.BLACK);
            g2d.drawOval(x - 6, y - 6, 12, 12);
            g2d.setFont(new Font("Arial", Font.BOLD, 12));
            g2d.setColor(Color.BLACK);
            g2d.drawString(String.valueOf(s + 1), x - 3, y + 4);
        }

        g2d.setColor(Color.WHITE);
        g2d.setStroke(new BasicStroke(2));
        g2d.drawRect(0, 0, WIDTH, HEIGHT);

        g2d.setColor(new Color(0, 0, 0, 180));
        g2d.fillRect(5, 5, 230, 85);
        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Arial", Font.BOLD, 14));
        g2d.drawString("Итерация: " + currentIteration + " / " + NUM_ITERATIONS, 10, 25);
        g2d.drawString("Покрытие: " + String.format("%.2f%%", engine.getGlobalBestFitness()), 10, 45);

        int progressWidth = (int)((double)currentIteration / NUM_ITERATIONS * 200);
        g2d.setColor(Color.GRAY);
        g2d.fillRect(10, 60, 200, 12);
        g2d.setColor(new Color(0, 200, 0));
        g2d.fillRect(10, 60, progressWidth, 12);
        g2d.setColor(Color.WHITE);
        g2d.drawRect(10, 60, 200, 12);

        drawConvergenceGraph(g2d, WIDTH - 210, 10, 200, 100);

        g2d.setColor(new Color(0, 0, 0, 180));
        g2d.fillRect(WIDTH - 210, HEIGHT - 90, 200, 80);
        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Arial", Font.PLAIN, 11));
        g2d.drawString("Зелёный → покрытие", WIDTH - 205, HEIGHT - 70);
        g2d.drawString("Жёлтый → перекрытие", WIDTH - 205, HEIGHT - 55);
        g2d.drawString("Красные круги → сенсоры", WIDTH - 205, HEIGHT - 40);
        g2d.drawString("Синие точки → рои", WIDTH - 205, HEIGHT - 25);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new SensorPlacementPSO());
    }
}