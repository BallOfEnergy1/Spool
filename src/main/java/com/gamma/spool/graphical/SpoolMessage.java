package com.gamma.spool.graphical;

import java.awt.GraphicsEnvironment;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

public class SpoolMessage {

    public static void showError(String title, String message) {
        if (GraphicsEnvironment.isHeadless()) return;
        try {
            SwingUtilities.invokeAndWait(() -> {
                JFrame jf = new JFrame("Spool Error");
                jf.setAlwaysOnTop(true);
                jf.setUndecorated(true);
                jf.setVisible(true);
                jf.setLocationRelativeTo(null);
                JOptionPane.showMessageDialog(jf, message, title, JOptionPane.ERROR_MESSAGE);
            });
        } catch (Exception ignored) {}
    }
}
