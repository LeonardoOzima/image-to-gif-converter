package com.gifcreator;

import javax.imageio.ImageIO;
import javax.imageio.stream.FileImageOutputStream;
import javax.imageio.stream.ImageOutputStream;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.Arrays;
import java.util.Comparator;

public class ImageToGifConverter {

    public static void convertImagesToGif(String imageDir) {
        try {
            // Create the output directory inside the input directory
            String outputGifDir = imageDir + File.separator + "processed";
            File outputDir = new File(outputGifDir);
            if (!outputDir.exists()) {
                outputDir.mkdirs();
            }

            // List all image files in the directory
            File dir = new File(imageDir);
            File[] imageFiles = dir.listFiles(new FilenameFilter() {
                public boolean accept(File dir, String name) {
                    return name.toLowerCase().endsWith(".png") && name.endsWith("-0.png");
                }
            });

            if (imageFiles == null || imageFiles.length == 0) {
                System.out.println("No images found in the directory.");
                return;
            }

            // Sort files by name
            Arrays.sort(imageFiles, Comparator.comparing(File::getName));

            // Process each filtered file
            for (File file : imageFiles) {
                String baseName = file.getName().replace("-0.png", "");
                int imageCount = 0;

                // Count the number of images in the sequence
                while (true) {
                    File imageFile;
                    imageFile = new File(imageDir + File.separator + baseName + "-" + imageCount + ".png");
                    if (!imageFile.exists()) {
                        System.out.println("Image file not found: " + imageFile.getAbsolutePath());
                        break;
                    }

                    imageCount++;
                }

                System.out.println("Number of images found for base name " + baseName + ": " + (imageCount - 1));

                // Read images and create GIF
                BufferedImage[] images = new BufferedImage[imageCount - 1];
                for (int i = 0; i < imageCount - 1; i++) {
                    String imagePath;
                    imagePath = imageDir + File.separator + baseName + "-" + i + ".png";
                    images[i] = ImageIO.read(new File(imagePath));
                }

                String outputGifPath = outputGifDir + File.separator + baseName + ".gif";
                createGif(images, outputGifPath, 50); // 50ms delay between frames
                System.out.println("GIF created successfully at: " + outputGifPath);
                System.out.println("Number of images in the GIF: " + (imageCount - 1));
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void createGif(BufferedImage[] images, String outputPath, int delayBetweenFrames) throws IOException {
        try (ImageOutputStream output = new FileImageOutputStream(new File(outputPath))) {
            GifSequenceWriter writer = new GifSequenceWriter(output, BufferedImage.TYPE_INT_ARGB, delayBetweenFrames,
                    true);

            for (BufferedImage image : images) {
                if (image != null) {
                    BufferedImage convertedImage = new BufferedImage(image.getWidth(), image.getHeight(),
                            BufferedImage.TYPE_INT_ARGB);
                    Graphics2D g2d = convertedImage.createGraphics();
                    g2d.setColor(new Color(0, 255, 0)); // Set the color to lime green
                    g2d.fillRect(0, 0, convertedImage.getWidth(), convertedImage.getHeight());
                    g2d.drawImage(image, 0, 0, null);
                    g2d.dispose();
                    writer.writeToSequence(convertedImage);
                }
            }

            writer.close();
        }
    }
}