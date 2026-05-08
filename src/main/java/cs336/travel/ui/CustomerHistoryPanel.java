package cs336.travel.ui;

import cs336.travel.model.CancelResult;
import cs336.travel.model.ReservationSummary;
import cs336.travel.model.Role;
import cs336.travel.model.SegmentDetail;
import cs336.travel.model.TravelClass;
import cs336.travel.service.CancelService;
import cs336.travel.service.ReservationService;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

public final class CustomerHistoryPanel extends JPanel {

    private static final NumberFormat MONEY = NumberFormat.getCurrencyInstance(Locale.US);
    private static final DateTimeFormatter DATETIME_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private static final String[] SUMMARY_COLUMNS = {
            "Reservation #", "Booked On", "Trip Type", "Segments", "Total Fare", "Status"
    };
    private static final String[] SEGMENT_COLUMNS = {
            "Segment", "Airline FlightNo", "From → To",
            "Depart", "Arrive", "Class", "Seat"
    };

    private static final String CARD_TABLE = "table";
    private static final String CARD_EMPTY = "empty";

    private final MainFrame frame;

    private final DefaultTableModel upcomingModel = newSummaryModel();
    private final DefaultTableModel pastModel     = newSummaryModel();

    private final JTable upcomingTable = readOnlyTable(upcomingModel);
    private final JTable pastTable     = readOnlyTable(pastModel);

    private final CardLayout upcomingCards = new CardLayout();
    private final JPanel upcomingArea = new JPanel(upcomingCards);
    private final CardLayout pastCards = new CardLayout();
    private final JPanel pastArea = new JPanel(pastCards);

    private final JTabbedPane tabs = new JTabbedPane();
    private final JButton viewSegmentsButton = new JButton("View segments...");
    private final JButton cancelButton       = new JButton("Cancel reservation");

    public CustomerHistoryPanel(MainFrame frame) {
        this.frame = frame;
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        add(buildHeader(), BorderLayout.NORTH);
        add(buildTabs(),   BorderLayout.CENTER);
        add(buildSouth(),  BorderLayout.SOUTH);
    }

