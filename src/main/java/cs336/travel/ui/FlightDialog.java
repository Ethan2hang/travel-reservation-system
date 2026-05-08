package cs336.travel.ui;

import cs336.travel.model.Aircraft;
import cs336.travel.model.Airline;
import cs336.travel.model.Airport;
import cs336.travel.model.CrudResult;
import cs336.travel.model.Flight;
import cs336.travel.service.CRService;

import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
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
import java.math.BigDecimal;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

final class FlightDialog extends JDialog {

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");
    private static final String[] DAY_TOKENS = {"MON","TUE","WED","THU","FRI","SAT","SUN"};

    private final Flight existing;

    private final JComboBox<Airline>  airlineCombo  = new JComboBox<>();
    private final JTextField          flightNumberField = new JTextField(8);
    private final JComboBox<Airport>  fromCombo     = new JComboBox<>();
    private final JComboBox<Airport>  toCombo       = new JComboBox<>();
    private final JTextField          departTimeField = new JTextField(6);
    private final JTextField          arriveTimeField = new JTextField(6);
    private final JCheckBox[]         dayBoxes      = new JCheckBox[7];
    private final JCheckBox           domesticBox   = new JCheckBox("Domestic");
    private final JTextField          basePriceField= new JTextField(8);
    private final JComboBox<Aircraft> aircraftCombo = new JComboBox<>();

    private final JLabel errorLabel = new JLabel(" ");
    private CrudResult outcome;

    private FlightDialog(JFrame owner, Flight existing) {
        super(owner, existing == null ? "Add Flight" : "Edit Flight", true);
        this.existing = existing;

        for (Airline a : CRService.listAirlines()) airlineCombo.addItem(a);
        for (Airport a : CRService.listAirports()) {
            fromCombo.addItem(a);
            toCombo.addItem(a);
        }
        airlineCombo.setRenderer(airlineRenderer());
        fromCombo.setRenderer(airportRenderer());
        toCombo.setRenderer(airportRenderer());
        aircraftCombo.setRenderer(aircraftRenderer());

        airlineCombo.addActionListener(e -> reloadAircraftCombo());
        reloadAircraftCombo();

        for (int i = 0; i < 7; i++) dayBoxes[i] = new JCheckBox(DAY_TOKENS[i]);

        if (existing != null) {
            select(airlineCombo, a -> a.airlineID().equals(existing.airlineID()));
            airlineCombo.setEnabled(false);  // PK locked on edit
            flightNumberField.setText(existing.flightNumber());
            flightNumberField.setEditable(false);
            select(fromCombo, a -> a.airportID().equals(existing.departureAirport()));
            select(toCombo,   a -> a.airportID().equals(existing.arrivalAirport()));
            departTimeField.setText(existing.departureTime().format(TIME_FMT));
            arriveTimeField.setText(existing.arrivalTime().format(TIME_FMT));
            for (String tok : existing.operatingDays().split(",")) {
                String t = tok.trim();
                for (int i = 0; i < 7; i++) if (DAY_TOKENS[i].equals(t)) dayBoxes[i].setSelected(true);
            }
            domesticBox.setSelected(existing.isDomestic());
            basePriceField.setText(existing.basePrice().toPlainString());
            reloadAircraftCombo();
            select(aircraftCombo, a -> a.aircraftID().equals(existing.aircraftID()));
        } else {
            domesticBox.setSelected(true);
        }

        buildLayout();
        setSize(560, 520);
        setLocationRelativeTo(owner);
    }

    static CrudResult openAdd(Component parent) { return open(parent, null); }
    static CrudResult openEdit(Component parent, Flight f) { return open(parent, f); }

    private static CrudResult open(Component parent, Flight existing) {
        JFrame owner = (JFrame) javax.swing.SwingUtilities.getWindowAncestor(parent);
        FlightDialog d = new FlightDialog(owner, existing);
        d.setVisible(true);
        return d.outcome;
    }

    private void reloadAircraftCombo() {
        Airline a = (Airline) airlineCombo.getSelectedItem();
        if (a == null) return;
        java.util.List<Aircraft> craft = CRService.listAircraftFor(a.airlineID());
        Aircraft prev = (Aircraft) aircraftCombo.getSelectedItem();
        aircraftCombo.setModel(new DefaultComboBoxModel<>(craft.toArray(new Aircraft[0])));
        if (prev != null) {
            for (int i = 0; i < aircraftCombo.getItemCount(); i++) {
                if (aircraftCombo.getItemAt(i).aircraftID().equals(prev.aircraftID())) {
                    aircraftCombo.setSelectedIndex(i);
                    return;
                }
            }
        }
    }

