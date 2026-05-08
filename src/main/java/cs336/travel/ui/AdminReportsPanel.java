package cs336.travel.ui;

import cs336.travel.dao.AirlineDAO;
import cs336.travel.model.AggregateRow;
import cs336.travel.model.Airline;
import cs336.travel.model.MonthlySales;
import cs336.travel.model.ReservationLookupRow;
import cs336.travel.model.RevenueDetail;
import cs336.travel.model.RevenueDetailRow;
import cs336.travel.model.Role;
import cs336.travel.model.TicketSaleRow;
import cs336.travel.model.TopCustomerRow;
import cs336.travel.service.AdminService;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.Month;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.Locale;

public final class AdminReportsPanel extends JPanel {

    private static final NumberFormat MONEY = NumberFormat.getCurrencyInstance(Locale.US);
    private static final DateTimeFormatter DATETIME_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private static final String[] SALES_COLUMNS = {
            "Ticket #", "Reservation #", "Customer", "Trip Type",
            "Purchase Date", "Booking Fee", "Fare", "Total"
    };
    private static final String[] LOOKUP_COLUMNS = {
            "Reservation #", "Customer", "Booked On", "Trip Type", "Status", "Total Fare"
    };

    private static final String[] REV_BY_FLIGHT_COLS    = {
            "Reservation #", "Customer", "Purchase Date", "Class", "Total"
    };
    private static final String[] REV_BY_AIRLINE_COLS   = {
            "Flight #", "Customer", "Purchase Date", "Class", "Total"
    };
    private static final String[] REV_BY_CUSTOMER_COLS  = {
            "Customer", "Reservation #", "Purchase Date", "Class", "Total"
    };
    private static final String[] TOP_CUSTOMER_COLS     = {
            "Username", "Name", "Tickets", "Total Revenue"
    };
    private static final String[] MOST_ACTIVE_COLS      = {
            "Airline Flight #", "Tickets Sold", "Total Revenue"
    };

    private static final String CARD_TABLE = "table";
    private static final String CARD_EMPTY = "empty";

    private static final String MODE_REV_FLIGHT   = "rev-flight";
    private static final String MODE_REV_AIRLINE  = "rev-airline";
    private static final String MODE_REV_CUSTOMER = "rev-customer";
    private static final String MODE_TOP_CUSTOMER = "top-customer";
    private static final String MODE_MOST_ACTIVE  = "most-active";
    private static final String INPUT_NONE        = "none";

    private final MainFrame frame;

    private final JComboBox<Integer> yearCombo  = new JComboBox<>();
    private final JComboBox<Month>   monthCombo = new JComboBox<>(Month.values());
    private final JLabel summaryLabel = new JLabel(" ");
    private final DefaultTableModel salesModel = new DefaultTableModel(SALES_COLUMNS, 0) {
        @Override public boolean isCellEditable(int r, int c) { return false; }
    };
    private final JTable salesTable = new JTable(salesModel);
    private final CardLayout salesCards = new CardLayout();
    private final JPanel salesArea = new JPanel(salesCards);
    private final JLabel emptyLabel = emptyLabel();

    // Reservations by Flight tab
    private final JComboBox<Airline> airlineCombo = new JComboBox<>();
    private final JTextField flightNumberField = new JTextField(8);
    private final JLabel flightSummaryLabel = new JLabel(" ");
    private final DefaultTableModel flightModel = new DefaultTableModel(LOOKUP_COLUMNS, 0) {
        @Override public boolean isCellEditable(int r, int c) { return false; }
    };
    private final JTable flightTable = new JTable(flightModel);
    private final CardLayout flightCards = new CardLayout();
    private final JPanel flightArea = new JPanel(flightCards);
    private final JLabel flightEmpty = emptyLabel();

    // Reservations by Customer tab
    private final JTextField nameQueryField = new JTextField(20);
    private final JLabel customerSummaryLabel = new JLabel(" ");
    private final DefaultTableModel customerModel = new DefaultTableModel(LOOKUP_COLUMNS, 0) {
        @Override public boolean isCellEditable(int r, int c) { return false; }
    };
    private final JTable customerTable = new JTable(customerModel);
    private final CardLayout customerCards = new CardLayout();
    private final JPanel customerArea = new JPanel(customerCards);
    private final JLabel customerEmpty = emptyLabel();

