package cs336.travel.ui;

import cs336.travel.model.Aircraft;
import cs336.travel.model.Airport;
import cs336.travel.model.CrudResult;
import cs336.travel.model.Flight;
import cs336.travel.model.Role;
import cs336.travel.service.CRService;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

public final class CRReferenceDataPanel extends JPanel {

    private static final NumberFormat MONEY = NumberFormat.getCurrencyInstance(Locale.US);

    private static final String[] AIRCRAFT_COLS = { "Aircraft ID", "Airline", "Seat Capacity" };
    private static final String[] AIRPORT_COLS  = { "Airport ID", "Name", "City", "Country" };
    private static final String[] FLIGHT_COLS   = {
            "Airline", "Flight #", "From", "To", "Depart", "Arrive",
            "Days", "Domestic", "Base Price", "Aircraft"
    };

    private final MainFrame frame;

    private final DefaultTableModel aircraftModel = readonly(AIRCRAFT_COLS);
    private final DefaultTableModel airportModel  = readonly(AIRPORT_COLS);
    private final DefaultTableModel flightModel   = readonly(FLIGHT_COLS);

    private final JTable aircraftTable = new JTable(aircraftModel);
    private final JTable airportTable  = new JTable(airportModel);
    private final JTable flightTable   = new JTable(flightModel);

    private final JButton aircraftAdd = new JButton("Add new...");
    private final JButton aircraftEdit= new JButton("Edit selected...");
    private final JButton aircraftDel = new JButton("Delete selected");
    private final JButton airportAdd  = new JButton("Add new...");
    private final JButton airportEdit = new JButton("Edit selected...");
    private final JButton airportDel  = new JButton("Delete selected");
    private final JButton flightAdd   = new JButton("Add new...");
    private final JButton flightEdit  = new JButton("Edit selected...");
    private final JButton flightDel   = new JButton("Delete selected");

    public CRReferenceDataPanel(MainFrame frame) {
        this.frame = frame;
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        add(buildHeader(), BorderLayout.NORTH);
        add(buildTabs(),   BorderLayout.CENTER);
    }

    private JPanel buildHeader() {
        JLabel title = new JLabel("Manage Reference Data");
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
        JTabbedPane tabs = new JTabbedPane();

        aircraftTable.setAutoCreateRowSorter(false);
        airportTable.setAutoCreateRowSorter(false);
        flightTable.setAutoCreateRowSorter(false);
        aircraftTable.getSelectionModel().addListSelectionListener(e -> updateButtonsEnabled());
        airportTable.getSelectionModel().addListSelectionListener(e  -> updateButtonsEnabled());
        flightTable.getSelectionModel().addListSelectionListener(e   -> updateButtonsEnabled());

        aircraftAdd.addActionListener(e -> onAircraftAdd());
        aircraftEdit.addActionListener(e -> onAircraftEdit());
        aircraftDel.addActionListener(e -> onAircraftDelete());
        airportAdd.addActionListener(e -> onAirportAdd());
        airportEdit.addActionListener(e -> onAirportEdit());
        airportDel.addActionListener(e -> onAirportDelete());
        flightAdd.addActionListener(e -> onFlightAdd());
        flightEdit.addActionListener(e -> onFlightEdit());
        flightDel.addActionListener(e -> onFlightDelete());

        tabs.addTab("Aircraft", buildTab(aircraftTable, aircraftAdd, aircraftEdit, aircraftDel));
        tabs.addTab("Airports", buildTab(airportTable,  airportAdd,  airportEdit,  airportDel));
        tabs.addTab("Flights",  buildTab(flightTable,   flightAdd,   flightEdit,   flightDel));
        return tabs;
    }

    private JPanel buildTab(JTable table, JButton add, JButton edit, JButton del) {
        JPanel buttonRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        buttonRow.add(add);
        buttonRow.add(edit);
        buttonRow.add(del);
        JPanel tab = new JPanel(new BorderLayout(0, 4));
        tab.add(new JScrollPane(table), BorderLayout.CENTER);
        tab.add(buttonRow,              BorderLayout.SOUTH);
        return tab;
    }

    public void refresh() {
        loadAircraft();
        loadAirports();
        loadFlights();
        aircraftTable.clearSelection();
        airportTable.clearSelection();
        flightTable.clearSelection();
        updateButtonsEnabled();
    }

    private void loadAircraft() {
        aircraftModel.setRowCount(0);
        for (Aircraft a : CRService.listAircraft()) {
            aircraftModel.addRow(new Object[]{ a.aircraftID(), a.airlineID(), a.seatCapacity() });
        }
    }

    private void loadAirports() {
        airportModel.setRowCount(0);
        for (Airport a : CRService.listAirports()) {
            airportModel.addRow(new Object[]{ a.airportID(), a.name(), a.city(), a.country() });
        }
    }

