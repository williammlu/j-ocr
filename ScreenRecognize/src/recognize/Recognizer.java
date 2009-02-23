package recognize;

import static recognize.ScreenVariables.recognizing;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.StringTokenizer;

public class Recognizer {

    final private Frame fr;
    final private Graphics g;
    final private int spaceWidth;
    final private int left;
    final private int top;
    final private int clipWidth;
    final private int clipHeight;
    final private int maxOffset;
    private final int[][] pixels;
    private int bgPix = 0;
    private String recognizedChars = "";
    private String filteredChars = "";
    private boolean bgPixelFound = false;
    private int offset;
    private ArrayList<String> matches;
    private ArrayList<Integer> matchedLetterTops;
    private ArrayList<Integer> matchWidths;
    private ArrayList<Double> matchRatiosWithScan;
    private ArrayList<Integer> matchOffsets;
    private ArrayList<Integer> matchCharWidths;
    private ArrayList<Double> matchRatiosWithChar;
    private boolean boundaryFound = false;
    private boolean prevCharBoundaryFound = true;
    private int numberofSpaces = 0;
    static private int maxCharWidth = 0;
    static private HashMap<String, int[][]> pixMap;
    private static Robot robot;

    static {
        if (robot == null) {
            try {
                robot = new Robot();
            } catch (AWTException e) {
                e.printStackTrace();
                System.out.println("Could not create robot instance");
                System.exit(1);
            }
        }
    }

