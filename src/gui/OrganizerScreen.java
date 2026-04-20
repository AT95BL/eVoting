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
 * Panel za organizatora — kreiranje glasanja, pregled i brojanje.
 */
public class OrganizerScreen extends JPanel {

    private final AppGUI                      app;
    private final LoginManager.UserLoginResult loginResult;

    private JTable       electionsTable;
    private DefaultTableModel tableModel;
    private JLabel       statusBar;

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
                BorderFactory.createMatteBorder(0,0,1,0, BORDER_COLOR),
                BorderFactory.createEmptyBorder(12, 24, 12, 24)));

        JLabel logo = new JLabel("🏛️  Organizatorski Panel");
        logo.setFont(FONT_HEADER);
        logo.setForeground(TEXT_PRIMARY);

        JPanel userInfo = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        userInfo.setOpaque(false);
        userInfo.add(badge("ORGANIZATOR", ACCENT_BLUE));
        userInfo.add(bodyLabel(loginResult.username));

        StyledButton logoutBtn = dangerButton("Odjava");
        logoutBtn.setPreferredSize(new Dimension(90, 30));
        logoutBtn.addActionListener(e -> app.showScreen(new MainScreen(app)));
        userInfo.add(logoutBtn);

        topBar.add(logo,    BorderLayout.WEST);
        topBar.add(userInfo,BorderLayout.EAST);
        add(topBar, BorderLayout.NORTH);

        // ── Glavni sadržaj ───────────────────────────────────────
        JPanel content = new JPanel(new BorderLayout(16, 0));
        content.setBackground(BG_DARK);
        content.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // Lijeva strana — forma za kreiranje glasanja
        content.add(buildCreatePanel(), BorderLayout.WEST);

        // Desna strana — lista glasanja
        content.add(buildListPanel(), BorderLayout.CENTER);

        add(content, BorderLayout.CENTER);

        // ── Status bar ───────────────────────────────────────────
        JPanel sb = new JPanel(new BorderLayout());
        sb.setBackground(BG_PANEL);
        sb.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1,0,0,0, BORDER_COLOR),
                BorderFactory.createEmptyBorder(6, 24, 6, 24)));
        statusBar = new JLabel("Sistem spreman.");
        statusBar.setFont(FONT_SMALL);
        statusBar.setForeground(TEXT_MUTED);
        sb.add(statusBar, BorderLayout.WEST);
        add(sb, BorderLayout.SOUTH);

        refreshTable();
    }

    // ── FORMA ZA KREIRANJE GLASANJA ──────────────────────────────

    private JTextField  titleField, descField;
    private JTextField  startField, endField;
    private JTextField[] candidateFields = new JTextField[5];

    private JPanel buildCreatePanel() {
        JPanel outer = card();
        outer.setPreferredSize(new Dimension(280, 0));
        outer.setLayout(new BoxLayout(outer, BoxLayout.Y_AXIS));

        JLabel title = headerLabel("Novo glasanje");
        title.setAlignmentX(LEFT_ALIGNMENT);
        outer.add(title);
        outer.add(Box.createVerticalStrut(16));

        titleField = styledField("Naslov glasanja");
        titleField.setAlignmentX(LEFT_ALIGNMENT);
        titleField.setMaximumSize(new Dimension(9999, 38));

        descField = styledField("Opis glasanja");
        descField.setAlignmentX(LEFT_ALIGNMENT);
        descField.setMaximumSize(new Dimension(9999, 38));

        startField = styledField("Početak: dd.MM.yyyy HH:mm");
        startField.setAlignmentX(LEFT_ALIGNMENT);
        startField.setMaximumSize(new Dimension(9999, 38));

        endField = styledField("Kraj: dd.MM.yyyy HH:mm");
        endField.setAlignmentX(LEFT_ALIGNMENT);
        endField.setMaximumSize(new Dimension(9999, 38));

        outer.add(mutedLabel("Naslov")); outer.add(Box.createVerticalStrut(4));
        outer.add(titleField);           outer.add(Box.createVerticalStrut(10));
        outer.add(mutedLabel("Opis"));   outer.add(Box.createVerticalStrut(4));
        outer.add(descField);            outer.add(Box.createVerticalStrut(10));
        outer.add(mutedLabel("Početak (prazno = odmah)")); outer.add(Box.createVerticalStrut(4));
        outer.add(startField);           outer.add(Box.createVerticalStrut(10));
        outer.add(mutedLabel("Kraj (prazno = bez ogr.)")); outer.add(Box.createVerticalStrut(4));
        outer.add(endField);             outer.add(Box.createVerticalStrut(14));

        outer.add(mutedLabel("Kandidati (min 2, max 5)"));
        outer.add(Box.createVerticalStrut(6));

        for (int i = 0; i < 5; i++) {
            candidateFields[i] = styledField("Kandidat " + (i+1) + (i < 2 ? " *" : " (opciono)"));
            candidateFields[i].setAlignmentX(LEFT_ALIGNMENT);
            candidateFields[i].setMaximumSize(new Dimension(9999, 34));
            outer.add(candidateFields[i]);
            outer.add(Box.createVerticalStrut(6));
        }

        outer.add(Box.createVerticalStrut(10));

        JLabel createStatus = new JLabel(" ");
        createStatus.setFont(FONT_SMALL);
        createStatus.setAlignmentX(LEFT_ALIGNMENT);
        outer.add(createStatus);
        outer.add(Box.createVerticalStrut(6));

        StyledButton createBtn = primaryButton("Kreiraj glasanje");
        createBtn.setAlignmentX(LEFT_ALIGNMENT);
        createBtn.setMaximumSize(new Dimension(9999, 40));
        createBtn.addActionListener(e -> handleCreate(createStatus));
        outer.add(createBtn);

        return outer;
    }

    private void handleCreate(JLabel statusLbl) {
        String t    = getFieldText(titleField, "Naslov glasanja");
        String d    = getFieldText(descField,  "Opis glasanja");
        String sStr = getFieldText(startField, "Početak: dd.MM.yyyy HH:mm");
        String eStr = getFieldText(endField,   "Kraj: dd.MM.yyyy HH:mm");

        if (t.isEmpty()) { showLbl(statusLbl, "Naslov je obavezan.", DANGER); return; }

        List<String> candidates = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            String placeholder = "Kandidat " + (i+1) + (i < 2 ? " *" : " (opciono)");
            String c = getFieldText(candidateFields[i], placeholder);
            if (!c.isEmpty()) candidates.add(c);
        }
        if (candidates.size() < 2) { showLbl(statusLbl, "Potrebna su min 2 kandidata.", DANGER); return; }

        Date start = parseDate(sStr);
        Date end   = parseDate(eStr);

        try {
            Election el = new Election(t, d, start, end, candidates, loginResult.username);
            ElectionManager.saveElection(el);
            showLbl(statusLbl, "✓ Glasanje kreirano!", SUCCESS);
            clearCreateForm();
            refreshTable();
            setStatus("Glasanje '" + t + "' uspješno kreirano.");
        } catch (Exception ex) {
            showLbl(statusLbl, "Greška: " + ex.getMessage(), DANGER);
        }
    }

    private void clearCreateForm() {
        titleField.setText(""); descField.setText("");
        startField.setText(""); endField.setText("");
        for (JTextField f : candidateFields) f.setText("");
    }

    // ── LISTA GLASANJA ───────────────────────────────────────────

    private JPanel buildListPanel() {
        JPanel outer = card();
        outer.setLayout(new BorderLayout(0, 12));

        // Header liste
        JPanel listHeader = new JPanel(new BorderLayout());
        listHeader.setOpaque(false);
        JLabel listTitle = headerLabel("Moja glasanja");
        StyledButton refreshBtn = ghostButton("↺ Osvježi");
        refreshBtn.setPreferredSize(new Dimension(100, 30));
        refreshBtn.addActionListener(e -> refreshTable());
        listHeader.add(listTitle,  BorderLayout.WEST);
        listHeader.add(refreshBtn, BorderLayout.EAST);

        // Tabela
        String[] cols = {"Naslov", "Status", "Glasova", "HMAC", "Akcija"};
        tableModel = new DefaultTableModel(cols, 0) {
            public boolean isCellEditable(int r, int c) { return c == 4; }
        };

        electionsTable = new JTable(tableModel) {
            @Override
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
                BorderFactory.createMatteBorder(0,0,1,0, BORDER_COLOR));
        electionsTable.setSelectionBackground(new Color(41, 128, 210, 60));

        // Akcija kolona — gumbi
        electionsTable.getColumnModel().getColumn(4).setCellRenderer(new ActionCellRenderer());
        electionsTable.getColumnModel().getColumn(4).setCellEditor(new ActionCellEditor());
        electionsTable.getColumnModel().getColumn(4).setPreferredWidth(200);
        electionsTable.getColumnModel().getColumn(0).setPreferredWidth(220);
        electionsTable.getColumnModel().getColumn(1).setPreferredWidth(80);
        electionsTable.getColumnModel().getColumn(2).setPreferredWidth(60);
        electionsTable.getColumnModel().getColumn(3).setPreferredWidth(60);

        JScrollPane scroll = new JScrollPane(electionsTable);
        scroll.setBackground(BG_CARD);
        scroll.getViewport().setBackground(BG_CARD);
        scroll.setBorder(new RoundBorder(BORDER_COLOR, 8));

        outer.add(listHeader, BorderLayout.NORTH);
        outer.add(scroll,     BorderLayout.CENTER);

        return outer;
    }

    void refreshTable() {
        SwingUtilities.invokeLater(() -> {
            tableModel.setRowCount(0);
            List<Election> elections = ElectionManager.loadAllElections();
            for (Election e : elections) {
                boolean hmacOk = ElectionManager.verifyElectionHMAC(e);
                int votes = VoteStorageManager.countVotes(e.getTitle());
                String status = e.isCurrentlyActive() ? "AKTIVNO" : "ZATVORENO";
                tableModel.addRow(new Object[]{
                    e.getTitle(), status, votes,
                    hmacOk ? "✓ OK" : "✗ GREŠKA",
                    e  // cijeli Election objekat za akcije
                });
            }
        });
    }

    // ── ACTION CELL RENDERER/EDITOR ──────────────────────────────

    class ActionCellRenderer implements TableCellRenderer {
        public Component getTableCellRendererComponent(JTable t, Object v, boolean sel,
                boolean focus, int row, int col) {
            JPanel p = new JPanel(new FlowLayout(FlowLayout.CENTER, 4, 4));
            p.setBackground(row % 2 == 0 ? BG_CARD : BG_PANEL);
            Election e = (Election) v;
            if (e.isCurrentlyActive()) {
                StyledButton b = dangerButton("Zatvori");
                b.setPreferredSize(new Dimension(80, 26));
                p.add(b);
            } else {
                StyledButton b = primaryButton("Broji glasove");
                b.setPreferredSize(new Dimension(110, 26));
                p.add(b);
            }
            return p;
        }
    }

    class ActionCellEditor extends DefaultCellEditor {
        private Election currentElection;
        private JPanel panel;

        public ActionCellEditor() {
            super(new JCheckBox());
            setClickCountToStart(1);
        }

        public Component getTableCellEditorComponent(JTable t, Object v, boolean sel, int row, int col) {
            currentElection = (Election) v;
            panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 4, 4));
            panel.setBackground(row % 2 == 0 ? BG_CARD : BG_PANEL);

            if (currentElection.isCurrentlyActive()) {
                StyledButton b = dangerButton("Zatvori");
                b.setPreferredSize(new Dimension(80, 26));
                b.addActionListener(e -> {
                    if (confirm(OrganizerScreen.this,
                            "Zatvoriti glasanje '" + currentElection.getTitle() + "'?")) {
                        currentElection.setActive(false);
                        try {
                            ElectionManager.saveElection(currentElection);
                            setStatus("Glasanje zatvoreno.");
                            refreshTable();
                        } catch (Exception ex) {
                            showError(OrganizerScreen.this, ex.getMessage());
                        }
                    }
                    stopCellEditing();
                });
                panel.add(b);
            } else {
                StyledButton b = primaryButton("Broji glasove");
                b.setPreferredSize(new Dimension(110, 26));
                b.addActionListener(e -> {
                    stopCellEditing();
                    handleCountVotes(currentElection);
                });
                panel.add(b);
            }
            return panel;
        }

        public Object getCellEditorValue() { return currentElection; }
    }

    // ── BROJANJE GLASOVA ─────────────────────────────────────────

    private void handleCountVotes(Election election) {
        String pass = JOptionPane.showInputDialog(this,
                "Unesite vašu lozinku za dekriptovanje glasova:",
                "Brojanje glasova — " + election.getTitle(),
                JOptionPane.PLAIN_MESSAGE);
        if (pass == null || pass.isEmpty()) return;

        setStatus("Dekriptovanje i prebrojavanje glasova...");

        SwingWorker<String, Void> worker = new SwingWorker<>() {
            protected String doInBackground() throws Exception {
                return ReportService.countVotesAndGenerateReport(
                        election, loginResult.username, pass);
            }
            protected void done() {
                try {
                    String report = get();
                    // Prikazi izvještaj u posebnom prozoru
                    showReportDialog(report, election.getTitle());
                    refreshTable();
                    setStatus("Brojanje završeno. Izvještaj generisan.");
                } catch (Exception ex) {
                    showError(OrganizerScreen.this,
                            "Greška pri brojanju: " + ex.getMessage());
                    setStatus("Greška pri brojanju.");
                }
            }
        };
        worker.execute();
    }

    private void showReportDialog(String report, String title) {
        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this),
                "Izvještaj — " + title, true);
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
        StyledButton closeBtn = ghostButton("Zatvori");
        closeBtn.addActionListener(e -> dialog.dispose());
        btnPanel.add(closeBtn);

        dialog.add(scroll,   BorderLayout.CENTER);
        dialog.add(btnPanel, BorderLayout.SOUTH);
        dialog.setVisible(true);
    }

    // ── POMOĆNE METODE ───────────────────────────────────────────

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
