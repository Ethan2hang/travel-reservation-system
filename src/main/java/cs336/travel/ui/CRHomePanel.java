package cs336.travel.ui;

import cs336.travel.Session;
import cs336.travel.service.AuthService;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;

public final class CRHomePanel extends JPanel {

    private final MainFrame frame;
    private final JLabel welcome = new JLabel();

    public CRHomePanel(MainFrame frame) {
        this.frame = frame;
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

        JPanel header = new JPanel(new BorderLayout());
        welcome.setFont(welcome.getFont().deriveFont(Font.BOLD, 18f));
        JButton logout = new JButton("Sign out");
        logout.addActionListener(e -> onLogoutClicked());
        header.add(welcome, BorderLayout.WEST);
        header.add(logout,  BorderLayout.EAST);

        JButton bookForCustomer = button("Book reservation for customer",
                () -> frame.showCRBooking());
        JButton editForCustomer = button("Edit reservation for customer",
                () -> frame.showCREditReservation());
        JButton manageInventory = button("Manage aircraft / airports / flights",
                () -> frame.showCRReferenceData());
        JButton waitlistPax     = button("Waitlist passengers",
                () -> frame.showCRReports(CRReportsPanel.TAB_WAITLIST));
        JButton flightsByAirport= button("Flights by airport",
                () -> frame.showCRReports(CRReportsPanel.TAB_FLIGHTS));
        JButton replyInquiries  = button("Reply to inquiries",
                () -> frame.showCRInquiries());

        JPanel body = new JPanel();
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.setMaximumSize(new Dimension(480, Integer.MAX_VALUE));
        body.add(Box.createVerticalStrut(12));
        body.add(bookForCustomer);
        body.add(Box.createVerticalStrut(8));
        body.add(editForCustomer);
        body.add(Box.createVerticalStrut(8));
        body.add(manageInventory);
        body.add(Box.createVerticalStrut(8));
        body.add(waitlistPax);
        body.add(Box.createVerticalStrut(8));
        body.add(flightsByAirport);
        body.add(Box.createVerticalStrut(8));
        body.add(replyInquiries);

        add(header, BorderLayout.NORTH);
        add(body,   BorderLayout.CENTER);
    }

    private JButton button(String text, Runnable handler) {
        JButton b = new JButton(text);
        b.setAlignmentX(Component.LEFT_ALIGNMENT);
        b.addActionListener(e -> handler.run());
        return b;
    }

    private JButton stub(String text) {
        return button(text, () -> JOptionPane.showMessageDialog(this,
                "This feature is not yet available.",
                text, JOptionPane.INFORMATION_MESSAGE));
    }

    public void refresh() {
        welcome.setText("Welcome, " + Session.displayName() + " (Customer Rep)");
    }

    private void onLogoutClicked() {
        AuthService.logout();
        frame.showLogin();
    }
}