    /**
     * This method fills pixel array with the contents of the raster.
     *
     * @param pixelToFill two dimensional integer array
     * @param left        left index of the rectangle
     * @param top         top index of the rectangle
     * @param clipWidth   clipWidth index of the rectangle
     * @param clipHeight  clipHeight index of the rectangle
     */
    private void fillPixelArrayWithRaster(int left, int top, int clipWidth, int clipHeight, int pixelToFill[][]) {
        Rectangle clipRect = new Rectangle(left, top, clipWidth, clipHeight);
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

    private void loadPixImages(String path) {
        pixMap = new HashMap<String, int[][]>();
        String pixFiles[] = new File(path).list();
        for (String file : pixFiles) {
            if (file.endsWith(".pix")) {
                int pixData[][] = null;
                try {
                    FileInputStream fis = new FileInputStream(new File(path, file));
                    ObjectInputStream ois = new ObjectInputStream(fis);
                    pixData = (int[][]) ois.readObject();
                    if (pixData[0].length > maxCharWidth) {
                        maxCharWidth = pixData[0].length;
                    }
                    ois.close();
                    fis.close();
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                }
                pixMap.put(file.substring(0, file.indexOf(".pix")), pixData);
                //System.out.println("file : " + file);

                if (fr != null) {
                    fr.getGraphics().clearRect(400, 700, 600, 100);
                    fr.getGraphics().drawString("loading... " + file, 500, 750);
                }
            }
        }

        if (fr != null) {
            fr.getGraphics().clearRect(400, 700, 600, 100);
            fr.getGraphics().drawString("Done loading", 500, 750);
        }
        System.out.println("Done loading");
    }

    public Recognizer(Frame fr, Graphics g, int spaceWidth, int left, int top, int clipWidth, int clipHeight, int maxOffset, String path, boolean refresh) {
        this.fr = fr;
        this.g = g;
        this.spaceWidth = spaceWidth;
        this.left = left;
        this.top = top;
        this.clipWidth = clipWidth;
        this.clipHeight = clipHeight;
        this.pixels = new int[clipHeight][clipWidth];
        this.maxOffset = maxOffset;
        if (refresh) {
            maxCharWidth = 0;
            loadPixImages(path);
        }
        fillPixelArrayWithRaster(left, top, clipWidth, clipHeight, this.pixels);
        performAreaRecognizing();
    }

    /**
     * recognising the characters in the rectangular area
     */
    private void performAreaRecognizing() {

        while (performCharRecognizing()) {
        }

        if (bgPixelFound) {
            StringTokenizer st = new StringTokenizer(recognizedChars, "|");
            while (st.hasMoreTokens()) {
                String token = st.nextToken();
                if (token.startsWith("space")) {
                    token = " ";
                } else if (token.startsWith("_")) {
                    token = "_";
                } else {
                    token = token.substring(0, token.indexOf("_"));
                    if (token.startsWith("star")) {
                        token = "*";
                    } else if (token.startsWith("pipe")) {
                        token = "|";
                    } else if (token.startsWith("doubleQuote")) {
                        token = "\"";
                    } else if (token.startsWith("colon")) {
                        token = ":";
                    } else if (token.startsWith("leftAngleBracket")) {
                        token = "<";
                    } else if (token.startsWith("rightAngleBracket")) {
                        token = ">";
                    } else if (token.startsWith("questionMark")) {
                        token = "?";
                    } else if (token.startsWith("backSlash")) {
                        token = "\\";
                    } else if (token.startsWith("slash")) {
                        token = "/";
                    }
                }
                filteredChars += token;
            }
            if (fr != null) {
                recognizing = false;
                JOptionPane.showMessageDialog(fr, "     " + filteredChars,
                        "HAHAHA",
                        JOptionPane.INFORMATION_MESSAGE);
            }
        }
        System.out.println("filteredChars " + filteredChars);
        System.out.println("recognizedChars " + recognizedChars);
        System.out.println("-------------------------------------------------------------------------------");
    }

    /**
     * find the best match from all possible matches
     *
     * @return boolean if char was recognized
     */
    private boolean performCharRecognizing() {
        System.out.println("performCharRecognizing");
        boolean charEncountered = false;
        int letterTop = 0;
        int letterBottom = 0;
        int xSliderStart = 0;
        int windowWidth = 1;
        int xSliderEnd = 0;
        int bgLineCount;
        matches = new ArrayList<String>(5);
        matchedLetterTops = new ArrayList<Integer>(5);
        matchWidths = new ArrayList<Integer>(5);
        matchRatiosWithScan = new ArrayList<Double>(5);
        matchOffsets = new ArrayList<Integer>(5);
        matchCharWidths = new ArrayList<Integer>(5);
        matchRatiosWithChar = new ArrayList<Double>(5);
        boolean computedXsliderStart = false;

        while (xSliderEnd < clipWidth && windowWidth <= maxCharWidth) {
            bgLineCount = 0;
            numberofSpaces = 0;
            if (!computedXsliderStart) {
                if (g != null) {
                    g.setColor(Color.GREEN);
                }
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
                    if ((pixCnt[0] < pixCnt[1] ? pixCnt[0] : pixCnt[1]) == 0 &&
                            (top + 1) <= (top + clipHeight - 2)) {
                        if (g != null) {
                            g.drawLine(left + j, top + 1, left + j,
                                    top + clipHeight - 2);
                        }
                        //for (int i = 1; i < clipHeight - 1; i++) {
                        bgPixelFound = true;
                        //bgPix = pixels[i][j];
                        bgPix = pixels[1][j];
                        //}
                    } else {
                        charEncountered = true;
                        xSliderStart = j;
                        break;
                    }
                }

                if (!charEncountered) {
                    break;
                }

                if (!bgPixelFound) {
                    if (fr != null) {
                        JOptionPane.showMessageDialog(fr,
                                "Could not find Background Pixel in the Rectangle",
                                "Error", JOptionPane.INFORMATION_MESSAGE);
                    }
                    break;
                }
                computedXsliderStart = true;
                xSliderEnd = xSliderStart + windowWidth - 1;
            }

            /*
               System.out.println("xSliderStart " + xSliderStart + ", xSliderEnd " + xSliderEnd);
               System.out.println("pixels.length " + pixels.length + ", pixels[0].length " + pixels[0].length);
               System.out.println("clipWidth " + clipWidth);
               */
            // scanning the character left to right from top to bottom to find letter top
            if (g != null) {
                g.setColor(Color.orange);
            }
            for (int i = 1; i < clipHeight - 1; i++) {
                int currentValue = 0;
                int pixCnt[] = new int[2];
                int pixVal[] = new int[2];
                int pixTracker = 0;
                for (int j = xSliderStart; j <= xSliderEnd; j++) {
                    if (j == xSliderStart) {
                        currentValue = pixels[i][j];
                        pixCnt[pixTracker % 2]++;
                        pixVal[pixTracker % 2] = pixels[i][j];
                    } else if (pixels[i][j] != currentValue) {
                        pixTracker++;
                        pixCnt[pixTracker % 2]++;
                        pixVal[pixTracker % 2] = pixels[i][j];
                    } else {
                        pixCnt[pixTracker % 2]++;
                    }
                }

                int minorityPixCnt = pixCnt[0] < pixCnt[1] ? pixCnt[0] :
                        pixCnt[1];
                int majorityIndex = pixCnt[0] > pixCnt[1] ? 0 : 1;
                //if only background pixel found, implies letter has not started, continue searching for letter top
                // replace the area scanned with horizontal line(orange)
                if (pixCnt[0] + pixCnt[1] > 0 && minorityPixCnt == 0 &&
                        pixVal[majorityIndex] == bgPix &&
                        (left + 1) <= (left + clipWidth - 2)) {
                    if (g != null) {
                        g.drawLine(left + xSliderStart, top + i,
                                left + xSliderEnd, top + i);
                    }
                } else {
                    letterTop = i;
                    break;
                }
            }

            // scanning the character left to right from bottom to top to find letter bottom
            for (int i = clipHeight - 2; i >= 1; i--) {
                int currentValue = 0;
                int pixCnt[] = new int[2];
                int pixVal[] = new int[2];
                int pixTracker = 0;
                for (int j = xSliderStart; j <= xSliderEnd; j++) {
                    if (j == xSliderStart) {
                        currentValue = pixels[i][j];
                        pixCnt[pixTracker % 2]++;
                        pixVal[pixTracker % 2] = pixels[i][j];
                    } else if (pixels[i][j] != currentValue) {
                        pixTracker++;
                        pixCnt[pixTracker % 2]++;
                        pixVal[pixTracker % 2] = pixels[i][j];
                    } else {
                        pixCnt[pixTracker % 2]++;
                    }
                }

                int minorityPixCnt = pixCnt[0] < pixCnt[1] ? pixCnt[0] :
                        pixCnt[1];
                int majorityIndex = pixCnt[0] > pixCnt[1] ? 0 : 1;
                //if only background pixel found, implies letter has not started, continue searching for letter bottom
                // replace the area scanned with horizontal line(orange)
                if (pixCnt[0] + pixCnt[1] > 0 && minorityPixCnt == 0 &&
                        pixVal[majorityIndex] == bgPix &&
                        (left + 1) <= (left + clipWidth - 2)) {
                    if (g != null) {
                        g.drawLine(left + xSliderStart, top + i,
                                left + xSliderEnd, top + i);
                    }
                } else {
                    letterBottom = i;
                    break;
                }
            }

//			System.out.println("top and bottom : " + letterTop + " " + letterBottom);
//			System.out.println("xSliderStart and xSliderEnd : " + xSliderStart + " " + xSliderEnd);

            if (!prevCharBoundaryFound) {
                for (int i = 0; i <= maxOffset; i++) {
                    offset = i;
                    checkAgainstKnowledge(letterTop, letterBottom, xSliderStart,
                            xSliderEnd);
                }
            } else {
                offset = 0;
                checkAgainstKnowledge(letterTop, letterBottom, xSliderStart,
                        xSliderEnd);
            }

            // Check for gap and then if no gap, increment windowWidth and continue scanning
            //if there is a gap record it and proceed to find the best match of current character
            //if there is a gap,scanning done till we encounter the next character to find gap length
            int j = xSliderEnd + 1;
            int currentValue = 0;
            int pixCnt[] = new int[2];
            int pixTracker = 0;
            while (j < clipWidth) {

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

                if ((pixCnt[0] == 0 || pixCnt[1] == 0) && (currentValue == bgPix)) {
                    boundaryFound = true;
                    bgLineCount++;
                    System.out.println("bgLineCount:" + bgLineCount);
                    if ((bgLineCount % spaceWidth) == 0) {
                        numberofSpaces++;
                        //System.out.println("numberofSpaces" + numberofSpaces);
                    }
                } else {
                    windowWidth++;
                    xSliderEnd = xSliderStart + windowWidth - 1;
                    //System.out.println("bgLineCount:" + bgLineCount);
                    break;
                }
                j++;
                //System.out.println("clipWidth : " + clipWidth + ", j : " + j);
            }

            if (j >= clipWidth || numberofSpaces > 0) {
                break;
            }

//			try {
//				Thread.sleep(1000);
//			} catch (InterruptedException mp) {
//				mp.printStackTrace();
//			}

        }

        return findBestMatch(xSliderStart);
    }

