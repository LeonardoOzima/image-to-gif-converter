package com.gifcreator;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.List;
import java.util.*;

public class App {
    private static JProgressBar progressBar;
    private static JButton convertButton;
    private static JButton previewButton;
    private static JFrame frame;
    private static JComboBox<String> modeComboBox;
    private static JTextField suffixTextField;
    private static JSlider speedSlider;
    private static JLabel gifPreviewLabel;
    private static JPanel previewPanel;
    private static int currentDelay = 100; // Valor padrão em ms

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            createAndShowGUI();
        });
    }

    private static void createAndShowGUI() {
        // Configuração do frame
        frame = new JFrame("GIF Creator Avançado");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(700, 400); // Tamanho aumentado para o preview
        frame.setLocationRelativeTo(null);

        // Painel principal com borda
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Painel de controles (esquerda)
        JPanel controlPanel = new JPanel(new GridBagLayout());
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.insets = new Insets(5, 5, 5, 5);
        constraints.anchor = GridBagConstraints.WEST;

        // Componentes
        JLabel inputLabel = new JLabel("Selecione a pasta:");
        JTextField inputPathText = new JTextField(20);
        inputPathText.setEditable(true);

        JButton selectInputButton = new JButton("Procurar");

        // Controles de modo
        JLabel modeLabel = new JLabel("Modo de processamento:");
        modeComboBox = new JComboBox<>(new String[] { "Recursivo", "Sequencial" });
        modeComboBox.setSelectedIndex(0);

        JLabel suffixLabel = new JLabel("Sufixo para modo sequencial:");
        suffixTextField = new JTextField("-0", 5);
        suffixTextField.setEnabled(false);

        // Controle de velocidade
        JLabel speedLabel = new JLabel("Velocidade (ms entre frames):");
        speedSlider = new JSlider(JSlider.HORIZONTAL, 1, 100, currentDelay);
        speedSlider.setMajorTickSpacing(100);
        speedSlider.setMinorTickSpacing(20);
        speedSlider.setPaintTicks(true);
        speedSlider.setPaintLabels(true);

        // Botões
        previewButton = new JButton("Visualizar Primeiro GIF");
        previewButton.setEnabled(false);

        convertButton = new JButton("Criar Todos GIFs");
        progressBar = new JProgressBar();
        progressBar.setStringPainted(true);
        progressBar.setVisible(false);

        // Layout dos controles
        constraints.gridx = 0;
        constraints.gridy = 0;
        controlPanel.add(inputLabel, constraints);

        constraints.gridx = 1;
        constraints.gridwidth = 2;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        controlPanel.add(inputPathText, constraints);

        constraints.gridx = 3;
        constraints.gridwidth = 1;
        constraints.fill = GridBagConstraints.NONE;
        controlPanel.add(selectInputButton, constraints);

        constraints.gridy = 1;
        constraints.gridx = 0;
        controlPanel.add(modeLabel, constraints);

        constraints.gridx = 1;
        controlPanel.add(modeComboBox, constraints);

        constraints.gridy = 2;
        constraints.gridx = 0;
        controlPanel.add(suffixLabel, constraints);

        constraints.gridx = 1;
        controlPanel.add(suffixTextField, constraints);

        constraints.gridy = 3;
        constraints.gridx = 0;
        controlPanel.add(speedLabel, constraints);

        constraints.gridx = 1;
        constraints.gridwidth = 3;
        controlPanel.add(speedSlider, constraints);

        constraints.gridy = 4;
        constraints.gridx = 1;
        constraints.gridwidth = 1;
        constraints.anchor = GridBagConstraints.CENTER;
        controlPanel.add(previewButton, constraints);

        constraints.gridy = 5;
        constraints.gridx = 1;
        controlPanel.add(convertButton, constraints);

        constraints.gridy = 6;
        constraints.gridx = 0;
        constraints.gridwidth = 4;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        controlPanel.add(progressBar, constraints);

        // Painel de preview (direita)
        previewPanel = new JPanel(new BorderLayout());
        previewPanel.setBorder(BorderFactory.createTitledBorder("Pré-visualização"));
        previewPanel.setPreferredSize(new Dimension(300, 300));

        gifPreviewLabel = new JLabel("Nenhuma pré-visualização disponível", SwingConstants.CENTER);
        gifPreviewLabel.setVerticalAlignment(SwingConstants.CENTER);
        previewPanel.add(gifPreviewLabel, BorderLayout.CENTER);

        // Adiciona os painéis ao mainPanel
        mainPanel.add(controlPanel, BorderLayout.WEST);
        mainPanel.add(previewPanel, BorderLayout.CENTER);

        // Listeners
        selectInputButton.addActionListener(e -> {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            if (fileChooser.showOpenDialog(frame) == JFileChooser.APPROVE_OPTION) {
                inputPathText.setText(fileChooser.getSelectedFile().getAbsolutePath());
                previewButton.setEnabled(true);
            }
        });

        modeComboBox.addActionListener(e -> {
            boolean isSequential = modeComboBox.getSelectedIndex() == 1;
            suffixTextField.setEnabled(isSequential);
        });

        speedSlider.addChangeListener(e -> {
            currentDelay = speedSlider.getValue();
        });

        previewButton.addActionListener(e -> {
            String inputPath = inputPathText.getText();
            if (inputPath.isEmpty()) {
                JOptionPane.showMessageDialog(frame, "Selecione uma pasta válida!", "Erro", JOptionPane.ERROR_MESSAGE);
                return;
            }

            // Configura o modo de processamento
            MultiConvert.ProcessingMode mode = modeComboBox.getSelectedIndex() == 0
                    ? MultiConvert.ProcessingMode.RECURSIVE
                    : MultiConvert.ProcessingMode.SEQUENTIAL;
            String suffix = modeComboBox.getSelectedIndex() == 1 ? suffixTextField.getText() : "-0";

            MultiConvert.setProcessingMode(mode, suffix);
            MultiConvert.setFrameDelay(currentDelay);

            new SwingWorker<Void, Void>() {
                @Override
                protected Void doInBackground() throws Exception {
                    setUIState(false, "Gerando pré-visualização...");
                    File previewGif = MultiConvert.generatePreview(inputPath);
                    SwingUtilities.invokeLater(() -> updatePreviewUI(previewGif));
                    return null;
                }

                @Override
                protected void done() {
                    setUIState(true, "");
                }
            }.execute();
        });

        convertButton.addActionListener(e -> {
            String inputPath = inputPathText.getText();
            if (inputPath.isEmpty()) {
                JOptionPane.showMessageDialog(frame, "Selecione uma pasta válida!", "Erro", JOptionPane.ERROR_MESSAGE);
                return;
            }

            MultiConvert.ProcessingMode mode = modeComboBox.getSelectedIndex() == 0
                    ? MultiConvert.ProcessingMode.RECURSIVE
                    : MultiConvert.ProcessingMode.SEQUENTIAL;
            String suffix = modeComboBox.getSelectedIndex() == 1 ? suffixTextField.getText() : "-0";

            MultiConvert.setProcessingMode(mode, suffix);
            MultiConvert.setFrameDelay(currentDelay);

            new SwingWorker<Void, Void>() {
                @Override
                protected Void doInBackground() throws Exception {
                    setUIState(false, "Criando GIFs...");
                    MultiConvert.convertImagesToGif(inputPath);
                    return null;
                }

                @Override
                protected void done() {
                    setUIState(true, "");
                    JOptionPane.showMessageDialog(frame,
                            "Conversão concluída!\nOs GIFs estão nas pastas 'processed'",
                            "Sucesso",
                            JOptionPane.INFORMATION_MESSAGE);
                }
            }.execute();
        });

        frame.add(mainPanel);
        frame.setVisible(true);
    }

    private static void updatePreviewUI(File gifFile) {
        if (gifFile != null && gifFile.exists()) {
            ImageIcon icon = new ImageIcon(gifFile.getAbsolutePath());
            Image scaledImage = icon.getImage().getScaledInstance(
                    previewPanel.getWidth() - 20,
                    previewPanel.getHeight() - 20,
                    Image.SCALE_SMOOTH);
            gifPreviewLabel.setIcon(new ImageIcon(scaledImage));
            gifPreviewLabel.setText("");
        } else {
            gifPreviewLabel.setIcon(null);
            gifPreviewLabel.setText("Pré-visualização não disponível");
        }
    }

    private static void setUIState(boolean enabled, String progressText) {
        convertButton.setEnabled(enabled);
        previewButton.setEnabled(enabled);
        progressBar.setVisible(!enabled);
        progressBar.setIndeterminate(!enabled);
        progressBar.setString(progressText);
    }
}