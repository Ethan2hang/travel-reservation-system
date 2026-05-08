package cs336.travel.ui;

import cs336.travel.model.InquiryListRow;
import cs336.travel.model.InquiryReplyResult;
import cs336.travel.model.Role;
import cs336.travel.service.CRService;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.time.format.DateTimeFormatter;
import java.util.List;

public final class CRInquiriesPanel extends JPanel {

    private static final DateTimeFormatter DATETIME_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private static final String[] OPEN_COLS = {
            "Inquiry #", "Customer", "Posted On", "Question"
    };
    private static final String[] ANSWERED_COLS = {
            "Inquiry #", "Customer", "Posted On", "Answered By", "Answered On"
    };
    private static final String CARD_TABLE = "table";
    private static final String CARD_EMPTY = "empty";

    private final MainFrame frame;

    // Open tab
    private final DefaultTableModel openModel = readonly(OPEN_COLS);
    private final JTable openTable = new JTable(openModel);
    private final CardLayout openCards = new CardLayout();
    private final JPanel openTableArea = new JPanel(openCards);
    private final JLabel openCustomerLabel = new JLabel(" ");
    private final JLabel openPostedLabel   = new JLabel(" ");
    private final JTextArea openQuestionArea = new JTextArea(4, 60);
    private final JTextArea replyArea        = new JTextArea(5, 60);
    private final JLabel replyError = new JLabel(" ");
    private final JButton sendButton = new JButton("Send reply");
    private List<InquiryListRow> openRows = List.of();

    // Answered tab
    private final DefaultTableModel answeredModel = readonly(ANSWERED_COLS);
    private final JTable answeredTable = new JTable(answeredModel);
    private final CardLayout answeredCards = new CardLayout();
    private final JPanel answeredTableArea = new JPanel(answeredCards);
    private final JLabel ansCustomerLabel = new JLabel(" ");
    private final JLabel ansPostedLabel   = new JLabel(" ");
    private final JTextArea ansQuestionArea = new JTextArea(4, 60);
    private final JLabel ansReplyMetaLabel = new JLabel(" ");
    private final JTextArea ansReplyArea    = new JTextArea(4, 60);
    private List<InquiryListRow> answeredRows = List.of();

    public CRInquiriesPanel(MainFrame frame) {
        this.frame = frame;
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        add(buildHeader(), BorderLayout.NORTH);
        add(buildTabs(),   BorderLayout.CENTER);
    }

    private JPanel buildHeader() {
        JLabel title = new JLabel("Customer Inquiries");
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
        tabs.addTab("Open",     buildOpenTab());
        tabs.addTab("Answered", buildAnsweredTab());
        return tabs;
    }

    // ---------------- Open tab ----------------
    private JPanel buildOpenTab() {
        openTable.setAutoCreateRowSorter(false);
        openTable.getSelectionModel().addListSelectionListener(e -> populateOpenSelection());

        openTableArea.add(new JScrollPane(openTable),                  CARD_TABLE);
        openTableArea.add(italicGray("No open questions right now."),  CARD_EMPTY);
        openCards.show(openTableArea, CARD_EMPTY);

        openQuestionArea.setLineWrap(true);
        openQuestionArea.setWrapStyleWord(true);
        openQuestionArea.setEditable(false);
        replyArea.setLineWrap(true);
        replyArea.setWrapStyleWord(true);
        replyError.setForeground(new Color(0xB00020));

        sendButton.setEnabled(false);
        sendButton.addActionListener(e -> onSendReply());

        JPanel meta = new JPanel();
        meta.setLayout(new javax.swing.BoxLayout(meta, javax.swing.BoxLayout.Y_AXIS));
        openCustomerLabel.setFont(openCustomerLabel.getFont().deriveFont(Font.BOLD));
        meta.add(openCustomerLabel);
        meta.add(openPostedLabel);

        JPanel form = new JPanel(new BorderLayout(0, 4));
        form.add(meta, BorderLayout.NORTH);

        JPanel center = new JPanel();
        center.setLayout(new javax.swing.BoxLayout(center, javax.swing.BoxLayout.Y_AXIS));
        JLabel qLabel = new JLabel("Question:");
        qLabel.setAlignmentX(LEFT_ALIGNMENT);
        center.add(qLabel);
        JScrollPane qScroll = new JScrollPane(openQuestionArea);
        qScroll.setAlignmentX(LEFT_ALIGNMENT);
        center.add(qScroll);

        JLabel rLabel = new JLabel("Reply:");
        rLabel.setAlignmentX(LEFT_ALIGNMENT);
        center.add(rLabel);
        JScrollPane rScroll = new JScrollPane(replyArea);
        rScroll.setAlignmentX(LEFT_ALIGNMENT);
        center.add(rScroll);
        replyError.setAlignmentX(LEFT_ALIGNMENT);
        center.add(replyError);

        JPanel sendRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        sendRow.add(sendButton);
        sendRow.setAlignmentX(LEFT_ALIGNMENT);
        center.add(sendRow);

        form.add(center, BorderLayout.CENTER);

        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, openTableArea, form);
        split.setResizeWeight(0.4);
        split.setDividerLocation(220);
        split.setBorder(BorderFactory.createEmptyBorder());

