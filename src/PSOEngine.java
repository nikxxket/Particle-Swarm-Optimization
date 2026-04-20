import java.util.ArrayList;
import java.util.List;
import java.util.Random;

class Particle {
    double[] position;
    double[] velocity;
    double[] bestPosition;
    double fitness;
    double bestFitness;

    Particle(int dims) {
        position = new double[dims];
        velocity = new double[dims];
        bestPosition = new double[dims];
    }
}

public class PSOEngine {
    private static final int WIDTH = 900;
    private static final int HEIGHT = 700;
    private static final int SENSOR_RADIUS = 100;
    private static final int SENSOR_RADIUS_SQ = SENSOR_RADIUS * SENSOR_RADIUS;
    private static final int NUM_PARTICLES = 40;
    private static final int NUM_SENSORS = 12;
    private static final double W = 0.7;
    private static final double C1 = 1.5;
    private static final double C2 = 1.5;
    private static final double MIN_X = -SENSOR_RADIUS - 20;
    private static final double MAX_X = WIDTH + SENSOR_RADIUS + 20;
    private static final double MIN_Y = -SENSOR_RADIUS - 20;
    private static final double MAX_Y = HEIGHT + SENSOR_RADIUS + 20;

    private List<Particle> particles;
    private double[] globalBestPosition;
    private double globalBestFitness;
    private Random random;

    public PSOEngine() {
        random = new Random();
        particles = new ArrayList<>();
        globalBestPosition = new double[NUM_SENSORS * 2];
        globalBestFitness = Double.NEGATIVE_INFINITY;
        initializePSO();
    }

    private void initializePSO() {
        for (int i = 0; i < NUM_PARTICLES; i++) {
            Particle p = new Particle(NUM_SENSORS * 2);
            for (int s = 0; s < NUM_SENSORS; s++) {
                p.position[s * 2] = random.nextDouble() * (WIDTH - 2*SENSOR_RADIUS) + SENSOR_RADIUS;
                p.position[s * 2 + 1] = random.nextDouble() * (HEIGHT - 2*SENSOR_RADIUS) + SENSOR_RADIUS;
                p.velocity[s * 2] = (random.nextDouble() - 0.5) * 25;
                p.velocity[s * 2 + 1] = (random.nextDouble() - 0.5) * 25;
            }
            p.fitness = evaluateFitness(p.position);
            p.bestPosition = p.position.clone();
            p.bestFitness = p.fitness;
            particles.add(p);

            if (p.fitness > globalBestFitness) {
                globalBestFitness = p.fitness;
                System.arraycopy(p.position, 0, globalBestPosition, 0, p.position.length);
            }
        }
    }

    private double evaluateFitness(double[] positions) {
        int step = 3;
        int covered = 0;
        int total = 0;

        for (int x = 0; x < WIDTH; x += step) {
            for (int y = 0; y < HEIGHT; y += step) {
                total++;
                boolean isCovered = false;
                for (int s = 0; s < NUM_SENSORS; s++) {
                    double sx = positions[s * 2];
                    double sy = positions[s * 2 + 1];
                    double dx = x - sx;
                    double dy = y - sy;
                    if (dx*dx + dy*dy <= SENSOR_RADIUS_SQ) {
                        isCovered = true;
                        break;
                    }
                }
                if (isCovered) covered++;
            }
        }
        double percent = (covered * 100.0) / total;

        double overlapPenalty = 0;
        int[][] overlapCount = new int[WIDTH/step + 1][HEIGHT/step + 1];
        for (int x = 0; x < WIDTH; x += step) {
            for (int y = 0; y < HEIGHT; y += step) {
                int xi = x/step, yi = y/step;
                for (int s = 0; s < NUM_SENSORS; s++) {
                    double sx = positions[s * 2];
                    double sy = positions[s * 2 + 1];
                    double dx = x - sx;
                    double dy = y - sy;
                    if (dx*dx + dy*dy <= SENSOR_RADIUS_SQ) {
                        overlapCount[xi][yi]++;
                    }
                }
                if (overlapCount[xi][yi] > 1) {
                    overlapPenalty += (overlapCount[xi][yi] - 1) * 0.5;
                }
            }
        }
        double maxPenalty = total * (NUM_SENSORS - 1) * 0.5;
        double penalty = (overlapPenalty / maxPenalty) * 5.0;
        return Math.max(0, Math.min(100, percent - penalty));
    }

    public void updatePSO() {
        for (Particle p : particles) {
            for (int d = 0; d < p.position.length; d++) {
                double r1 = random.nextDouble();
                double r2 = random.nextDouble();
                double cognitive = C1 * r1 * (p.bestPosition[d] - p.position[d]);
                double social = C2 * r2 * (globalBestPosition[d] - p.position[d]);
                p.velocity[d] = W * p.velocity[d] + cognitive + social;
                double maxVel = 20.0;
                p.velocity[d] = Math.max(-maxVel, Math.min(maxVel, p.velocity[d]));
            }

            for (int d = 0; d < p.position.length; d++) {
                p.position[d] += p.velocity[d];
                if (p.position[d] < MIN_X) {
                    p.position[d] = MIN_X + (MIN_X - p.position[d]);
                    p.velocity[d] = -p.velocity[d];
                }
                if (p.position[d] > MAX_X) {
                    p.position[d] = MAX_X - (p.position[d] - MAX_X);
                    p.velocity[d] = -p.velocity[d];
                }
                p.position[d] = Math.max(MIN_X, Math.min(MAX_X, p.position[d]));
            }

            p.fitness = evaluateFitness(p.position);

            if (p.fitness > p.bestFitness) {
                p.bestFitness = p.fitness;
                System.arraycopy(p.position, 0, p.bestPosition, 0, p.position.length);
                if (p.fitness > globalBestFitness) {
                    globalBestFitness = p.fitness;
                    System.arraycopy(p.position, 0, globalBestPosition, 0, p.position.length);
                }
            }
        }
    }

    public List<Particle> getParticles() {
        return particles;
    }

    public double[] getGlobalBestPosition() {
        return globalBestPosition;
    }

    public double getGlobalBestFitness() {
        return globalBestFitness;
    }
}