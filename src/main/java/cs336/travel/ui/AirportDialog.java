package cs336.travel.ui;

import cs336.travel.model.Airport;
import cs336.travel.model.CrudResult;
import cs336.travel.service.CRService;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

final class AirportDialog extends JDialog {

    private final Airport existing;
    private final JTextField idField      = new JTextField(6);
    private final JTextField nameField    = new JTextField(20);
    private final JTextField cityField    = new JTextField(20);
    private final JTextField countryField = new JTextField(20);
    private final JLabel errorLabel = new JLabel(" ");
    private CrudResult outcome;

    private AirportDialog(JFrame owner, Airport existing) {
        super(owner, existing == null ? "Add Airport" : "Edit Airport", true);
        this.existing = existing;
        if (existing != null) {
            idField.setText(existing.airportID());
            // Airport ID is editable in edit mode; the service refuses the
            // rename when the old code still has Flight references.
            nameField.setText(existing.name());
            cityField.setText(existing.city());
            countryField.setText(existing.country());
        }
        buildLayout();
        setSize(440, 320);
        setLocationRelativeTo(owner);
    }

    static CrudResult openAdd(Component parent) { return open(parent, null); }
    static CrudResult openEdit(Component parent, Airport a) { return open(parent, a); }

    private static CrudResult open(Component parent, Airport existing) {
        JFrame owner = (JFrame) javax.swing.SwingUtilities.getWindowAncestor(parent);
        AirportDialog d = new AirportDialog(owner, existing);
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
        addRow(form, g, 0, "Airport ID (3 letters)", idField);
        addRow(form, g, 1, "Name",                   nameField);
        addRow(form, g, 2, "City",                   cityField);
        addRow(form, g, 3, "Country",                countryField);

        errorLabel.setForeground(new Color(0xB00020));
        errorLabel.setBorder(BorderFactory.createEmptyBorder(0, 16, 8, 16));

        JButton save = new JButton("Save");
        save.addActionListener(e -> onSave());
        JButton cancel = new JButton("Cancel");
        cancel.addActionListener(e -> { outcome = null; dispose(); });
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 4));
        buttons.add(save);
        buttons.add(cancel);

        JPanel root = new JPanel(new BorderLayout());
        root.add(form,       BorderLayout.NORTH);
        root.add(errorLabel, BorderLayout.CENTER);
        root.add(buttons,    BorderLayout.SOUTH);
        setContentPane(root);
    }

    private static void addRow(JPanel form, GridBagConstraints g, int row,
                               String label, JTextField field) {
        g.gridy = row;
        g.gridx = 0; g.weightx = 0; form.add(new JLabel(label), g);
        g.gridx = 1; g.weightx = 1; field.setPreferredSize(new Dimension(260, 24));
        form.add(field, g);
    }

    private void onSave() {
        String id = idField.getText().trim().toUpperCase();
        if (id.length() != 3) {
            errorLabel.setText("Airport ID must be exactly 3 letters.");
            return;
        }
        String name    = nameField.getText().trim();
        String city    = cityField.getText().trim();
        String country = countryField.getText().trim();
        if (name.isEmpty() || name.length() > 150) {
            errorLabel.setText("Name is required (max 150 chars).");
            return;
        }
        if (city.isEmpty() || city.length() > 100) {
            errorLabel.setText("City is required (max 100 chars).");
            return;
        }
        if (country.isEmpty() || country.length() > 100) {
            errorLabel.setText("Country is required (max 100 chars).");
            return;
        }

        CrudResult r = (existing == null)
                ? CRService.createAirport(id, name, city, country)
                : CRService.updateAirport(existing.airportID(), id, name, city, country);
        switch (r) {
            case CrudResult.Success s -> { outcome = s; dispose(); }
            case CrudResult.Refused x -> errorLabel.setText(x.reason());
            case CrudResult.Error  x -> errorLabel.setText(x.message());
        }
    }
}
