package recognize;

import static recognize.ScreenVariables.*;

import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import java.awt.*;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;

class Recorder {
    Recorder(Frame fr, Graphics g) {
        performRecording(fr, g);
    }

    /**
     * records a character enclosed in a rectangle by scanning from all four sides
     * from left to right, right to left, top to bottom and bottom to top in order
     * ie, draws a new recangle exactly along the character boundaries  and records the character
     *
     * @param fr Frame
     * @param g  Graphics
     */
    private void performRecording(final Frame fr, final Graphics g) {
        g.setColor(Color.PINK);
        int bgPix = 0;
        boolean bgPixelFound = false;
        //scan left to right from top to bottom till character encountered
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

            // while scanning replace pixels of every bg line(vertical) found with 9.
            if ((pixCnt[0] < pixCnt[1] ? pixCnt[0] : pixCnt[1]) == 0 &&
                    (top + 1) <= (top + clipHeight - 2)) {
                g.drawLine(left + j, top + 1, left + j, top + clipHeight - 2);
                for (int i = 1; i < clipHeight - 1; i++) {
                    bgPixelFound = true;
                    bgPix = pixels[i][j];
                    pixels[i][j] = 9;
                }
            } else {
                break;
            }
        }
        //scan right to left from top to bottom till character encountered
        for (int j = clipWidth - 2; j >= 1; j--) {
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
            // while scanning replace pixels of every bg line(vertical) found with 9.
            if ((pixCnt[0] < pixCnt[1] ? pixCnt[0] : pixCnt[1]) == 0 &&
                    (top + 1) <= (top + clipHeight - 2)) {
                g.drawLine(left + j, top + 1, left + j, top + clipHeight - 2);
                for (int i = 1; i < clipHeight - 1; i++) {
                    bgPixelFound = true;
                    bgPix = pixels[i][j];
                    pixels[i][j] = 9;
                }
            } else {
                break;
            }
        }

