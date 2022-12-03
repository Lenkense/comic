package com.comic;

import java.util.LinkedHashSet;
import java.util.Set;

import ij.process.BinaryProcessor;

public class Trail{

    public final static int fg = 0;

    public final static int bg = 255;

    Set<Integer> trail;
    int xmax = 0;
    int ymax = 0;
    int xmin = 0;
    int ymin = 0;
    int boundingBoxWidth = 0;
    int boundingBoxHeight = 0;
    double widthRatio = 0.0; 
    double heightRatio = 0.0;
    double aspectRatio = 0.0;
    double areaRatio = 0.0;
    int first[];
    int last[];
    //TODO: consider deleteing delta
    int delta[];
    int id = 0;
    int count = 0;
    long key = 0;

    public Trail(BinaryProcessor binary, int x, int y){
        int width = binary.getWidth();
        int height = binary.getHeight();

        trail = getTrail(binary, x, y);
        boundingBox(trail, width, height);
        first = new int[2];
        last = new int[2];
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
        for(int i: trail){
            find = i;
        }
        last[0] = find % width;
        last[1] = find / width;
        delta[0] = last[0] - first[0];
        delta[1] = last[1] - first[1];
        
        key = xmin + ymin * width;
        key *= width * height;
        key += xmax + ymax * width;

    }

    public static Set<Integer> getTrail(BinaryProcessor binary, int x, int y){
        int width = binary.getWidth();
        int height = binary.getHeight();
        int x2 = x;
        int y2 = y;
        int element = x2 + y2 * width;
        Set<Integer> trail = new LinkedHashSet<Integer>();
        boolean test = trail.add(element);
        int last = 6;
        int candidate = 0;

        /*
            *  pixels are be enumerated in the following way:
            *      7  0  1
            *      6  X  2
            *      5  4  3 
            */
        int offset[][] = {  {0,-1},
                            {1,-1},
                            {1,0},
                            {1,1},
                            {0,1},
                            {-1,1},
                            {-1,0},
                            {-1,-1}};

        while(test){
            int next = last;
            for(int i=1; i < 8; i++){
                candidate = (last + i) % 8;
                if(fg == binary.getPixel(
                    x2 + offset[candidate][0],
                    y2 + offset[candidate][1]) ){
                    next = candidate;
                }
            }
            candidate = next;
            x2 += offset[candidate][0];
            y2 += offset[candidate][1];

            // preventing out of bounds
            x2 = (x2 < 0) ? 0 : x2;
            x2 = (x2 > width - 1) ? width - 1 : x2;
            y2 = (y2 < 0) ? 0 : y2;
            y2 = (y2 > height - 1) ? height - 1 : y2;

            last = (candidate + 4) % 8; // this is a 180º rotation
            element = x2 + y2 * width;
            test = trail.add(element);
        }
        return trail;
    }
    
    public boolean equals(Trail that) {
        return (this.xmax == that.xmax) 
                && (this.ymax == that.ymax) 
                && (this.xmin == that.xmin) 
                && (this.ymin == that.ymin); 
    }

    @Override
    public int hashCode(){
        Long hash = this.key;
        return hash.intValue();
    }

    public void boundingBox(Set<Integer> trail, int width, int height){
        xmax = 0;
        ymax = 0;
        xmin = width - 1;
        ymin = height - 1;
        for(int i:trail){
            int x = i % width;
            int y = i / width;
            xmax = (x > xmax) ? x : xmax;
            ymax = (y > ymax) ? y : ymax;
            xmin = (x < xmin) ? x : xmin;
            ymin = (y < ymin) ? y : ymin;
        }
    }

    }