    // Revenue & Activity tab
    private final JRadioButton modeRevFlight   = new JRadioButton("Revenue: by Flight");
    private final JRadioButton modeRevAirline  = new JRadioButton("Revenue: by Airline");
    private final JRadioButton modeRevCustomer = new JRadioButton("Revenue: by Customer");
    private final JRadioButton modeTopCustomer = new JRadioButton("Top Customer (by total revenue)", true);
    private final JRadioButton modeMostActive  = new JRadioButton("Most Active Flights (by tickets sold)");
    private final CardLayout activityInputCards = new CardLayout();
    private final JPanel activityInputArea = new JPanel(activityInputCards);
    private final CardLayout activityResultCards = new CardLayout();
    private final JPanel activityResultArea = new JPanel(activityResultCards);
    private final JLabel activitySummaryLabel = new JLabel(" ");

    private final JComboBox<Airline> revFlightAirline = new JComboBox<>();
    private final JTextField         revFlightNumber  = new JTextField(8);
    private final JComboBox<Airline> revAirlineCombo  = new JComboBox<>();
    private final JTextField         revCustomerName  = new JTextField(20);

    private final DefaultTableModel revFlightModel    = readonly(REV_BY_FLIGHT_COLS);
    private final DefaultTableModel revAirlineModel   = readonly(REV_BY_AIRLINE_COLS);
    private final DefaultTableModel revCustomerModel  = readonly(REV_BY_CUSTOMER_COLS);
    private final DefaultTableModel topCustomerModel  = readonly(TOP_CUSTOMER_COLS);
    private final DefaultTableModel mostActiveModel   = readonly(MOST_ACTIVE_COLS);

    public AdminReportsPanel(MainFrame frame) {
        this.frame = frame;
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        add(buildHeader(), BorderLayout.NORTH);
        add(buildTabs(),   BorderLayout.CENTER);
    }

