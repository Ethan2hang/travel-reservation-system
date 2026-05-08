package cs336.travel.ui;

import cs336.travel.model.Customer;
import cs336.travel.service.CRService;

import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.util.List;

/**
 * "Book reservation for a customer" — wraps the existing CustomerSearchPanel
 * in {@link CustomerSearchPanel.BookingMode#CR_ON_BEHALF} mode and surfaces a
 * customer picker above it. Whatever the rep selects becomes the
 * on-behalf customer for the embedded booking flow.
 */
public final class CRBookingPanel extends JPanel {

    private final MainFrame frame;
    private final JComboBox<Customer> customerCombo = new JComboBox<>();
    private final CustomerSearchPanel innerSearch;

    public CRBookingPanel(MainFrame frame) {
        this.frame = frame;
        this.innerSearch = new CustomerSearchPanel(
                frame, CustomerSearchPanel.BookingMode.CR_ON_BEHALF);

        setLayout(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        add(buildHeader(), BorderLayout.NORTH);
        add(innerSearch,   BorderLayout.CENTER);

        customerCombo.setRenderer(new javax.swing.DefaultListCellRenderer() {
            @Override
            public java.awt.Component getListCellRendererComponent(
                    javax.swing.JList<?> list, Object value, int index,
                    boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof Customer c) setText(c.username() + " (" + c.name() + ")");
                return this;
            }
        });
        customerCombo.addActionListener(e -> {
            Customer picked = (Customer) customerCombo.getSelectedItem();
            if (picked != null) innerSearch.setOnBehalfCustomer(picked);
        });
    }

    private JPanel buildHeader() {
        JLabel title = new JLabel("Book Reservation for a Customer");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 18f));

        JPanel pickerRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        pickerRow.add(new JLabel("Customer:"));
        pickerRow.add(customerCombo);

        JPanel header = new JPanel(new BorderLayout());
        header.add(title,     BorderLayout.NORTH);
        header.add(pickerRow, BorderLayout.CENTER);
        header.setBorder(BorderFactory.createEmptyBorder(0, 0, 8, 0));
        return header;
    }

    /** Called by {@link MainFrame} on each card swap. */
    public void refresh() {
        List<Customer> customers = CRService.listAllCustomers();
        Customer prev = (Customer) customerCombo.getSelectedItem();
        customerCombo.setModel(new DefaultComboBoxModel<>(customers.toArray(new Customer[0])));
        if (prev != null) customerCombo.setSelectedItem(prev);
        if (customerCombo.getSelectedItem() != null) {
            innerSearch.setOnBehalfCustomer((Customer) customerCombo.getSelectedItem());
        }
        innerSearch.refresh();
    }
}
