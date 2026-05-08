package cs336.travel.ui;

import cs336.travel.model.CrudResult;
import cs336.travel.model.Customer;
import cs336.travel.model.Employee;
import cs336.travel.service.AdminService;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.function.Function;

/**
 * Add/Edit dialog for Customer Reps and Customers. Single class with a
 * {@link Mode} so we don't fork two near-identical dialogs.
 */
final class UserDialog extends JDialog {

    enum Kind { CUSTOMER_REP, CUSTOMER }

    private final Kind kind;
    private final Integer existingId; // null = create mode

    private final JTextField usernameField = new JTextField(20);
    private final JPasswordField passwordField = new JPasswordField(20);
    private final JTextField nameField = new JTextField(20);
    private final JTextField emailField = new JTextField(20);
    private final JTextField phoneField = new JTextField(20);
    private final JLabel errorLabel = new JLabel(" ");

    private CrudResult outcome;

    private UserDialog(JFrame owner, Kind kind, Integer existingId, String title) {
        super(owner, title, true);
        this.kind = kind;
        this.existingId = existingId;
        buildLayout();
        setSize(480, 340);
        setLocationRelativeTo(owner);
    }

    /** Opens an Add dialog. Returns the {@link CrudResult} when the user dismisses, or null if Cancel. */
    static CrudResult openAdd(Component parent, Kind kind) {
        JFrame owner = (JFrame) javax.swing.SwingUtilities.getWindowAncestor(parent);
        UserDialog d = new UserDialog(owner, kind, null,
                kind == Kind.CUSTOMER_REP ? "Add Customer Rep" : "Add Customer");
        d.setVisible(true);
        return d.outcome;
    }

    /** Opens an Edit dialog pre-filled with the given record. */
    static CrudResult openEditRep(Component parent, Employee e) {
        JFrame owner = (JFrame) javax.swing.SwingUtilities.getWindowAncestor(parent);
        UserDialog d = new UserDialog(owner, Kind.CUSTOMER_REP, e.employeeID(),
                "Edit Customer Rep");
        d.usernameField.setText(e.username());
        d.nameField.setText(e.name());
        d.passwordField.setText("");
        d.setVisible(true);
        return d.outcome;
    }

    static CrudResult openEditCustomer(Component parent, Customer c) {
        JFrame owner = (JFrame) javax.swing.SwingUtilities.getWindowAncestor(parent);
        UserDialog d = new UserDialog(owner, Kind.CUSTOMER, c.customerID(),
                "Edit Customer");
        d.usernameField.setText(c.username());
        d.nameField.setText(c.name());
        d.emailField.setText(c.email());
        d.phoneField.setText(c.phone() == null ? "" : c.phone());
        d.passwordField.setText("");
        d.setVisible(true);
        return d.outcome;
    }

    private void buildLayout() {
        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(BorderFactory.createEmptyBorder(12, 16, 8, 16));
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(4, 4, 4, 4);
        g.anchor = GridBagConstraints.WEST;
        g.fill = GridBagConstraints.HORIZONTAL;

        int row = 0;
        addRow(form, g, row++, "Username",  usernameField);
        String pwLabel = existingId == null ? "Password" : "Password (blank = keep current)";
        addRow(form, g, row++, pwLabel,     passwordField);
        addRow(form, g, row++, "Name",      nameField);
        if (kind == Kind.CUSTOMER) {
            addRow(form, g, row++, "Email",         emailField);
            addRow(form, g, row++, "Phone (opt.)",  phoneField);
        }

        errorLabel.setForeground(new Color(0xB00020));

        JButton save = new JButton("Save");
        save.addActionListener(e -> onSave());
        JButton cancel = new JButton("Cancel");
        cancel.addActionListener(e -> { outcome = null; dispose(); });

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 4));
        buttons.add(save);
        buttons.add(cancel);

        JPanel root = new JPanel(new BorderLayout());
        root.add(form,        BorderLayout.NORTH);
        root.add(errorLabel,  BorderLayout.CENTER);
        errorLabel.setBorder(BorderFactory.createEmptyBorder(0, 16, 8, 16));
        root.add(buttons,     BorderLayout.SOUTH);
        setContentPane(root);
    }

    private static void addRow(JPanel form, GridBagConstraints g, int row,
                               String label, JTextField field) {
        g.gridy = row;
        g.gridx = 0;  g.weightx = 0;  form.add(new JLabel(label), g);
        g.gridx = 1;  g.weightx = 1;  field.setPreferredSize(new Dimension(280, 24));
        form.add(field, g);
    }

    private void onSave() {
        String username = usernameField.getText().trim();
        String password = new String(passwordField.getPassword());
        String name     = nameField.getText().trim();

        String err = validate(username, password, name);
        if (err != null) { errorLabel.setText(err); return; }

        CrudResult result;
        if (kind == Kind.CUSTOMER_REP) {
            result = (existingId == null)
                    ? AdminService.createCustomerRep(username, password, name)
                    : AdminService.updateCustomerRep(existingId, username, password, name);
        } else {
            String email = emailField.getText().trim();
            String phone = phoneField.getText().trim();
            result = (existingId == null)
                    ? AdminService.createCustomer(username, password, name, email, phone)
                    : AdminService.updateCustomer(existingId, username, password, name, email, phone);
        }

        switch (result) {
            case CrudResult.Success s -> { outcome = s; dispose(); }
            case CrudResult.Refused r -> errorLabel.setText(r.reason());
            case CrudResult.Error  e  -> errorLabel.setText(e.message());
        }
    }

    private String validate(String username, String password, String name) {
        if (username.isEmpty())                    return "Username is required.";
        if (lengthOutside(username, 3, 50))        return "Username must be 3–50 characters.";
        if (existingId == null) {
            if (password.isEmpty())                return "Password is required.";
            if (lengthOutside(password, 6, 255))   return "Password must be 6–255 characters.";
        } else if (!password.isEmpty() && lengthOutside(password, 6, 255)) {
            return "Password must be 6–255 characters (leave blank to keep current).";
        }
        if (name.isEmpty())                        return "Name is required.";
        if (lengthOutside(name, 1, 100))           return "Name must be 1–100 characters.";
        if (kind == Kind.CUSTOMER) {
            String email = emailField.getText().trim();
            if (email.isEmpty())                   return "Email is required.";
            if (!email.contains("@") || lengthOutside(email, 3, 150))
                return "Email looks invalid.";
        }
        return null;
    }

    private static Function<String, Integer> len = String::length;
    private static boolean lengthOutside(String s, int min, int max) {
        int n = len.apply(s);
        return n < min || n > max;
    }
}
