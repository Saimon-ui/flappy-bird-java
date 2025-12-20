import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.util.ArrayList;
import java.util.Random;
import java.io.File;

public class FlappyBird extends JPanel implements ActionListener, KeyListener {

    // Screen
    int boardWidth = Toolkit.getDefaultToolkit().getScreenSize().width;
    int boardHeight = Toolkit.getDefaultToolkit().getScreenSize().height;

    // Bird
    int birdX = boardWidth / 4;
    int birdY = boardHeight / 2;
    int birdWidth = 44;
    int birdHeight = 30;
    int velocityY = 0;
    int gravity = 1;
    int jumpStrength = -12;

    // Images
    BufferedImage birdImg;
    BufferedImage pipeImg;
    BufferedImage bgImg;

    // Pipe class
    class Pipe {
        int x, y, width = 90, height;
        boolean passed = false;

        Pipe(int x, int y, int height) {
            this.x = x;
            this.y = y;
            this.height = height;
        }
    }

    ArrayList<Pipe> pipes = new ArrayList<>();
    Random random = new Random();

    Timer gameLoop;
    Timer pipeTimer;

    boolean started = false;
    boolean gameOver = false;
    int score = 0;

    public FlappyBird() {
        setFocusable(true);
        addKeyListener(this);

        loadImages(); // uses file paths

        pipeTimer = new Timer(1500, e -> placePipes());
        gameLoop = new Timer(20, this);

        pipeTimer.start();
        gameLoop.start();

        // Helps keyboard focus
        SwingUtilities.invokeLater(this::requestFocusInWindow);
    }

    // Guaranteed image loading (no getResource/classpath issues)
   void loadImages() {
    try {
        File birdFile = new File("src/images/bird.png");
        File pipeFile = new File("src/images/pipe.png");
        File bgFile   = new File("src/images/background.png");

        System.out.println("Bird exists: " + birdFile.exists());
        System.out.println("Pipe exists: " + pipeFile.exists());
        System.out.println("BG exists: " + bgFile.exists());
        System.out.println("Working dir: " + new File(".").getAbsolutePath());

        birdImg = ImageIO.read(birdFile);
        pipeImg = ImageIO.read(pipeFile);
        bgImg   = ImageIO.read(bgFile);

        if (birdImg == null || pipeImg == null || bgImg == null) {
            throw new RuntimeException("ImageIO.read returned null");
        }
    } catch (Exception e) {
        JOptionPane.showMessageDialog(this, "Failed to load images");
        e.printStackTrace();
        System.exit(1);
    }
}

    // Place pipes with spacing control (prevents overlap)
    void placePipes() {
        if (!pipes.isEmpty()) {
            Pipe last = pipes.get(pipes.size() - 1);
            if (last.x > boardWidth - 420) return;
        }

        int gap = 240;
        int minHeight = 120;
        int maxHeight = boardHeight - gap - minHeight;

        int topHeight = random.nextInt(maxHeight - minHeight) + minHeight;
        int bottomY = topHeight + gap;
        int bottomHeight = boardHeight - bottomY;

        pipes.add(new Pipe(boardWidth, 0, topHeight));
        pipes.add(new Pipe(boardWidth, bottomY, bottomHeight));
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;

        // Pixel art look (no blur)
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);

        // Background
        g2.drawImage(bgImg, 0, 0, boardWidth, boardHeight, null);

        // Pipes (top pipe flipped)
        for (Pipe p : pipes) {
            if (p.y == 0) {
                g2.drawImage(pipeImg, p.x, p.height, p.width, -p.height, null);
            } else {
                g2.drawImage(pipeImg, p.x, p.y, p.width, p.height, null);
            }
        }

        // Bird
        g2.drawImage(birdImg, birdX, birdY, birdWidth, birdHeight, null);

        // UI text
        g2.setColor(Color.WHITE);
        g2.setFont(new Font("Arial", Font.BOLD, 40));

        if (!started) {
            g2.drawString("PRESS SPACE TO START", boardWidth / 2 - 260, boardHeight / 2);
        } else if (gameOver) {
            g2.drawString("GAME OVER", boardWidth / 2 - 140, boardHeight / 2 - 40);
            g2.drawString("Score: " + score, boardWidth / 2 - 100, boardHeight / 2 + 20);
            g2.drawString("Press SPACE", boardWidth / 2 - 120, boardHeight / 2 + 80);
        } else {
            g2.drawString(String.valueOf(score), boardWidth / 2, 60);
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (!started || gameOver) {
            repaint();
            return;
        }

        velocityY += gravity;
        birdY += velocityY;

        for (Pipe p : pipes) {
            p.x -= 5;

            if (!p.passed && p.x + p.width < birdX) {
                p.passed = true;
                score++;
            }

            if (collision(p)) {
                gameOver = true;
            }
        }

        if (birdY < 0 || birdY + birdHeight > boardHeight) {
            gameOver = true;
        }

        pipes.removeIf(p -> p.x + p.width < 0);
        repaint();
    }

    // Smaller hitboxes so it feels fair
    boolean collision(Pipe p) {
        Rectangle bird = new Rectangle(birdX + 6, birdY + 6, birdWidth - 12, birdHeight - 12);
        Rectangle pipe = new Rectangle(p.x + 12, p.y + 12, p.width - 24, p.height - 24);
        return bird.intersects(pipe);
    }

    @Override
    public void keyPressed(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_SPACE) {
            if (!started) {
                started = true;
                velocityY = jumpStrength;
                return;
            }
            if (gameOver) {
                resetGame();
                return;
            }
            velocityY = jumpStrength;
        }

        if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
            System.exit(0);
        }
    }

    void resetGame() {
        birdY = boardHeight / 2;
        velocityY = 0;
        pipes.clear();
        score = 0;
        started = false;
        gameOver = false;
    }

    public static void main(String[] args) {
        JFrame frame = new JFrame("Flappy Bird");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setUndecorated(true);
        frame.setExtendedState(JFrame.MAXIMIZED_BOTH);

        frame.add(new FlappyBird());
        frame.setVisible(true);
    }

    public void keyReleased(KeyEvent e) {}
    public void keyTyped(KeyEvent e) {}
}
