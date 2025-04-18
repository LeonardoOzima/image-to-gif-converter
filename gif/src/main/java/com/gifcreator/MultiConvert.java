package com.gifcreator;

import javax.imageio.ImageIO;
import javax.imageio.stream.FileImageOutputStream;
import javax.imageio.stream.ImageOutputStream;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

public class MultiConvert {
    private static int frameDelay = 100;

    public static void setFrameDelay(int delay) {
        frameDelay = delay;
    }

    public static File generatePreview(String inputPath) {
        File dir = new File(inputPath);
        if (!dir.isDirectory())
            return null;

        File tempDir = new File(System.getProperty("java.io.tmpdir"), "gifcreator_preview");
        if (!tempDir.exists())
            tempDir.mkdirs();

        try {
            BufferedImage[] previewImages;
            if (currentMode == ProcessingMode.RECURSIVE) {
                File[] images = dir.listFiles((d, name) -> name.toLowerCase().matches(".*\\.(png|jpg|jpeg)"));
                if (images == null || images.length == 0)
                    return null;

                previewImages = new BufferedImage[images.length];
                for (int i = 0; i < previewImages.length; i++) {
                    previewImages[i] = createImageWithBackground(ImageIO.read(images[i]));
                }
            } else {
                File[] seedFiles = dir.listFiles((d, name) -> name.toLowerCase().matches(".*\\.(png|jpg|jpeg)") &&
                        name.contains(targetSuffix));
                if (seedFiles == null || seedFiles.length == 0)
                    return null;

                String baseName = extractBaseName(seedFiles[0].getName());
                List<File> sequence = findImageSequence(dir, baseName);
                if (sequence.isEmpty())
                    return null;

                previewImages = new BufferedImage[sequence.size()];
                for (int i = 0; i < previewImages.length; i++) {
                    previewImages[i] = createImageWithBackground(ImageIO.read(sequence.get(i)));
                }
            }

            File previewFile = new File(tempDir, "preview.gif");
            createGif(previewImages, previewFile.getAbsolutePath(), frameDelay);
            return previewFile;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    private static BufferedImage createImageWithBackground(BufferedImage original) throws IOException {
        BufferedImage newImage = new BufferedImage(
                original.getWidth(),
                original.getHeight(),
                BufferedImage.TYPE_INT_ARGB);

        Graphics2D g2d = newImage.createGraphics();
        g2d.setColor(Color.GREEN); // Fundo verde
        g2d.fillRect(0, 0, newImage.getWidth(), newImage.getHeight());
        g2d.drawImage(original, 0, 0, null);
        g2d.dispose();

        return newImage;
    }

    public enum ProcessingMode {
        RECURSIVE, // Processa todas as imagens em pastas recursivamente
        SEQUENTIAL // Processa sequências de imagens com base em sufixo
    }

    private static File rootOutputDir;
    private static File rootInputDir;
    private static ProcessingMode currentMode = ProcessingMode.RECURSIVE;
    private static String targetSuffix = "-0"; // Sufixo padrão para modo sequencial

    public static void setProcessingMode(ProcessingMode mode, String suffix) {
        currentMode = mode;
        if (suffix != null) {
            targetSuffix = suffix;
        }
    }

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
            if (currentMode == ProcessingMode.RECURSIVE) {
                processDirectoryRecursive(dir);
            } else {
                processDirectorySequential(dir);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void processDirectoryRecursive(File dir) {
        // Processa o diretório atual
        processSingleDirectory(dir);

        // Processa subdiretórios recursivamente
        File[] subDirs = dir.listFiles(file -> file.isDirectory() && !file.equals(rootOutputDir));
        if (subDirs != null) {
            for (File subDir : subDirs) {
                processDirectoryRecursive(subDir);
            }
        }
    }

    private static void processDirectorySequential(File dir) {
        // Encontra todas as imagens com o sufixo alvo
        File[] seedFiles = dir.listFiles((d, name) -> {
            String lowerName = name.toLowerCase();
            return (lowerName.endsWith(".png") || lowerName.endsWith(".jpg") || lowerName.endsWith(".jpeg"))
                    && name.contains(targetSuffix);
        });

        if (seedFiles == null || seedFiles.length == 0) {
            System.out.println(
                    "Nenhuma imagem com sufixo '" + targetSuffix + "' encontrada em: " + dir.getAbsolutePath());
            return;
        }

        // Para cada imagem semente, encontra a sequência completa
        for (File seedFile : seedFiles) {
            String baseName = extractBaseName(seedFile.getName());
            List<File> sequence = findImageSequence(dir, baseName);

            if (!sequence.isEmpty()) {
                createGifFromSequence(sequence, dir);
            }
        }
    }

    private static String extractBaseName(String fileName) {
        int suffixIndex = fileName.indexOf(targetSuffix);
        return suffixIndex > 0 ? fileName.substring(0, suffixIndex)
                : fileName.replace(".png", "").replace(".jpg", "").replace(".jpeg", "");
    }

    private static List<File> findImageSequence(File dir, String baseName) {
        List<File> sequence = new ArrayList<>();

        // Padrão para encontrar imagens sequenciais (baseName-0, baseName-1, etc.)
        Pattern pattern = Pattern.compile(Pattern.quote(baseName) + "-(\\d+)\\.(png|jpg|jpeg)",
                Pattern.CASE_INSENSITIVE);

        // Primeiro encontre todos os arquivos que correspondem ao padrão
        File[] allFiles = dir.listFiles((d, name) -> pattern.matcher(name).matches());

        if (allFiles != null && allFiles.length > 0) {
            // Ordenar os arquivos pelo número sequencial
            Arrays.sort(allFiles, (f1, f2) -> {
                Matcher m1 = pattern.matcher(f1.getName());
                Matcher m2 = pattern.matcher(f2.getName());
                m1.find();
                m2.find();
                return Integer.compare(Integer.parseInt(m1.group(1)),
                        Integer.parseInt(m2.group(1)));
            });

            // Adicionar à sequência mantendo a ordem
            Collections.addAll(sequence, allFiles);
        }

        return sequence;
    }

    private static void processSingleDirectory(File dir) {
        Path relativePath = rootInputDir.toPath().relativize(dir.toPath());
        String baseName = relativePath.toString().isEmpty() ? rootInputDir.getName()
                : relativePath.toString().replace(File.separatorChar, '-');

        File[] imageFiles = dir.listFiles((d, name) -> {
            String lowerName = name.toLowerCase();
            return lowerName.endsWith(".png") || lowerName.endsWith(".jpg") || lowerName.endsWith(".jpeg");
        });

        List<BufferedImage> frames = new ArrayList<>();
        if (imageFiles != null && imageFiles.length > 0) {
            Arrays.sort(imageFiles, new NumericFileComparator());

            for (File imageFile : imageFiles) {
                try {
                    BufferedImage image = ImageIO.read(imageFile);
                    if (image != null) {
                        frames.add(convertImageWithGreenBackground(image));
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

    private static void createGifFromSequence(List<File> imageFiles, File sourceDir) {
        Path relativePath = rootInputDir.toPath().relativize(sourceDir.toPath());
        String baseName = relativePath.toString().isEmpty() ? rootInputDir.getName()
                : relativePath.toString().replace(File.separatorChar, '-') + "-"
                        + extractBaseName(imageFiles.get(0).getName());

        List<BufferedImage> frames = new ArrayList<>();
        for (File imageFile : imageFiles) {
            try {
                BufferedImage image = ImageIO.read(imageFile);
                if (image != null) {
                    frames.add(convertImageWithGreenBackground(image));
                }
            } catch (IOException e) {
                System.err.println("Erro ao ler imagem: " + imageFile.getAbsolutePath());
            }
        }

        if (!frames.isEmpty()) {
            String uniqueName = generateUniqueName(baseName);
            String outputPath = new File(rootOutputDir, uniqueName).getAbsolutePath();
            try {
                createGif(frames.toArray(new BufferedImage[0]), outputPath, frameDelay);
                System.out.println("GIF sequencial criado: " + outputPath);
            } catch (IOException e) {
                System.err.println("Erro ao criar GIF sequencial: " + e.getMessage());
            }
        }
    }

    private static BufferedImage convertImageWithGreenBackground(BufferedImage original) {
        BufferedImage converted = new BufferedImage(
                original.getWidth(), original.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = converted.createGraphics();
        g2d.setColor(Color.GREEN);
        g2d.fillRect(0, 0, converted.getWidth(), converted.getHeight());
        g2d.drawImage(original, 0, 0, null);
        g2d.dispose();
        return converted;
    }

    private static class NumericFileComparator implements Comparator<File> {
        @Override
        public int compare(File f1, File f2) {
            int n1 = extractNumber(f1.getName());
            int n2 = extractNumber(f2.getName());
            return Integer.compare(n1, n2);
        }

        private int extractNumber(String name) {
            String num = name.replaceAll("\\D+", "");
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
                writer.writeToSequence(image);
            }
            writer.close();
        }
    }
}