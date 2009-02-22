package recognize;

import static recognize.ScreenVariables.*;

import javax.swing.event.MouseInputListener;
import java.awt.*;
import java.awt.event.MouseEvent;

class MyMouseListener implements MouseInputListener {
    private final Frame fr;

    MyMouseListener(Frame fr) {
        this.fr = fr;
    }

    public void mouseClicked(MouseEvent e) {
        mouseDragged = false;
        recording = false;
        analyzing = false;
        recognizing = false;
        threshold = -1;
        fr.repaint(0);
    }

    public void mousePressed(MouseEvent e) {
        startX = e.getX();
        startY = e.getY();
    }

    public void mouseReleased(MouseEvent e) {
    }

    public void mouseEntered(MouseEvent e) {
    }

    public void mouseExited(MouseEvent e) {
    }

    public void mouseDragged(MouseEvent e) {
        mouseDragged = true;
        recording = false;
        analyzing = false;
        recognizing = false;
        endX = e.getX();
        endY = e.getY();
        threshold = -1;
        clipWidth = Math.abs(endX - startX) + 1;
        clipHeight = Math.abs(endY - startY) + 1;
        fr.repaint(0);
    }

    public void mouseMoved(MouseEvent e) {
        if (!recording && threshold > -1 && clipRect != null && clipRect.contains(e.getPoint())) {
            fr.getGraphics().clearRect((int) clipRect.getMinX() - 80, (int) clipRect.getMinY() - 30, 80, 50);
            fr.getGraphics().drawString("thres = " + threshold, (int) clipRect.getMinX() - 80, (int) clipRect.getMinY() - 30 + 10);
            int j = e.getX() - left;
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
            fr.getGraphics().drawString("j = " + j, (int) clipRect.getMinX() - 80, (int) clipRect.getMinY() - 30 + 20);
            fr.getGraphics().drawString("black = " + pixCnt[0] + ":" + pixCnt[1], (int) clipRect.getMinX() - 80, (int) clipRect.getMinY() - 30 + 30);
            fr.getGraphics().drawString("sum = " + (pixCnt[0] + pixCnt[1]), (int) clipRect.getMinX() - 80, (int) clipRect.getMinY() - 30 + 40);
        }
    }

}
