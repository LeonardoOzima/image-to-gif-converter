package com.gifcreator;

import com.madgag.gif.fmsware.AnimatedGifEncoder;
import com.madgag.gif.fmsware.GifDecoder;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

public class RemoveGreenBackground {

    private static final int GREEN_THRESHOLD = 150;

    public static void processGifs(String gifDir) {
        File dir = new File(gifDir);
        if (!dir.exists() || !dir.isDirectory()) {
            System.out.println("Invalid directory: " + gifDir);
            return;
        }

        // Create the output directory inside the input directory
        String outputGifDir = gifDir + File.separator + "transparent";
        File outputDir = new File(outputGifDir);
        if (!outputDir.exists()) {
            outputDir.mkdirs();
        }

        // List all GIF files in the directory
        File[] gifFiles = dir.listFiles((d, name) -> name.toLowerCase().endsWith(".gif"));

        if (gifFiles == null || gifFiles.length == 0) {
            System.out.println("No GIFs found in the directory.");
            return;
        }

        // Process each GIF file
        for (File gifFile : gifFiles) {
            try {
                GifDecoder decoder = new GifDecoder();
                int status = decoder.read(gifFile.getAbsolutePath());
                if (status != GifDecoder.STATUS_OK) {
                    System.out.println("Failed to read GIF: " + gifFile.getAbsolutePath());
                    continue;
                }

                AnimatedGifEncoder encoder = new AnimatedGifEncoder();
                encoder.start(outputGifDir + File.separator + gifFile.getName());
                encoder.setRepeat(decoder.getLoopCount());

                for (int i = 0; i < decoder.getFrameCount(); i++) {
                    BufferedImage frame = decoder.getFrame(i);
                    BufferedImage transparentFrame = makeTransparent(frame);
                    encoder.setDelay(decoder.getDelay(i));
                    encoder.addFrame(transparentFrame);
                }

                encoder.finish();
                System.out.println("Processed GIF saved at: " + outputGifDir + File.separator + gifFile.getName());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private static BufferedImage makeTransparent(BufferedImage image) {
        BufferedImage transparentImage = new BufferedImage(image.getWidth(), image.getHeight(),
                BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = transparentImage.createGraphics();
        g2d.drawImage(image, 0, 0, null);

        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                int pixel = image.getRGB(x, y);
                Color color = new Color(pixel, true);
                if (color.getGreen() > GREEN_THRESHOLD && color.getRed() < GREEN_THRESHOLD
                        && color.getBlue() < GREEN_THRESHOLD) {
                    transparentImage.setRGB(x, y, 0x00FFFFFF & pixel); // Set alpha to 0 (transparent)
                }
            }
        }

        g2d.dispose();
        return transparentImage;
    }
}