package gui;

import model.Election;
import utility.*;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;

import static gui.AppGUI.*;
import static gui.UIComponents.*;

/**
 * Organizer panel — create elections, view status, count votes and generate reports.
 */
public class OrganizerScreen extends JPanel {

    private final AppGUI                       app;
    private final LoginManager.UserLoginResult loginResult;

    private DefaultTableModel tableModel;
    private JTable            electionsTable;
    private JLabel            statusBar;

    // Create-election form fields
    private JTextField   titleField, descField, startField, endField;
    private JTextField[] candidateFields = new JTextField[5];

    public OrganizerScreen(AppGUI app, LoginManager.UserLoginResult loginResult) {
        this.app         = app;
        this.loginResult = loginResult;
        setBackground(BG_DARK);
        setLayout(new BorderLayout());
        buildUI();
    }

    private void buildUI() {

        // ── Top bar ──────────────────────────────────────────────
        JPanel topBar = new JPanel(new BorderLayout());
        topBar.setBackground(BG_PANEL);
        topBar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, BORDER_COLOR),
                BorderFactory.createEmptyBorder(12, 24, 12, 24)));

        JLabel logo = new JLabel("🏛️  Organizer Panel");
        logo.setFont(FONT_HEADER);
        logo.setForeground(TEXT_PRIMARY);

        JPanel userInfo = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        userInfo.setOpaque(false);
        userInfo.add(badge("ORGANIZER", ACCENT_BLUE));
        userInfo.add(bodyLabel(loginResult.username));
        StyledButton logoutBtn = dangerButton("Sign Out");
        logoutBtn.setPreferredSize(new Dimension(90, 30));
        logoutBtn.addActionListener(e -> app.showScreen(new MainScreen(app)));
        userInfo.add(logoutBtn);

        topBar.add(logo,    BorderLayout.WEST);
        topBar.add(userInfo,BorderLayout.EAST);
        add(topBar, BorderLayout.NORTH);

        // ── Main content ─────────────────────────────────────────
        JPanel content = new JPanel(new BorderLayout(16, 0));
        content.setBackground(BG_DARK);
        content.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        content.add(buildCreatePanel(), BorderLayout.WEST);
        content.add(buildListPanel(),   BorderLayout.CENTER);
        add(content, BorderLayout.CENTER);

        // ── Status bar ───────────────────────────────────────────
        JPanel sb = new JPanel(new BorderLayout());
        sb.setBackground(BG_PANEL);
        sb.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, BORDER_COLOR),
                BorderFactory.createEmptyBorder(6, 24, 6, 24)));
        statusBar = new JLabel("System ready.");
        statusBar.setFont(FONT_SMALL);
        statusBar.setForeground(TEXT_MUTED);
        sb.add(statusBar, BorderLayout.WEST);
        add(sb, BorderLayout.SOUTH);

        refreshTable();
    }

    // ── CREATE ELECTION FORM ─────────────────────────────────────

    private JPanel buildCreatePanel() {
        JPanel p = card();
        p.setPreferredSize(new Dimension(280, 0));
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));

        JLabel title = headerLabel("New Election");
        title.setAlignmentX(LEFT_ALIGNMENT);
        p.add(title);
        p.add(Box.createVerticalStrut(16));

        titleField = styledField("Election title");
        titleField.setAlignmentX(LEFT_ALIGNMENT);
        titleField.setMaximumSize(new Dimension(9999, 38));

        descField = styledField("Description");
        descField.setAlignmentX(LEFT_ALIGNMENT);
        descField.setMaximumSize(new Dimension(9999, 38));

        startField = styledField("Start: dd.MM.yyyy HH:mm");
        startField.setAlignmentX(LEFT_ALIGNMENT);
        startField.setMaximumSize(new Dimension(9999, 38));

        endField = styledField("End: dd.MM.yyyy HH:mm");
        endField.setAlignmentX(LEFT_ALIGNMENT);
        endField.setMaximumSize(new Dimension(9999, 38));

        p.add(mutedLabel("Title"));       p.add(Box.createVerticalStrut(4));
        p.add(titleField);                 p.add(Box.createVerticalStrut(10));
        p.add(mutedLabel("Description")); p.add(Box.createVerticalStrut(4));
        p.add(descField);                  p.add(Box.createVerticalStrut(10));
        p.add(mutedLabel("Start (leave empty = now)"));   p.add(Box.createVerticalStrut(4));
        p.add(startField);                 p.add(Box.createVerticalStrut(10));
        p.add(mutedLabel("End (leave empty = no limit)")); p.add(Box.createVerticalStrut(4));
        p.add(endField);                   p.add(Box.createVerticalStrut(14));

        p.add(mutedLabel("Candidates (min 2, max 5)"));
        p.add(Box.createVerticalStrut(6));

        for (int i = 0; i < 5; i++) {
            String ph = "Candidate " + (i+1) + (i < 2 ? " *" : " (optional)");
            candidateFields[i] = styledField(ph);
            candidateFields[i].setAlignmentX(LEFT_ALIGNMENT);
            candidateFields[i].setMaximumSize(new Dimension(9999, 34));
            p.add(candidateFields[i]);
            p.add(Box.createVerticalStrut(6));
        }

        p.add(Box.createVerticalStrut(10));

        JLabel createStatus = new JLabel(" ");
        createStatus.setFont(FONT_SMALL);
        createStatus.setAlignmentX(LEFT_ALIGNMENT);
        p.add(createStatus);
        p.add(Box.createVerticalStrut(6));

        StyledButton createBtn = primaryButton("Create Election");
        createBtn.setAlignmentX(LEFT_ALIGNMENT);
        createBtn.setMaximumSize(new Dimension(9999, 40));
        createBtn.addActionListener(e -> handleCreate(createStatus));
        p.add(createBtn);

        return p;
    }

    private void handleCreate(JLabel statusLbl) {
        String t = getFieldText(titleField, "Election title");
        String d = getFieldText(descField,  "Description");
        String s = getFieldText(startField, "Start: dd.MM.yyyy HH:mm");
        String e = getFieldText(endField,   "End: dd.MM.yyyy HH:mm");

        if (t.isEmpty()) { showLbl(statusLbl, "Title is required.", DANGER); return; }

        List<String> candidates = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            String ph = "Candidate " + (i+1) + (i < 2 ? " *" : " (optional)");
            String c  = getFieldText(candidateFields[i], ph);
            if (!c.isEmpty()) candidates.add(c);
        }
        if (candidates.size() < 2) { showLbl(statusLbl, "At least 2 candidates required.", DANGER); return; }

        try {
            Election el = new Election(t, d, parseDate(s), parseDate(e),
                                       candidates, loginResult.username);
            ElectionManager.saveElection(el);
            showLbl(statusLbl, "✓ Election created!", SUCCESS);
            clearForm();
            refreshTable();
            setStatus("Election '" + t + "' created successfully.");
        } catch (Exception ex) {
            showLbl(statusLbl, "Error: " + ex.getMessage(), DANGER);
        }
    }

    private void clearForm() {
        titleField.setText(""); descField.setText("");
        startField.setText(""); endField.setText("");
        for (JTextField f : candidateFields) f.setText("");
    }

    // ── ELECTIONS TABLE ──────────────────────────────────────────

    private JPanel buildListPanel() {
        JPanel p = card();
        p.setLayout(new BorderLayout(0, 12));

        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        JLabel listTitle = headerLabel("My Elections");
        StyledButton refreshBtn = ghostButton("↺ Refresh");
        refreshBtn.setPreferredSize(new Dimension(100, 30));
        refreshBtn.addActionListener(e -> refreshTable());
        header.add(listTitle,  BorderLayout.WEST);
        header.add(refreshBtn, BorderLayout.EAST);

        String[] cols = {"Title", "Status", "Votes", "HMAC", "Action"};
        tableModel = new DefaultTableModel(cols, 0) {
            public boolean isCellEditable(int r, int c) { return c == 4; }
        };

        electionsTable = new JTable(tableModel) {
            public Component prepareRenderer(TableCellRenderer r, int row, int col) {
                Component c = super.prepareRenderer(r, row, col);
                c.setBackground(row % 2 == 0 ? BG_CARD : BG_PANEL);
                c.setForeground(TEXT_PRIMARY);
                return c;
            }
        };
        electionsTable.setBackground(BG_CARD);
        electionsTable.setForeground(TEXT_PRIMARY);
        electionsTable.setFont(FONT_BODY);
        electionsTable.setRowHeight(36);
        electionsTable.setShowGrid(false);
        electionsTable.setIntercellSpacing(new Dimension(0, 0));
        electionsTable.getTableHeader().setBackground(BG_PANEL);
        electionsTable.getTableHeader().setForeground(TEXT_SECONDARY);
        electionsTable.getTableHeader().setFont(FONT_SMALL);
        electionsTable.getTableHeader().setBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, BORDER_COLOR));
        electionsTable.setSelectionBackground(new Color(41, 128, 210, 60));

        electionsTable.getColumnModel().getColumn(4).setCellRenderer(new ActionRenderer());
        electionsTable.getColumnModel().getColumn(4).setCellEditor(new ActionEditor());
        electionsTable.getColumnModel().getColumn(4).setPreferredWidth(200);
        electionsTable.getColumnModel().getColumn(0).setPreferredWidth(220);
        electionsTable.getColumnModel().getColumn(1).setPreferredWidth(80);
        electionsTable.getColumnModel().getColumn(2).setPreferredWidth(50);
        electionsTable.getColumnModel().getColumn(3).setPreferredWidth(70);

        JScrollPane scroll = new JScrollPane(electionsTable);
        scroll.setBackground(BG_CARD);
        scroll.getViewport().setBackground(BG_CARD);
        scroll.setBorder(new UIComponents.RoundBorder(BORDER_COLOR, 8));

        p.add(header, BorderLayout.NORTH);
        p.add(scroll, BorderLayout.CENTER);
        return p;
    }

    void refreshTable() {
        SwingUtilities.invokeLater(() -> {
            tableModel.setRowCount(0);
            for (Election e : ElectionManager.loadAllElections()) {
                boolean hmacOk = ElectionManager.verifyElectionHMAC(e);
                tableModel.addRow(new Object[]{
                    e.getTitle(),
                    e.isCurrentlyActive() ? "ACTIVE" : "CLOSED",
                    VoteStorageManager.countVotes(e.getTitle()),
                    hmacOk ? "✓ OK" : "✗ ERROR",
                    e
                });
            }
        });
    }

    // ── TABLE ACTION RENDERER / EDITOR ───────────────────────────

    class ActionRenderer implements TableCellRenderer {
        public Component getTableCellRendererComponent(
                JTable t, Object v, boolean sel, boolean focus, int row, int col) {
            JPanel p = new JPanel(new FlowLayout(FlowLayout.CENTER, 4, 4));
            p.setBackground(row % 2 == 0 ? BG_CARD : BG_PANEL);
            Election e = (Election) v;
            StyledButton b = e.isCurrentlyActive()
                    ? dangerButton("Close")
                    : primaryButton("Count Votes");
            b.setPreferredSize(new Dimension(e.isCurrentlyActive() ? 80 : 110, 26));
            p.add(b);
            return p;
        }
    }

    class ActionEditor extends DefaultCellEditor {
        private Election current;
        private JPanel   panel;

        public ActionEditor() {
            super(new JCheckBox());
            setClickCountToStart(1);
        }

        public Component getTableCellEditorComponent(
                JTable t, Object v, boolean sel, int row, int col) {
            current = (Election) v;
            panel   = new JPanel(new FlowLayout(FlowLayout.CENTER, 4, 4));
            panel.setBackground(row % 2 == 0 ? BG_CARD : BG_PANEL);

            if (current.isCurrentlyActive()) {
                StyledButton b = dangerButton("Close");
                b.setPreferredSize(new Dimension(80, 26));
                b.addActionListener(e -> {
                    if (confirm(OrganizerScreen.this,
                            "Close election '" + current.getTitle() + "'?")) {
                        current.setActive(false);
                        try {
                            ElectionManager.saveElection(current);
                            setStatus("Election closed.");
                            refreshTable();
                        } catch (Exception ex) {
                            showError(OrganizerScreen.this, ex.getMessage());
                        }
                    }
                    stopCellEditing();
                });
                panel.add(b);
            } else {
                StyledButton b = primaryButton("Count Votes");
                b.setPreferredSize(new Dimension(110, 26));
                b.addActionListener(e -> { stopCellEditing(); handleCount(current); });
                panel.add(b);
            }
            return panel;
        }

        public Object getCellEditorValue() { return current; }
    }

    // ── VOTE COUNTING ────────────────────────────────────────────

    private void handleCount(Election election) {
        String pass = JOptionPane.showInputDialog(this,
                "Enter your password to decrypt and count votes:",
                "Count Votes — " + election.getTitle(),
                JOptionPane.PLAIN_MESSAGE);
        if (pass == null || pass.isEmpty()) return;

        setStatus("Decrypting and counting votes...");

        SwingWorker<String, Void> worker = new SwingWorker<>() {
            protected String doInBackground() throws Exception {
                return ReportService.countVotesAndGenerateReport(
                        election, loginResult.username, pass);
            }
            protected void done() {
                try {
                    showReportDialog(get(), election.getTitle());
                    refreshTable();
                    setStatus("Vote counting complete. Report generated.");
                } catch (Exception ex) {
                    showError(OrganizerScreen.this, "Error during counting: " + ex.getMessage());
                    setStatus("Error during vote counting.");
                }
            }
        };
        worker.execute();
    }

    private void showReportDialog(String report, String title) {
        JDialog dialog = new JDialog(
                (Frame) SwingUtilities.getWindowAncestor(this),
                "Results Report — " + title, true);
        dialog.setSize(640, 500);
        dialog.setLocationRelativeTo(this);
        dialog.getContentPane().setBackground(BG_DARK);

        JTextArea ta = new JTextArea(report);
        ta.setFont(FONT_MONO);
        ta.setBackground(BG_CARD);
        ta.setForeground(TEXT_PRIMARY);
        ta.setEditable(false);
        ta.setBorder(BorderFactory.createEmptyBorder(12, 16, 12, 16));

        JScrollPane scroll = new JScrollPane(ta);
        scroll.setBorder(BorderFactory.createEmptyBorder());

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        btnPanel.setBackground(BG_DARK);
        StyledButton closeBtn = ghostButton("Close");
        closeBtn.addActionListener(e -> dialog.dispose());
        btnPanel.add(closeBtn);

        dialog.add(scroll,   BorderLayout.CENTER);
        dialog.add(btnPanel, BorderLayout.SOUTH);
        dialog.setVisible(true);
    }

    // ── HELPERS ──────────────────────────────────────────────────

    private void setStatus(String text) {
        SwingUtilities.invokeLater(() -> statusBar.setText(text));
    }

    private void showLbl(JLabel l, String text, Color color) {
        SwingUtilities.invokeLater(() -> { l.setText(text); l.setForeground(color); });
    }

    private Date parseDate(String s) {
        if (s == null || s.isEmpty()) return null;
        try { return new SimpleDateFormat("dd.MM.yyyy HH:mm").parse(s); }
        catch (Exception e) { return null; }
    }
}
