package com.comic;

import ij.process.BinaryProcessor;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 *
 */
public class Trail {

    /**
     *
     */
    public static final int FG = 0;
    /**
     *
     */
    public static final int BG = 255;
    /**
     *  pixels are enumerated in the following way.
     *      7  0  1
     *      6  X  2
     *      5  4  3
     */
    public static final int[][] OFFSET = {
        {0, -1},
        {1, -1},
        {1, 0},
        {1, 1},
        {0, 1},
        {-1, 1},
        {-1, 0},
        {-1, -1}
    };
    private static final int DEFAULT_NEIGHBOUR = 6;

    /**
     *
     */
    private Set<Integer> trail;
    private int colorTrail = 0xFF00FF;
    private int xmax;
    private int ymax;
    private int xmin;
    private int ymin;
    private int boundingBoxWidth;
    private int boundingBoxHeight;
    private double widthRatio;
    private double heightRatio;
    private double aspectRatio;
    private double areaRatio;
    private int[] delta;
    private int id;
    private int count;
    private long key;

    /**
     *
     */
    public Trail(BinaryProcessor binary, int x, int y) {
        int width = binary.getWidth();
        int height = binary.getHeight();

        trail = getTrail(binary, x, y);
        boundingBox(width, height);
        int[] first = new int[2];
        int[] last = new int[2];
        delta = new int[2];

        boundingBoxWidth = xmax - xmin;
        boundingBoxHeight = ymax - ymin;
        widthRatio = 1.0 * boundingBoxWidth / width;
        heightRatio = 1.0 * boundingBoxHeight / height;
        aspectRatio = 1.0 * boundingBoxWidth / boundingBoxHeight;
        areaRatio = widthRatio * heightRatio;
        first[0] = x;
        first[1] = y;

        int find = 0;
        for (int i: trail) {
            find = i;
        }
        last[0] = find % width;
        last[1] = find / width;
        delta[0] = last[0] - first[0];
        delta[1] = last[1] - first[1];

        key = xmin + ymin * width;
        key *= width * height;
        key += xmax + ymax * width;

        count = 1;

    }

    /**
     *
     */
    private static Set<Integer> getTrail(BinaryProcessor binary, int x, int y) {
        int width = binary.getWidth();
        int height = binary.getHeight();
        int x2 = x;
        int y2 = y;
        int element = x2 + y2 * width;
        Set<Integer> trail = new LinkedHashSet<Integer>();
        boolean test = trail.add(element);
        int last = DEFAULT_NEIGHBOUR;
        int candidate = 0;

        while (test) {
            int next = last;
            for (int i = 1; i < OFFSET.length; i++) {
                candidate = (last + i) % OFFSET.length;
                if (FG == binary.getPixel(
                    x2 + OFFSET[candidate][0],
                    y2 + OFFSET[candidate][1])) {
                    next = candidate;
                }
            }
            candidate = next;
            x2 += OFFSET[candidate][0];
            y2 += OFFSET[candidate][1];

            // preventing out of bounds
            x2 = (x2 < 0) ? 0 : x2;
            x2 = (x2 > width - 1) ? width - 1 : x2;
            y2 = (y2 < 0) ? 0 : y2;
            y2 = (y2 > height - 1) ? height - 1 : y2;

            // next line is a 180º rotation
            last = (candidate + OFFSET.length / 2) % OFFSET.length;
            element = x2 + y2 * width;
            test = trail.add(element);
        }
        return trail;
    }

    /**
     *
     */
    private void boundingBox(int width, int height) {
        xmax = 0;
        ymax = 0;
        xmin = width - 1;
        ymin = height - 1;
        for (int i : this.trail) {
            int x = i % width;
            int y = i / width;
            xmax = (x > xmax) ? x : xmax;
            ymax = (y > ymax) ? y : ymax;
            xmin = (x < xmin) ? x : xmin;
            ymin = (y < ymin) ? y : ymin;
        }
    }

    public Set<Integer> get_trail() {
        return trail;
    }

    public void clear_trail() {
        this.trail.clear();
    }

    public int get_colorTrail() {
        return colorTrail;
    }

    public int get_xmax() {
        return xmax;
    }

    public int get_ymax() {
        return ymax;
    }

    public int get_xmin() {
        return xmin;
    }

    public int get_ymin() {
        return ymin;
    }

    public int get_boundingBoxWidth() {
        return boundingBoxWidth;
    }

    public int get_boundingBoxHeight() {
        return boundingBoxHeight;
    }

    public double get_widthRatio() {
        return widthRatio;
    }

    public double get_heightRatio() {
        return heightRatio;
    }

    public double get_aspectRatio() {
        return aspectRatio;
    }

    public double get_areaRatio() {
        return areaRatio;
    }

    public int[] get_delta() {
        return delta;
    }

    public int get_id() {
        return id;
    }

    public int set_id(int n) {
        id = n;
        return id;
    }

    public int get_count() {
        return count;
    }

    public int update_count() {
        count++;
        return count;
    }

    public long get_key() {
        return key;
    }

    /*TODO:
     * implment class and equals with override
     * use it in code
     */

    @Override
    public boolean equals(Object o) {
        Trail that = (Trail) o;
        return this.xmax == that.xmax
                && this.ymax == that.ymax
                && this.xmin == that.xmin
                && this.ymin == that.ymin;
    }

    @Override
    public int hashCode() {
        Long hash = this.key;
        return hash.intValue();
    }
}
