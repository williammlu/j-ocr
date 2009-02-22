package recognize;

import static recognize.ScreenVariables.*;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.image.BufferedImage;

class MyKeyListener implements KeyListener {

    private final Frame fr;
    private final Robot robot;

    MyKeyListener(Frame fr, Robot robot) {
        this.fr = fr;
        this.robot = robot;
    }

    /**
     * This method fills pixel array with the contents of the raster.
     *
     * @param pixelToFill two dimensional integer array
     */
    private void fillPixelArrayWithRaster(int pixelToFill[][]) {
        clipRect = new Rectangle(left, top, clipWidth, clipHeight);
        BufferedImage clipImage = robot.createScreenCapture(clipRect);
        int[] raster = clipImage.getData().getPixels(0, 0, clipWidth, clipHeight, (int[]) null);
        int rows = 0;
        int columns = 0;
        boolean isfirstcolumn = true;

        for (int i = 0; i < raster.length; i = i + 3) {
            if (((i / 3) % (clipWidth)) == 0) {
                rows = 0;
                if (!isfirstcolumn) {
                    columns++;
                }
            } else {
                rows++;
            }
            if (((i / 3) % clipHeight) == 0) {
                isfirstcolumn = false;
            }
            pixelToFill[columns][rows] = (raster[i + 2]) | (raster[i + 1] << 8) |
                    (raster[i] << 16 | 0xFF000000);
        }
    }

    public void keyTyped(KeyEvent e) {
        // for analyze
        if (!recording && (e.getKeyChar() == 'a' || e.getKeyChar() == 'A') && mouseDragged && clipWidth != 0 && clipHeight != 0) {
            analyzing = true;
            threshold++;
            pixels = new int[clipHeight][clipWidth];
            fillPixelArrayWithRaster(pixels);
            fr.repaint(0);
        } else if ((e.getKeyChar() == 's' || e.getKeyChar() == 'S') && mouseDragged) {// for save
            recording = true;
            pixels = new int[clipHeight][clipWidth];
            fillPixelArrayWithRaster(pixels);
            fr.repaint(0);
        } else if (!analyzing && (e.getKeyChar() == 'r' || e.getKeyChar() == 'R') && mouseDragged) {// for recognize
            recognizing = true;
//			pixels = new int[clipHeight][clipWidth];
//			fillPixelArrayWithRaster(pixels);
            fr.repaint(0);
        }
    }

    public void keyPressed(KeyEvent e) {
    }

    public void keyReleased(KeyEvent e) {
    }

}
