package cs336.travel.ui;

import cs336.travel.dao.ReservationDAO;
import cs336.travel.dao.TicketFlightDAO.EditSegmentRow;
import cs336.travel.model.Customer;
import cs336.travel.model.EditResult;
import cs336.travel.model.ReservationSummary;
import cs336.travel.model.Role;
import cs336.travel.model.SegmentEditDelta;
import cs336.travel.model.TravelClass;
import cs336.travel.service.CRService;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.DefaultCellEditor;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Font;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public final class CREditReservationPanel extends JPanel {

    private static final NumberFormat MONEY = NumberFormat.getCurrencyInstance(Locale.US);
    private static final DateTimeFormatter DATETIME_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private static final String CARD_EMPTY  = "empty";
    private static final String CARD_EDIT   = "edit";
    private static final String CARD_LIST   = "list";

    private static final String INPUT_BY_RES  = "by-res";
    private static final String INPUT_BY_CUST = "by-cust";

    private static final String[] EDIT_COLUMNS = {
            "Seg", "Airline Flight", "Depart", "From → To", "Class", "Seat", "Meal"
    };
    private static final int COL_CLASS = 4;
    private static final int COL_SEAT  = 5;
    private static final int COL_MEAL  = 6;

    private static final String[] CUST_RES_COLUMNS = {
            "Reservation #", "Booked On", "Trip Type", "Status", "Total Fare"
    };

    private final MainFrame frame;

    // Filter row
    private final JRadioButton byResRadio  = new JRadioButton("By Reservation #", true);
    private final JRadioButton byCustRadio = new JRadioButton("By Customer");
    private final CardLayout inputCards = new CardLayout();
    private final JPanel inputArea = new JPanel(inputCards);
    private final JTextField resNumberField = new JTextField(8);
    private final JComboBox<Customer> customerCombo = new JComboBox<>();

    // Result cards
    private final CardLayout resultCards = new CardLayout();
    private final JPanel resultArea = new JPanel(resultCards);
    private final JLabel emptyLabel = italicGray("Look up a reservation to edit.");

    // Edit card (Card 2)
    private final JLabel headerLine = new JLabel(" ");
    private final DefaultTableModel editModel = new DefaultTableModel(EDIT_COLUMNS, 0) {
        @Override public boolean isCellEditable(int r, int c) {
            return c == COL_CLASS || c == COL_SEAT || c == COL_MEAL;
        }
        @Override public Class<?> getColumnClass(int c) {
            return c == COL_CLASS ? TravelClass.class : String.class;
        }
    };
    private final JTable editTable = new JTable(editModel);
    private final JButton saveButton  = new JButton("Save changes");
    private final JButton cancelButton= new JButton("Cancel");

    // List card (Card 3)
    private final DefaultTableModel custResModel = new DefaultTableModel(CUST_RES_COLUMNS, 0) {
        @Override public boolean isCellEditable(int r, int c) { return false; }
    };
    private final JTable custResTable = new JTable(custResModel);
    private final JButton openSelectedButton = new JButton("Open selected for editing");

    // State for currently-loaded reservation
    private List<EditSegmentRow> originalSegments = new ArrayList<>();
    private ReservationDAO.EditHeader currentHeader;

    public CREditReservationPanel(MainFrame frame) {
        this.frame = frame;
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        add(buildHeader(),  BorderLayout.NORTH);
        add(buildResults(), BorderLayout.CENTER);
    }

    private JPanel buildHeader() {
        JLabel title = new JLabel("Edit Reservation");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 18f));
        JButton back = new JButton("Back to CR Home");
        back.addActionListener(e -> frame.showHomeFor(Role.CUSTOMER_REP));

        JPanel topRow = new JPanel(new BorderLayout());
        topRow.add(title, BorderLayout.WEST);
        topRow.add(back,  BorderLayout.EAST);

        ButtonGroup g = new ButtonGroup();
        g.add(byResRadio);
        g.add(byCustRadio);
        byResRadio.addActionListener(e  -> inputCards.show(inputArea, INPUT_BY_RES));
        byCustRadio.addActionListener(e -> inputCards.show(inputArea, INPUT_BY_CUST));

        JButton lookupBtn = new JButton("Look up");
        lookupBtn.addActionListener(e -> onLookupByReservation());
        JPanel byRes = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        byRes.add(new JLabel("Reservation #:"));
        byRes.add(resNumberField);
        byRes.add(lookupBtn);

        JButton loadBtn = new JButton("Load");
        loadBtn.addActionListener(e -> onLoadByCustomer());
        customerCombo.setRenderer(customerRenderer());
        JPanel byCust = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        byCust.add(new JLabel("Customer:"));
        byCust.add(customerCombo);
        byCust.add(loadBtn);

        inputArea.add(byRes,  INPUT_BY_RES);
        inputArea.add(byCust, INPUT_BY_CUST);

        JPanel modeRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        modeRow.add(byResRadio);
        modeRow.add(byCustRadio);

        JPanel filter = new JPanel();
        filter.setLayout(new javax.swing.BoxLayout(filter, javax.swing.BoxLayout.Y_AXIS));
        filter.add(modeRow);
        filter.add(inputArea);

        JPanel header = new JPanel(new BorderLayout());
        header.add(topRow, BorderLayout.NORTH);
        header.add(filter, BorderLayout.CENTER);
        header.setBorder(BorderFactory.createEmptyBorder(0, 0, 8, 0));
        return header;
    }

    private JPanel buildResults() {
        // Card 1: empty state
        JPanel empty = new JPanel(new BorderLayout());
        empty.add(emptyLabel, BorderLayout.CENTER);

        // Card 2: edit
        headerLine.setFont(headerLine.getFont().deriveFont(Font.BOLD, 13f));
        headerLine.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        editTable.setAutoCreateRowSorter(false);
        configureEditTable();

        saveButton.addActionListener(e -> onSaveClicked());
        cancelButton.addActionListener(e -> {
            originalSegments = new ArrayList<>();
            currentHeader = null;
            editModel.setRowCount(0);
            resultCards.show(resultArea, CARD_EMPTY);
        });
        JPanel editButtons = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        editButtons.add(saveButton);
        editButtons.add(cancelButton);

        JPanel editCard = new JPanel(new BorderLayout(0, 4));
        editCard.add(headerLine,                 BorderLayout.NORTH);
        editCard.add(new JScrollPane(editTable), BorderLayout.CENTER);
        editCard.add(editButtons,                BorderLayout.SOUTH);

        // Card 3: list of reservations for a customer
        custResTable.setAutoCreateRowSorter(false);
        openSelectedButton.setEnabled(false);
        custResTable.getSelectionModel().addListSelectionListener(e ->
                openSelectedButton.setEnabled(custResTable.getSelectedRowCount() == 1));
        openSelectedButton.addActionListener(e -> onOpenSelectedFromList());
        JPanel listButtons = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        listButtons.add(openSelectedButton);
        JPanel listCard = new JPanel(new BorderLayout(0, 4));
        listCard.add(new JScrollPane(custResTable), BorderLayout.CENTER);
        listCard.add(listButtons,                   BorderLayout.SOUTH);

        resultArea.add(empty,    CARD_EMPTY);
        resultArea.add(editCard, CARD_EDIT);
        resultArea.add(listCard, CARD_LIST);
        resultCards.show(resultArea, CARD_EMPTY);
        return resultArea;
    }

    private void configureEditTable() {
        JComboBox<TravelClass> classCombo = new JComboBox<>(TravelClass.values());
        classCombo.setRenderer(new javax.swing.DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(
                    javax.swing.JList<?> list, Object value, int index,
                    boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof TravelClass tc) setText(tc.label());
                return this;
            }
        });
        editTable.getColumnModel().getColumn(COL_CLASS)
                .setCellEditor(new DefaultCellEditor(classCombo));
        editTable.getColumnModel().getColumn(COL_CLASS).setCellRenderer(
                new javax.swing.table.DefaultTableCellRenderer() {
                    @Override
                    public Component getTableCellRendererComponent(
                            JTable table, Object value, boolean isSelected,
                            boolean hasFocus, int row, int column) {
                        super.getTableCellRendererComponent(
                                table, value instanceof TravelClass tc ? tc.label() : value,
                                isSelected, hasFocus, row, column);
                        return this;
                    }
                });

        editTable.getColumnModel().getColumn(0).setPreferredWidth(40);
        editTable.getColumnModel().getColumn(1).setPreferredWidth(110);
        editTable.getColumnModel().getColumn(2).setPreferredWidth(120);
        editTable.getColumnModel().getColumn(3).setPreferredWidth(120);
        editTable.getColumnModel().getColumn(COL_CLASS).setPreferredWidth(90);
        editTable.getColumnModel().getColumn(COL_SEAT).setPreferredWidth(60);
        editTable.getColumnModel().getColumn(COL_MEAL).setPreferredWidth(140);
    }

    public void refresh() {
        // Reload customer list every time the panel is shown.
        List<Customer> customers = CRService.listAllCustomers();
        customerCombo.setModel(new DefaultComboBoxModel<>(customers.toArray(new Customer[0])));
        // Don't reset already-loaded edit state on every refresh — but if
        // there's none, default to empty.
        if (currentHeader == null) {
            resultCards.show(resultArea, CARD_EMPTY);
        }
    }

    private void onLookupByReservation() {
        String text = resNumberField.getText().trim();
        int reservationID;
        try {
            reservationID = Integer.parseInt(text);
        } catch (NumberFormatException nfe) {
            JOptionPane.showMessageDialog(this,
                    "Reservation # must be a number.",
                    "Invalid input", JOptionPane.WARNING_MESSAGE);
            return;
        }
        loadIntoEditCard(reservationID);
    }

    private void onLoadByCustomer() {
        Customer c = (Customer) customerCombo.getSelectedItem();
        if (c == null) {
            JOptionPane.showMessageDialog(this,
                    "Pick a customer.",
                    "Invalid input", JOptionPane.WARNING_MESSAGE);
            return;
        }
        List<ReservationSummary> rows = CRService.listReservationsForCustomer(c.customerID());
        custResModel.setRowCount(0);
        for (ReservationSummary r : rows) {
            custResModel.addRow(new Object[]{
                    r.reservationID(),
                    r.bookedOn().format(DATETIME_FMT),
                    r.tripType().name().equals("ROUND_TRIP") ? "Round-trip" : "One-way",
                    r.status(),
                    MONEY.format(r.totalFare())
            });
        }
        if (rows.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    c.username() + " has no reservations.",
                    "Empty", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        resultCards.show(resultArea, CARD_LIST);
    }

    private void onOpenSelectedFromList() {
        int row = custResTable.getSelectedRow();
        if (row < 0) return;
        int reservationID = (int) custResModel.getValueAt(row, 0);
        loadIntoEditCard(reservationID);
    }

    private void loadIntoEditCard(int reservationID) {
        Optional<ReservationDAO.EditHeader> hdrOpt = CRService.findReservationHeader(reservationID);
        if (hdrOpt.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "No reservation #" + reservationID + ".",
                    "Not found", JOptionPane.WARNING_MESSAGE);
            return;
        }
        ReservationDAO.EditHeader h = hdrOpt.get();
        if (!"CONFIRMED".equals(h.status())) {
            JOptionPane.showMessageDialog(this,
                    "This reservation has been cancelled and cannot be edited.",
                    "Cannot edit", JOptionPane.WARNING_MESSAGE);
            return;
        }

        List<EditSegmentRow> segments = CRService.loadEditableSegments(reservationID);
        if (segments.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Reservation #" + reservationID + " has no segments.",
                    "Empty", JOptionPane.WARNING_MESSAGE);
            return;
        }

        currentHeader = h;
        originalSegments = segments;

        headerLine.setText(
                "Reservation #" + h.reservationID()
                        + " — " + h.customerUsername() + " (" + h.customerName() + ")"
                        + " — Booked " + h.bookedOn().format(DATETIME_FMT)
                        + " — Status: " + h.status()
                        + " — Trip: " + h.tripType()
                        + " — Total Fare: " + MONEY.format(h.totalFare())
                        + " (will recompute on Save)");

        editModel.setRowCount(0);
        for (EditSegmentRow s : segments) {
            editModel.addRow(new Object[]{
                    s.segmentOrder(),
                    Format.flightLabel(s.airlineID(), s.flightNumber()),
                    s.departureDateTime().format(DATETIME_FMT),
                    s.fromAirport() + " → " + s.toAirport(),
                    s.currentClass(),
                    s.currentSeat(),
                    s.currentMeal() == null ? "" : s.currentMeal()
            });
        }
        resultCards.show(resultArea, CARD_EDIT);
    }

    private void onSaveClicked() {
        if (editTable.isEditing()) editTable.getCellEditor().stopCellEditing();
        if (currentHeader == null) return;

        List<SegmentEditDelta> deltas = new ArrayList<>();
        StringBuilder diff = new StringBuilder("Save changes to Reservation #")
                .append(currentHeader.reservationID()).append("?\n  Changes:\n");
        boolean any = false;
        BigDecimal newTotal = BigDecimal.ZERO;
        BigDecimal oldTotal = BigDecimal.ZERO;

        for (int i = 0; i < originalSegments.size(); i++) {
            EditSegmentRow orig = originalSegments.get(i);
            TravelClass newCls = (TravelClass) editModel.getValueAt(i, COL_CLASS);
            String      newSeat = String.valueOf(editModel.getValueAt(i, COL_SEAT));
            String      newMeal = String.valueOf(editModel.getValueAt(i, COL_MEAL));
            String      origMeal = orig.currentMeal() == null ? "" : orig.currentMeal();

            Optional<TravelClass> dCls  = orig.currentClass() == newCls
                    ? Optional.empty() : Optional.of(newCls);
            Optional<String>      dSeat = orig.currentSeat().equals(newSeat)
                    ? Optional.empty() : Optional.of(newSeat);
            Optional<String>      dMeal = origMeal.equals(newMeal)
                    ? Optional.empty() : Optional.of(newMeal);

            BigDecimal origFare = orig.basePrice().multiply(multiplier(orig.currentClass()));
            BigDecimal newFare  = orig.basePrice().multiply(multiplier(newCls));
            oldTotal = oldTotal.add(origFare);
            newTotal = newTotal.add(newFare);

            if (dCls.isPresent()) {
                diff.append("    Seg ").append(orig.segmentOrder()).append(" class:  ")
                    .append(orig.currentClass().label()).append(" → ").append(newCls.label())
                    .append("   (fare ").append(MONEY.format(origFare))
                    .append(" → ").append(MONEY.format(newFare)).append(")\n");
                any = true;
            }
            if (dSeat.isPresent()) {
                diff.append("    Seg ").append(orig.segmentOrder()).append(" seat:   ")
                    .append(orig.currentSeat()).append(" → ").append(newSeat).append("\n");
                any = true;
            }
            if (dMeal.isPresent()) {
                diff.append("    Seg ").append(orig.segmentOrder()).append(" meal:   ")
                    .append(origMeal.isEmpty() ? "(none)" : origMeal)
                    .append(" → ")
                    .append(newMeal.isEmpty() ? "(none)" : newMeal)
                    .append("\n");
                any = true;
            }
            if (dCls.isPresent() || dSeat.isPresent() || dMeal.isPresent()) {
                deltas.add(new SegmentEditDelta(orig.segmentOrder(), dCls, dSeat, dMeal));
            }
        }

        if (!any) {
            JOptionPane.showMessageDialog(this,
                    "No changes to save.",
                    "Nothing to do", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        diff.append("  New total fare: ").append(MONEY.format(newTotal))
            .append(" (was ").append(MONEY.format(oldTotal)).append(")");
        Object[] options = {"Save", "Discard"};
        int choice = JOptionPane.showOptionDialog(this, diff.toString(),
                "Confirm changes", JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE, null, options, options[1]);
        if (choice != 0) return;

        EditResult result = CRService.editReservation(currentHeader.reservationID(), deltas);
        switch (result) {
            case EditResult.Success s -> {
                JOptionPane.showMessageDialog(this,
                        "Reservation #" + s.reservationID()
                                + " updated. New total fare: "
                                + MONEY.format(s.newTotalFare()) + ".",
                        "Saved", JOptionPane.INFORMATION_MESSAGE);
                loadIntoEditCard(currentHeader.reservationID());
            }
            case EditResult.Refused r -> JOptionPane.showMessageDialog(this,
                    r.reason(), "Cannot edit", JOptionPane.WARNING_MESSAGE);
            case EditResult.Error e -> JOptionPane.showMessageDialog(this,
                    e.message(), "Save failed", JOptionPane.ERROR_MESSAGE);
        }
    }

    private static BigDecimal multiplier(TravelClass cls) {
        return cs336.travel.service.PricingService.multiplier(cls);
    }

    private static javax.swing.DefaultListCellRenderer customerRenderer() {
        return new javax.swing.DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(
                    javax.swing.JList<?> list, Object value, int index,
                    boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof Customer c) setText(c.username() + " (" + c.name() + ")");
                return this;
            }
        };
    }

    private static JLabel italicGray(String text) {
        JLabel l = new JLabel(text, javax.swing.SwingConstants.CENTER);
        l.setFont(l.getFont().deriveFont(Font.ITALIC, 14f));
        l.setForeground(Color.GRAY);
        return l;
    }
}
