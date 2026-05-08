package cs336.travel;

import cs336.travel.ui.MainFrame;

import javax.swing.SwingUtilities;

public final class App {

    private App() {}

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new MainFrame().setVisible(true));
    }
}
