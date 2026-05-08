package cs336.travel.ui;

import cs336.travel.Session;
import cs336.travel.dao.AirportDAO;
import cs336.travel.model.Airport;
import cs336.travel.model.BookingResult;
import cs336.travel.model.Customer;
import cs336.travel.model.FlightSearchResult;
import cs336.travel.model.Role;
import cs336.travel.model.RoundTripResult;
import cs336.travel.dao.AircraftDAO;
import cs336.travel.dao.TicketFlightDAO;
import cs336.travel.dao.WaitlistDAO;
import cs336.travel.model.SelectedSegment;
import cs336.travel.model.TravelClass;
import cs336.travel.model.TripType;
import cs336.travel.model.WaitlistResult;
import cs336.travel.service.BookingService;
import cs336.travel.service.FlightSearchService;
import cs336.travel.service.WaitlistService;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.RowFilter;
import javax.swing.RowSorter;
import javax.swing.SortOrder;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class CustomerSearchPanel extends JPanel {

    /**
     * Whose reservation the Book button creates. The same panel renders for
     * both flows so search/sort/filter/results logic isn't duplicated.
     */
    public enum BookingMode { CUSTOMER_SELF, CR_ON_BEHALF }

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private static final String CARD_INTRO  = "intro";
    private static final String CARD_EMPTY  = "empty";
    private static final String CARD_ONEWAY = "oneway";
    private static final String CARD_ROUND  = "roundtrip";

    private static final String LEG_TABLE = "table";
    private static final String LEG_EMPTY = "empty";

    private static final String[] COLUMNS = {
            "Date", "Airline", "Flight #", "From", "To",
            "Depart", "Arrive", "Duration", "Stops", "Class", "Price"
    };

    private static final NumberFormat MONEY = NumberFormat.getCurrencyInstance(Locale.US);

    // Column indices into COLUMNS — used to set comparators / sort keys / filter predicates.
    private static final int COL_AIRLINE  = 1;
    private static final int COL_DEPART   = 5;
    private static final int COL_ARRIVE   = 6;
    private static final int COL_DURATION = 7;
    private static final int COL_STOPS    = 8;
    private static final int COL_PRICE    = 10;

    private static final String ANY_AIRLINE = "Any";

    private static final SortPreset DEFAULT_PRESET = new SortPreset("Default", -1, null);
    private static final SortPreset[] SORT_PRESETS = {
            DEFAULT_PRESET,
            new SortPreset("Price ↑",    COL_PRICE,    SortOrder.ASCENDING),
            new SortPreset("Price ↓",    COL_PRICE,    SortOrder.DESCENDING),
            new SortPreset("Depart ↑",   COL_DEPART,   SortOrder.ASCENDING),
            new SortPreset("Depart ↓",   COL_DEPART,   SortOrder.DESCENDING),
            new SortPreset("Arrive ↑",   COL_ARRIVE,   SortOrder.ASCENDING),
            new SortPreset("Arrive ↓",   COL_ARRIVE,   SortOrder.DESCENDING),
            new SortPreset("Duration ↑", COL_DURATION, SortOrder.ASCENDING),
            new SortPreset("Duration ↓", COL_DURATION, SortOrder.DESCENDING),
    };

    private final MainFrame frame;
    private final BookingMode mode;
    private Customer onBehalfCustomer;

    private final JRadioButton oneWayRadio    = new JRadioButton("One-way", true);
    private final JRadioButton roundTripRadio = new JRadioButton("Round-trip");

    private final JRadioButton exactDatesRadio = new JRadioButton("Exact dates", true);
    private final JRadioButton flexDatesRadio  = new JRadioButton("Flexible (±3 days)");

    private final JRadioButton economyRadio  = new JRadioButton("Economy", true);
    private final JRadioButton businessRadio = new JRadioButton("Business");
    private final JRadioButton firstRadio    = new JRadioButton("First");

    private final JComboBox<Airport> fromCombo = new JComboBox<>();
    private final JComboBox<Airport> toCombo   = new JComboBox<>();

    private final JLabel returnDateLabel = new JLabel("Return date (yyyy-MM-dd)");
    private final JFormattedTextField outboundDateField = new JFormattedTextField();
    private final JFormattedTextField returnDateField   = new JFormattedTextField();

    private final DefaultTableModel oneWayModel  = newModel();
    private final DefaultTableModel outboundModel = newModel();
    private final DefaultTableModel returnModel   = newModel();

    private final TableRowSorter<DefaultTableModel> oneWaySorter   = buildSorter(oneWayModel);
    private final TableRowSorter<DefaultTableModel> outboundSorter = buildSorter(outboundModel);
    private final TableRowSorter<DefaultTableModel> returnSorter   = buildSorter(returnModel);

    private final JComboBox<SortPreset> oneWaySortCombo   = newSortCombo(oneWaySorter);
    private final JComboBox<SortPreset> outboundSortCombo = newSortCombo(outboundSorter);
    private final JComboBox<SortPreset> returnSortCombo   = newSortCombo(returnSorter);

    // Filter controls — applied across all three sorters at once.
    private final JTextField priceMinField   = new JTextField(5);
    private final JTextField priceMaxField   = new JTextField(5);
    private final JRadioButton stopsAnyRadio   = new JRadioButton("Any", true);
    private final JRadioButton stops0Radio     = new JRadioButton("0");
    private final JRadioButton stops1Radio     = new JRadioButton("1");
    private final JRadioButton stops2PlusRadio = new JRadioButton("2+");
    private final JComboBox<String> airlineCombo = new JComboBox<>(new String[]{ANY_AIRLINE});
    private final JTextField takeoffFromField = new JTextField(5);
    private final JTextField takeoffToField   = new JTextField(5);
    private final JTextField landingFromField = new JTextField(5);
    private final JTextField landingToField   = new JTextField(5);

    private final CardLayout resultsCards = new CardLayout();
    private final JPanel resultsArea = new JPanel(resultsCards);

    private final CardLayout outboundCards = new CardLayout();
    private final JPanel outboundArea = new JPanel(outboundCards);
    private final JLabel outboundHeading = new JLabel();

    private final CardLayout returnCards = new CardLayout();
    private final JPanel returnArea = new JPanel(returnCards);
    private final JLabel returnHeading = new JLabel();

    // Parallel source lists: model row index → original FlightSearchResult.
    // Needed because the table cells are pre-formatted strings and the sorter
    // reorders the view, so we cannot re-derive airlineID / departureDateTime
    // from the visible cell text.
    private final List<FlightSearchResult> oneWaySource   = new ArrayList<>();
    private final List<FlightSearchResult> outboundSource = new ArrayList<>();
    private final List<FlightSearchResult> returnSource   = new ArrayList<>();

    private final JTable oneWayTable   = buildTable(oneWayModel,   oneWaySorter);
    private final JTable outboundTable = buildTable(outboundModel, outboundSorter);
    private final JTable returnTable   = buildTable(returnModel,   returnSorter);

    private final JButton bookButton = new JButton("Book Selected Flight(s)");

    public CustomerSearchPanel(MainFrame frame) {
        this(frame, BookingMode.CUSTOMER_SELF);
    }

    public CustomerSearchPanel(MainFrame frame, BookingMode mode) {
        this.frame = frame;
        this.mode  = mode;
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        configureDateField(outboundDateField);
        configureDateField(returnDateField);
        returnDateField.setEnabled(false);
        returnDateLabel.setEnabled(false);

        ButtonGroup tripGroup = new ButtonGroup();
        tripGroup.add(oneWayRadio);
        tripGroup.add(roundTripRadio);
        oneWayRadio.addActionListener(e -> onTripTypeChanged());
        roundTripRadio.addActionListener(e -> onTripTypeChanged());

        ButtonGroup dateModeGroup = new ButtonGroup();
        dateModeGroup.add(exactDatesRadio);
        dateModeGroup.add(flexDatesRadio);

        ButtonGroup classGroup = new ButtonGroup();
        classGroup.add(economyRadio);
        classGroup.add(businessRadio);
        classGroup.add(firstRadio);

        ButtonGroup stopsGroup = new ButtonGroup();
        stopsGroup.add(stopsAnyRadio);
        stopsGroup.add(stops0Radio);
        stopsGroup.add(stops1Radio);
        stopsGroup.add(stops2PlusRadio);

        fromCombo.setRenderer(new AirportRenderer());
        toCombo.setRenderer(new AirportRenderer());

        JButton searchButton = new JButton("Search");
        searchButton.addActionListener(e -> onSearchClicked());
        JButton backButton = new JButton("Back");
        // Back goes to the home screen for whoever's signed in — works for
        // both the customer-self panel (CUSTOMER role) and the CR on-behalf
        // wrapper (CUSTOMER_REP role lands on CR home).
        backButton.addActionListener(e -> frame.showHomeFor(Session.role()));

        JPanel tripRow = flowRow();
        tripRow.add(oneWayRadio);
        tripRow.add(roundTripRadio);
        tripRow.add(new JLabel("From"));
        tripRow.add(fromCombo);
        tripRow.add(new JLabel("To"));
        tripRow.add(toCombo);

        JPanel modeRow = flowRow();
        modeRow.add(exactDatesRadio);
        modeRow.add(flexDatesRadio);

        JPanel classRow = flowRow();
        classRow.add(economyRadio);
        classRow.add(businessRadio);
        classRow.add(firstRadio);

        JPanel dateRow = flowRow();
        dateRow.add(new JLabel("Depart date (yyyy-MM-dd)"));
        dateRow.add(outboundDateField);
        dateRow.add(returnDateLabel);
        dateRow.add(returnDateField);

        JPanel buttonRow = flowRow();
        buttonRow.add(searchButton);
        buttonRow.add(backButton);

        JPanel form = new JPanel();
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));
        form.add(tripRow);
        form.add(modeRow);
        form.add(classRow);
        form.add(dateRow);
        form.add(buttonRow);

        JLabel title = new JLabel("Search Flights");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 18f));

        JPanel header = new JPanel(new BorderLayout());
        header.add(title,             BorderLayout.NORTH);
        header.add(form,              BorderLayout.CENTER);
        header.add(buildFilterPane(), BorderLayout.SOUTH);

        resultsArea.add(introCard(),          CARD_INTRO);
        resultsArea.add(emptyCard(),          CARD_EMPTY);
        resultsArea.add(buildOneWayCard(),    CARD_ONEWAY);
        resultsArea.add(buildRoundTripCard(), CARD_ROUND);
        resultsCards.show(resultsArea, CARD_INTRO);

        bookButton.addActionListener(e -> onBookClicked());
        bookButton.setEnabled(false);
        oneWayTable.getSelectionModel().addListSelectionListener(e -> updateBookEnabled());
        outboundTable.getSelectionModel().addListSelectionListener(e -> updateBookEnabled());
        returnTable.getSelectionModel().addListSelectionListener(e -> updateBookEnabled());

        JPanel south = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 4));
        south.add(bookButton);

        add(header,      BorderLayout.NORTH);
        add(resultsArea, BorderLayout.CENTER);
        add(south,       BorderLayout.SOUTH);
    }

    private void configureDateField(JFormattedTextField f) {
        // No Format on the field: DateTimeFormatter.toFormat() blows up
        // inside InternationalFormatter.install() on JDK 21 even when the
        // value is a LocalDate (TemporalAccessor). Parse text manually.
        f.setColumns(10);
        f.setText(LocalDate.now().format(DATE_FMT));
        f.addActionListener(e -> onSearchClicked());
    }

    private static DefaultTableModel newModel() {
        return new DefaultTableModel(COLUMNS, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
    }

    private static JPanel flowRow() {
        return new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
    }

    private static JComponent introCard() {
        JLabel l = new JLabel("Pick a route and date, then click Search.", SwingConstants.CENTER);
        l.setFont(l.getFont().deriveFont(Font.ITALIC, 14f));
        return l;
    }

    private static JComponent emptyCard() {
        JLabel l = new JLabel("No flights match your search.", SwingConstants.CENTER);
        l.setFont(l.getFont().deriveFont(Font.ITALIC, 14f));
        return l;
    }

    private JPanel buildOneWayCard() {
        JPanel card = new JPanel(new BorderLayout(0, 4));
        card.add(sortRow(oneWaySortCombo),    BorderLayout.NORTH);
        card.add(new JScrollPane(oneWayTable), BorderLayout.CENTER);
        return card;
    }

    private JPanel buildRoundTripCard() {
        // Vertical Box (not GridLayout 2x1) so each leg keeps its preferred
        // height. Wrapped in a JScrollPane so when the embedding container is
        // short — e.g. CRBookingPanel reserves NORTH for title + customer
        // picker — the user can scroll the legs instead of seeing them
        // squeezed to <100px each.
        JPanel root = new JPanel();
        root.setLayout(new BoxLayout(root, BoxLayout.Y_AXIS));
        root.add(buildLegSection(outboundHeading, outboundArea, outboundTable, outboundSortCombo));
        root.add(javax.swing.Box.createVerticalStrut(12));
        root.add(buildLegSection(returnHeading,   returnArea,   returnTable,   returnSortCombo));

        JScrollPane scroll = new JScrollPane(root);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getVerticalScrollBar().setUnitIncrement(16);

        JPanel card = new JPanel(new BorderLayout());
        card.add(scroll, BorderLayout.CENTER);
        return card;
    }

    private JPanel buildLegSection(JLabel heading,
                                   JPanel area,
                                   JTable table,
                                   JComboBox<SortPreset> combo) {
        heading.setFont(heading.getFont().deriveFont(Font.BOLD, 13f));

        area.add(new JScrollPane(table), LEG_TABLE);
        area.add(legEmptyLabel(),        LEG_EMPTY);
        // Anchor each leg to a sensible minimum so the outer scroll pane,
        // not the legs themselves, absorbs the squeeze when vertical space
        // is tight (CR booking flow). 220px = heading+sortRow ~36 +
        // ~5 visible table rows.
        java.awt.Dimension legSize = new java.awt.Dimension(800, 220);
        area.setPreferredSize(legSize);
        area.setMinimumSize(legSize);

        JPanel north = new JPanel();
        north.setLayout(new BoxLayout(north, BoxLayout.Y_AXIS));
        heading.setAlignmentX(LEFT_ALIGNMENT);
        JPanel sortRow = sortRow(combo);
        sortRow.setAlignmentX(LEFT_ALIGNMENT);
        north.add(heading);
        north.add(sortRow);

        JPanel section = new JPanel(new BorderLayout(0, 4));
        section.add(north, BorderLayout.NORTH);
        section.add(area,  BorderLayout.CENTER);
        section.setAlignmentX(LEFT_ALIGNMENT);
        return section;
    }

    private JPanel buildFilterPane() {
        JButton applyBtn = new JButton("Apply Filters");
        applyBtn.addActionListener(e -> onApplyFilters());
        JButton clearBtn = new JButton("Clear");
        clearBtn.addActionListener(e -> onClearFilters());

        JPanel rowA = flowRow();
        rowA.add(new JLabel("Filters — Price min"));
        rowA.add(priceMinField);
        rowA.add(new JLabel("max"));
        rowA.add(priceMaxField);
        rowA.add(new JLabel("    Stops:"));
        rowA.add(stopsAnyRadio);
        rowA.add(stops0Radio);
        rowA.add(stops1Radio);
        rowA.add(stops2PlusRadio);
        rowA.add(new JLabel("    Airline:"));
        rowA.add(airlineCombo);

        JPanel rowB = flowRow();
        rowB.add(new JLabel("Take-off (HH:mm)"));
        rowB.add(takeoffFromField);
        rowB.add(new JLabel("to"));
        rowB.add(takeoffToField);
        rowB.add(new JLabel("    Landing (HH:mm)"));
        rowB.add(landingFromField);
        rowB.add(new JLabel("to"));
        rowB.add(landingToField);
        rowB.add(applyBtn);
        rowB.add(clearBtn);

        JPanel pane = new JPanel();
        pane.setLayout(new BoxLayout(pane, BoxLayout.Y_AXIS));
        pane.add(rowA);
        pane.add(rowB);
        return pane;
    }

    private void onApplyFilters() {
        BigDecimal min, max;
        try {
            min = parseOptionalPrice(priceMinField.getText());
            max = parseOptionalPrice(priceMaxField.getText());
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Price must be numeric.",
                    "Invalid filter", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (min != null && max != null && min.compareTo(max) > 0) {
            JOptionPane.showMessageDialog(this, "Min price cannot exceed Max price.",
                    "Invalid filter", JOptionPane.WARNING_MESSAGE);
            return;
        }

        LocalTime depFrom, depTo, arrFrom, arrTo;
        try {
            depFrom = parseOptionalTime(takeoffFromField.getText());
            depTo   = parseOptionalTime(takeoffToField.getText());
            arrFrom = parseOptionalTime(landingFromField.getText());
            arrTo   = parseOptionalTime(landingToField.getText());
        } catch (DateTimeParseException ex) {
            JOptionPane.showMessageDialog(this, "Time must be HH:mm.",
                    "Invalid filter", JOptionPane.WARNING_MESSAGE);
            return;
        }

        Integer stopsExact = null;
        boolean stopsTwoPlus = false;
        if (stops0Radio.isSelected())          stopsExact   = 0;
        else if (stops1Radio.isSelected())     stopsExact   = 1;
        else if (stops2PlusRadio.isSelected()) stopsTwoPlus = true;

        Object airlineSel = airlineCombo.getSelectedItem();
        String airline = (airlineSel == null || ANY_AIRLINE.equals(airlineSel))
                ? null : airlineSel.toString();

        boolean anyActive = min != null || max != null
                || depFrom != null || depTo != null
                || arrFrom != null || arrTo != null
                || stopsExact != null || stopsTwoPlus
                || airline != null;

        if (!anyActive) {
            applyFilterToAllSorters(null);
            return;
        }

        final BigDecimal fMin = min, fMax = max;
        final LocalTime fDepFrom = depFrom, fDepTo = depTo;
        final LocalTime fArrFrom = arrFrom, fArrTo = arrTo;
        final Integer fStopsExact = stopsExact;
        final boolean fStopsTwoPlus = stopsTwoPlus;
        final String fAirline = airline;

        RowFilter<DefaultTableModel, Integer> filter = new RowFilter<>() {
            @Override
            public boolean include(Entry<? extends DefaultTableModel, ? extends Integer> e) {
                BigDecimal price = parsePrice(String.valueOf(e.getValue(COL_PRICE)));
                if (fMin != null && price.compareTo(fMin) < 0) return false;
                if (fMax != null && price.compareTo(fMax) > 0) return false;

                int stops = ((Number) e.getValue(COL_STOPS)).intValue();
                if (fStopsExact != null && stops != fStopsExact) return false;
                if (fStopsTwoPlus && stops < 2)                  return false;

                if (fAirline != null) {
                    String a = String.valueOf(e.getValue(COL_AIRLINE));
                    if (!a.equals(fAirline)) return false;
                }

                LocalTime dep = LocalTime.parse(String.valueOf(e.getValue(COL_DEPART)));
                if (fDepFrom != null && dep.isBefore(fDepFrom)) return false;
                if (fDepTo   != null && dep.isAfter(fDepTo))   return false;

                LocalTime arr = LocalTime.parse(String.valueOf(e.getValue(COL_ARRIVE)));
                if (fArrFrom != null && arr.isBefore(fArrFrom)) return false;
                if (fArrTo   != null && arr.isAfter(fArrTo))   return false;

                return true;
            }
        };
        applyFilterToAllSorters(filter);
    }

    private void onClearFilters() {
        priceMinField.setText("");
        priceMaxField.setText("");
        stopsAnyRadio.setSelected(true);
        if (airlineCombo.getItemCount() > 0) airlineCombo.setSelectedIndex(0);
        takeoffFromField.setText("");
        takeoffToField.setText("");
        landingFromField.setText("");
        landingToField.setText("");
        applyFilterToAllSorters(null);
    }

    private void applyFilterToAllSorters(RowFilter<DefaultTableModel, Integer> f) {
        oneWaySorter.setRowFilter(f);
        outboundSorter.setRowFilter(f);
        returnSorter.setRowFilter(f);
    }

    private void refreshAirlineCombo(DefaultTableModel... models) {
        Set<String> airlines = new LinkedHashSet<>();
        airlines.add(ANY_AIRLINE);
        for (DefaultTableModel m : models) {
            for (int r = 0; r < m.getRowCount(); r++) {
                Object v = m.getValueAt(r, COL_AIRLINE);
                if (v != null) airlines.add(v.toString());
            }
        }
        airlineCombo.setModel(new DefaultComboBoxModel<>(airlines.toArray(new String[0])));
    }

    private static BigDecimal parseOptionalPrice(String s) {
        if (s == null || s.trim().isEmpty()) return null;
        return new BigDecimal(s.trim().replaceAll("[^0-9.]", ""));
    }

    private static LocalTime parseOptionalTime(String s) {
        if (s == null || s.trim().isEmpty()) return null;
        return LocalTime.parse(s.trim());
    }

    private static JTable buildTable(DefaultTableModel model, TableRowSorter<DefaultTableModel> sorter) {
        JTable t = new JTable(model);
        t.setRowSorter(sorter);
        t.setAutoCreateRowSorter(false);
        return t;
    }

    private static JPanel sortRow(JComboBox<SortPreset> combo) {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 2));
        row.add(new JLabel("Sort by:"));
        row.add(combo);
        return row;
    }

    private static TableRowSorter<DefaultTableModel> buildSorter(DefaultTableModel model) {
        TableRowSorter<DefaultTableModel> s = new TableRowSorter<>(model);
        Comparator<Object> byTime     = Comparator.comparing(o -> LocalTime.parse(String.valueOf(o)));
        Comparator<Object> byDuration = Comparator.comparingInt(o -> parseDurationMinutes(String.valueOf(o)));
        Comparator<Object> byPrice    = Comparator.comparing(o -> parsePrice(String.valueOf(o)));
        s.setComparator(COL_DEPART,   byTime);
        s.setComparator(COL_ARRIVE,   byTime);
        s.setComparator(COL_DURATION, byDuration);
        s.setComparator(COL_PRICE,    byPrice);
        return s;
    }

    private static JComboBox<SortPreset> newSortCombo(TableRowSorter<DefaultTableModel> sorter) {
        JComboBox<SortPreset> combo = new JComboBox<>(SORT_PRESETS);
        combo.addActionListener(e -> {
            SortPreset p = (SortPreset) combo.getSelectedItem();
            if (p == null || p.order() == null) {
                sorter.setSortKeys(null);
            } else {
                sorter.setSortKeys(List.of(new RowSorter.SortKey(p.column(), p.order())));
            }
        });
        return combo;
    }

    private static int parseDurationMinutes(String cell) {
        int hIdx = cell.indexOf('h');
        int mIdx = cell.indexOf('m');
        int h = Integer.parseInt(cell.substring(0, hIdx));
        int m = Integer.parseInt(cell.substring(hIdx + 1, mIdx));
        return h * 60 + m;
    }

    private static BigDecimal parsePrice(String cell) {
        return new BigDecimal(cell.replaceAll("[^0-9.]", ""));
    }

    private record SortPreset(String label, int column, SortOrder order) {
        @Override public String toString() { return label; }
    }

    private static JLabel legEmptyLabel() {
        JLabel l = new JLabel("No flights match for this leg.", SwingConstants.CENTER);
        l.setFont(l.getFont().deriveFont(Font.ITALIC, 13f));
        return l;
    }

    /** Called by {@link MainFrame} on each card swap. */
    public void refresh() {
        loadAirports();
    }

    /** CR mode only: which customer the next booking is on behalf of. */
    public void setOnBehalfCustomer(Customer c) {
        if (mode != BookingMode.CR_ON_BEHALF) {
            throw new IllegalStateException(
                    "setOnBehalfCustomer only valid in CR_ON_BEHALF mode");
        }
        this.onBehalfCustomer = c;
        updateBookEnabled();
    }

    private void loadAirports() {
        List<Airport> airports = AirportDAO.listAll();
        Airport prevFrom = (Airport) fromCombo.getSelectedItem();
        Airport prevTo   = (Airport) toCombo.getSelectedItem();

        fromCombo.setModel(new DefaultComboBoxModel<>(airports.toArray(new Airport[0])));
        toCombo.setModel(new DefaultComboBoxModel<>(airports.toArray(new Airport[0])));

        if (prevFrom != null) fromCombo.setSelectedItem(prevFrom);
        if (prevTo   != null) toCombo.setSelectedItem(prevTo);
        if (toCombo.getSelectedIndex() == fromCombo.getSelectedIndex() && airports.size() > 1) {
            toCombo.setSelectedIndex((fromCombo.getSelectedIndex() + 1) % airports.size());
        }
    }

    private void onTripTypeChanged() {
        boolean roundTrip = roundTripRadio.isSelected();
        returnDateField.setEnabled(roundTrip);
        returnDateLabel.setEnabled(roundTrip);
        updateBookEnabled();
    }

    private void updateBookEnabled() {
        bookButton.setEnabled(canBookNow());
    }

    private boolean canBookNow() {
        if (mode == BookingMode.CR_ON_BEHALF && onBehalfCustomer == null) return false;
        if (roundTripRadio.isSelected()) {
            return outboundTable.getSelectedRowCount() == 1
                    && returnTable.getSelectedRowCount() == 1;
        }
        return oneWayTable.getSelectedRowCount() == 1;
    }

    private FlightSearchResult selectedFrom(JTable table,
                                            TableRowSorter<DefaultTableModel> sorter,
                                            List<FlightSearchResult> source) {
        int viewRow = table.getSelectedRow();
        if (viewRow < 0) return null;
        int modelRow = sorter.convertRowIndexToModel(viewRow);
        if (modelRow < 0 || modelRow >= source.size()) return null;
        return source.get(modelRow);
    }

    private void onBookClicked() {
        // Mode-aware role gate: customer self-book OR CR booking on behalf.
        if (mode == BookingMode.CUSTOMER_SELF) {
            if (Session.role() != Role.CUSTOMER) {
                JOptionPane.showMessageDialog(this,
                        "Only signed-in customers can book from this screen.",
                        "Booking", JOptionPane.WARNING_MESSAGE);
                return;
            }
        } else { // CR_ON_BEHALF
            if (Session.role() != Role.CUSTOMER_REP) {
                JOptionPane.showMessageDialog(this,
                        "Only signed-in customer reps can book on behalf.",
                        "Booking", JOptionPane.WARNING_MESSAGE);
                return;
            }
            if (onBehalfCustomer == null) {
                JOptionPane.showMessageDialog(this,
                        "Pick a customer first.",
                        "Missing customer", JOptionPane.WARNING_MESSAGE);
                return;
            }
        }
        TravelClass cls = selectedClass();
        TripType tripType;
        List<FlightSearchResult> picks = new ArrayList<>();

        if (roundTripRadio.isSelected()) {
            FlightSearchResult out = selectedFrom(outboundTable, outboundSorter, outboundSource);
            FlightSearchResult ret = selectedFrom(returnTable,   returnSorter,   returnSource);
            if (out == null || ret == null) return;
            tripType = TripType.ROUND_TRIP;
            picks.add(out);
            picks.add(ret);
        } else {
            FlightSearchResult one = selectedFrom(oneWayTable, oneWaySorter, oneWaySource);
            if (one == null) return;
            tripType = TripType.ONE_WAY;
            picks.add(one);
        }

        if (!confirmBooking(picks)) return;

        List<SelectedSegment> segments = new ArrayList<>();
        for (FlightSearchResult r : picks) {
            segments.add(new SelectedSegment(
                    r.airlineID(), r.flightNumber(),
                    LocalDateTime.of(r.flightDate(), r.departureTime()),
                    r.basePrice()));
        }

        int    bookingCustomerID;
        Integer createdByEmployeeID;
        if (mode == BookingMode.CR_ON_BEHALF) {
            bookingCustomerID   = onBehalfCustomer.customerID();
            createdByEmployeeID = Session.employee().employeeID();
        } else {
            bookingCustomerID   = Session.customer().customerID();
            createdByEmployeeID = null;
        }

        BookingResult result = BookingService.book(
                bookingCustomerID, createdByEmployeeID, tripType, cls, segments);

        switch (result) {
            case BookingResult.Success s -> JOptionPane.showMessageDialog(this,
                    successMessage(s),
                    "Booking confirmed", JOptionPane.INFORMATION_MESSAGE);
            case BookingResult.Full   f -> showFullDialog(f, segments, cls);
            case BookingResult.Error  e -> JOptionPane.showMessageDialog(this,
                    e.message(), "Booking failed", JOptionPane.ERROR_MESSAGE);
        }
    }

    private String successMessage(BookingResult.Success s) {
        if (mode == BookingMode.CR_ON_BEHALF) {
            return "Booked on behalf — Reservation #" + s.reservationID()
                    + " for " + onBehalfCustomer.username()
                    + ".  Ticket #" + s.ticketNumber() + ".";
        }
        return "Reservation #" + s.reservationID()
                + " confirmed.  Ticket #" + s.ticketNumber() + ".";
    }

    private boolean confirmBooking(List<FlightSearchResult> picks) {
        TravelClass cls = selectedClass();
        BigDecimal total = BigDecimal.ZERO;
        StringBuilder sb = new StringBuilder();
        String title;
        if (mode == BookingMode.CR_ON_BEHALF && onBehalfCustomer != null) {
            sb.append("Book on behalf of ").append(onBehalfCustomer.username())
              .append(" (").append(onBehalfCustomer.name()).append("):\n");
            title = "Book on behalf of " + onBehalfCustomer.username();
        } else {
            sb.append("Book Reservation:\n");
            title = "Confirm booking";
        }
        String[] labels = {"Outbound", "Return"};
        for (int i = 0; i < picks.size(); i++) {
            FlightSearchResult r = picks.get(i);
            BigDecimal segPrice = r.displayedPrice();
            total = total.add(segPrice);
            sb.append("  ").append(labels[i]).append(": ")
              .append(r.airlineID()).append(' ').append(r.flightNumber()).append("  ")
              .append(r.departureAirport()).append("→").append(r.arrivalAirport()).append("  ")
              .append(r.flightDate()).append(' ').append(r.departureTime()).append("  ")
              .append(cls.label()).append("  ")
              .append(MONEY.format(segPrice)).append('\n');
        }
        BigDecimal grandTotal = total.add(BookingService.BOOKING_FEE);
        sb.append("  Booking fee: ").append(MONEY.format(BookingService.BOOKING_FEE)).append('\n');
        sb.append("  ──────────────────────────\n");
        sb.append("  Total:       ").append(MONEY.format(grandTotal));

        int choice = JOptionPane.showConfirmDialog(this, sb.toString(),
                title, JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        return choice == JOptionPane.OK_OPTION;
    }

    private void showFullDialog(BookingResult.Full f,
                                List<SelectedSegment> segments,
                                TravelClass cls) {
        // CR mode: WaitlistService rejects non-customer callers, so don't
        // even offer the waitlist option. Inform the rep and let them try
        // a different flight or date.
        if (mode == BookingMode.CR_ON_BEHALF) {
            JOptionPane.showMessageDialog(this,
                    f.airlineID() + " " + f.flightNumber() + " on " + f.flightDate()
                            + " is full. Try different dates or another flight.",
                    "Flight is full", JOptionPane.WARNING_MESSAGE);
            return;
        }
        // BookingService.book short-circuits on the first failing segment, so
        // re-probe each leg here to give an honest message in the multi-segment
        // case (one leg full vs both legs full).
        List<SelectedSegment> fullSegs = new ArrayList<>();
        for (SelectedSegment s : segments) {
            int booked   = TicketFlightDAO.countBookedSeats(
                    s.airlineID(), s.flightNumber(), s.departureDateTime());
            int capacity = AircraftDAO.getCapacityForFlight(
                    s.airlineID(), s.flightNumber());
            if (booked >= capacity) fullSegs.add(s);
        }
        if (fullSegs.isEmpty()) fullSegs.add(segmentMatching(segments, f)); // belt-and-braces

        boolean partial = fullSegs.size() < segments.size();
        if (partial) {
            // Per spec pragma: do not proceed in the partial case. User has
            // not been charged — they can retry with different dates.
            SelectedSegment full = fullSegs.get(0);
            JOptionPane.showMessageDialog(this,
                    Format.flightLabel(full.airlineID(), full.flightNumber())
                            + " on " + full.departureDateTime().toLocalDate()
                            + " is full. Cancel and try again with different dates?",
                    "Flight is full", JOptionPane.WARNING_MESSAGE);
            return;
        }

        StringBuilder body = new StringBuilder();
        if (fullSegs.size() == 1) {
            SelectedSegment one = fullSegs.get(0);
            int pos = WaitlistDAO.countWaiting(
                    one.airlineID(), one.flightNumber(),
                    one.departureDateTime().toLocalDate()) + 1;
            body.append(Format.flightLabel(one.airlineID(), one.flightNumber()))
                .append(" on ").append(one.departureDateTime().toLocalDate())
                .append(" is full. Position ").append(pos).append(" if you join.");
        } else {
            body.append("Both legs of your round-trip are full:\n");
            for (SelectedSegment s : fullSegs) {
                int pos = WaitlistDAO.countWaiting(
                        s.airlineID(), s.flightNumber(),
                        s.departureDateTime().toLocalDate()) + 1;
                body.append("  ")
                    .append(Format.flightLabel(s.airlineID(), s.flightNumber()))
                    .append(" on ").append(s.departureDateTime().toLocalDate())
                    .append(" — position ").append(pos).append(" if you join\n");
            }
        }

        String addLabel = fullSegs.size() == 1
                ? "Add to waitlist"
                : "Add to waitlist for both legs";
        Object[] options = {addLabel, "Cancel"};
        int choice = JOptionPane.showOptionDialog(this, body.toString(),
                "Flight is full", JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE, null, options, options[1]);
        if (choice != 0) return;

        WaitlistResult result = WaitlistService.addToWaitlist(
                Session.customer().customerID(), segments, cls);
        switch (result) {
            case WaitlistResult.Success s -> JOptionPane.showMessageDialog(this,
                    formatWaitlistSuccess(segments, s),
                    "Added to waitlist", JOptionPane.INFORMATION_MESSAGE);
            case WaitlistResult.Error e -> JOptionPane.showMessageDialog(this,
                    e.message(), "Waitlist failed", JOptionPane.ERROR_MESSAGE);
        }
    }

    private static SelectedSegment segmentMatching(List<SelectedSegment> segments,
                                                   BookingResult.Full f) {
        for (SelectedSegment s : segments) {
            if (s.airlineID().equals(f.airlineID())
                    && s.flightNumber().equals(f.flightNumber())
                    && s.departureDateTime().toLocalDate().equals(f.flightDate())) {
                return s;
            }
        }
        return segments.get(0);
    }

    private static String formatWaitlistSuccess(List<SelectedSegment> segments,
                                                WaitlistResult.Success s) {
        StringBuilder sb = new StringBuilder("Added to waitlist:\n");
        for (int i = 0; i < segments.size(); i++) {
            SelectedSegment seg = segments.get(i);
            sb.append("  ")
              .append(Format.flightLabel(seg.airlineID(), seg.flightNumber()))
              .append(" on ").append(seg.departureDateTime().toLocalDate())
              .append(" (position ").append(s.positions().get(i)).append(")\n");
        }
        sb.append("We'll notify you if a seat opens.");
        return sb.toString();
    }

    private void onSearchClicked() {
        Airport from = (Airport) fromCombo.getSelectedItem();
        Airport to   = (Airport) toCombo.getSelectedItem();
        if (from == null || to == null) {
            JOptionPane.showMessageDialog(this, "Pick both a from and a to airport.",
                    "Missing input", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (from.airportID().equals(to.airportID())) {
            JOptionPane.showMessageDialog(this, "From and to must differ.",
                    "Invalid input", JOptionPane.WARNING_MESSAGE);
            return;
        }

        LocalDate outDate = parseDate(outboundDateField.getText());
        if (outDate == null) {
            invalidDate("Depart date must be yyyy-MM-dd.");
            return;
        }

        boolean flexible = flexDatesRadio.isSelected();
        TravelClass cls = selectedClass();

        if (roundTripRadio.isSelected()) {
            LocalDate retDate = parseDate(returnDateField.getText());
            if (retDate == null) {
                invalidDate("Return date must be yyyy-MM-dd.");
                return;
            }
            if (retDate.isBefore(outDate)) {
                JOptionPane.showMessageDialog(this,
                        "Return date must be on or after the depart date.",
                        "Invalid dates", JOptionPane.WARNING_MESSAGE);
                return;
            }
            RoundTripResult rt = FlightSearchService.searchRoundTrip(
                    from.airportID(), to.airportID(), outDate, retDate, flexible, cls);
            renderRoundTrip(from.airportID(), to.airportID(), outDate, retDate, rt);
        } else {
            List<FlightSearchResult> results = FlightSearchService.searchOneWay(
                    from.airportID(), to.airportID(), outDate, flexible, cls);
            renderOneWay(results);
        }
    }

    private TravelClass selectedClass() {
        if (businessRadio.isSelected()) return TravelClass.BUSINESS;
        if (firstRadio.isSelected())    return TravelClass.FIRST;
        return TravelClass.ECONOMY;
    }

    private static LocalDate parseDate(String text) {
        try {
            return LocalDate.parse(text.trim(), DATE_FMT);
        } catch (DateTimeParseException ex) {
            return null;
        }
    }

    private void invalidDate(String message) {
        JOptionPane.showMessageDialog(this, message, "Invalid date", JOptionPane.WARNING_MESSAGE);
    }

    private void renderOneWay(List<FlightSearchResult> results) {
        fillModel(oneWayModel, oneWaySource, results);
        oneWayTable.clearSelection();
        resetSort(oneWaySortCombo, oneWaySorter);
        onClearFilters();
        refreshAirlineCombo(oneWayModel);
        updateBookEnabled();
        if (results.isEmpty()) {
            resultsCards.show(resultsArea, CARD_EMPTY);
        } else {
            resultsCards.show(resultsArea, CARD_ONEWAY);
        }
    }

    private void renderRoundTrip(String from, String to,
                                 LocalDate outDate, LocalDate retDate,
                                 RoundTripResult rt) {
        outboundHeading.setText("Outbound: " + from + " → " + to + " on " + outDate);
        returnHeading.setText  ("Return: "   + to + " → " + from + " on " + retDate);

        fillModel(outboundModel, outboundSource, rt.outbound());
        outboundTable.clearSelection();
        resetSort(outboundSortCombo, outboundSorter);
        outboundCards.show(outboundArea, rt.outbound().isEmpty() ? LEG_EMPTY : LEG_TABLE);

        fillModel(returnModel, returnSource, rt.ret());
        returnTable.clearSelection();
        resetSort(returnSortCombo, returnSorter);
        returnCards.show(returnArea, rt.ret().isEmpty() ? LEG_EMPTY : LEG_TABLE);

        onClearFilters();
        refreshAirlineCombo(outboundModel, returnModel);
        updateBookEnabled();

        resultsCards.show(resultsArea, CARD_ROUND);
    }

    private static void resetSort(JComboBox<SortPreset> combo, TableRowSorter<?> sorter) {
        combo.setSelectedIndex(0);
        sorter.setSortKeys(null);
    }

    private static void fillModel(DefaultTableModel model,
                                  List<FlightSearchResult> source,
                                  List<FlightSearchResult> rows) {
        model.setRowCount(0);
        source.clear();
        source.addAll(rows);
        for (FlightSearchResult r : rows) {
            model.addRow(new Object[]{
                    r.flightDate().toString(),
                    r.airlineName() + " (" + r.airlineID() + ")",
                    r.flightNumber(),
                    r.departureAirport(),
                    r.arrivalAirport(),
                    r.departureTime().toString(),
                    r.arrivalTime().toString(),
                    formatDuration(r.duration()),
                    r.stops(),
                    r.travelClass().label(),
                    MONEY.format(r.displayedPrice())
            });
        }
    }

    private static String formatDuration(Duration d) {
        long h = d.toHours();
        long m = d.toMinutesPart();
        return h + "h" + String.format("%02dm", m);
    }

    private static final class AirportRenderer extends javax.swing.DefaultListCellRenderer {
        @Override
        public java.awt.Component getListCellRendererComponent(
                javax.swing.JList<?> list, Object value, int index,
                boolean isSelected, boolean cellHasFocus) {
            super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            if (value instanceof Airport a) {
                setText(a.airportID() + " — " + a.name() + " (" + a.city() + ")");
            }
            return this;
        }
    }
}