        if (!bgPixelFound) {
            JOptionPane.showMessageDialog(fr,
                    "Could not find Background Pixel in the Rectangle", "Error",
                    JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        //scan top to bottom from left to right till character encountered
        for (int i = 1; i < clipHeight - 1; i++) {
            int currentValue = 0;
            int pixCnt[] = new int[2];
            int pixVal[] = new int[2];
            int pixTracker = 0;
            boolean firstEncountered = false;
            for (int j = 1; j < clipWidth - 1; j++) {
                if (!firstEncountered && pixels[i][j] != 9) {
                    currentValue = pixels[i][j];
                    firstEncountered = true;
                    pixCnt[pixTracker % 2]++;
                    pixVal[pixTracker % 2] = pixels[i][j];
                } else if (firstEncountered && pixels[i][j] != currentValue &&
                        pixels[i][j] != 9) {
                    pixTracker++;
                    pixCnt[pixTracker % 2]++;
                    pixVal[pixTracker % 2] = pixels[i][j];
                } else if (firstEncountered && pixels[i][j] != 9) {
                    pixCnt[pixTracker % 2]++;
                }
            }
            // while scanning replace pixels of every bg line(horizontal) found with 9.
            int minorityPixCnt = pixCnt[0] < pixCnt[1] ? pixCnt[0] : pixCnt[1];
            int majorityIndex = pixCnt[0] > pixCnt[1] ? 0 : 1;
            if (pixCnt[0] + pixCnt[1] > 0 && minorityPixCnt == 0 &&
                    pixVal[majorityIndex] == bgPix &&
                    (left + 1) <= (left + clipWidth - 2)) {
                g.drawLine(left + 1, top + i, left + clipWidth - 2, top + i);
                for (int j = 1; j < clipWidth - 1; j++) {
                    pixels[i][j] = 9;
                }
            } else {
                break;
            }
        }

        for (int i = clipHeight - 2; i >= 1; i--) {
            int currentValue = 0;
            int pixCnt[] = new int[2];
            int pixVal[] = new int[2];
            int pixTracker = 0;
            boolean firstEncountered = false;
            for (int j = 1; j < clipWidth - 1; j++) {
                if (!firstEncountered && pixels[i][j] != 9) {
                    currentValue = pixels[i][j];
                    firstEncountered = true;
                    pixCnt[pixTracker % 2]++;
                    pixVal[pixTracker % 2] = pixels[i][j];
                } else if (firstEncountered && pixels[i][j] != currentValue &&
                        pixels[i][j] != 9) {
                    pixTracker++;
                    pixCnt[pixTracker % 2]++;
                    pixVal[pixTracker % 2] = pixels[i][j];
                } else if (firstEncountered && pixels[i][j] != 9) {
                    pixCnt[pixTracker % 2]++;
                }
            }
            // while scanning replace pixels of every bg line(horizontal) found with 9.
            int minorityPixCnt = pixCnt[0] < pixCnt[1] ? pixCnt[0] : pixCnt[1];
            int majorityIndex = pixCnt[0] > pixCnt[1] ? 0 : 1;
            if (pixCnt[0] + pixCnt[1] > 0 && minorityPixCnt == 0 &&
                    pixVal[majorityIndex] == bgPix &&
                    (left + 1) <= (left + clipWidth - 2)) {
                g.drawLine(left + 1, top + i, left + clipWidth - 2, top + i);
                for (int j = 1; j < clipWidth - 1; j++) {
                    pixels[i][j] = 9;
                }
            } else {
                break;
            }
        }
        //find the number of rows taken by the character
        int rowsForChar = 0;
        for (int i = 1; i < clipHeight - 1; i++) {
            for (int j = 1; j < clipWidth - 1; j++) {
                if (pixels[i][j] != 9) {
                    rowsForChar++;
                    break;
                }
            }
        }
        // create an array depending on the size of the character to store its pixels
        final int pixForChar[][] = new int[rowsForChar][];
        int rowIndex = 0;
        for (int i = 1; i < clipHeight - 1; i++) {
            int width = 0;
            boolean containsData = false;
            // find the width of each row taken by the character
            for (int j = 1; j < clipWidth - 1; j++) {
                if (pixels[i][j] != 9) {
                    width++;
                    containsData = true;
                }
            }
            // if a row contains character pixels/foreground pixels, scan the row and
            // represent  bgpixels and foreground pixels of the corresponding row by 0 and 1 respectively
            //in pixForChar array
            if (containsData) {
                pixForChar[rowIndex] = new int[width];
                int colIndex = 0;
                for (int j = 1; j < clipWidth - 1; j++) {
                    if (pixels[i][j] != 9) {
                        if (pixels[i][j] == bgPix) {
                            pixForChar[rowIndex][colIndex] = 0;
                        } else {
                            pixForChar[rowIndex][colIndex] = 1;
                        }
                        colIndex++;
                    }
                }
                rowIndex++;
            }
        }

        /*
                    for (int j = 1; j < clipHeight - 1; j++) {
                     for (int i = 1; i < clipWidth - 1; i++) {
                      System.out.print(pixels[i][j] + " ");
                     }
                     System.out.println();
                    }
                    System.out.println("--------------------");
                   */

        for (int[] aPixForChar : pixForChar) {
            for (int anAPixForChar : aPixForChar) {
                System.out.print(anAPixForChar + " ");
            }
            System.out.println();
        }

        EventQueue.invokeLater(new Runnable() {
            public void run() {
                try {
                    final JFileChooser fc = new JFileChooser();
                    fc.setFileFilter(new FileFilter() {
                        public boolean accept(File f) {
                            return f.getName().endsWith(".pix");
                        }

                        public String getDescription() {
                            return "Pixel File";
                        }
                    });
                    fc.setAcceptAllFileFilterUsed(true);
                    fc.showSaveDialog(fr);
                    File selFile = fc.getSelectedFile();
                    //filename suffixed with "_.pix" instead of ".pix" since
                    // "_" is used to find subtring of filename in Recognizer.java
                    if (selFile != null) {
                        // if filename doesnot endwith pix add as suffix " _.pix"
                        if (!selFile.getName().endsWith(".pix")) {
                            selFile = new File(selFile.getAbsolutePath() + "_" +
                                    ".pix");
                        }
                        //if filename ends with .pix do  the necessary to include "_"
                        //before pix in the filename
                        else {
                            String fileName = selFile.getAbsolutePath();
                            fileName = fileName.substring(0,
                                    fileName.indexOf(".pix"));
                            selFile = new File(fileName + "_" + ".pix");
                        }
                        FileOutputStream fos = new FileOutputStream(selFile);
                        ObjectOutputStream oos = new ObjectOutputStream(fos);
                        oos.writeObject(pixForChar);
                        oos.close();
                        fos.close();
                    }
                } catch (IOException ie) {
                    ie.printStackTrace();
                }
            }
        });
    }

}
