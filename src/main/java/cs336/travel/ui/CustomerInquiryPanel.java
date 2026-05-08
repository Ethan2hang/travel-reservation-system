package cs336.travel.ui;

import cs336.travel.model.InquiryRow;
import cs336.travel.model.Role;
import cs336.travel.service.InquiryService;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.time.format.DateTimeFormatter;
import java.util.List;

public final class CustomerInquiryPanel extends JPanel {

    private static final DateTimeFormatter DATETIME_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final int MAX_W = 720;

    private static final String CARD_TABLE = "table";
    private static final String CARD_EMPTY = "empty";

    private static final String[] COLUMNS = { "Posted On", "Status", "Question", "Answer" };
    private static final int COL_ANSWER = 3;
    private static final String AWAITING = "(awaiting reply)";

    private final MainFrame frame;
    private final JTextArea questionArea = new JTextArea(5, 60);
    private final JLabel errorLabel = new JLabel(" ");
    private final JButton submitButton = new JButton("Submit question");

    private final DefaultTableModel historyModel = new DefaultTableModel(COLUMNS, 0) {
        @Override public boolean isCellEditable(int r, int c) { return false; }
    };
    private final JTable historyTable = new JTable(historyModel);

    private final CardLayout historyCards = new CardLayout();
    private final JPanel historyArea = new JPanel(historyCards);

    public CustomerInquiryPanel(MainFrame frame) {
        this.frame = frame;
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        add(buildHeader(),  BorderLayout.NORTH);
        add(buildBody(),    BorderLayout.CENTER);
        add(buildSouth(),   BorderLayout.SOUTH);
    }

    private JPanel buildHeader() {
        JLabel title = new JLabel("Contact Support");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 18f));
        JButton refresh = new JButton("Refresh");
        refresh.addActionListener(e -> refreshHistory());

        JPanel header = new JPanel(new BorderLayout());
        header.add(title,   BorderLayout.WEST);
        header.add(refresh, BorderLayout.EAST);
        header.setBorder(BorderFactory.createEmptyBorder(0, 0, 8, 0));
        return header;
    }

    private JPanel buildBody() {
        // ---- Section 1: Ask a question ----
        JLabel askHeader = new JLabel("Ask a question");
        askHeader.setFont(askHeader.getFont().deriveFont(Font.BOLD, 14f));
        askHeader.setAlignmentX(Component.LEFT_ALIGNMENT);

        questionArea.setLineWrap(true);
        questionArea.setWrapStyleWord(true);
        JScrollPane areaScroll = new JScrollPane(questionArea);
        areaScroll.setAlignmentX(Component.LEFT_ALIGNMENT);
        areaScroll.setMaximumSize(new Dimension(MAX_W, 120));
        areaScroll.setPreferredSize(new Dimension(MAX_W, 100));

        errorLabel.setForeground(new Color(0xB00020));
        errorLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        submitButton.addActionListener(e -> onSubmitClicked());
        JPanel submitRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        submitRow.add(submitButton);
        submitRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        submitRow.setMaximumSize(new Dimension(MAX_W, 36));

        // ---- Section 2: Past questions ----
        JLabel pastHeader = new JLabel("Your past questions");
        pastHeader.setFont(pastHeader.getFont().deriveFont(Font.BOLD, 14f));
        pastHeader.setAlignmentX(Component.LEFT_ALIGNMENT);

        configureHistoryTable();
        JScrollPane tableScroll = new JScrollPane(historyTable);
        historyArea.add(tableScroll, CARD_TABLE);
        historyArea.add(emptyLabel(), CARD_EMPTY);
        historyArea.setAlignmentX(Component.LEFT_ALIGNMENT);
        historyArea.setMaximumSize(new Dimension(MAX_W, Integer.MAX_VALUE));
        historyArea.setPreferredSize(new Dimension(MAX_W, 240));

        JPanel body = new JPanel();
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.add(askHeader);
        body.add(Box.createVerticalStrut(4));
        body.add(areaScroll);
        body.add(errorLabel);
        body.add(submitRow);
        body.add(Box.createVerticalStrut(16));
        body.add(pastHeader);
        body.add(Box.createVerticalStrut(4));
        body.add(historyArea);
        return body;
    }

    private void configureHistoryTable() {
        historyTable.setAutoCreateRowSorter(false);
        historyTable.getColumnModel().getColumn(0).setPreferredWidth(120);
        historyTable.getColumnModel().getColumn(1).setPreferredWidth(70);
        historyTable.getColumnModel().getColumn(2).setPreferredWidth(280);
        historyTable.getColumnModel().getColumn(3).setPreferredWidth(220);
        historyTable.getColumnModel().getColumn(COL_ANSWER).setCellRenderer(new AnswerRenderer());
    }

    private JPanel buildSouth() {
        JButton back = new JButton("Back");
        back.addActionListener(e -> frame.showHomeFor(Role.CUSTOMER));
        JPanel south = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        south.add(back);
        return south;
    }

    /** Called by {@link MainFrame} on every navigation to this panel. */
    public void refresh() {
        clearError();
        refreshHistory();
    }

    private void refreshHistory() {
        List<InquiryRow> rows;
        try {
            rows = InquiryService.listMyInquiries();
        } catch (RuntimeException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(),
                    "Couldn't load questions", JOptionPane.ERROR_MESSAGE);
            return;
        }
        historyModel.setRowCount(0);
        for (InquiryRow r : rows) {
            historyModel.addRow(new Object[]{
                    r.postedAt().format(DATETIME_FMT),
                    r.status(),
                    r.question(),
                    r.answer() == null ? AWAITING : r.answer()
            });
        }
        historyCards.show(historyArea, rows.isEmpty() ? CARD_EMPTY : CARD_TABLE);
    }

    private void onSubmitClicked() {
        clearError();
        submitButton.setEnabled(false);
        try {
            InquiryService.postQuestion(questionArea.getText());
            questionArea.setText("");
            refreshHistory();
        } catch (IllegalArgumentException ex) {
            errorLabel.setText(ex.getMessage());
        } catch (RuntimeException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(),
                    "Submit failed", JOptionPane.ERROR_MESSAGE);
        } finally {
            submitButton.setEnabled(true);
        }
    }

    private void clearError() {
        errorLabel.setText(" ");
    }

    private static JLabel emptyLabel() {
        JLabel l = new JLabel("You haven't asked any questions yet.", SwingConstants.CENTER);
        l.setFont(l.getFont().deriveFont(Font.ITALIC, 13f));
        l.setForeground(Color.GRAY);
        return l;
    }

    /** Renders the Answer column with italic-grey "(awaiting reply)" placeholder. */
    private static final class AnswerRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(
                JTable table, Object value, boolean isSelected,
                boolean hasFocus, int row, int column) {
            Component c = super.getTableCellRendererComponent(
                    table, value, isSelected, hasFocus, row, column);
            if (AWAITING.equals(value)) {
                c.setFont(c.getFont().deriveFont(Font.ITALIC));
                if (!isSelected) c.setForeground(Color.GRAY);
            } else {
                c.setFont(c.getFont().deriveFont(Font.PLAIN));
                if (!isSelected) c.setForeground(Color.BLACK);
            }
            return c;
        }
    }
}