    private void buildLayout() {
        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(BorderFactory.createEmptyBorder(12, 16, 8, 16));
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(4, 4, 4, 4);
        g.anchor = GridBagConstraints.WEST;
        g.fill = GridBagConstraints.HORIZONTAL;

        int row = 0;
        addRow(form, g, row++, "Airline",       airlineCombo);
        addRow(form, g, row++, "Flight #",      flightNumberField);
        addRow(form, g, row++, "Departure",     fromCombo);
        addRow(form, g, row++, "Arrival",       toCombo);
        addRow(form, g, row++, "Depart (HH:mm)", departTimeField);
        addRow(form, g, row++, "Arrive (HH:mm)", arriveTimeField);

        JPanel daysRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        for (JCheckBox b : dayBoxes) daysRow.add(b);
        addRow(form, g, row++, "Operating days", daysRow);

        addRow(form, g, row++, "Domestic",       domesticBox);
        addRow(form, g, row++, "Base price",     basePriceField);
        addRow(form, g, row++, "Aircraft",       aircraftCombo);

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
        g.gridx = 1; g.weightx = 1;
        if (field instanceof JTextField || field instanceof JComboBox)
            field.setPreferredSize(new Dimension(320, 24));
        form.add(field, g);
    }

    private void onSave() {
        Airline airline = (Airline) airlineCombo.getSelectedItem();
        if (airline == null) { errorLabel.setText("Pick an airline."); return; }
        String flightNumber = flightNumberField.getText().trim();
        if (flightNumber.isEmpty() || flightNumber.length() > 10) {
            errorLabel.setText("Flight # is required (max 10 chars).");
            return;
        }
        Airport from = (Airport) fromCombo.getSelectedItem();
        Airport to   = (Airport) toCombo.getSelectedItem();
        if (from == null || to == null) { errorLabel.setText("Pick from and to airports."); return; }
        if (from.airportID().equals(to.airportID())) {
            errorLabel.setText("Departure and arrival must differ.");
            return;
        }
        LocalTime depart, arrive;
        try {
            depart = LocalTime.parse(departTimeField.getText().trim(), TIME_FMT);
            arrive = LocalTime.parse(arriveTimeField.getText().trim(), TIME_FMT);
        } catch (DateTimeParseException ex) {
            errorLabel.setText("Times must be HH:mm.");
            return;
        }
        StringBuilder days = new StringBuilder();
        for (int i = 0; i < 7; i++) {
            if (dayBoxes[i].isSelected()) {
                if (days.length() > 0) days.append(',');
                days.append(DAY_TOKENS[i]);
            }
        }
        if (days.length() == 0) {
            errorLabel.setText("Select at least one operating day.");
            return;
        }
        BigDecimal price;
        try {
            price = new BigDecimal(basePriceField.getText().trim());
        } catch (NumberFormatException ex) {
            errorLabel.setText("Base price must be a number.");
            return;
        }
        if (price.signum() < 0) {
            errorLabel.setText("Base price must be 0 or greater.");
            return;
        }
        Aircraft aircraft = (Aircraft) aircraftCombo.getSelectedItem();
        if (aircraft == null) {
            errorLabel.setText("Pick an aircraft (first add one if none exist for this airline).");
            return;
        }

        Flight f = new Flight(
                airline.airlineID(), flightNumber, aircraft.aircraftID(),
                from.airportID(), to.airportID(),
                depart, arrive,
                days.toString(),
                domesticBox.isSelected(),
                price);

        CrudResult r = (existing == null) ? CRService.createFlight(f) : CRService.updateFlight(f);
        switch (r) {
            case CrudResult.Success s -> { outcome = s; dispose(); }
            case CrudResult.Refused x -> errorLabel.setText(x.reason());
            case CrudResult.Error  x -> errorLabel.setText(x.message());
        }
    }

    private static <T> void select(JComboBox<T> combo, java.util.function.Predicate<T> pred) {
        for (int i = 0; i < combo.getItemCount(); i++) {
            if (pred.test(combo.getItemAt(i))) { combo.setSelectedIndex(i); return; }
        }
    }

    private static javax.swing.DefaultListCellRenderer airlineRenderer() {
        return new javax.swing.DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(
                    javax.swing.JList<?> list, Object value, int index,
                    boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof Airline a) setText(a.airlineID() + " — " + a.airlineName());
                return this;
            }
        };
    }

    private static javax.swing.DefaultListCellRenderer airportRenderer() {
        return new javax.swing.DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(
                    javax.swing.JList<?> list, Object value, int index,
                    boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof Airport a) setText(a.airportID() + " — " + a.name());
                return this;
            }
        };
    }

    private static javax.swing.DefaultListCellRenderer aircraftRenderer() {
        return new javax.swing.DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(
                    javax.swing.JList<?> list, Object value, int index,
                    boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof Aircraft a) setText(a.aircraftID() + " (cap " + a.seatCapacity() + ")");
                return this;
            }
        };
    }
}
