package recognize;

import static recognize.ScreenVariables.*;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

public class Transframe extends JFrame {
    final private Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
    final private Rectangle rectangle = new Rectangle(screenSize);
    private BufferedImage image = null;
    final private String path;
    private Recognizer recognize;

    private Transframe(String path) throws AWTException {
        this.path = path;
        setLocation(0, 0);
        setUndecorated(true);
        setSize(screenSize);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        Robot robot = new Robot();
        image = robot.createScreenCapture(rectangle);
        addKeyListener(new MyKeyListener(this, robot));
        MyMouseListener myMouseListener = new MyMouseListener(this);
        addMouseListener(myMouseListener);
        addMouseMotionListener(myMouseListener);
        setVisible(true);
    }

    public void paint(Graphics g) {
        g.drawImage(image, 0, 0, this);
        if (mouseDragged) {
            g.setColor(Color.RED);
            if (startX < endX) {
                left = startX;
            } else {
                left = endX;
            }

            if (startY < endY) {
                top = startY;
            } else {
                top = endY;
            }
            g.drawRect(left, top, Math.abs(endX - startX), Math.abs(endY - startY));
        }

        if (analyzing) {
            g.setColor(Color.BLACK);
            for (int j = 1; j < clipWidth - 1; j++) {
                int currentValue = 0;
                int pixCnt[] = new int[2];
                int pixTracker = 0;
                for (int i = 1; i < clipHeight - 1; i++) {
                    if (i == 1) {
                        currentValue = pixels[i][j];
                        pixCnt[pixTracker % 2]++;
                    } else if (pixels[i][j] != currentValue) {
                        pixTracker++;
                        pixCnt[pixTracker % 2]++;
                    } else {
                        pixCnt[pixTracker % 2]++;
                    }
                }
                if ((pixCnt[0] < pixCnt[1] ? pixCnt[0] : pixCnt[1]) == threshold && (top + 1) <= (top + clipHeight - 2)) {
                    g.drawLine(left + j, top + 1, left + j, top + clipHeight - 2);
                }
            }
        } else if (recording) {
            new Recorder(this, g, new File(path));
            recording = false;
        } else if (recognizing) {
            final int spaceWidth = 6;
            final int maxOffset = 3;
            if (recognize == null)
                recognize = new Recognizer(this, g, spaceWidth, left, top, clipWidth, clipHeight, maxOffset, path, true);
            else
                recognize = new Recognizer(this, g, spaceWidth, left, top, clipWidth, clipHeight, maxOffset, path, false);
            System.out.println("recognisedChars in transframe" + recognize.getRecognizedChars());
            recognizing = false;
        } else {
            System.out.println("doing nothing");
        }
    }

    /**
     * Application entry point.
     *
     * @param args String[]
     * @throws java.io.IOException can throw Exception of wrong path is given
     */
    @SuppressWarnings({"JavaDoc"})
    public static void main(String[] args) throws IOException {

        if (args.length < 1) {
            System.out.println("Parameter <directory> expected");
            System.exit(1);

        }
        if (!(new File(args[0])).isDirectory() || !(new File(args[0])).canRead()) {
            throw new IOException("directory " + args[0] + " not found or no permissions to read");
        }
        final String path = args[0];
        // Path is the place where the program will load the pix files.
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                // Path is the place where the program will load the pix files.
                //path = "D:\\pixfilesMono";
                try {
                    new Transframe(path);
                } catch (AWTException e) {
                    e.printStackTrace();
                    System.exit(1);
                }
            }
        });
    }
}
