package cs336.travel.ui;

import cs336.travel.Session;
import cs336.travel.model.NotificationItem;
import cs336.travel.service.AuthService;
import cs336.travel.service.NotificationService;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.time.format.DateTimeFormatter;
import java.util.List;

public final class CustomerHomePanel extends JPanel {

    private static final DateTimeFormatter DATETIME_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final MainFrame frame;
    private final JLabel welcome = new JLabel();
    private final JPanel banner = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 6));
    private final JLabel bannerLabel = new JLabel();
    private final JButton viewAlertsButton = new JButton("View alerts");

    public CustomerHomePanel(MainFrame frame) {
        this.frame = frame;
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

        JPanel header = new JPanel(new BorderLayout());
        welcome.setFont(welcome.getFont().deriveFont(Font.BOLD, 18f));
        JButton logout = new JButton("Sign out");
        logout.addActionListener(e -> onLogoutClicked());
        header.add(welcome, BorderLayout.WEST);
        header.add(logout,  BorderLayout.EAST);

        banner.setBackground(new Color(0xFFF5C2));
        banner.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(0xC9A227)),
                BorderFactory.createEmptyBorder(2, 8, 2, 8)));
        banner.add(bannerLabel);
        viewAlertsButton.addActionListener(e -> onViewAlertsClicked());
        banner.add(viewAlertsButton);
        banner.setVisible(false);

        JPanel north = new JPanel();
        north.setLayout(new BoxLayout(north, BoxLayout.Y_AXIS));
        header.setAlignmentX(Component.LEFT_ALIGNMENT);
        banner.setAlignmentX(Component.LEFT_ALIGNMENT);
        north.add(header);
        north.add(Box.createVerticalStrut(8));
        north.add(banner);

        JPanel body = new JPanel();
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        JButton searchFlights = new JButton("Search Flights");
        searchFlights.setAlignmentX(Component.LEFT_ALIGNMENT);
        searchFlights.addActionListener(e -> frame.showCustomerSearch());

        JButton myReservations = new JButton("My Reservations");
        myReservations.setAlignmentX(Component.LEFT_ALIGNMENT);
        myReservations.addActionListener(e -> frame.showCustomerHistory());

        JButton contactSupport = new JButton("Contact Support");
        contactSupport.setAlignmentX(Component.LEFT_ALIGNMENT);
        contactSupport.addActionListener(e -> frame.showCustomerInquiry());

        body.add(Box.createVerticalStrut(12));
        body.add(searchFlights);
        body.add(Box.createVerticalStrut(8));
        body.add(myReservations);
        body.add(Box.createVerticalStrut(8));
        body.add(contactSupport);

        add(north, BorderLayout.NORTH);
        add(body,  BorderLayout.CENTER);
    }

    public void refresh() {
        welcome.setText("Welcome, " + Session.displayName());
        refreshBanner();
    }

    private void refreshBanner() {
        try {
            int unread = NotificationService.listUnread().size();
            if (unread > 0) {
                bannerLabel.setText("You have " + unread
                        + (unread == 1 ? " unread alert." : " unread alerts."));
                banner.setVisible(true);
            } else {
                banner.setVisible(false);
            }
        } catch (RuntimeException ex) {
            banner.setVisible(false);
        }
        revalidate();
        repaint();
    }

    private void onViewAlertsClicked() {
        List<NotificationItem> items;
        try {
            items = NotificationService.listUnread();
        } catch (RuntimeException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(),
                    "Couldn't load alerts", JOptionPane.ERROR_MESSAGE);
            return;
        }

        DefaultTableModel model = new DefaultTableModel(
                new Object[]{"When", "Message"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        for (NotificationItem n : items) {
            model.addRow(new Object[]{n.createdAt().format(DATETIME_FMT), n.message()});
        }

        JTable table = new JTable(model);
        table.setAutoCreateRowSorter(false);
        JScrollPane scroll = new JScrollPane(table);
        scroll.setPreferredSize(new Dimension(640, 220));

        JFrame owner = (JFrame) SwingUtilities.getWindowAncestor(this);
        JDialog dialog = new JDialog(owner, "Unread alerts", true);
        dialog.setLayout(new BorderLayout());
        dialog.add(scroll, BorderLayout.CENTER);

        JButton markRead = new JButton("Mark all read");
        markRead.addActionListener(e -> {
            NotificationService.markAllRead();
            dialog.dispose();
            refreshBanner();
        });
        JButton close = new JButton("Close");
        close.addActionListener(e -> dialog.dispose());

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttons.add(markRead);
        buttons.add(close);
        dialog.add(buttons, BorderLayout.SOUTH);

        dialog.pack();
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }

    private void onLogoutClicked() {
        AuthService.logout();
        frame.showLogin();
    }
}
