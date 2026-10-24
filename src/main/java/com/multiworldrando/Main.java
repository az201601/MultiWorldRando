package com.multiworldrando;

import com.multiworldrando.games.hm1.Hm1Patcher;
import java.awt.GraphicsEnvironment;
import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileNameExtensionFilter;

public class Main {

    public static void main(String[] args) {
        boolean isGui = !GraphicsEnvironment.isHeadless() && args.length == 0;

        File inputRom = null;
        File outputRom = null;

        if (args.length >= 2) {
            inputRom = new File(args[0]);
            outputRom = new File(args[1]);
        } else if (args.length == 1) {
            inputRom = new File(args[0]);
            outputRom = new File(inputRom.getParentFile(), "Harvest_Moon_GBC_Rando.gbc");
        } else {
            // Check for ROM in current directory
            File default1 = new File("Harvest Moon GB (U) [C][!].gbc");
            if (default1.exists()) {
                inputRom = default1;
                outputRom = new File("Harvest_Moon_GBC_Rando.gbc");
            } else if (isGui) {
                // If double-clicked without arguments and ROM not in current directory, show file chooser
                try {
                    JFileChooser chooser = new JFileChooser(new File("."));
                    chooser.setDialogTitle("Select Harvest Moon GBC ROM");
                    chooser.setFileFilter(new FileNameExtensionFilter("Game Boy (Color) ROMs (*.gb, *.gbc)", "gb", "gbc"));

                    int res = chooser.showOpenDialog(null);
                    if (res == JFileChooser.APPROVE_OPTION) {
                        inputRom = chooser.getSelectedFile();
                        outputRom = new File(inputRom.getParentFile(), "Harvest_Moon_GBC_Rando.gbc");
                    } else {
                        return; // User cancelled
                    }
                } catch (Exception e) {
                    // Fall back to CLI message
                }
            }
        }

        if (inputRom == null || !inputRom.exists()) {
            String msg = "Input ROM not found!\n\nPlease provide a Harvest Moon GBC ROM, or place 'Harvest Moon GB (U) [C][!].gbc' in the same folder as this program.";
            System.err.println(msg);
            if (isGui) {
                JOptionPane.showMessageDialog(null, msg, "MultiWorld Randomizer", JOptionPane.WARNING_MESSAGE);
            }
            return;
        }

        try {
            System.out.println("Patching ROM: " + inputRom.getAbsolutePath());
            File result = Hm1Patcher.patchRom(inputRom, outputRom, null);
            String successMsg = "ROM patched successfully!\n\nOutput: " + result.getAbsolutePath();
            System.out.println(successMsg);

            if (isGui) {
                JOptionPane.showMessageDialog(null, successMsg, "MultiWorld Randomizer", JOptionPane.INFORMATION_MESSAGE);
            }
        } catch (Throwable t) {
            StringWriter sw = new StringWriter();
            t.printStackTrace(new PrintWriter(sw));
            System.err.println("Error patching ROM:\n" + sw.toString());

            if (isGui) {
                JOptionPane.showMessageDialog(null, "Error patching ROM:\n\n" + t.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
}
