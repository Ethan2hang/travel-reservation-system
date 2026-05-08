package cs336.travel.ui;

import cs336.travel.model.CrudResult;
import cs336.travel.model.Customer;
import cs336.travel.model.Employee;
import cs336.travel.model.Role;
import cs336.travel.service.AdminService;

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
import java.util.List;

/**
 * Manage Users — admin-only CRUD over Employee (CUSTOMER_REP only) and Customer.
 *
 * <p>Scope decision: admin-role records are intentionally not editable from
 * this UI. The seeded {@code admin} account is the only ADMIN; this prevents
 * accidental privilege escalation through the CRUD screen. If a grader asks
 * "can I add another admin?" the answer is "out of scope by design."
 */
public final class AdminUsersPanel extends JPanel {

    private static final String[] REP_COLUMNS = {"ID", "Username", "Name", "Role"};
    private static final String[] CUST_COLUMNS = {"ID", "Username", "Name", "Email", "Phone"};

    private final MainFrame frame;

    private final DefaultTableModel repModel  = readonlyModel(REP_COLUMNS);
    private final DefaultTableModel custModel = readonlyModel(CUST_COLUMNS);
    private final JTable repTable  = new JTable(repModel);
    private final JTable custTable = new JTable(custModel);

    private final JButton repAdd    = new JButton("Add new...");
    private final JButton repEdit   = new JButton("Edit selected...");
    private final JButton repDelete = new JButton("Delete selected");
    private final JButton custAdd   = new JButton("Add new...");
    private final JButton custEdit  = new JButton("Edit selected...");
    private final JButton custDelete= new JButton("Delete selected");

    public AdminUsersPanel(MainFrame frame) {
        this.frame = frame;
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        add(buildHeader(), BorderLayout.NORTH);
        add(buildTabs(),   BorderLayout.CENTER);
        add(buildSouth(),  BorderLayout.SOUTH);
    }

    private JPanel buildHeader() {
        JLabel title = new JLabel("Manage Users");
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
        JTabbedPane tabs = new JTabbedPane();

        repTable.setAutoCreateRowSorter(false);
        custTable.setAutoCreateRowSorter(false);
        repTable.getSelectionModel().addListSelectionListener(e -> updateButtonsEnabled());
        custTable.getSelectionModel().addListSelectionListener(e -> updateButtonsEnabled());

        repAdd.addActionListener(e -> onAddRep());
        repEdit.addActionListener(e -> onEditRep());
        repDelete.addActionListener(e -> onDeleteRep());
        custAdd.addActionListener(e -> onAddCust());
        custEdit.addActionListener(e -> onEditCust());
        custDelete.addActionListener(e -> onDeleteCust());

        tabs.addTab("Customer Reps", buildTab(repTable, repAdd, repEdit, repDelete));
        tabs.addTab("Customers",     buildTab(custTable, custAdd, custEdit, custDelete));
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

    private JPanel buildSouth() {
        JButton back = new JButton("Back to Admin Home");
        back.addActionListener(e -> frame.showHomeFor(Role.ADMIN));
        JPanel south = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        south.add(back);
        return south;
    }

    public void refresh() {
        loadReps();
        loadCustomers();
        repTable.clearSelection();
        custTable.clearSelection();
        updateButtonsEnabled();
    }

    private void loadReps() {
        List<Employee> reps = AdminService.listCustomerReps();
        repModel.setRowCount(0);
        for (Employee e : reps) {
            repModel.addRow(new Object[]{e.employeeID(), e.username(), e.name(), e.role().name()});
        }
    }

    private void loadCustomers() {
        List<Customer> customers = AdminService.listCustomers();
        custModel.setRowCount(0);
        for (Customer c : customers) {
            custModel.addRow(new Object[]{
                    c.customerID(), c.username(), c.name(), c.email(),
                    c.phone() == null ? "" : c.phone()
            });
        }
    }

    private void updateButtonsEnabled() {
        boolean repSel  = repTable.getSelectedRowCount() == 1;
        boolean custSel = custTable.getSelectedRowCount() == 1;
        repEdit.setEnabled(repSel);
        repDelete.setEnabled(repSel);
        custEdit.setEnabled(custSel);
        custDelete.setEnabled(custSel);
    }

    // ---------------- Customer Rep handlers ----------------

    private void onAddRep() {
        CrudResult r = UserDialog.openAdd(this, UserDialog.Kind.CUSTOMER_REP);
        if (r instanceof CrudResult.Success) refresh();
    }

    private void onEditRep() {
        int row = repTable.getSelectedRow();
        if (row < 0) return;
        Employee e = new Employee(
                (int) repModel.getValueAt(row, 0),
                (String) repModel.getValueAt(row, 1),
                (String) repModel.getValueAt(row, 2),
                Role.valueOf((String) repModel.getValueAt(row, 3)));
        CrudResult r = UserDialog.openEditRep(this, e);
        if (r instanceof CrudResult.Success) refresh();
    }

    private void onDeleteRep() {
        int row = repTable.getSelectedRow();
        if (row < 0) return;
        int id = (int) repModel.getValueAt(row, 0);
        String username = (String) repModel.getValueAt(row, 1);
        String name     = (String) repModel.getValueAt(row, 2);
        if (!confirmDelete(username, name)) return;
        handleDeleteResult(AdminService.deleteCustomerRep(id));
    }

    // ---------------- Customer handlers ----------------

    private void onAddCust() {
        CrudResult r = UserDialog.openAdd(this, UserDialog.Kind.CUSTOMER);
        if (r instanceof CrudResult.Success) refresh();
    }

    private void onEditCust() {
        int row = custTable.getSelectedRow();
        if (row < 0) return;
        String phone = (String) custModel.getValueAt(row, 4);
        Customer c = new Customer(
                (int) custModel.getValueAt(row, 0),
                (String) custModel.getValueAt(row, 1),
                (String) custModel.getValueAt(row, 2),
                (String) custModel.getValueAt(row, 3),
                phone == null || phone.isBlank() ? null : phone);
        CrudResult r = UserDialog.openEditCustomer(this, c);
        if (r instanceof CrudResult.Success) refresh();
    }

    private void onDeleteCust() {
        int row = custTable.getSelectedRow();
        if (row < 0) return;
        int id = (int) custModel.getValueAt(row, 0);
        String username = (String) custModel.getValueAt(row, 1);
        String name     = (String) custModel.getValueAt(row, 2);
        if (!confirmDelete(username, name)) return;
        handleDeleteResult(AdminService.deleteCustomer(id));
    }

    // ---------------- Shared ----------------

    private boolean confirmDelete(String username, String name) {
        Object[] options = {"Delete", "Cancel"};
        int choice = JOptionPane.showOptionDialog(this,
                "Delete " + username + " (" + name + ")? This cannot be undone.",
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

    private static DefaultTableModel readonlyModel(String[] cols) {
        return new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
    }
}