    /**
     * finding the best match from all possible matches
     *
     * @param xSliderStart left boundary of the alphabet
     * @return returns boolean
     */
    private boolean findBestMatch(int xSliderStart) {
        boolean retValue;
        if (prevCharBoundaryFound) {
            prevCharBoundaryFound = false;
        }
        if (matches.size() > 0) {
            int size = matchRatiosWithScan.size();

            int highestMatchWidth = 0;
            for (int i = 0; i < size; i++) {
                int matchWidth = matchWidths.get(i);
                if (matchWidth > highestMatchWidth) {
                    highestMatchWidth = matchWidth;
                }
                System.out.println("matchWidth " + matchWidths.get(i) +
                        ", matchRatioWithScan " +
                        matchRatiosWithScan.get(i) +
                        ", matchOffset " + matchOffsets.get(i) +
                        ", matchCharWidth " + matchCharWidths.get(i) +
                        ", matchRatioWithChar " +
                        matchRatiosWithChar.get(i) + "; for " +
                        matches.get(i));
            }

            double highestMatchRatioWithScan = 0;
            for (int i = 0; i < size; i++) {
                double matchRatioWithScan = matchRatiosWithScan.get(i);
                if (matchRatioWithScan > highestMatchRatioWithScan &&
                        matchWidths.get(i) == highestMatchWidth) {
                    highestMatchRatioWithScan = matchRatioWithScan;
                }
            }

            int lowestOffset = maxOffset;
            for (int i = 0; i < size; i++) {
                int matchOffset = matchOffsets.get(i);
                if (matchOffset < lowestOffset &&
                        matchWidths.get(i) == highestMatchWidth &&
                        matchRatiosWithScan.get(i) == highestMatchRatioWithScan) {
                    lowestOffset = matchOffset;
                }
            }

            int highestMatchCharWidth = 0;
            if (boundaryFound) {
                boundaryFound = false;
                prevCharBoundaryFound = true;
                for (int i = 0; i < size; i++) {
                    int matchCharWidth = matchCharWidths.get(i);
                    if (matchCharWidth > highestMatchCharWidth &&
                            matchCharWidth <= highestMatchWidth &&
                            matchWidths.get(i) == highestMatchWidth &&
                            matchRatiosWithScan.get(i) == highestMatchRatioWithScan &&
                            matchOffsets.get(i) == lowestOffset) {
                        highestMatchCharWidth = matchCharWidth;
                    }
                }
            } else {
                for (int i = 0; i < size; i++) {
                    int matchCharWidth = matchCharWidths.get(i);
                    if (matchCharWidth > highestMatchCharWidth &&
                            matchWidths.get(i) == highestMatchWidth &&
                            matchRatiosWithScan.get(i) == highestMatchRatioWithScan &&
                            matchOffsets.get(i) == lowestOffset) {
                        highestMatchCharWidth = matchCharWidth;
                    }
                }
            }

            int index = 0;
            double highestMatchRatioWithChar = 0;
            for (int i = 0; i < size; i++) {
                double matchRatioWithChar = matchRatiosWithChar.get(i);
                if (matchRatioWithChar > highestMatchRatioWithChar &&
                        matchWidths.get(i) == highestMatchWidth &&
                        matchRatiosWithScan.get(i) == highestMatchRatioWithScan &&
                        matchOffsets.get(i) == lowestOffset &&
                        matchCharWidths.get(i) == highestMatchCharWidth) {
                    highestMatchRatioWithChar = matchRatioWithChar;
                    index = i;
                }
            }

            System.out.println("Matched : matchWidth " + matchWidths.get(index) +
                    ", matchRatioWithScan " +
                    matchRatiosWithScan.get(index) +
                    ", matchOffset " + matchOffsets.get(index) +
                    ", matchCharWidth " + matchCharWidths.get(index) +
                    ", matchRatioWithChar " +
                    matchRatiosWithChar.get(index) + "; for " +
                    matches.get(index));

            recognizedChars += "|" + matches.get(index);
            //System.out.println("numberofSpaces when return value true"+numberofSpaces);
            // including the blank spaces found after character in recognised chars
            for (int i = 0; i < numberofSpaces; i++) {
                recognizedChars += "|" + "space";
            }

            int[][] storedPix = pixMap.get(matches.get(index));
            // this is where we replace the pixels array with the stored pix
            for (int i = 0; i < storedPix.length; i++) {
                for (int j = 0;
                     j < storedPix[i].length - matchOffsets.get(index);
                     j++) {
                    if (storedPix[i][j + matchOffsets.get(index)] == 1) {
                        pixels[matchedLetterTops.get(index) + i][xSliderStart +
                                j] = bgPix;
                    }
                }
            }
            retValue = true;
        } else {
            //code for including trailing spaces in recognisedChars

//			System.out.println("numberofSpaces when return value false " + numberofSpaces);
//			for (int i = 0; i < numberofSpaces; i++) {
//				recognizedChars += "|" + "space";
//			}

            retValue = false;
        }
        System.out.println("retValue " + retValue);
        System.out.println("*****************************");
        return retValue;
    }

