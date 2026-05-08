package cs336.travel.ui;

import cs336.travel.model.Airline;
import cs336.travel.model.Airport;
import cs336.travel.model.AirportFlightRow;
import cs336.travel.model.Role;
import cs336.travel.model.WaitlistRow;
import cs336.travel.service.CRService;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Font;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Locale;

public final class CRReportsPanel extends JPanel {

    public static final int TAB_WAITLIST = 0;
    public static final int TAB_FLIGHTS  = 1;

    private static final NumberFormat MONEY = NumberFormat.getCurrencyInstance(Locale.US);
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter DATETIME_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private static final String[] WAITLIST_COLS = {
            "Customer", "Email", "Phone", "Class", "Status", "Requested At"
    };
    private static final String[] FLIGHT_COLS = {
            "Direction", "Airline Flight", "Other Endpoint",
            "Depart", "Arrive", "Days", "Aircraft", "Base Price"
    };

    private static final String CARD_TABLE = "table";
    private static final String CARD_EMPTY = "empty";

    private final MainFrame frame;
    private final JTabbedPane tabs = new JTabbedPane();

    // Waitlist tab
    private final JComboBox<Airline> airlineCombo = new JComboBox<>();
    private final JTextField flightNumberField   = new JTextField(8);
    private final JFormattedTextField dateField  = new JFormattedTextField();
    private final JLabel waitlistSummary = new JLabel(" ");
    private final DefaultTableModel waitlistModel = readonly(WAITLIST_COLS);
    private final JTable waitlistTable = new JTable(waitlistModel);
    private final CardLayout waitlistCards = new CardLayout();
    private final JPanel waitlistArea = new JPanel(waitlistCards);
    private final JLabel waitlistEmpty = italicGray(" ");

    // Flights-by-airport tab
    private final JComboBox<Airport> airportCombo = new JComboBox<>();
    private final JLabel flightSummary = new JLabel(" ");
    private final DefaultTableModel flightModel = readonly(FLIGHT_COLS);
    private final JTable flightTable = new JTable(flightModel);
    private final CardLayout flightCards = new CardLayout();
    private final JPanel flightArea = new JPanel(flightCards);
    private final JLabel flightEmpty = italicGray(" ");

    public CRReportsPanel(MainFrame frame) {
        this.frame = frame;
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        add(buildHeader(), BorderLayout.NORTH);
        add(buildTabs(),   BorderLayout.CENTER);
    }

