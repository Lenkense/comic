package com.comic;

import java.io.File;
import java.io.FilenameFilter;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

import ij.ImagePlus;
import ij.io.FileSaver;
import ij.process.BinaryProcessor;
import ij.process.ByteProcessor;
import ij.process.ColorProcessor;
import ij.process.ImageProcessor;

public class Comic {

    public static ByteProcessor convertRGBtoGray(ColorProcessor processor) {
        final int width = processor.getWidth();
        final int height = processor.getHeight();
        ByteProcessor byteProc = new ByteProcessor(width, height);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int color = processor.get(x, y);
                int b = color & 255;
                int g = (color >> 8) & 255;
                int r = (color >> 16) & 255;
                int grayLevel = 255 - (r + g + b) / 3;
                byteProc.set(x, y, grayLevel);
            }
        }
        return byteProc;
    }

    public static void printTrail(ImageProcessor processor, Trail trail, String path) {
        final int width = processor.getWidth();
        final int height = processor.getHeight();
        ColorProcessor example = new ColorProcessor(width, height);
        for (int j = trail.ymin; j <= trail.ymax; j++) {
            for (int i = trail.xmin; i <= trail.xmax; i++) {
                example.set(i, j, processor.get(i, j));
                if (trail.trail.contains(i + j * width)) {
                    for (int a = -1; a < 2; a++){
                        for (int b = -1; b < 2; b++) {
                            int x = i + a;
                            x = (x < 0) ? 0 : x;
                            x = (x > width - 1) ? width - 1 : x;
                            int y = j + b;
                            y = (y < 0) ? 0 : y;
                            y = (y > height - 1) ? height - 1 : y;
                            example.set(x, y, 0xFF00FF);
                        }
                    }
                }
            }
        }
        String name = String.format("%05d", trail.id);
        ImagePlus img = new ImagePlus("", example);
        FileSaver fs = new FileSaver(img);
        fs.saveAsJpeg(path + name + ".jpg");
    }

    public static void printGray(BinaryProcessor processor, Trail trail, String path) {
        final int width = processor.getWidth();
        final int height = processor.getHeight();
        ByteProcessor gray = new ByteProcessor(width, height);
        for (int j = trail.ymin; j <= trail.ymax; j++) {
            for (int i = trail.xmin; i <= trail.xmax; i++) {
                gray.set(i,j,processor.get(i,j));
            }
        }
        String name = String.format("%05d", trail.id);
        ImagePlus img = new ImagePlus("", gray);
        FileSaver fs = new FileSaver(img);
        fs.saveAsJpeg(path + name + "_gray.jpg");
    }

    public static BinaryProcessor PercentileBinarize(
        ByteProcessor byteProc, double percentile) {
        int width = byteProc.getWidth();
        int height = byteProc.getHeight();   
        int histrogram[] = byteProc.getHistogram();
        int threshold = 0;
        int sum = 0;
        for (int i = 0; i < 256; i++){
            sum += histrogram[i];
            if (sum > height * width * percentile){
                break;
            }
            threshold = i;
        }

        BinaryProcessor binary = new BinaryProcessor(byteProc);
        for (int y=0;y<height;y++)
        {
            for (int x=0;x<width;x++)
            {
                int grayLevel = (byteProc.get(x,y) < threshold) ? Trail.fg : Trail.bg;
                binary.set(x,y,grayLevel);
            }
        }

        return binary;
    }

    public static void main (String args[]) {
        // PARAMETERS
        String path = "dump/";
        int MIN_DIST = 10;
        double MAX_AREA = 0.125;
        double MIN_AREA = 0.0035;
        double MAX_ASPECT_RATIO = 5.0;
        double MIN_LENGTH_RATIO = 0.01;
        double PERCENTILE_THRESHOLD = 0.2;

        /*TODO:
         * create project
         * GITHUB
         * move class to a different file
         *      implment class and equals with override
         */

        //Creating a File object for directory
        File directoryPath = new File(".");
        //Creating filter for jpg files
        FilenameFilter jpgFilefilter = new FilenameFilter(){
            public boolean accept(File dir, String name) {
                String lowercaseName = name.toLowerCase();
                if (lowercaseName.endsWith(".jpg")) {
                    return true;
                } else {
                    return false;
                }
            }
        };        
        String imageFilesList[] = directoryPath.list(jpgFilefilter);
        Arrays.sort(imageFilesList);

        File dir = new File(path);
        if (!dir.exists()){
            dir.mkdirs();
        }

        for (String fileName : imageFilesList) {
            String prefix = path + fileName.substring(0,
                    fileName.length() - 4) + "/";
            System.out.println(fileName);
        
            ImagePlus imgPlus = new ImagePlus(fileName);
            ImageProcessor imgProcessor = imgPlus.getProcessor();
            int width = imgProcessor.getWidth();
            int height = imgProcessor.getHeight();
            int minsize = (width > height) ? width : height;
            minsize *= MIN_LENGTH_RATIO;

            ColorProcessor colorProcessor = imgProcessor.convertToColorProcessor();
            colorProcessor.filterRGB(ColorProcessor.RGB_FIND_EDGES, 0.0);

            //Here I have tried convolutions before finding edges
            //colorProcessor.sharpen();

            ByteProcessor byteProc = convertRGBtoGray(colorProcessor);

            BinaryProcessor binary = PercentileBinarize(byteProc,
                                PERCENTILE_THRESHOLD);

            // Here I have tried morphological operators on binary:
            // binary.dilate();
            // binary.erode(); 

            Map<Long,Trail> images =
                new LinkedHashMap<Long,Trail>();
            for (int y=0;y<height;y++) {
                for(int x=0;x<width;x++) {
                    if (binary.getPixel(x,y) == Trail.fg){
                        Trail trail = new Trail(binary, x, y);

                        if (
                            trail.delta[0] * trail.delta[0] 
                                + trail.delta[1] * trail.delta[1] < MIN_DIST * MIN_DIST
                            && trail.boundingBoxHeight > minsize
                            && trail.boundingBoxWidth > minsize
                            && trail.areaRatio > MIN_AREA  
                            && trail.areaRatio < MAX_AREA
                            && trail.aspectRatio < MAX_ASPECT_RATIO
                        ){
                            long key = trail.key;
                            if(images.get(key) == null){
                                trail.id = images.size();
                                trail.count = 1;
                                images.put(key, trail);
                            }else{
                                Trail update = images.get(key);
                                update.count++;
                            }
                        } 
                    }
                }
            }

            dir = new File(prefix);
            if (!dir.exists()){
                dir.mkdirs();
            }

            ImagePlus img = new ImagePlus("", byteProc);
            FileSaver fs = new FileSaver(img);
            fs.saveAsJpeg(prefix + "gray.jpg");
            for(Map.Entry<Long,Trail> entry: images.entrySet()){
                Trail trail = entry.getValue();

                printTrail(imgProcessor, trail, prefix);
                printGray(binary, trail, prefix);

                System.out.format("%05d, ", trail.id);
                System.out.format("%013d, ", trail.key);
                System.out.format("%d, ", trail.boundingBoxWidth);
                System.out.format("%d, ", trail.boundingBoxHeight);
                System.out.format("%f, ", trail.widthRatio);
                System.out.format("%f, ", trail.heightRatio);
                System.out.format("%f, ", trail.aspectRatio);
                System.out.format("%f, ", trail.areaRatio);
                System.out.format("%d, ", trail.delta[0]);
                System.out.format("%d, ", trail.delta[1]);
                System.out.format("%d%n", trail.count);
            }
        }
    }
}

