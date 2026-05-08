package cs336.travel.ui;

import cs336.travel.model.Role;

import javax.swing.JFrame;
import javax.swing.JPanel;
import java.awt.CardLayout;
import java.awt.Dimension;

/**
 * Top-level window. Holds every screen as a card and swaps between them
 * after login / logout.
 */
public final class MainFrame extends JFrame {

    private static final String CARD_LOGIN            = "login";
    private static final String CARD_CUSTOMER         = "customer";
    private static final String CARD_CUSTOMER_SEARCH  = "customer-search";
    private static final String CARD_CUSTOMER_HISTORY = "customer-history";
    private static final String CARD_CUSTOMER_INQUIRY = "customer-inquiry";
    private static final String CARD_ADMIN            = "admin";
    private static final String CARD_ADMIN_USERS      = "admin-users";
    private static final String CARD_ADMIN_REPORTS    = "admin-reports";
    private static final String CARD_CR               = "cr";
    private static final String CARD_CR_BOOKING       = "cr-booking";
    private static final String CARD_CR_EDIT          = "cr-edit";
    private static final String CARD_CR_REFDATA       = "cr-refdata";
    private static final String CARD_CR_REPORTS       = "cr-reports";
    private static final String CARD_CR_INQUIRIES     = "cr-inquiries";

    private final CardLayout cards = new CardLayout();
    private final JPanel deck = new JPanel(cards);

    private final LoginScreen loginScreen;
    private final CustomerHomePanel customerHome;
    private final CustomerSearchPanel customerSearch;
    private final CustomerHistoryPanel customerHistory;
    private final CustomerInquiryPanel customerInquiry;
    private final AdminHomePanel adminHome;
    private final AdminUsersPanel adminUsers;
    private final AdminReportsPanel adminReports;
    private final CRHomePanel crHome;
    private final CRBookingPanel crBooking;
    private final CREditReservationPanel crEdit;
    private final CRReferenceDataPanel crRefData;
    private final CRReportsPanel crReports;
    private final CRInquiriesPanel crInquiries;

    public MainFrame() {
        super("CS336 Travel Reservation");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setPreferredSize(new Dimension(900, 600));

        loginScreen     = new LoginScreen(this);
        customerHome    = new CustomerHomePanel(this);
        customerSearch  = new CustomerSearchPanel(this);
        customerHistory = new CustomerHistoryPanel(this);
        customerInquiry = new CustomerInquiryPanel(this);
        adminHome       = new AdminHomePanel(this);
        adminUsers      = new AdminUsersPanel(this);
        adminReports    = new AdminReportsPanel(this);
        crHome          = new CRHomePanel(this);
        crBooking       = new CRBookingPanel(this);
        crEdit          = new CREditReservationPanel(this);
        crRefData       = new CRReferenceDataPanel(this);
        crReports       = new CRReportsPanel(this);
        crInquiries     = new CRInquiriesPanel(this);

        deck.add(loginScreen,     CARD_LOGIN);
        deck.add(customerHome,    CARD_CUSTOMER);
        deck.add(customerSearch,  CARD_CUSTOMER_SEARCH);
        deck.add(customerHistory, CARD_CUSTOMER_HISTORY);
        deck.add(customerInquiry, CARD_CUSTOMER_INQUIRY);
        deck.add(adminHome,       CARD_ADMIN);
        deck.add(adminUsers,      CARD_ADMIN_USERS);
        deck.add(adminReports,    CARD_ADMIN_REPORTS);
        deck.add(crHome,          CARD_CR);
        deck.add(crBooking,       CARD_CR_BOOKING);
        deck.add(crEdit,          CARD_CR_EDIT);
        deck.add(crRefData,       CARD_CR_REFDATA);
        deck.add(crReports,       CARD_CR_REPORTS);
        deck.add(crInquiries,     CARD_CR_INQUIRIES);

        setContentPane(deck);
        pack();
        setLocationRelativeTo(null);
        showLogin();
    }

    public void showLogin() {
        loginScreen.reset();
        cards.show(deck, CARD_LOGIN);
    }

    public void showHomeFor(Role role) {
        switch (role) {
            case ADMIN        -> { adminHome.refresh();    cards.show(deck, CARD_ADMIN); }
            case CUSTOMER_REP -> { crHome.refresh();       cards.show(deck, CARD_CR); }
            case CUSTOMER     -> { customerHome.refresh(); cards.show(deck, CARD_CUSTOMER); }
        }
    }

    public void showCustomerSearch() {
        customerSearch.refresh();
        cards.show(deck, CARD_CUSTOMER_SEARCH);
    }

    public void showCustomerHistory() {
        customerHistory.refresh();
        cards.show(deck, CARD_CUSTOMER_HISTORY);
    }

    public void showCustomerInquiry() {
        customerInquiry.refresh();
        cards.show(deck, CARD_CUSTOMER_INQUIRY);
    }

    public void showAdminUsers() {
        adminUsers.refresh();
        cards.show(deck, CARD_ADMIN_USERS);
    }

    public void showAdminReports() {
        adminReports.refresh();
        cards.show(deck, CARD_ADMIN_REPORTS);
    }

    public void showCRBooking() {
        crBooking.refresh();
        cards.show(deck, CARD_CR_BOOKING);
    }

    public void showCREditReservation() {
        crEdit.refresh();
        cards.show(deck, CARD_CR_EDIT);
    }

    public void showCRReferenceData() {
        crRefData.refresh();
        cards.show(deck, CARD_CR_REFDATA);
    }

    public void showCRReports(int initialTabIndex) {
        crReports.refresh();
        crReports.selectTab(initialTabIndex);
        cards.show(deck, CARD_CR_REPORTS);
    }

    public void showCRInquiries() {
        crInquiries.refresh();
        cards.show(deck, CARD_CR_INQUIRIES);
    }
}