    private void loadFlights() {
        flightModel.setRowCount(0);
        for (Flight f : CRService.listFlights()) {
            flightModel.addRow(new Object[]{
                    f.airlineID(),
                    f.flightNumber(),
                    f.departureAirport(),
                    f.arrivalAirport(),
                    f.departureTime().toString(),
                    f.arrivalTime().toString(),
                    f.operatingDays(),
                    f.isDomestic() ? "Yes" : "No",
                    MONEY.format(f.basePrice()),
                    f.aircraftID()
            });
        }
    }

    private void updateButtonsEnabled() {
        boolean aOne = aircraftTable.getSelectedRowCount() == 1;
        boolean pOne = airportTable.getSelectedRowCount()  == 1;
        boolean fOne = flightTable.getSelectedRowCount()   == 1;
        aircraftEdit.setEnabled(aOne); aircraftDel.setEnabled(aOne);
        airportEdit.setEnabled(pOne);  airportDel.setEnabled(pOne);
        flightEdit.setEnabled(fOne);   flightDel.setEnabled(fOne);
    }

    // ---------------- Aircraft handlers ----------------
    private void onAircraftAdd() {
        if (AircraftDialog.openAdd(this) instanceof CrudResult.Success) refresh();
    }
    private void onAircraftEdit() {
        Aircraft a = selectedAircraft();
        if (a == null) return;
        if (AircraftDialog.openEdit(this, a) instanceof CrudResult.Success) refresh();
    }
    private void onAircraftDelete() {
        Aircraft a = selectedAircraft();
        if (a == null) return;
        if (!confirmDelete("aircraft " + a.aircraftID())) return;
        handleDeleteResult(CRService.deleteAircraft(a.aircraftID()));
    }
    private Aircraft selectedAircraft() {
        int row = aircraftTable.getSelectedRow();
        if (row < 0) return null;
        String id = (String) aircraftModel.getValueAt(row, 0);
        return CRService.listAircraft().stream()
                .filter(x -> x.aircraftID().equals(id))
                .findFirst().orElse(null);
    }

    // ---------------- Airport handlers ----------------
    private void onAirportAdd() {
        if (AirportDialog.openAdd(this) instanceof CrudResult.Success) refresh();
    }
    private void onAirportEdit() {
        Airport a = selectedAirport();
        if (a == null) return;
        if (AirportDialog.openEdit(this, a) instanceof CrudResult.Success) refresh();
    }
    private void onAirportDelete() {
        Airport a = selectedAirport();
        if (a == null) return;
        if (!confirmDelete("airport " + a.airportID() + " (" + a.name() + ")")) return;
        handleDeleteResult(CRService.deleteAirport(a.airportID()));
    }
    private Airport selectedAirport() {
        int row = airportTable.getSelectedRow();
        if (row < 0) return null;
        String id = (String) airportModel.getValueAt(row, 0);
        return CRService.listAirports().stream()
                .filter(x -> x.airportID().equals(id))
                .findFirst().orElse(null);
    }

    // ---------------- Flight handlers ----------------
    private void onFlightAdd() {
        if (FlightDialog.openAdd(this) instanceof CrudResult.Success) refresh();
    }
    private void onFlightEdit() {
        Flight f = selectedFlight();
        if (f == null) return;
        if (FlightDialog.openEdit(this, f) instanceof CrudResult.Success) refresh();
    }
    private void onFlightDelete() {
        Flight f = selectedFlight();
        if (f == null) return;
        if (!confirmDelete("flight " + f.airlineID() + " " + f.flightNumber())) return;
        handleDeleteResult(CRService.deleteFlight(f.airlineID(), f.flightNumber()));
    }
    private Flight selectedFlight() {
        int row = flightTable.getSelectedRow();
        if (row < 0) return null;
        String airline = (String) flightModel.getValueAt(row, 0);
        String fno     = (String) flightModel.getValueAt(row, 1);
        return CRService.listFlights().stream()
                .filter(x -> x.airlineID().equals(airline) && x.flightNumber().equals(fno))
                .findFirst().orElse(null);
    }

    // ---------------- Shared ----------------
    private boolean confirmDelete(String label) {
        Object[] options = {"Delete", "Cancel"};
        int choice = JOptionPane.showOptionDialog(this,
                "Delete " + label + "? This cannot be undone.",
                "Confirm delete", JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE, null, options, options[1]);
        return choice == 0;
    }

    private void handleDeleteResult(CrudResult result) {
        switch (result) {
            case CrudResult.Success s -> refresh();
            case CrudResult.Refused r -> JOptionPane.showMessageDialog(this,
                    r.reason(), "Cannot delete", JOptionPane.WARNING_MESSAGE);
            case CrudResult.Error e -> JOptionPane.showMessageDialog(this,
                    e.message(), "Delete failed", JOptionPane.ERROR_MESSAGE);
        }
    }

    private static DefaultTableModel readonly(String[] cols) {
        return new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
    }
}
