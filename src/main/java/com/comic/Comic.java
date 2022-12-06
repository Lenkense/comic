package com.comic;

import ij.ImagePlus;
import ij.io.FileSaver;
import ij.process.BinaryProcessor;
import ij.process.ByteProcessor;
import ij.process.ColorProcessor;
import ij.process.ImageProcessor;
import java.io.File;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 *
 */
public final class Comic {

    private static final int RED = 0x0000FF;
    private static final int GREEN = 0x00FF00;
    private static final int BLUE = 0xFF0000;
    private static final int RED_OFFSET = 0;
    private static final int GREEN_OFFSET = 8;
    private static final int BLUE_OFFSET = 16;
    private static final int MAX_BYTE = 255;
    private static final int NUMBER_OF_CHANNELS = 3;

    private static final int MIN_DIST = 10;
    private static final double MAX_AREA = 0.125;
    private static final double MIN_AREA = 0.0035;
    private static final double MAX_ASPECT_RATIO = 5.0;
    private static final double MIN_LENGTH_RATIO = 0.01;
    private static final double PERCENTILE_THRESHOLD = 0.2;

    private Comic() {
        throw new UnsupportedOperationException();
    }

    /**
     *
     */
    private static ByteProcessor convertRGBtoGray(ColorProcessor processor) {
        final int width = processor.getWidth();
        final int height = processor.getHeight();
        ByteProcessor byteProc = new ByteProcessor(width, height);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int color = processor.get(x, y);
                int b = (color & RED) >> RED_OFFSET;
                int g = (color & GREEN) >> GREEN_OFFSET;
                int r = (color & BLUE) >> BLUE_OFFSET;
                int grayLevel = MAX_BYTE - (r + g + b) / NUMBER_OF_CHANNELS;
                byteProc.set(x, y, grayLevel);
            }
        }
        return byteProc;
    }

    /**
     *
     */
    private static void paintTrail(ColorProcessor example,
            int x, int y, int value) {
        final int width = example.getWidth();
        final int height = example.getHeight();
        example.set(x, y, value);
        for (int[] r: Trail.OFFSET) {
            int x2 = x + r[0];
            x2 = (x2 < 0) ? 0 : x2;
            x2 = (x2 > width - 1) ? width - 1 : x2;
            int y2 = y + r[1];
            y2 = (y2 < 0) ? 0 : y2;
            y2 = (y2 > height - 1) ? height - 1 : y2;
            example.set(x2, y2, value);
        }
    }

    /**
     *
     */
    private static void printTrail(ImageProcessor processor,
            Trail trail, String path) {
        final int width = processor.getWidth();
        final int height = processor.getHeight();
        ColorProcessor example = new ColorProcessor(width, height);
        for (int j = trail.get_ymin(); j <= trail.get_ymax(); j++) {
            for (int i = trail.get_xmin(); i <= trail.get_xmax(); i++) {
                example.set(i, j, processor.get(i, j));
                if (trail.get_trail().contains(i + j * width)) {
                    paintTrail(example, i, j, trail.get_colorTrail());
                }
            }
        }
        String name = String.format("%05d", trail.get_id());
        ImagePlus img = new ImagePlus("", example);
        FileSaver fs = new FileSaver(img);
        fs.saveAsJpeg(path + name + ".jpg");
        img.close();
    }

    /**
     *
     */
    private static void printGray(BinaryProcessor processor,
            Trail trail, String path) {
        final int width = processor.getWidth();
        final int height = processor.getHeight();
        ByteProcessor gray = new ByteProcessor(width, height);
        for (int j = trail.get_ymin(); j <= trail.get_ymax(); j++) {
            for (int i = trail.get_xmin(); i <= trail.get_xmax(); i++) {
                gray.set(i, j, processor.get(i, j));
            }
        }
        String name = String.format("%05d", trail.get_id());
        ImagePlus img = new ImagePlus("", gray);
        FileSaver fs = new FileSaver(img);
        fs.saveAsJpeg(path + name + "_gray.jpg");
        img.close();
    }

    /**
     *
     */
    private static BinaryProcessor binarizePercentile(
        ByteProcessor byteProc, double percentile) {
        int width = byteProc.getWidth();
        int height = byteProc.getHeight();
        int[] histrogram = byteProc.getHistogram();
        int threshold = 0;
        int sum = 0;
        for (int i = 0; i <= MAX_BYTE; i++) {
            sum += histrogram[i];
            if (sum > height * width * percentile) {
                break;
            }
            threshold = i;
        }

        BinaryProcessor binary = new BinaryProcessor(byteProc);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int grayLevel =
                    (byteProc.get(x, y) < threshold) ? Trail.FG : Trail.BG;
                binary.set(x, y, grayLevel);
            }
        }
        return binary;
    }

    /**
     *
     */
    public static void main(String[] args) {
        // PARAMETERS
        String outputPath = "dump/";
        String fileExtension = ".jpg";

        //Creating a File object for directory
        File directoryPath = new File(".");
        //Creating filter for jpg files
        FilenameFilter jpgFilefilter = new FilenameFilter() {
            public boolean accept(File dir, String name) {
                String lowercaseName = name.toLowerCase();
                return lowercaseName.endsWith(fileExtension);
            }
        };
        String[] imageFilesList = directoryPath.list(jpgFilefilter);
        Arrays.sort(imageFilesList);

        File dir = new File(outputPath);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        for (String fileName : imageFilesList) {
            String prefix = outputPath + fileName.substring(0,
                    fileName.length() - fileExtension.length()) + "/";

            ImagePlus imgPlus = new ImagePlus(fileName);
            ImageProcessor imgProcessor = imgPlus.getProcessor();
            int width = imgProcessor.getWidth();
            int height = imgProcessor.getHeight();
            int minsize = (width > height) ? width : height;
            minsize *= MIN_LENGTH_RATIO;

            ColorProcessor colorProcessor = imgProcessor.convertToColorProcessor();

            //Here I have tried convolutions before finding edges
            //colorProcessor.sharpen();
            //colorProcessor.blurGaussian(0.1);//PARAMETER
            colorProcessor.filterRGB(ColorProcessor.RGB_FIND_EDGES, 0.0);

            ByteProcessor byteProc = convertRGBtoGray(colorProcessor);

            BinaryProcessor binary = binarizePercentile(byteProc,
                                PERCENTILE_THRESHOLD);

            // Here I have tried morphological operators on binary:
            //binary.dilate();
            //binary.erode();

            Map<Long, Trail> images =
                new LinkedHashMap<Long, Trail>();
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    if (binary.getPixel(x, y) == Trail.FG) {
                        Trail trail = new Trail(binary, x, y);

                        if (trail.get_delta()[0] * trail.get_delta()[0]
                            + trail.get_delta()[1] * trail.get_delta()[1]
                            < MIN_DIST * MIN_DIST
                            && trail.get_boundingBoxHeight() > minsize
                            && trail.get_boundingBoxWidth() > minsize
                            && trail.get_areaRatio() > MIN_AREA
                            && trail.get_areaRatio() < MAX_AREA
                            && trail.get_aspectRatio() < MAX_ASPECT_RATIO
                        ) {
                            long key = trail.get_key();
                            if (images.get(key) == null) {
                                trail.set_id(images.size());
                                images.put(key, trail);
                            } else {
                                Trail update = images.get(key);
                                update.update_count();
                            }
                        } else {
                            trail.clear_trail();
                        }
                    }
                }
            }

            imgPlus.close();
            dir = new File(prefix);
            if (!dir.exists()) {
                dir.mkdirs();
            }

            ImagePlus img = new ImagePlus("", byteProc);
            FileSaver fs = new FileSaver(img);
            fs.saveAsJpeg(prefix + "gray.jpg");
            img.close();

            try {
                FileWriter results =
                    new FileWriter(prefix + "results.txt");
                results.write(String.format("%s%n", fileName));
                for (Map.Entry<Long, Trail> entry: images.entrySet()) {
                    Trail trail = entry.getValue();

                    printTrail(imgProcessor, trail, prefix);
                    printGray(binary, trail, prefix);
                    trail.clear_trail();

                    results.write(String.format("%05d, ", trail.get_id()));
                    results.write(String.format("%013d, ", trail.get_key()));
                    results.write(String.format("%d, ", trail.get_boundingBoxWidth()));
                    results.write(String.format("%d, ", trail.get_boundingBoxHeight()));
                    results.write(String.format("%f, ", trail.get_widthRatio()));
                    results.write(String.format("%f, ", trail.get_heightRatio()));
                    results.write(String.format("%f, ", trail.get_aspectRatio()));
                    results.write(String.format("%f, ", trail.get_areaRatio()));
                    results.write(String.format("%d, ", trail.get_delta()[0]));
                    results.write(String.format("%d, ", trail.get_delta()[1]));
                    results.write(String.format("%d%n", trail.get_count()));

                }
                results.close();
                images.clear();
            } catch (IOException e) {
                System.out.println(e.getMessage());
            }
        }
    }
}

