package com.gifcreator;

import javax.imageio.ImageIO;
import javax.imageio.stream.FileImageOutputStream;
import javax.imageio.stream.ImageOutputStream;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

public class MultiConvert {

    private static File rootOutputDir; // Diretório de saída raiz
    private static File rootInputDir; // Diretório de entrada raiz

    public static void convertImagesToGif(String imageDir) {
        File dir = new File(imageDir);
        if (!dir.isDirectory()) {
            System.out.println("Não é um diretório: " + imageDir);
            return;
        }

        // Configura diretórios apenas na primeira chamada
        if (rootOutputDir == null) {
            rootInputDir = dir;
            rootOutputDir = new File(dir.getParentFile(), "processed");
            if (!rootOutputDir.exists()) {
                rootOutputDir.mkdirs();
            }
        }

        try {
            processDirectory(dir);

            // Processa subdiretórios recursivamente
            File[] subDirs = dir.listFiles(file -> file.isDirectory() && !file.equals(rootOutputDir));
            if (subDirs != null) {
                for (File subDir : subDirs) {
                    convertImagesToGif(subDir.getAbsolutePath());
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void processDirectory(File dir) {
        // Calcula o caminho relativo a partir do diretório raiz de entrada
        Path relativePath = rootInputDir.toPath().relativize(dir.toPath());
        String baseName;

        if (relativePath.toString().isEmpty()) {
            baseName = rootInputDir.getName(); // Nome do diretório raiz
        } else {
            baseName = relativePath.toString().replace(File.separatorChar, '-');
        }

        // Lista e ordena as imagens numericamente
        File[] imageFiles = dir.listFiles((d, name) -> {
            String lowerName = name.toLowerCase();
            return lowerName.endsWith(".png") || lowerName.endsWith(".jpg") || lowerName.endsWith(".jpeg");
        });

        List<BufferedImage> frames = new ArrayList<>();
        if (imageFiles != null && imageFiles.length > 0) {
            // Ordenação numérica correta (1, 2, 10 em vez de 1, 10, 2)
            Arrays.sort(imageFiles, new NumericFileComparator());

            for (File imageFile : imageFiles) {
                try {
                    BufferedImage image = ImageIO.read(imageFile);
                    if (image != null) {
                        frames.add(image);
                    }
                } catch (IOException e) {
                    System.err.println("Erro ao ler imagem: " + imageFile.getAbsolutePath());
                }
            }

            if (!frames.isEmpty()) {
                String uniqueName = generateUniqueName(baseName);
                String outputPath = new File(rootOutputDir, uniqueName).getAbsolutePath();
                try {
                    createGif(frames.toArray(new BufferedImage[0]), outputPath, 100);
                    System.out.println("GIF criado: " + outputPath);
                } catch (IOException e) {
                    System.err.println("Erro ao criar GIF: " + e.getMessage());
                }
            }
        }
    }

    // Comparador para ordenação numérica de arquivos
    private static class NumericFileComparator implements Comparator<File> {
        @Override
        public int compare(File f1, File f2) {
            int n1 = extractNumber(f1.getName());
            int n2 = extractNumber(f2.getName());
            return Integer.compare(n1, n2);
        }

        private int extractNumber(String name) {
            String num = name.replaceAll("\\D+", ""); // Remove todos os não-dígitos
            return num.isEmpty() ? 0 : Integer.parseInt(num);
        }
    }

    private static String generateUniqueName(String baseName) {
        String baseFile = baseName + ".gif";
        File targetFile = new File(rootOutputDir, baseFile);

        if (!targetFile.exists()) {
            return baseFile;
        }

        int counter = 1;
        String newName;
        do {
            newName = baseName + "_" + counter + ".gif";
            targetFile = new File(rootOutputDir, newName);
            counter++;
        } while (targetFile.exists());

        return newName;
    }

    public static void createGif(BufferedImage[] images, String outputPath, int delayBetweenFrames) throws IOException {
        try (ImageOutputStream output = new FileImageOutputStream(new File(outputPath))) {
            GifSequenceWriter writer = new GifSequenceWriter(output, BufferedImage.TYPE_INT_ARGB, delayBetweenFrames,
                    true);

            for (BufferedImage image : images) {
                if (image != null) {
                    // Cria uma imagem com fundo verde
                    BufferedImage convertedImage = new BufferedImage(
                            image.getWidth(),
                            image.getHeight(),
                            BufferedImage.TYPE_INT_ARGB);
                    Graphics2D g2d = convertedImage.createGraphics();

                    // Preenche o fundo com verde
                    g2d.setColor(Color.GREEN);
                    g2d.fillRect(0, 0, convertedImage.getWidth(), convertedImage.getHeight());

                    // Desenha a imagem original sobre o fundo verde
                    g2d.drawImage(image, 0, 0, null);
                    g2d.dispose();

                    writer.writeToSequence(convertedImage);
                }
            }
            writer.close();
        }
    }
}