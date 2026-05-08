package cs336.travel.ui;

import cs336.travel.model.Role;
import cs336.travel.service.AuthService;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.util.Optional;

public final class LoginScreen extends JPanel {

    private static final int CARD_W  = 360;
    private static final int FIELD_W = 280;

    private final MainFrame frame;
    private final JTextField usernameField = new JTextField(18);
    private final JPasswordField passwordField = new JPasswordField(18);
    private final JLabel errorLabel = new JLabel(" ");
    private final JButton loginButton = new JButton("Sign in");

    public LoginScreen(MainFrame frame) {
        this.frame = frame;
        setLayout(new GridBagLayout());

        JPanel form = new JPanel();
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));
        form.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(0xCCCCCC)),
                BorderFactory.createEmptyBorder(24, 24, 24, 24)));
        form.setMaximumSize(new Dimension(CARD_W, Short.MAX_VALUE));

        JLabel title = new JLabel("Travel Reservation");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 22f));
        title.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel subtitle = new JLabel("Sign in with your username and password");
        subtitle.setAlignmentX(Component.CENTER_ALIGNMENT);

        capWidth(loginButton, FIELD_W);
        loginButton.setAlignmentX(Component.CENTER_ALIGNMENT);

        errorLabel.setForeground(Color.RED.darker());
        errorLabel.setHorizontalAlignment(SwingConstants.CENTER);
        errorLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        errorLabel.setMaximumSize(new Dimension(FIELD_W, 24));

        JLabel demoHint = new JLabel("Demo: admin/admin123 · rep1/rep123 · alice/alice123");
        demoHint.setForeground(new Color(0x808080));
        demoHint.setFont(demoHint.getFont().deriveFont(Font.PLAIN, 11f));
        demoHint.setAlignmentX(Component.CENTER_ALIGNMENT);

        form.add(title);
        form.add(Box.createVerticalStrut(4));
        form.add(subtitle);
        form.add(Box.createVerticalStrut(20));
        form.add(labelledField("Username", usernameField, FIELD_W));
        form.add(Box.createVerticalStrut(10));
        form.add(labelledField("Password", passwordField, FIELD_W));
        form.add(Box.createVerticalStrut(14));
        form.add(loginButton);
        form.add(Box.createVerticalStrut(8));
        form.add(errorLabel);
        form.add(Box.createVerticalStrut(4));
        form.add(demoHint);

        loginButton.addActionListener(e -> onLoginClicked());
        passwordField.addActionListener(e -> onLoginClicked());
        usernameField.addActionListener(e -> passwordField.requestFocusInWindow());

        GridBagConstraints gc = new GridBagConstraints();
        gc.weightx = 1.0;
        gc.weighty = 1.0;
        gc.fill    = GridBagConstraints.NONE;
        gc.anchor  = GridBagConstraints.CENTER;
        add(form, gc);
    }

    private static JPanel labelledField(String label, JComponent field, int width) {
        JPanel row = new JPanel();
        row.setLayout(new BoxLayout(row, BoxLayout.Y_AXIS));
        row.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel l = new JLabel(label);
        l.setAlignmentX(Component.LEFT_ALIGNMENT);

        field.setAlignmentX(Component.LEFT_ALIGNMENT);
        capWidth(field, width);

        row.add(l);
        row.add(field);
        row.setMaximumSize(new Dimension(
                width, l.getPreferredSize().height + field.getPreferredSize().height));
        return row;
    }

    private static void capWidth(JComponent c, int width) {
        Dimension d = new Dimension(width, c.getPreferredSize().height);
        c.setPreferredSize(d);
        c.setMaximumSize(d);
    }

    public void reset() {
        usernameField.setText("");
        passwordField.setText("");
        errorLabel.setText(" ");
        usernameField.requestFocusInWindow();
    }

    private void onLoginClicked() {
        String username = usernameField.getText().trim();
        String password = new String(passwordField.getPassword());

        loginButton.setEnabled(false);
        try {
            Optional<Role> role = AuthService.login(username, password);
            if (role.isEmpty()) {
                errorLabel.setText("Invalid username or password.");
                passwordField.setText("");
                passwordField.requestFocusInWindow();
                return;
            }
            errorLabel.setText(" ");
            frame.showHomeFor(role.get());
        } finally {
            loginButton.setEnabled(true);
        }
    }
}
