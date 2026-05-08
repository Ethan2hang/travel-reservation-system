package cs336.travel.ui;

import cs336.travel.Session;
import cs336.travel.service.AuthService;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;

public final class AdminHomePanel extends JPanel {

    private final MainFrame frame;
    private final JLabel welcome = new JLabel();

    public AdminHomePanel(MainFrame frame) {
        this.frame = frame;
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

        JPanel header = new JPanel(new BorderLayout());
        welcome.setFont(welcome.getFont().deriveFont(Font.BOLD, 18f));
        JButton logout = new JButton("Sign out");
        logout.addActionListener(e -> onLogoutClicked());
        header.add(welcome, BorderLayout.WEST);
        header.add(logout,  BorderLayout.EAST);

        JButton manageUsers = new JButton("Manage users");
        manageUsers.setAlignmentX(Component.LEFT_ALIGNMENT);
        manageUsers.addActionListener(e -> frame.showAdminUsers());

        JButton reports = new JButton("Reports");
        reports.setAlignmentX(Component.LEFT_ALIGNMENT);
        reports.addActionListener(e -> frame.showAdminReports());

        JPanel body = new JPanel();
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.setMaximumSize(new Dimension(480, Integer.MAX_VALUE));
        body.add(Box.createVerticalStrut(12));
        body.add(manageUsers);
        body.add(Box.createVerticalStrut(8));
        body.add(reports);

        add(header, BorderLayout.NORTH);
        add(body,   BorderLayout.CENTER);
    }

    public void refresh() {
        welcome.setText("Welcome, " + Session.displayName() + " (Admin)");
    }

    private void onLogoutClicked() {
        AuthService.logout();
        frame.showLogin();
    }
}