    private JPanel buildHeader() {
        JLabel title = new JLabel("My Reservations");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 18f));

        JButton refresh = new JButton("Refresh");
        refresh.addActionListener(e -> refresh());

        JPanel header = new JPanel(new BorderLayout());
        header.add(title,   BorderLayout.WEST);
        header.add(refresh, BorderLayout.EAST);
        header.setBorder(BorderFactory.createEmptyBorder(0, 0, 8, 0));
        return header;
    }

    private JTabbedPane buildTabs() {
        upcomingArea.add(new JScrollPane(upcomingTable), CARD_TABLE);
        upcomingArea.add(emptyLabel("No upcoming reservations."), CARD_EMPTY);
        pastArea.add(new JScrollPane(pastTable), CARD_TABLE);
        pastArea.add(emptyLabel("No past reservations."), CARD_EMPTY);

        tabs.addTab("Upcoming", upcomingArea);
        tabs.addTab("Past",     pastArea);
        tabs.addChangeListener(e -> updateViewSegmentsEnabled());

        upcomingTable.getSelectionModel().addListSelectionListener(e -> updateViewSegmentsEnabled());
        pastTable.getSelectionModel().addListSelectionListener(e -> updateViewSegmentsEnabled());
        return tabs;
    }

    private JPanel buildSouth() {
        JButton back = new JButton("Back");
        back.addActionListener(e -> frame.showHomeFor(Role.CUSTOMER));

        viewSegmentsButton.setEnabled(false);
        viewSegmentsButton.addActionListener(e -> onViewSegmentsClicked());

        cancelButton.setEnabled(false);
        cancelButton.addActionListener(e -> onCancelClicked());

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        left.add(back);
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 4));
        right.add(viewSegmentsButton);
        right.add(cancelButton);

        JPanel south = new JPanel(new BorderLayout());
        south.add(left,  BorderLayout.WEST);
        south.add(right, BorderLayout.EAST);
        return south;
    }

    /** Re-fetches both tabs from the service. Called on every navigation. */
    public void refresh() {
        loadTab(upcomingModel, upcomingArea, upcomingCards, true);
        loadTab(pastModel,     pastArea,     pastCards,     false);
        upcomingTable.clearSelection();
        pastTable.clearSelection();
        updateViewSegmentsEnabled();
    }

    private void loadTab(DefaultTableModel model, JPanel area, CardLayout cards, boolean upcoming) {
        List<ReservationSummary> rows = ReservationService.listMyReservations(upcoming);
        model.setRowCount(0);
        for (ReservationSummary r : rows) {
            model.addRow(new Object[]{
                    r.reservationID(),
                    r.bookedOn().format(DATETIME_FMT),
                    r.tripType() == cs336.travel.model.TripType.ROUND_TRIP ? "Round-trip" : "One-way",
                    r.segmentCount(),
                    MONEY.format(r.totalFare()),
                    r.status()
            });
        }
        cards.show(area, rows.isEmpty() ? CARD_EMPTY : CARD_TABLE);
    }

    private void updateViewSegmentsEnabled() {
        JTable t = activeTable();
        boolean oneSelected = t != null && t.getSelectedRowCount() == 1;
        viewSegmentsButton.setEnabled(oneSelected);
        // Cancel only makes sense for currently-CONFIRMED reservations on the
        // Upcoming tab. Past tab + already-CANCELLED rows are read-only.
        cancelButton.setEnabled(oneSelected && cancelEligibleSelection());
    }

    private boolean cancelEligibleSelection() {
        if (tabs.getSelectedIndex() != 0) return false;
        int row = upcomingTable.getSelectedRow();
        if (row < 0) return false;
        Object status = upcomingModel.getValueAt(row, 5);
        return "CONFIRMED".equals(status);
    }

    private JTable activeTable() {
        return tabs.getSelectedIndex() == 0 ? upcomingTable : pastTable;
    }

    private DefaultTableModel activeModel() {
        return tabs.getSelectedIndex() == 0 ? upcomingModel : pastModel;
    }

    private void onViewSegmentsClicked() {
        JTable t = activeTable();
        int row = t.getSelectedRow();
        if (row < 0) return;
        int reservationID = (int) activeModel().getValueAt(row, 0);

        List<SegmentDetail> segments;
        try {
            segments = ReservationService.listSegments(reservationID);
        } catch (RuntimeException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(),
                    "Couldn't load segments", JOptionPane.ERROR_MESSAGE);
            return;
        }
        showSegmentsDialog(reservationID, segments);
    }

    private void onCancelClicked() {
        int row = upcomingTable.getSelectedRow();
        if (row < 0) return;
        int reservationID = (int) upcomingModel.getValueAt(row, 0);
        String totalCell  = String.valueOf(upcomingModel.getValueAt(row, 4));

        List<SegmentDetail> segments;
        try {
            segments = ReservationService.listSegments(reservationID);
        } catch (RuntimeException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(),
                    "Couldn't load segments", JOptionPane.ERROR_MESSAGE);
            return;
        }

        boolean anyEconomy = segments.stream()
                .anyMatch(s -> s.travelClass() == TravelClass.ECONOMY);
        if (anyEconomy) {
            JOptionPane.showMessageDialog(this,
                    "Economy reservations cannot be cancelled per airline policy.",
                    "Cannot cancel", JOptionPane.WARNING_MESSAGE);
            return;
        }

        StringBuilder sb = new StringBuilder()
                .append("Cancel reservation #").append(reservationID)
                .append(" (Total: ").append(totalCell).append(")?\n");
        String[] labels = {"Outbound", "Return"};
        for (int i = 0; i < segments.size(); i++) {
            SegmentDetail s = segments.get(i);
            String legLabel = i < labels.length ? labels[i] : "Segment " + s.segmentOrder();
            sb.append("  ").append(legLabel).append(": ")
              .append(Format.flightLabel(s.airlineID(), s.flightNumber())).append("  ")
              .append(s.fromAirport()).append("→").append(s.toAirport()).append("  ")
              .append(s.departureDateTime().toLocalDate()).append("  ")
              .append(s.travelClass().label()).append('\n');
        }
        sb.append("Cancellation will free the seat(s) and notify the next "
                + "waitlisted customer if any.");

        Object[] options = {"Cancel reservation", "Keep it"};
        int choice = JOptionPane.showOptionDialog(this, sb.toString(),
                "Confirm cancellation", JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE, null, options, options[1]);
        if (choice != 0) return;

        CancelResult result = CancelService.cancel(reservationID);
        switch (result) {
            case CancelResult.Success s -> {
                JOptionPane.showMessageDialog(this,
                        "Reservation #" + s.reservationID() + " cancelled. "
                                + s.promotedCount() + " waitlisted customer(s) promoted.",
                        "Cancelled", JOptionPane.INFORMATION_MESSAGE);
                refresh();
            }
            case CancelResult.Refused r -> JOptionPane.showMessageDialog(this,
                    r.reason(), "Cannot cancel", JOptionPane.WARNING_MESSAGE);
            case CancelResult.Error e -> JOptionPane.showMessageDialog(this,
                    e.message(), "Cancel failed", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void showSegmentsDialog(int reservationID, List<SegmentDetail> segments) {
        DefaultTableModel model = new DefaultTableModel(SEGMENT_COLUMNS, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        for (SegmentDetail s : segments) {
            model.addRow(new Object[]{
                    s.segmentOrder(),
                    Format.flightLabel(s.airlineID(), s.flightNumber()),
                    s.fromAirport() + " → " + s.toAirport(),
                    s.departureDateTime().format(DATETIME_FMT),
                    s.arrivalDateTime().format(DATETIME_FMT),
                    s.travelClass().label(),
                    s.seatNumber()
            });
        }

        JTable table = new JTable(model);
        table.setAutoCreateRowSorter(false);
        JScrollPane scroll = new JScrollPane(table);
        scroll.setPreferredSize(new Dimension(760, 260));

        JFrame owner = (JFrame) SwingUtilities.getWindowAncestor(this);
        JDialog dialog = new JDialog(owner, "Reservation #" + reservationID + " — segments", true);
        dialog.setLayout(new BorderLayout());
        dialog.add(scroll, BorderLayout.CENTER);

        JButton close = new JButton("Close");
        close.addActionListener(e -> dialog.dispose());
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttons.add(close);
        dialog.add(buttons, BorderLayout.SOUTH);

        dialog.pack();
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }

    private static DefaultTableModel newSummaryModel() {
        return new DefaultTableModel(SUMMARY_COLUMNS, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
    }

    private static JTable readOnlyTable(DefaultTableModel model) {
        JTable t = new JTable(model);
        t.setAutoCreateRowSorter(false);
        return t;
    }

    private static JLabel emptyLabel(String text) {
        JLabel l = new JLabel(text, SwingConstants.CENTER);
        l.setFont(l.getFont().deriveFont(Font.ITALIC, 14f));
        return l;
    }
}