    /**
     * check the area so far scanned with the knowledge base and find whether  it matches any character in the knowledge base
     *
     * @param letterTop    top boundary of the letter scanned
     * @param letterBottom bottom boundary of the letter scanned
     * @param xStart       left boundary of the letter scanned
     * @param xEnd         right boundary of the letter scanned
     */
    private void checkAgainstKnowledge(final int letterTop,
                                       final int letterBottom, final int xStart,
                                       final int xEnd) {
        int srcWidth = xEnd - xStart + 1;
        int srcHeight = letterBottom - letterTop + 1;
        int[][] copy = new int[srcHeight][srcWidth];

        // abcdefghijklmn  o  pqrstuvwxyz	w h  v	vw kw jw uw tw fw x y z "'.,~`;: ! =  != ` +-*/?><,./":';}{][|\+      -_     = _-_ __  ~`!@#$%^&*()
        // vignesha@hcl.in; vaidhyanathang@hcl.in
        // Creating Copy Array
        for (int i = 0; i < srcHeight; i++) {
            for (int j = 0; j < srcWidth; j++) {
                if (pixels[letterTop + i][xStart + j] == bgPix) {
                    copy[i][j] = 0;
                } else {
                    copy[i][j] = 1;
                }
            }
        }

//		for (int i = 0; i < srcHeight; i++) {
//			for (int j = 0; j < srcWidth; j++) {
//				System.out.print(copy[i][j] + " ");
//			}
//			System.out.println();
//		}
//		System.out.println("------------------------------");


        for (String key : pixMap.keySet()) {
            //System.out.println("key " + key);
            int[][] storedPix = pixMap.get(key);

            if ((storedPix.length != srcHeight) ||
                    (storedPix[0].length - offset < srcWidth)) {
                //System.out.println("continuing 1");
                //System.out.println("**************");
                continue;
            }

//			System.out.println("storedPix.length " + storedPix.length);
//			System.out.println("srcHeight " + srcHeight);
//			System.out.println("storedPix[0].length " + storedPix[0].length);
//			System.out.println("srcWidth " + srcWidth);
//			System.out.println("-----------------");

            // Performing Matching against stored pixels
            boolean match = true;
            int matchCount = 0;
            mainLoop:
            for (int i = 0; i < srcHeight; i++) {
                for (int j = 0; j < srcWidth; j++) {

                    //exact match-ideal case
                    if (storedPix[i][j + offset] == copy[i][j]) {
                        matchCount++;
                    } else {
                        // looking at only the 1's when there is a mismatch, as the zeros can be taken by neighbours
                        if (!prevCharBoundaryFound) {
                            if (storedPix[i][j + offset] == 1) {
                                // looking at all the pixels left to the mismatch. If there is a one then it is not
                                // a valid pixel loss after negation.
                                for (int k = 0; k <= j; k++) {
                                    if (copy[i][k] == 1) {
                                        match = false;
                                        break mainLoop;
                                    }
                                }
                            }
                        } else {
                            //If we are missing a one that means this is definitely not a valid char. Overlapping can only
                            //take the zeros not the 1's.
                            if (storedPix[i][j] == 1) {
                                match = false;
                                break mainLoop;
                            }
                        }
                    }
                    //System.out.print(copy[i][j] + " ");
                }
            }
            //System.out.println("-----------------");

            // match ratio with the currently scanned portion
            double matchRatioWithScan = (1.0 * matchCount) /
                    (srcWidth * srcHeight);
            // width wise match ratio with the stored pix taking into account the offset
            double matchRatioWithChar = (1.0 * srcWidth) /
                    (storedPix[0].length - offset);

            if (match && (matchCount > 0) && matchRatioWithScan > 0.8 &&
                    matchRatioWithChar >=
                            0.6 /*&& (srcWidth == (storedPix[0].length - offset))*/) {
                //System.out.println("Match found for " + key);
                matches.add(key);
                matchedLetterTops.add(letterTop);
                matchWidths.add(srcWidth);
                matchRatiosWithScan.add(matchRatioWithScan);
                matchOffsets.add(offset);
                matchCharWidths.add(storedPix[0].length - offset);
                matchRatiosWithChar.add(matchRatioWithChar);
            }
//			else {
//				System.out.println("continuing 2");
//				System.out.println("**************");
//			}
        }
    }

    public String getRecognizedChars() {
        return recognizedChars;
    }

    public String getFilteredChars() {
        return filteredChars;
    }
}
