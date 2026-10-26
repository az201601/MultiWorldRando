package com.multiworldrando.ui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.io.File;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingWorker;
import javax.swing.filechooser.FileNameExtensionFilter;

import com.multiworldrando.core.Randomizer;
import com.multiworldrando.core.Settings;

/**
 * Main window: pick a Harvest Moon GB ROM, tick the options you want,
 * click Generate. The randomized ROM and spoiler log appear next to the ROM.
 */
public final class RandomizerFrame extends JFrame {

    private static final long serialVersionUID = 1L;

    private final JTextField romField = new JTextField();
    private final JCheckBox startingGoldBox =
            new JCheckBox("Randomize starting gold (0-500G)");
    private final JCheckBox drawerMoneyBox =
            new JCheckBox("Randomize grandpa's rainy day money drawer (0-1000G)");
    private final JTextField seedField = new JTextField();
    private final JButton generateButton = new JButton("Generate");
    private final JTextArea logArea = new JTextArea();

    public RandomizerFrame() {
        super("Harvest Moon GB Randomizer - MultiWorldRando");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        buildUi();
        pack();
        setMinimumSize(new Dimension(560, 420));
        setLocationRelativeTo(null);
    }

    private void buildUi() {
        JPanel root = new JPanel(new BorderLayout(8, 8));
        root.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel form = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(4, 4, 4, 4);
        c.anchor = GridBagConstraints.WEST;

        c.gridx = 0;
        c.gridy = 0;
        form.add(new JLabel("Harvest Moon GB ROM:"), c);

        c.gridx = 1;
        c.weightx = 1.0;
        c.fill = GridBagConstraints.HORIZONTAL;
        romField.setEditable(false);
        form.add(romField, c);

        c.gridx = 2;
        c.weightx = 0;
        c.fill = GridBagConstraints.NONE;
        JButton browse = new JButton("Browse\u2026");
        browse.addActionListener(e -> browseForRom());
        form.add(browse, c);

        c.gridx = 1;
        c.gridy = 1;
        c.gridwidth = 2;
        form.add(startingGoldBox, c);

        c.gridy = 2;
        form.add(drawerMoneyBox, c);

        c.gridy = 3;
        form.add(new JLabel(
                "<html><i>Seeds are generated automatically when you click Generate.</i></html>"), c);

        c.gridy = 4;
        c.gridwidth = 1;
        c.gridx = 0;
        form.add(new JLabel("Seed:"), c);

        c.gridx = 1;
        c.gridwidth = 2;
        c.fill = GridBagConstraints.HORIZONTAL;
        seedField.setEditable(false);
        seedField.setText("(generated on Generate)");
        form.add(seedField, c);

        root.add(form, BorderLayout.NORTH);

        logArea.setEditable(false);
        logArea.setLineWrap(true);
        logArea.setWrapStyleWord(true);
        JScrollPane scroll = new JScrollPane(logArea);
        scroll.setBorder(BorderFactory.createTitledBorder("Log"));
        root.add(scroll, BorderLayout.CENTER);

        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        generateButton.addActionListener(e -> onGenerate());
        bottom.add(generateButton);
        root.add(bottom, BorderLayout.SOUTH);

        setContentPane(root);
    }

    private void browseForRom() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Choose the Harvest Moon GB ROM");
        chooser.setFileFilter(new FileNameExtensionFilter(
                "Game Boy ROMs (*.gb, *.gbc)", "gb", "gbc"));
        if (romField.getText().length() > 0) {
            chooser.setCurrentDirectory(new File(romField.getText()).getParentFile());
        }
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            romField.setText(chooser.getSelectedFile().getAbsolutePath());
        }
    }

    private void onGenerate() {
        File romFile = new File(romField.getText().trim());
        if (romField.getText().trim().isEmpty() || !romFile.isFile()) {
            log("Please choose a Harvest Moon GB ROM first.");
            return;
        }

        Settings settings = new Settings();
        settings.setRandomizeStartingGold(startingGoldBox.isSelected());
        settings.setRandomizeDrawerMoney(drawerMoneyBox.isSelected());

        generateButton.setEnabled(false);
        log("Generating\u2026");

        // Keep the UI responsive while validating and patching.
        new SwingWorker<Randomizer.Result, Void>() {
            @Override
            protected Randomizer.Result doInBackground() throws Exception {
                return Randomizer.randomize(romFile, settings, null);
            }

            @Override
            protected void done() {
                generateButton.setEnabled(true);
                try {
                    Randomizer.Result result = get();
                    seedField.setText(
                            com.multiworldrando.core.SeedGenerator.format(result.seed));
                    log("Done!");
                    log("Seed: " + com.multiworldrando.core.SeedGenerator.format(result.seed));
                    log("Randomized ROM: " + result.romOut.getAbsolutePath());
                    log("Spoiler log:    " + result.spoilerOut.getAbsolutePath());
                } catch (Exception ex) {
                    Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
                    log("Failed: " + cause.getMessage());
                }
            }
        }.execute();
    }

    private void log(String line) {
        logArea.append(line + "\n");
        logArea.setCaretPosition(logArea.getDocument().getLength());
    }
}