    private JPanel buildHeader() {
        JLabel title = new JLabel("Reports");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 18f));
        JButton refresh = new JButton("Refresh");
        refresh.addActionListener(e -> refresh());
        JButton back = new JButton("Back to CR Home");
        back.addActionListener(e -> frame.showHomeFor(Role.CUSTOMER_REP));

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        right.add(refresh);
        right.add(back);
        JPanel header = new JPanel(new BorderLayout());
        header.add(title, BorderLayout.WEST);
        header.add(right, BorderLayout.EAST);
        header.setBorder(BorderFactory.createEmptyBorder(0, 0, 8, 0));
        return header;
    }

    private JTabbedPane buildTabs() {
        tabs.addTab("Waitlist Passengers", buildWaitlistTab());
        tabs.addTab("Flights by Airport",  buildFlightsTab());
        return tabs;
    }

    private JPanel buildWaitlistTab() {
        // Don't query the DB during construction — MainFrame builds every panel
        // up front, before any login, so Session.role() is null and CRService
        // would reject the call. Populate in refresh() instead.
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
        // No Format on the field; manual parse on submit avoids a JDK 21 JFormattedTextField + DateTimeFormatter conflict.
        dateField.setColumns(10);
        dateField.setText(LocalDate.now().format(DATE_FMT));

        JButton lookup = new JButton("Look up");
        lookup.addActionListener(e -> runWaitlistLookup());

        JPanel filterRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        filterRow.add(new JLabel("Airline:"));
        filterRow.add(airlineCombo);
        filterRow.add(new JLabel("Flight #:"));
        filterRow.add(flightNumberField);
        filterRow.add(new JLabel("Date (yyyy-MM-dd):"));
        filterRow.add(dateField);
        filterRow.add(lookup);

        waitlistSummary.setFont(waitlistSummary.getFont().deriveFont(Font.BOLD, 13f));
        waitlistSummary.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));

        waitlistTable.setAutoCreateRowSorter(false);
        waitlistArea.add(new JScrollPane(waitlistTable), CARD_TABLE);
        waitlistArea.add(waitlistEmpty,                  CARD_EMPTY);
        waitlistEmpty.setText("Pick airline + flight # + date, then Look up.");
        waitlistCards.show(waitlistArea, CARD_EMPTY);

        JPanel center = new JPanel(new BorderLayout());
        center.add(waitlistSummary, BorderLayout.NORTH);
        center.add(waitlistArea,    BorderLayout.CENTER);

        JPanel tab = new JPanel(new BorderLayout(0, 4));
        tab.add(filterRow, BorderLayout.NORTH);
        tab.add(center,    BorderLayout.CENTER);
        return tab;
    }

    private JPanel buildFlightsTab() {
        // Same deferral as the waitlist tab — combo is populated in refresh().
        airportCombo.setRenderer(new javax.swing.DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(
                    javax.swing.JList<?> list, Object value, int index,
                    boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof Airport a) setText(a.airportID() + " — " + a.name());
                return this;
            }
        });

        JButton lookup = new JButton("Look up");
        lookup.addActionListener(e -> runFlightsLookup());

        JPanel filterRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        filterRow.add(new JLabel("Airport:"));
        filterRow.add(airportCombo);
        filterRow.add(lookup);

        flightSummary.setFont(flightSummary.getFont().deriveFont(Font.BOLD, 13f));
        flightSummary.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));

        flightTable.setAutoCreateRowSorter(false);
        flightArea.add(new JScrollPane(flightTable), CARD_TABLE);
        flightArea.add(flightEmpty,                  CARD_EMPTY);
        flightEmpty.setText("Pick an airport, then Look up.");
        flightCards.show(flightArea, CARD_EMPTY);

        JPanel center = new JPanel(new BorderLayout());
        center.add(flightSummary, BorderLayout.NORTH);
        center.add(flightArea,    BorderLayout.CENTER);

        JPanel tab = new JPanel(new BorderLayout(0, 4));
        tab.add(filterRow, BorderLayout.NORTH);
        tab.add(center,    BorderLayout.CENTER);
        return tab;
    }

    /** Called on every navigation. Reloads combos from the DB. */
    public void refresh() {
        Airline prevAirline = (Airline) airlineCombo.getSelectedItem();
        airlineCombo.removeAllItems();
        for (Airline a : CRService.listAirlines()) airlineCombo.addItem(a);
        if (prevAirline != null) {
            for (int i = 0; i < airlineCombo.getItemCount(); i++) {
                if (airlineCombo.getItemAt(i).airlineID().equals(prevAirline.airlineID())) {
                    airlineCombo.setSelectedIndex(i);
                    break;
                }
            }
        }
        Airport prevAirport = (Airport) airportCombo.getSelectedItem();
        airportCombo.removeAllItems();
        for (Airport a : CRService.listAirports()) airportCombo.addItem(a);
        if (prevAirport != null) {
            for (int i = 0; i < airportCombo.getItemCount(); i++) {
                if (airportCombo.getItemAt(i).airportID().equals(prevAirport.airportID())) {
                    airportCombo.setSelectedIndex(i);
                    break;
                }
            }
        }
    }

    public void selectTab(int index) {
        if (index >= 0 && index < tabs.getTabCount()) tabs.setSelectedIndex(index);
    }

    private void runWaitlistLookup() {
        Airline a = (Airline) airlineCombo.getSelectedItem();
        String flightNumber = flightNumberField.getText().trim();
        String dateText     = dateField.getText().trim();
        if (a == null || flightNumber.isEmpty() || dateText.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Pick airline, flight #, and date.",
                    "Missing input", JOptionPane.WARNING_MESSAGE);
            return;
        }
        LocalDate date;
        try {
            date = LocalDate.parse(dateText, DATE_FMT);
        } catch (DateTimeParseException ex) {
            JOptionPane.showMessageDialog(this,
                    "Date must be yyyy-MM-dd.",
                    "Invalid date", JOptionPane.WARNING_MESSAGE);
            return;
        }

        List<WaitlistRow> rows = CRService.listWaitlistFor(
                a.airlineID(), flightNumber, date);
        waitlistModel.setRowCount(0);
        int waiting = 0, promoted = 0, expired = 0;
        for (WaitlistRow r : rows) {
            switch (r.status()) {
                case "WAITING"  -> waiting++;
                case "PROMOTED" -> promoted++;
                case "EXPIRED"  -> expired++;
            }
            waitlistModel.addRow(new Object[]{
                    r.username() + " (" + r.customerName() + ")",
                    r.email(),
                    r.phone() == null ? "" : r.phone(),
                    r.travelClass().label(),
                    r.status(),
                    r.requestedAt().format(DATETIME_FMT)
            });
        }
        String label = a.airlineID() + " " + flightNumber + " on " + date;
        if (rows.isEmpty()) {
            waitlistSummary.setText("No one is on the waitlist for " + label + ".");
            waitlistEmpty.setText("No one is on the waitlist for " + label + ".");
            waitlistCards.show(waitlistArea, CARD_EMPTY);
        } else {
            StringBuilder s = new StringBuilder("Waitlist for ").append(label).append(": ")
                    .append(rows.size()).append(" passenger")
                    .append(rows.size() == 1 ? "" : "s").append(" (")
                    .append(waiting).append(" WAITING");
            if (promoted > 0) s.append(", ").append(promoted).append(" PROMOTED");
            if (expired  > 0) s.append(", ").append(expired).append(" EXPIRED");
            s.append(")");
            waitlistSummary.setText(s.toString());
            waitlistCards.show(waitlistArea, CARD_TABLE);
        }
    }

    private void runFlightsLookup() {
        Airport a = (Airport) airportCombo.getSelectedItem();
        if (a == null) {
            JOptionPane.showMessageDialog(this, "Pick an airport.",
                    "Missing input", JOptionPane.WARNING_MESSAGE);
            return;
        }
        List<AirportFlightRow> rows = CRService.listFlightsAt(a.airportID());
        flightModel.setRowCount(0);
        int departing = 0, arriving = 0;
        for (AirportFlightRow r : rows) {
            if ("Departing".equals(r.direction())) departing++; else arriving++;
            flightModel.addRow(new Object[]{
                    r.direction(),
                    Format.flightLabel(r.airlineID(), r.flightNumber()),
                    r.otherEndpoint(),
                    r.departureTime().toString(),
                    r.arrivalTime().toString(),
                    r.operatingDays(),
                    r.aircraftID(),
                    MONEY.format(r.basePrice())
            });
        }
        if (rows.isEmpty()) {
            flightSummary.setText("No flights for " + a.airportID() + ".");
            flightEmpty.setText("No flights for " + a.airportID() + ".");
            flightCards.show(flightArea, CARD_EMPTY);
        } else {
            flightSummary.setText("Flights at " + a.airportID() + ": "
                    + rows.size() + " total ("
                    + departing + " departing, " + arriving + " arriving)");
            flightCards.show(flightArea, CARD_TABLE);
        }
    }

    private static DefaultTableModel readonly(String[] cols) {
        return new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
    }

    private static JLabel italicGray(String text) {
        JLabel l = new JLabel(text, SwingConstants.CENTER);
        l.setFont(l.getFont().deriveFont(Font.ITALIC, 14f));
        l.setForeground(Color.GRAY);
        return l;
    }
}
