package gui;

import utility.LoginManager;
import user.UserRegistration;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;

import static gui.AppGUI.*;
import static gui.UIComponents.*;

/**
 * Main screen with three tabs: Login, Register Organizer, Register Voter.
 */
public class MainScreen extends JPanel {

    private final AppGUI app;
    private JPanel     cardPanel;
    private CardLayout cardLayout;

    // Login fields
    private JTextField     loginP12Field;
    private JTextField     loginUserField;
    private JPasswordField loginPassField;
    private JLabel         loginStatusLabel;

    // Register Organizer fields
    private JTextField     regOrgNameField;
    private JTextField     regOrgIdField;
    private JPasswordField regOrgPassField;

    // Register Voter fields
    private JTextField     regVoterNameField;
    private JTextField     regVoterUserField;
    private JPasswordField regVoterPassField;

    public MainScreen(AppGUI app) {
        this.app = app;
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

        JLabel logo = new JLabel("🔐  E-Voting System");
        logo.setFont(FONT_HEADER);
        logo.setForeground(TEXT_PRIMARY);

        JLabel subtitle = mutedLabel("Cryptography and Computer Security  |  ETF Banja Luka");
        topBar.add(logo,    BorderLayout.WEST);
        topBar.add(subtitle,BorderLayout.EAST);
        add(topBar, BorderLayout.NORTH);

        // ── Center — tabbed card ─────────────────────────────────
        JPanel center = new JPanel(new GridBagLayout());
        center.setBackground(BG_DARK);

        JPanel mainCard = card();
        mainCard.setLayout(new BorderLayout());
        mainCard.setPreferredSize(new Dimension(620, 460));

        // Tab buttons
        JPanel tabBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        tabBar.setOpaque(false);
        tabBar.setBorder(BorderFactory.createEmptyBorder(0, 0, 20, 0));

        JButton tabLogin   = makeTab("Login",               true);
        JButton tabRegOrg  = makeTab("Register Organizer",  false);
        JButton tabRegVot  = makeTab("Register Voter",      false);

        tabBar.add(tabLogin);
        tabBar.add(Box.createHorizontalStrut(4));
        tabBar.add(tabRegOrg);
        tabBar.add(Box.createHorizontalStrut(4));
        tabBar.add(tabRegVot);

        cardLayout = new CardLayout();
        cardPanel  = new JPanel(cardLayout);
        cardPanel.setOpaque(false);

        cardPanel.add(buildLoginPanel(),   "login");
        cardPanel.add(buildRegOrgPanel(),  "regOrg");
        cardPanel.add(buildRegVotPanel(),  "regVot");

        mainCard.add(tabBar,   BorderLayout.NORTH);
        mainCard.add(cardPanel,BorderLayout.CENTER);

        tabLogin .addActionListener(e -> { cardLayout.show(cardPanel, "login");  styleTab(tabLogin,  tabRegOrg, tabRegVot); });
        tabRegOrg.addActionListener(e -> { cardLayout.show(cardPanel, "regOrg"); styleTab(tabRegOrg, tabLogin,  tabRegVot); });
        tabRegVot.addActionListener(e -> { cardLayout.show(cardPanel, "regVot"); styleTab(tabRegVot, tabLogin,  tabRegOrg); });

        center.add(mainCard);
        add(center, BorderLayout.CENTER);

        // ── Bottom bar ───────────────────────────────────────────
        JPanel bottomBar = new JPanel(new BorderLayout());
        bottomBar.setBackground(BG_PANEL);
        bottomBar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, BORDER_COLOR),
                BorderFactory.createEmptyBorder(8, 24, 8, 24)));
        bottomBar.add(mutedLabel(
                "All votes are protected with AES-256/CBC encryption and RSA-2048 digital signatures"),
                BorderLayout.WEST);
        add(bottomBar, BorderLayout.SOUTH);
    }

    // ── LOGIN PANEL ──────────────────────────────────────────────

    private JPanel buildLoginPanel() {
        JPanel p = new JPanel();
        p.setOpaque(false);
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));

        JLabel title = headerLabel("Sign in to the system");
        title.setAlignmentX(LEFT_ALIGNMENT);
        JLabel sub = bodyLabel("Provide your .p12 certificate file and credentials");
        sub.setAlignmentX(LEFT_ALIGNMENT);
        p.add(title);
        p.add(Box.createVerticalStrut(4));
        p.add(sub);
        p.add(Box.createVerticalStrut(20));

        // .p12 row with Browse button
        JPanel p12Row = new JPanel(new BorderLayout(8, 0));
        p12Row.setOpaque(false);
        p12Row.setAlignmentX(LEFT_ALIGNMENT);
        p12Row.setMaximumSize(new Dimension(9999, 38));

        loginP12Field = styledField("Path to .p12 file...");
        StyledButton browseBtn = ghostButton("Browse");
        browseBtn.setPreferredSize(new Dimension(80, 38));
        browseBtn.addActionListener(e -> {
            JFileChooser fc = new JFileChooser(".");
            fc.setFileFilter(new javax.swing.filechooser.FileFilter() {
                public boolean accept(java.io.File f) {
                    return f.isDirectory() || f.getName().endsWith(".p12");
                }
                public String getDescription() { return "PKCS#12 files (*.p12)"; }
            });
            if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                loginP12Field.setText(fc.getSelectedFile().getAbsolutePath());
                loginP12Field.setForeground(TEXT_PRIMARY);
            }
        });
        p12Row.add(loginP12Field, BorderLayout.CENTER);
        p12Row.add(browseBtn,    BorderLayout.EAST);

        loginUserField = styledField("Username");
        loginUserField.setAlignmentX(LEFT_ALIGNMENT);
        loginUserField.setMaximumSize(new Dimension(9999, 38));

        loginPassField = styledPassword("Password");
        loginPassField.setAlignmentX(LEFT_ALIGNMENT);
        loginPassField.setMaximumSize(new Dimension(9999, 38));

        loginStatusLabel = new JLabel(" ");
        loginStatusLabel.setFont(FONT_SMALL);
        loginStatusLabel.setAlignmentX(LEFT_ALIGNMENT);

        StyledButton loginBtn = primaryButton("Sign In");
        loginBtn.setAlignmentX(LEFT_ALIGNMENT);
        loginBtn.setMaximumSize(new Dimension(9999, 42));
        loginBtn.setFont(FONT_HEADER);
        loginBtn.addActionListener(e -> handleLogin());
        loginPassField.addActionListener(e -> handleLogin());

        p.add(mutedLabel("Certificate (.p12 file)"));
        p.add(Box.createVerticalStrut(6));
        p.add(p12Row);
        p.add(Box.createVerticalStrut(12));
        p.add(mutedLabel("Username"));
        p.add(Box.createVerticalStrut(6));
        p.add(loginUserField);
        p.add(Box.createVerticalStrut(12));
        p.add(mutedLabel("Password"));
        p.add(Box.createVerticalStrut(6));
        p.add(loginPassField);
        p.add(Box.createVerticalStrut(8));
        p.add(loginStatusLabel);
        p.add(Box.createVerticalStrut(16));
        p.add(loginBtn);
        return p;
    }

    private void handleLogin() {
        String p12  = getFieldText(loginP12Field, "Path to .p12 file...");
        String user = getFieldText(loginUserField, "Username");
        String pass = new String(loginPassField.getPassword());

        if (p12.isEmpty() || user.isEmpty() || pass.isEmpty()) {
            showStatus(loginStatusLabel, "All fields are required.", DANGER);
            return;
        }

        showStatus(loginStatusLabel, "Validating certificate...", WARNING);

        SwingWorker<LoginManager.UserLoginResult, Void> worker = new SwingWorker<>() {
            protected LoginManager.UserLoginResult doInBackground() {
                return LoginManager.login(p12, user, pass);
            }
            protected void done() {
                try {
                    LoginManager.UserLoginResult result = get();
                    if (result == null) {
                        showStatus(loginStatusLabel, "Login failed. Check your credentials.", DANGER);
                    } else {
                        showStatus(loginStatusLabel, "✓ Login successful!", SUCCESS);
                        Timer t = new Timer(500, ev -> {
                            if ("ORGANIZER".equals(result.userType)) {
                                app.showScreen(new OrganizerScreen(app, result));
                            } else {
                                app.showScreen(new VoterScreen(app, result));
                            }
                        });
                        t.setRepeats(false);
                        t.start();
                    }
                } catch (Exception ex) {
                    showStatus(loginStatusLabel, "Error: " + ex.getMessage(), DANGER);
                }
            }
        };
        worker.execute();
    }

    // ── REGISTER ORGANIZER PANEL ─────────────────────────────────

    private JPanel buildRegOrgPanel() {
        JPanel p = new JPanel();
        p.setOpaque(false);
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));

        JLabel title = headerLabel("Register Organizer");
        title.setAlignmentX(LEFT_ALIGNMENT);
        JLabel sub = bodyLabel("Creates a digital certificate signed by Organizer-CA");
        sub.setAlignmentX(LEFT_ALIGNMENT);
        p.add(title);
        p.add(Box.createVerticalStrut(4));
        p.add(sub);
        p.add(Box.createVerticalStrut(20));

        regOrgNameField = styledField("Organization name");
        regOrgNameField.setAlignmentX(LEFT_ALIGNMENT);
        regOrgNameField.setMaximumSize(new Dimension(9999, 38));

        regOrgIdField = styledField("ID number (used as username for login)");
        regOrgIdField.setAlignmentX(LEFT_ALIGNMENT);
        regOrgIdField.setMaximumSize(new Dimension(9999, 38));

        regOrgPassField = styledPassword("Password");
        regOrgPassField.setAlignmentX(LEFT_ALIGNMENT);
        regOrgPassField.setMaximumSize(new Dimension(9999, 38));

        JLabel regStatus = new JLabel(" ");
        regStatus.setFont(FONT_SMALL);
        regStatus.setAlignmentX(LEFT_ALIGNMENT);

        StyledButton regBtn = successButton("Register Organizer");
        regBtn.setAlignmentX(LEFT_ALIGNMENT);
        regBtn.setMaximumSize(new Dimension(9999, 42));
        regBtn.addActionListener(e -> {
            String name = getFieldText(regOrgNameField, "Organization name");
            String id   = getFieldText(regOrgIdField,   "ID number (used as username for login)");
            String pass = new String(regOrgPassField.getPassword());
            if (name.isEmpty() || id.isEmpty() || pass.isEmpty()) {
                showStatus(regStatus, "All fields are required.", DANGER); return;
            }
            try {
                UserRegistration.register(name, id, pass, "ORGANIZER");
                showStatus(regStatus, "✓ Organizer registered! File: " + id + ".p12", SUCCESS);
                regOrgNameField.setText(""); regOrgIdField.setText(""); regOrgPassField.setText("");
            } catch (Exception ex) {
                showStatus(regStatus, "Error: " + ex.getMessage(), DANGER);
            }
        });

        p.add(mutedLabel("Organization name"));      p.add(Box.createVerticalStrut(6));
        p.add(regOrgNameField);                       p.add(Box.createVerticalStrut(12));
        p.add(mutedLabel("ID number (username)"));   p.add(Box.createVerticalStrut(6));
        p.add(regOrgIdField);                         p.add(Box.createVerticalStrut(12));
        p.add(mutedLabel("Password"));               p.add(Box.createVerticalStrut(6));
        p.add(regOrgPassField);                       p.add(Box.createVerticalStrut(8));
        p.add(regStatus);                             p.add(Box.createVerticalStrut(16));
        p.add(regBtn);
        return p;
    }

    // ── REGISTER VOTER PANEL ─────────────────────────────────────

    private JPanel buildRegVotPanel() {
        JPanel p = new JPanel();
        p.setOpaque(false);
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));

        JLabel title = headerLabel("Register Voter");
        title.setAlignmentX(LEFT_ALIGNMENT);
        JLabel sub = bodyLabel("Creates a digital certificate signed by Voter-CA");
        sub.setAlignmentX(LEFT_ALIGNMENT);
        p.add(title);
        p.add(Box.createVerticalStrut(4));
        p.add(sub);
        p.add(Box.createVerticalStrut(20));

        regVoterNameField = styledField("Full name");
        regVoterNameField.setAlignmentX(LEFT_ALIGNMENT);
        regVoterNameField.setMaximumSize(new Dimension(9999, 38));

        regVoterUserField = styledField("Username");
        regVoterUserField.setAlignmentX(LEFT_ALIGNMENT);
        regVoterUserField.setMaximumSize(new Dimension(9999, 38));

        regVoterPassField = styledPassword("Password");
        regVoterPassField.setAlignmentX(LEFT_ALIGNMENT);
        regVoterPassField.setMaximumSize(new Dimension(9999, 38));

        JLabel regStatus = new JLabel(" ");
        regStatus.setFont(FONT_SMALL);
        regStatus.setAlignmentX(LEFT_ALIGNMENT);

        StyledButton regBtn = successButton("Register Voter");
        regBtn.setAlignmentX(LEFT_ALIGNMENT);
        regBtn.setMaximumSize(new Dimension(9999, 42));
        regBtn.addActionListener(e -> {
            String name = getFieldText(regVoterNameField, "Full name");
            String user = getFieldText(regVoterUserField, "Username");
            String pass = new String(regVoterPassField.getPassword());
            if (name.isEmpty() || user.isEmpty() || pass.isEmpty()) {
                showStatus(regStatus, "All fields are required.", DANGER); return;
            }
            try {
                UserRegistration.register(name, user, pass, "VOTER");
                showStatus(regStatus, "✓ Voter registered! File: " + user + ".p12", SUCCESS);
                regVoterNameField.setText(""); regVoterUserField.setText(""); regVoterPassField.setText("");
            } catch (Exception ex) {
                showStatus(regStatus, "Error: " + ex.getMessage(), DANGER);
            }
        });

        p.add(mutedLabel("Full name"));   p.add(Box.createVerticalStrut(6));
        p.add(regVoterNameField);          p.add(Box.createVerticalStrut(12));
        p.add(mutedLabel("Username"));    p.add(Box.createVerticalStrut(6));
        p.add(regVoterUserField);          p.add(Box.createVerticalStrut(12));
        p.add(mutedLabel("Password"));    p.add(Box.createVerticalStrut(6));
        p.add(regVoterPassField);          p.add(Box.createVerticalStrut(8));
        p.add(regStatus);                  p.add(Box.createVerticalStrut(16));
        p.add(regBtn);
        return p;
    }

    // ── HELPERS ──────────────────────────────────────────────────

    private JButton makeTab(String text, boolean active) {
        JButton b = new JButton(text) {
            boolean isActive = active;
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(isActive ? ACCENT_BLUE : new Color(45, 65, 95));
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 6, 6));
                g2.dispose();
                super.paintComponent(g);
            }
            public void setActive(boolean a) { isActive = a; repaint(); }
        };
        b.setFont(FONT_SMALL);
        b.setForeground(active ? Color.WHITE : TEXT_SECONDARY);
        b.setContentAreaFilled(false);
        b.setBorderPainted(false);
        b.setFocusPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setPreferredSize(new Dimension(170, 32));
        return b;
    }

    private void styleTab(JButton active, JButton... others) {
        active.setForeground(Color.WHITE);
        for (JButton o : others) o.setForeground(TEXT_SECONDARY);
    }

    private void showStatus(JLabel label, String text, Color color) {
        SwingUtilities.invokeLater(() -> { label.setText(text); label.setForeground(color); });
    }
}
