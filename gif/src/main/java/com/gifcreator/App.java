package com.gifcreator;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.File;

public class App {
    private static JProgressBar progressBar;
    private static JButton convertButton;
    private static JFrame frame;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            createAndShowGUI();
        });
    }

    private static void createAndShowGUI() {
        // Configuração do frame
        frame = new JFrame("GIF Creator");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(500, 250);
        frame.setLocationRelativeTo(null);

        // Painel principal
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.insets = new Insets(5, 5, 5, 5);
        constraints.anchor = GridBagConstraints.WEST;

        // Componentes
        JLabel inputLabel = new JLabel("Selecione a pasta:");
        JTextField inputPathText = new JTextField(25);
        inputPathText.setEditable(true);

        JButton selectInputButton = new JButton("Procurar");
        convertButton = new JButton("Criar GIFs");
        progressBar = new JProgressBar();
        progressBar.setStringPainted(true);
        progressBar.setVisible(false);

        // Layout
        constraints.gridx = 0;
        constraints.gridy = 0;
        panel.add(inputLabel, constraints);

        constraints.gridx = 1;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        panel.add(inputPathText, constraints);

        constraints.gridx = 2;
        constraints.fill = GridBagConstraints.NONE;
        panel.add(selectInputButton, constraints);

        constraints.gridy = 1;
        constraints.gridx = 0;
        constraints.gridwidth = 3;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        panel.add(progressBar, constraints);

        constraints.gridy = 2;
        constraints.gridx = 1;
        constraints.gridwidth = 1;
        constraints.anchor = GridBagConstraints.CENTER;
        panel.add(convertButton, constraints);

        // Listeners
        selectInputButton.addActionListener(e -> {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            if (fileChooser.showOpenDialog(panel) == JFileChooser.APPROVE_OPTION) {
                inputPathText.setText(fileChooser.getSelectedFile().getAbsolutePath());
            }
        });

        convertButton.addActionListener(e -> {
            String inputPath = inputPathText.getText();

            if (inputPath.isEmpty()) {
                JOptionPane.showMessageDialog(panel, "Selecione uma pasta válida!", "Erro", JOptionPane.ERROR_MESSAGE);
                return;
            }
            File dir = new File(inputPath);

            if (!dir.isDirectory()) {
                JOptionPane.showMessageDialog(
                        panel,
                        "Caminho inválido! Selecione ou cole um diretório válido.",
                        "Erro",
                        JOptionPane.ERROR_MESSAGE);
                return;
            }
            // Executa em thread separada para não travar a interface
            new SwingWorker<Void, Void>() {
                @Override
                protected Void doInBackground() throws Exception {
                    setUIState(false);
                    MultiConvert.convertImagesToGif(inputPath);
                    return null;
                }

                @Override
                protected void done() {
                    setUIState(true);
                    JOptionPane.showMessageDialog(panel,
                            "Conversão concluída!\nOs GIFs estão nas pastas 'processed'",
                            "Sucesso",
                            JOptionPane.INFORMATION_MESSAGE);
                }
            }.execute();
        });

        frame.add(panel);
        frame.setVisible(true);
    }

    private static void setUIState(boolean enabled) {
        convertButton.setEnabled(enabled);
        progressBar.setVisible(!enabled);
        progressBar.setIndeterminate(!enabled);
    }
}