        JPanel tab = new JPanel(new BorderLayout());
        tab.add(split, BorderLayout.CENTER);
        return tab;
    }

    private void populateOpenSelection() {
        int viewRow = openTable.getSelectedRow();
        if (viewRow < 0 || viewRow >= openRows.size()) {
            openCustomerLabel.setText(" ");
            openPostedLabel.setText(" ");
            openQuestionArea.setText("");
            replyArea.setText("");
            replyError.setText(" ");
            sendButton.setEnabled(false);
            return;
        }
        InquiryListRow r = openRows.get(viewRow);
        openCustomerLabel.setText(
                "Customer: " + r.customerUsername() + " (" + r.customerName() + ")");
        openPostedLabel.setText("Posted on: " + r.postedAt().format(DATETIME_FMT));
        openQuestionArea.setText(r.question());
        openQuestionArea.setCaretPosition(0);
        replyArea.setText("");
        replyError.setText(" ");
        sendButton.setEnabled(true);
    }

    private void onSendReply() {
        int viewRow = openTable.getSelectedRow();
        if (viewRow < 0 || viewRow >= openRows.size()) return;
        InquiryListRow r = openRows.get(viewRow);
        replyError.setText(" ");
        sendButton.setEnabled(false);
        try {
            InquiryReplyResult result = CRService.replyTo(r.inquiryID(), replyArea.getText());
            switch (result) {
                case InquiryReplyResult.Success s -> {
                    JOptionPane.showMessageDialog(this,
                            "Reply sent to " + r.customerUsername() + ".",
                            "Reply sent", JOptionPane.INFORMATION_MESSAGE);
                    refresh();
                }
                case InquiryReplyResult.AlreadyAnswered a -> {
                    JOptionPane.showMessageDialog(this,
                            "Another rep already answered this inquiry. Refreshing.",
                            "Already answered", JOptionPane.INFORMATION_MESSAGE);
                    refresh();
                }
                case InquiryReplyResult.Refused x -> replyError.setText(x.reason());
                case InquiryReplyResult.Error  x -> replyError.setText(x.message());
            }
        } finally {
            // sendButton re-enables only when a row is selected.
            sendButton.setEnabled(openTable.getSelectedRow() >= 0);
        }
    }

    // ---------------- Answered tab ----------------
    private JPanel buildAnsweredTab() {
        answeredTable.setAutoCreateRowSorter(false);
        answeredTable.getSelectionModel().addListSelectionListener(e -> populateAnsweredSelection());

        answeredTableArea.add(new JScrollPane(answeredTable),               CARD_TABLE);
        answeredTableArea.add(italicGray("No answered questions yet."),     CARD_EMPTY);
        answeredCards.show(answeredTableArea, CARD_EMPTY);

        ansQuestionArea.setLineWrap(true);
        ansQuestionArea.setWrapStyleWord(true);
        ansQuestionArea.setEditable(false);
        ansReplyArea.setLineWrap(true);
        ansReplyArea.setWrapStyleWord(true);
        ansReplyArea.setEditable(false);

        JPanel meta = new JPanel();
        meta.setLayout(new javax.swing.BoxLayout(meta, javax.swing.BoxLayout.Y_AXIS));
        ansCustomerLabel.setFont(ansCustomerLabel.getFont().deriveFont(Font.BOLD));
        meta.add(ansCustomerLabel);
        meta.add(ansPostedLabel);
        meta.add(ansReplyMetaLabel);

        JPanel form = new JPanel();
        form.setLayout(new javax.swing.BoxLayout(form, javax.swing.BoxLayout.Y_AXIS));
        form.add(meta);
        JLabel qLabel = new JLabel("Question:");
        qLabel.setAlignmentX(LEFT_ALIGNMENT);
        form.add(qLabel);
        JScrollPane qScroll = new JScrollPane(ansQuestionArea);
        qScroll.setAlignmentX(LEFT_ALIGNMENT);
        form.add(qScroll);
        JLabel rLabel = new JLabel("Reply:");
        rLabel.setAlignmentX(LEFT_ALIGNMENT);
        form.add(rLabel);
        JScrollPane rScroll = new JScrollPane(ansReplyArea);
        rScroll.setAlignmentX(LEFT_ALIGNMENT);
        form.add(rScroll);

        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, answeredTableArea, form);
        split.setResizeWeight(0.45);
        split.setDividerLocation(220);
        split.setBorder(BorderFactory.createEmptyBorder());

        JPanel tab = new JPanel(new BorderLayout());
        tab.add(split, BorderLayout.CENTER);
        return tab;
    }

    private void populateAnsweredSelection() {
        int viewRow = answeredTable.getSelectedRow();
        if (viewRow < 0 || viewRow >= answeredRows.size()) {
            ansCustomerLabel.setText(" ");
            ansPostedLabel.setText(" ");
            ansReplyMetaLabel.setText(" ");
            ansQuestionArea.setText("");
            ansReplyArea.setText("");
            return;
        }
        InquiryListRow r = answeredRows.get(viewRow);
        ansCustomerLabel.setText(
                "Customer: " + r.customerUsername() + " (" + r.customerName() + ")");
        ansPostedLabel.setText("Posted on: " + r.postedAt().format(DATETIME_FMT));
        ansReplyMetaLabel.setText(
                "Answered by " + (r.answeredByUsername() == null ? "(unknown)" : r.answeredByUsername())
                        + " on "
                        + (r.answeredAt() == null ? "(unknown)" : r.answeredAt().format(DATETIME_FMT)));
        ansQuestionArea.setText(r.question());
        ansQuestionArea.setCaretPosition(0);
        ansReplyArea.setText(r.answer() == null ? "" : r.answer());
        ansReplyArea.setCaretPosition(0);
    }

    // ---------------- Refresh ----------------
    public void refresh() {
        openRows = CRService.listOpenInquiries();
        openModel.setRowCount(0);
        for (InquiryListRow r : openRows) {
            openModel.addRow(new Object[]{
                    r.inquiryID(),
                    r.customerUsername() + " (" + r.customerName() + ")",
                    r.postedAt().format(DATETIME_FMT),
                    truncate(r.question(), 80)
            });
        }
        openCards.show(openTableArea, openRows.isEmpty() ? CARD_EMPTY : CARD_TABLE);
        openTable.clearSelection();
        populateOpenSelection();

        answeredRows = CRService.listAnsweredInquiries();
        answeredModel.setRowCount(0);
        for (InquiryListRow r : answeredRows) {
            answeredModel.addRow(new Object[]{
                    r.inquiryID(),
                    r.customerUsername() + " (" + r.customerName() + ")",
                    r.postedAt().format(DATETIME_FMT),
                    r.answeredByUsername() == null ? "(unknown)" : r.answeredByUsername(),
                    r.answeredAt() == null ? "(unknown)" : r.answeredAt().format(DATETIME_FMT)
            });
        }
        answeredCards.show(answeredTableArea, answeredRows.isEmpty() ? CARD_EMPTY : CARD_TABLE);
        answeredTable.clearSelection();
        populateAnsweredSelection();
    }

    private static String truncate(String s, int max) {
        if (s == null) return "";
        s = s.replaceAll("\\s+", " ").trim();
        return s.length() <= max ? s : s.substring(0, max - 1) + "…";
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
