package cs336.travel.ui;

import cs336.travel.model.Aircraft;
import cs336.travel.model.Airline;
import cs336.travel.model.CrudResult;
import cs336.travel.service.CRService;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
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

final class AircraftDialog extends JDialog {

    private final Aircraft existing;  // null = create mode
    private final JTextField idField        = new JTextField(20);
    private final JComboBox<Airline> airlineCombo = new JComboBox<>();
    private final JTextField capacityField  = new JTextField(8);
    private final JLabel errorLabel = new JLabel(" ");
    private CrudResult outcome;

    private AircraftDialog(JFrame owner, Aircraft existing) {
        super(owner, existing == null ? "Add Aircraft" : "Edit Aircraft", true);
        this.existing = existing;
        for (Airline a : CRService.listAirlines()) airlineCombo.addItem(a);
        airlineCombo.setRenderer(new javax.swing.DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(
                    javax.swing.JList<?> list, Object value, int index,
                    boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof Airline a) setText(a.airlineID() + " — " + a.airlineName());
                return this;
            }
        });
        if (existing != null) {
            idField.setText(existing.aircraftID());
            // Aircraft ID is editable in edit mode; the service refuses the
            // rename when the old ID still has Flight references.
            for (int i = 0; i < airlineCombo.getItemCount(); i++) {
                if (airlineCombo.getItemAt(i).airlineID().equals(existing.airlineID())) {
                    airlineCombo.setSelectedIndex(i);
                    break;
                }
            }
            capacityField.setText(String.valueOf(existing.seatCapacity()));
        }
        buildLayout();
        setSize(440, 280);
        setLocationRelativeTo(owner);
    }

    static CrudResult openAdd(Component parent) {
        return open(parent, null);
    }

    static CrudResult openEdit(Component parent, Aircraft a) {
        return open(parent, a);
    }

    private static CrudResult open(Component parent, Aircraft existing) {
        JFrame owner = (JFrame) javax.swing.SwingUtilities.getWindowAncestor(parent);
        AircraftDialog d = new AircraftDialog(owner, existing);
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
        addRow(form, g, 0, "Aircraft ID",   idField);
        addRow(form, g, 1, "Airline",       airlineCombo);
        addRow(form, g, 2, "Seat Capacity", capacityField);

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
                               String label, javax.swing.JComponent field) {
        g.gridy = row;
        g.gridx = 0; g.weightx = 0; form.add(new JLabel(label), g);
        g.gridx = 1; g.weightx = 1; field.setPreferredSize(new Dimension(260, 24));
        form.add(field, g);
    }

    private void onSave() {
        String id = idField.getText().trim();
        if (id.isEmpty() || id.length() > 20) {
            errorLabel.setText("Aircraft ID is required (max 20 chars).");
            return;
        }
        Airline airline = (Airline) airlineCombo.getSelectedItem();
        if (airline == null) {
            errorLabel.setText("Pick an airline.");
            return;
        }
        int capacity;
        try {
            capacity = Integer.parseInt(capacityField.getText().trim());
        } catch (NumberFormatException ex) {
            errorLabel.setText("Seat capacity must be an integer.");
            return;
        }
        if (capacity < 1) {
            errorLabel.setText("Seat capacity must be at least 1.");
            return;
        }

        CrudResult r = (existing == null)
                ? CRService.createAircraft(id, airline.airlineID(), capacity)
                : CRService.updateAircraft(existing.aircraftID(), id,
                                           airline.airlineID(), capacity);
        switch (r) {
            case CrudResult.Success s -> { outcome = s; dispose(); }
            case CrudResult.Refused x -> errorLabel.setText(x.reason());
            case CrudResult.Error  x -> errorLabel.setText(x.message());
        }
    }
}