    private JPanel buildHeader() {
        JLabel title = new JLabel("Reports");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 18f));
        JButton back = new JButton("Back to Admin Home");
        back.addActionListener(e -> frame.showHomeFor(Role.ADMIN));

        JPanel header = new JPanel(new BorderLayout());
        header.add(title, BorderLayout.WEST);
        header.add(back,  BorderLayout.EAST);
        header.setBorder(BorderFactory.createEmptyBorder(0, 0, 8, 0));
        return header;
    }

    private JTabbedPane buildTabs() {
        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Sales by Month",            buildSalesTab());
        tabs.addTab("Reservations by Flight",    buildFlightTab());
        tabs.addTab("Reservations by Customer",  buildCustomerTab());
        tabs.addTab("Revenue & Activity",        buildActivityTab());
        return tabs;
    }

    private JPanel buildActivityTab() {
        ButtonGroup group = new ButtonGroup();
        group.add(modeRevFlight);
        group.add(modeRevAirline);
        group.add(modeRevCustomer);
        group.add(modeTopCustomer);
        group.add(modeMostActive);
        modeRevFlight.addActionListener(e   -> showInput(MODE_REV_FLIGHT));
        modeRevAirline.addActionListener(e  -> showInput(MODE_REV_AIRLINE));
        modeRevCustomer.addActionListener(e -> showInput(MODE_REV_CUSTOMER));
        modeTopCustomer.addActionListener(e -> showInput(INPUT_NONE));
        modeMostActive.addActionListener(e  -> showInput(INPUT_NONE));

        JPanel modeRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        modeRow.add(modeRevFlight);
        modeRow.add(modeRevAirline);
        modeRow.add(modeRevCustomer);
        modeRow.add(modeTopCustomer);
        modeRow.add(modeMostActive);

        // Per-mode input cards.
        for (Airline a : AirlineDAO.listAll()) {
            revFlightAirline.addItem(a);
            revAirlineCombo.addItem(a);
        }
        revFlightAirline.setRenderer(airlineRenderer());
        revAirlineCombo.setRenderer(airlineRenderer());

        JPanel inputFlight = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        inputFlight.add(new JLabel("Airline:"));
        inputFlight.add(revFlightAirline);
        inputFlight.add(new JLabel("Flight #:"));
        inputFlight.add(revFlightNumber);

        JPanel inputAirline = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        inputAirline.add(new JLabel("Airline:"));
        inputAirline.add(revAirlineCombo);

        JPanel inputCust = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        inputCust.add(new JLabel("Customer name contains:"));
        inputCust.add(revCustomerName);

        JPanel inputNone = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        // Empty placeholder card: keeps the row's layout but shows nothing.

        activityInputArea.add(inputFlight, MODE_REV_FLIGHT);
        activityInputArea.add(inputAirline, MODE_REV_AIRLINE);
        activityInputArea.add(inputCust,    MODE_REV_CUSTOMER);
        activityInputArea.add(inputNone,    INPUT_NONE);

        JButton run = new JButton("Run report");
        run.addActionListener(e -> runActivityReport());
        JPanel runRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        runRow.add(run);

        JPanel north = new JPanel();
        north.setLayout(new BoxLayout(north, BoxLayout.Y_AXIS));
        north.add(modeRow);
        north.add(activityInputArea);
        north.add(runRow);

        // Result cards — one per mode.
        activityResultArea.add(scrollOf(revFlightModel),   MODE_REV_FLIGHT);
        activityResultArea.add(scrollOf(revAirlineModel),  MODE_REV_AIRLINE);
        activityResultArea.add(scrollOf(revCustomerModel), MODE_REV_CUSTOMER);
        activityResultArea.add(scrollOf(topCustomerModel), MODE_TOP_CUSTOMER);
        activityResultArea.add(scrollOf(mostActiveModel),  MODE_MOST_ACTIVE);

        activitySummaryLabel.setFont(activitySummaryLabel.getFont().deriveFont(Font.BOLD, 13f));
        activitySummaryLabel.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));

        JPanel center = new JPanel(new BorderLayout());
        center.add(activitySummaryLabel, BorderLayout.NORTH);
        center.add(activityResultArea,   BorderLayout.CENTER);

        JPanel tab = new JPanel(new BorderLayout(0, 4));
        tab.add(north,  BorderLayout.NORTH);
        tab.add(center, BorderLayout.CENTER);

        showInput(INPUT_NONE);                       // default to Top Customer
        activityResultCards.show(activityResultArea, MODE_TOP_CUSTOMER);
        activitySummaryLabel.setText("Pick a mode and click Run report.");
        return tab;
    }

    private void showInput(String which) {
        activityInputCards.show(activityInputArea, which);
    }

    private void runActivityReport() {
        if (modeRevFlight.isSelected())          runRevenueByFlight();
        else if (modeRevAirline.isSelected())    runRevenueByAirline();
        else if (modeRevCustomer.isSelected())   runRevenueByCustomer();
        else if (modeMostActive.isSelected())    runMostActiveFlights();
        else                                     runTopCustomer();
    }

    private void runRevenueByFlight() {
        Airline a = (Airline) revFlightAirline.getSelectedItem();
        String flightNumber = revFlightNumber.getText().trim();
        if (a == null || flightNumber.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Pick an airline and enter a flight number.",
                    "Missing input", JOptionPane.WARNING_MESSAGE);
            return;
        }
        RevenueDetail d = AdminService.revenueByFlight(a.airlineID(), flightNumber);
        revFlightModel.setRowCount(0);
        for (RevenueDetailRow r : d.rows()) {
            revFlightModel.addRow(new Object[]{
                    r.reservationID(),
                    r.customerUsername() + " (" + r.customerName() + ")",
                    r.purchaseDateTime().format(DATETIME_FMT),
                    r.travelClass() == null ? "—" : r.travelClass().label(),
                    MONEY.format(r.lineTotal())
            });
        }
        activitySummaryLabel.setText(d.summaryText());
        activityResultCards.show(activityResultArea, MODE_REV_FLIGHT);
    }

    private void runRevenueByAirline() {
        Airline a = (Airline) revAirlineCombo.getSelectedItem();
        if (a == null) {
            JOptionPane.showMessageDialog(this, "Pick an airline.",
                    "Missing input", JOptionPane.WARNING_MESSAGE);
            return;
        }
        RevenueDetail d = AdminService.revenueByAirline(a.airlineID());
        revAirlineModel.setRowCount(0);
        for (RevenueDetailRow r : d.rows()) {
            String fno = (r.airlineID() == null ? "" : r.airlineID() + " ")
                       + (r.flightNumber() == null ? "" : r.flightNumber());
            revAirlineModel.addRow(new Object[]{
                    fno.trim().isEmpty() ? "—" : fno,
                    r.customerUsername() + " (" + r.customerName() + ")",
                    r.purchaseDateTime().format(DATETIME_FMT),
                    r.travelClass() == null ? "—" : r.travelClass().label(),
                    MONEY.format(r.lineTotal())
            });
        }
        activitySummaryLabel.setText(d.summaryText());
        activityResultCards.show(activityResultArea, MODE_REV_AIRLINE);
    }

    private void runRevenueByCustomer() {
        String q = revCustomerName.getText().trim();
        if (q.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Enter a name fragment.",
                    "Missing input", JOptionPane.WARNING_MESSAGE);
            return;
        }
        RevenueDetail d = AdminService.revenueByCustomer(q);
        revCustomerModel.setRowCount(0);
        for (RevenueDetailRow r : d.rows()) {
            revCustomerModel.addRow(new Object[]{
                    r.customerUsername() + " (" + r.customerName() + ")",
                    r.reservationID(),
                    r.purchaseDateTime().format(DATETIME_FMT),
                    r.travelClass() == null ? "—" : r.travelClass().label(),
                    MONEY.format(r.lineTotal())
            });
        }
        activitySummaryLabel.setText(d.summaryText());
        activityResultCards.show(activityResultArea, MODE_REV_CUSTOMER);
    }

    private void runTopCustomer() {
        java.util.Optional<TopCustomerRow> top = AdminService.topCustomerByRevenue();
        topCustomerModel.setRowCount(0);
        if (top.isEmpty()) {
            activitySummaryLabel.setText("No revenue yet — no confirmed reservations on file.");
        } else {
            TopCustomerRow t = top.get();
            topCustomerModel.addRow(new Object[]{
                    t.username(), t.name(), t.ticketCount(), MONEY.format(t.totalRevenue())
            });
            activitySummaryLabel.setText(
                    "Highest-revenue customer: " + t.username() + " (" + t.name() + ") — "
                            + MONEY.format(t.totalRevenue())
                            + " across " + t.ticketCount() + " ticket"
                            + (t.ticketCount() == 1 ? "" : "s") + ".");
        }
        activityResultCards.show(activityResultArea, MODE_TOP_CUSTOMER);
    }

    private void runMostActiveFlights() {
        java.util.List<AggregateRow> rows = AdminService.mostActiveFlights(10);
        mostActiveModel.setRowCount(0);
        for (AggregateRow r : rows) {
            mostActiveModel.addRow(new Object[]{
                    r.label(), r.count(), MONEY.format(r.revenue())
            });
        }
        activitySummaryLabel.setText("Top " + rows.size() + " flight"
                + (rows.size() == 1 ? "" : "s") + " by tickets sold.");
        activityResultCards.show(activityResultArea, MODE_MOST_ACTIVE);
    }

    private static DefaultTableModel readonly(String[] cols) {
        return new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
    }

    private static JScrollPane scrollOf(DefaultTableModel m) {
        JTable t = new JTable(m);
        t.setAutoCreateRowSorter(false);
        return new JScrollPane(t);
    }

    private static javax.swing.DefaultListCellRenderer airlineRenderer() {
        return new javax.swing.DefaultListCellRenderer() {
            @Override
            public java.awt.Component getListCellRendererComponent(
                    javax.swing.JList<?> list, Object value, int index,
                    boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof Airline a) setText(a.airlineID() + " — " + a.airlineName());
                return this;
            }
        };
    }

    private JPanel buildFlightTab() {
        for (Airline a : AirlineDAO.listAll()) airlineCombo.addItem(a);
        airlineCombo.setRenderer(new javax.swing.DefaultListCellRenderer() {
            @Override
            public java.awt.Component getListCellRendererComponent(
                    javax.swing.JList<?> list, Object value, int index,
                    boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof Airline a) setText(a.airlineID() + " — " + a.airlineName());
                return this;
            }
        });

        JButton run = new JButton("Run lookup");
        run.addActionListener(e -> runFlightLookup());

        JPanel filterRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        filterRow.add(new JLabel("Airline:"));
        filterRow.add(airlineCombo);
        filterRow.add(new JLabel("Flight #:"));
        filterRow.add(flightNumberField);
        filterRow.add(run);

        flightSummaryLabel.setFont(flightSummaryLabel.getFont().deriveFont(Font.BOLD, 13f));
        flightSummaryLabel.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));

        flightTable.setAutoCreateRowSorter(false);
        flightArea.add(new JScrollPane(flightTable), CARD_TABLE);
        flightArea.add(flightEmpty,                  CARD_EMPTY);
        flightCards.show(flightArea, CARD_EMPTY);
        flightEmpty.setText("Pick an airline and flight number, then Run lookup.");

        JPanel center = new JPanel(new BorderLayout());
        center.add(flightSummaryLabel, BorderLayout.NORTH);
        center.add(flightArea,         BorderLayout.CENTER);

        JPanel tab = new JPanel(new BorderLayout(0, 4));
        tab.add(filterRow, BorderLayout.NORTH);
        tab.add(center,    BorderLayout.CENTER);
        return tab;
    }

    private JPanel buildCustomerTab() {
        JButton run = new JButton("Run lookup");
        run.addActionListener(e -> runCustomerLookup());
        nameQueryField.addActionListener(e -> runCustomerLookup());

        JPanel filterRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        filterRow.add(new JLabel("Customer name contains:"));
        filterRow.add(nameQueryField);
        filterRow.add(run);

        customerSummaryLabel.setFont(customerSummaryLabel.getFont().deriveFont(Font.BOLD, 13f));
        customerSummaryLabel.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));

        customerTable.setAutoCreateRowSorter(false);
        customerArea.add(new JScrollPane(customerTable), CARD_TABLE);
        customerArea.add(customerEmpty,                  CARD_EMPTY);
        customerCards.show(customerArea, CARD_EMPTY);
        customerEmpty.setText("Type a name fragment, then Run lookup.");

        JPanel center = new JPanel(new BorderLayout());
        center.add(customerSummaryLabel, BorderLayout.NORTH);
        center.add(customerArea,         BorderLayout.CENTER);

        JPanel tab = new JPanel(new BorderLayout(0, 4));
        tab.add(filterRow, BorderLayout.NORTH);
        tab.add(center,    BorderLayout.CENTER);
        return tab;
    }

    private void runFlightLookup() {
        Airline a = (Airline) airlineCombo.getSelectedItem();
        String flightNumber = flightNumberField.getText().trim();
        if (a == null || flightNumber.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Pick an airline and enter a flight number.",
                    "Missing input", JOptionPane.WARNING_MESSAGE);
            return;
        }
        var rows = AdminService.reservationsForFlight(a.airlineID(), flightNumber);
        renderLookup(flightModel, flightArea, flightCards, flightSummaryLabel, flightEmpty,
                rows,
                rows.size() + " reservation"
                        + (rows.size() == 1 ? "" : "s")
                        + " touching " + a.airlineID() + " " + flightNumber + ".",
                "No reservations touch " + a.airlineID() + " " + flightNumber + ".");
    }

    private void runCustomerLookup() {
        String q = nameQueryField.getText().trim();
        if (q.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Enter a name fragment.",
                    "Missing input", JOptionPane.WARNING_MESSAGE);
            return;
        }
        var rows = AdminService.reservationsForCustomerName(q);
        renderLookup(customerModel, customerArea, customerCards, customerSummaryLabel, customerEmpty,
                rows,
                rows.size() + " reservation"
                        + (rows.size() == 1 ? "" : "s")
                        + " whose customer matches \"" + q + "\".",
                "No reservations match \"" + q + "\".");
    }

    private void renderLookup(DefaultTableModel model, JPanel area, CardLayout cards,
                              JLabel summary, JLabel empty,
                              java.util.List<ReservationLookupRow> rows,
                              String summaryText, String emptyText) {
        model.setRowCount(0);
        for (ReservationLookupRow r : rows) {
            model.addRow(new Object[]{
                    r.reservationID(),
                    r.customerUsername() + " (" + r.customerName() + ")",
                    r.bookedOn().format(DATETIME_FMT),
                    r.tripType() == null
                            ? "—"
                            : (r.tripType() == cs336.travel.model.TripType.ROUND_TRIP
                                    ? "Round-trip" : "One-way"),
                    r.status(),
                    MONEY.format(r.totalFare())
            });
        }
        summary.setText(summaryText);
        empty.setText(emptyText);
        cards.show(area, rows.isEmpty() ? CARD_EMPTY : CARD_TABLE);
    }

    private JPanel buildSalesTab() {
        // Years: -1 / current / +1 (small picker, not a free-text spinner).
        int currentYear = LocalDate.now().getYear();
        for (int y = currentYear - 1; y <= currentYear + 1; y++) yearCombo.addItem(y);
        yearCombo.setSelectedItem(currentYear);
        monthCombo.setSelectedItem(LocalDate.now().getMonth());
        monthCombo.setRenderer(new javax.swing.DefaultListCellRenderer() {
            @Override
            public java.awt.Component getListCellRendererComponent(
                    javax.swing.JList<?> list, Object value, int index,
                    boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof Month m) {
                    setText(m.getDisplayName(TextStyle.FULL, Locale.ENGLISH));
                }
                return this;
            }
        });

        JButton run = new JButton("Run report");
        run.addActionListener(e -> runReport());

        JPanel filterRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        filterRow.add(new JLabel("Month:"));
        filterRow.add(yearCombo);
        filterRow.add(monthCombo);
        filterRow.add(run);

        summaryLabel.setFont(summaryLabel.getFont().deriveFont(Font.BOLD, 13f));
        summaryLabel.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));

        salesTable.setAutoCreateRowSorter(false);
        salesArea.add(new JScrollPane(salesTable), CARD_TABLE);
        salesArea.add(emptyLabel,                  CARD_EMPTY);
        salesCards.show(salesArea, CARD_EMPTY);

        JPanel center = new JPanel(new BorderLayout());
        center.add(summaryLabel, BorderLayout.NORTH);
        center.add(salesArea,    BorderLayout.CENTER);

        JPanel tab = new JPanel(new BorderLayout(0, 4));
        tab.add(filterRow, BorderLayout.NORTH);
        tab.add(center,    BorderLayout.CENTER);
        return tab;
    }

    /** Called by {@link MainFrame} on each card swap — runs the default report. */
    public void refresh() {
        runReport();
    }

    private void runReport() {
        int year  = (int) yearCombo.getSelectedItem();
        Month m   = (Month) monthCombo.getSelectedItem();
        MonthlySales report = AdminService.salesForMonth(year, m.getValue());
        renderSales(report);
    }

    private void renderSales(MonthlySales s) {
        String monthName = Month.of(s.month()).getDisplayName(TextStyle.FULL, Locale.ENGLISH)
                + " " + s.year();

        salesModel.setRowCount(0);
        for (TicketSaleRow r : s.rows()) {
            salesModel.addRow(new Object[]{
                    r.ticketNumber(),
                    r.reservationID(),
                    r.customerUsername() + " (" + r.customerName() + ")",
                    r.tripType() == cs336.travel.model.TripType.ROUND_TRIP ? "Round-trip" : "One-way",
                    r.purchaseDateTime().format(DATETIME_FMT),
                    MONEY.format(r.bookingFee()),
                    MONEY.format(r.totalFare()),
                    MONEY.format(r.lineTotal())
            });
        }

        if (s.ticketCount() == 0) {
            summaryLabel.setText("No ticket sales for " + monthName + ".");
            emptyLabel.setText("No ticket sales for " + monthName + ".");
            salesCards.show(salesArea, CARD_EMPTY);
        } else {
            summaryLabel.setText(
                    "Sales for " + monthName + ": "
                            + s.ticketCount() + " ticket" + (s.ticketCount() == 1 ? "" : "s")
                            + " — total revenue " + MONEY.format(s.grandTotal())
                            + " (booking fees " + MONEY.format(s.bookingFeeTotal())
                            + ", fares " + MONEY.format(s.fareTotal()) + ")");
            salesCards.show(salesArea, CARD_TABLE);
        }
    }

    private static JLabel emptyLabel() {
        JLabel l = new JLabel(" ", SwingConstants.CENTER);
        l.setFont(l.getFont().deriveFont(Font.ITALIC, 14f));
        l.setForeground(Color.GRAY);
        return l;
    }
}
