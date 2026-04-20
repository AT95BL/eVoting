package gui;

import utility.LoginManager;
import user.UserRegistration;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;

import static gui.AppGUI.*;
import static gui.UIComponents.*;

/**
 * Glavni ekran — Login i Registracija tabovi.
 */
public class MainScreen extends JPanel {

    private final AppGUI app;
    private JPanel cardPanel;
    private CardLayout cardLayout;

    // Login polja
    private JTextField  loginP12Field;
    private JTextField  loginUserField;
    private JPasswordField loginPassField;
    private JLabel      loginStatusLabel;

    // Reg organizator polja
    private JTextField  regOrgNameField;
    private JTextField  regOrgIdField;
    private JPasswordField regOrgPassField;

    // Reg glasač polja
    private JTextField  regVoterNameField;
    private JTextField  regVoterUserField;
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
                BorderFactory.createMatteBorder(0,0,1,0, BORDER_COLOR),
                BorderFactory.createEmptyBorder(12, 24, 12, 24)));

        JLabel logo = new JLabel("🔐  E-Voting Sistem");
        logo.setFont(FONT_HEADER);
        logo.setForeground(TEXT_PRIMARY);

        JLabel version = mutedLabel("Kriptografija i računarska zaštita  |  ETF Banja Luka");
        topBar.add(logo,   BorderLayout.WEST);
        topBar.add(version,BorderLayout.EAST);
        add(topBar, BorderLayout.NORTH);

        // ── Centralni dio — tabovi ───────────────────────────────
        JPanel centerPanel = new JPanel(new GridBagLayout());
        centerPanel.setBackground(BG_DARK);

        JPanel mainCard = card();
        mainCard.setLayout(new BorderLayout());
        mainCard.setPreferredSize(new Dimension(620, 440));

        // Tab gumbi
        JPanel tabBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        tabBar.setOpaque(false);
        tabBar.setBorder(BorderFactory.createEmptyBorder(0, 0, 20, 0));

        JButton tabLogin   = makeTab("Prijava",           true);
        JButton tabRegOrg  = makeTab("Reg. Organizatora", false);
        JButton tabRegVot  = makeTab("Reg. Glasača",      false);

        tabBar.add(tabLogin);
        tabBar.add(Box.createHorizontalStrut(4));
        tabBar.add(tabRegOrg);
        tabBar.add(Box.createHorizontalStrut(4));
        tabBar.add(tabRegVot);

        // Card panel za sadržaj tabova
        cardLayout = new CardLayout();
        cardPanel  = new JPanel(cardLayout);
        cardPanel.setOpaque(false);

        cardPanel.add(buildLoginPanel(),   "login");
        cardPanel.add(buildRegOrgPanel(),  "regOrg");
        cardPanel.add(buildRegVotPanel(),  "regVot");

        mainCard.add(tabBar,    BorderLayout.NORTH);
        mainCard.add(cardPanel, BorderLayout.CENTER);

        // Tab akcije
        tabLogin .addActionListener(e -> { cardLayout.show(cardPanel,"login");  setTabActive(tabLogin, tabRegOrg, tabRegVot); });
        tabRegOrg.addActionListener(e -> { cardLayout.show(cardPanel,"regOrg"); setTabActive(tabRegOrg, tabLogin, tabRegVot); });
        tabRegVot.addActionListener(e -> { cardLayout.show(cardPanel,"regVot"); setTabActive(tabRegVot, tabLogin, tabRegOrg); });

        centerPanel.add(mainCard);
        add(centerPanel, BorderLayout.CENTER);

        // ── Bottom bar ───────────────────────────────────────────
        JPanel bottomBar = new JPanel(new BorderLayout());
        bottomBar.setBackground(BG_PANEL);
        bottomBar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1,0,0,0, BORDER_COLOR),
                BorderFactory.createEmptyBorder(8, 24, 8, 24)));
        bottomBar.add(mutedLabel("Svi glasovi su zaštićeni AES-256/CBC enkripcijom i RSA-2048 digitalnim potpisom"), BorderLayout.WEST);
        add(bottomBar, BorderLayout.SOUTH);
    }

    // ── LOGIN PANEL ─────────────────────────────────────────────

    private JPanel buildLoginPanel() {
        JPanel p = new JPanel();
        p.setOpaque(false);
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));

        JLabel title = headerLabel("Prijava u sistem");
        title.setAlignmentX(LEFT_ALIGNMENT);
        JLabel sub   = bodyLabel("Unesite vaš .p12 sertifikat i kredencijale");
        sub.setAlignmentX(LEFT_ALIGNMENT);

        p.add(title);
        p.add(Box.createVerticalStrut(4));
        p.add(sub);
        p.add(Box.createVerticalStrut(20));

        // .p12 polje sa browse dugmetom
        JPanel p12Row = new JPanel(new BorderLayout(8, 0));
        p12Row.setOpaque(false);
        p12Row.setAlignmentX(LEFT_ALIGNMENT);
        p12Row.setMaximumSize(new Dimension(9999, 38));

        loginP12Field = styledField("Putanja do .p12 fajla...");
        StyledButton browseBtn = ghostButton("Pretraži");
        browseBtn.setPreferredSize(new Dimension(90, 38));
        browseBtn.addActionListener(e -> {
            JFileChooser fc = new JFileChooser(".");
            fc.setFileFilter(new javax.swing.filechooser.FileFilter() {
                public boolean accept(java.io.File f) { return f.isDirectory() || f.getName().endsWith(".p12"); }
                public String getDescription() { return "PKCS#12 fajlovi (*.p12)"; }
            });
            if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                loginP12Field.setText(fc.getSelectedFile().getAbsolutePath());
                loginP12Field.setForeground(TEXT_PRIMARY);
            }
        });
        p12Row.add(loginP12Field, BorderLayout.CENTER);
        p12Row.add(browseBtn,    BorderLayout.EAST);

        loginUserField = styledField("Korisničko ime");
        loginUserField.setAlignmentX(LEFT_ALIGNMENT);
        loginUserField.setMaximumSize(new Dimension(9999, 38));

        loginPassField = styledPassword("Lozinka");
        loginPassField.setAlignmentX(LEFT_ALIGNMENT);
        loginPassField.setMaximumSize(new Dimension(9999, 38));

        loginStatusLabel = new JLabel(" ");
        loginStatusLabel.setFont(FONT_SMALL);
        loginStatusLabel.setAlignmentX(LEFT_ALIGNMENT);

        StyledButton loginBtn = primaryButton("Prijavi se");
        loginBtn.setAlignmentX(LEFT_ALIGNMENT);
        loginBtn.setMaximumSize(new Dimension(9999, 42));
        loginBtn.setFont(FONT_HEADER);
        loginBtn.addActionListener(e -> handleLogin());

        p.add(mutedLabel("Sertifikat (.p12 fajl)"));
        p.add(Box.createVerticalStrut(6));
        p.add(p12Row);
        p.add(Box.createVerticalStrut(12));
        p.add(mutedLabel("Korisničko ime"));
        p.add(Box.createVerticalStrut(6));
        p.add(loginUserField);
        p.add(Box.createVerticalStrut(12));
        p.add(mutedLabel("Lozinka"));
        p.add(Box.createVerticalStrut(6));
        p.add(loginPassField);
        p.add(Box.createVerticalStrut(8));
        p.add(loginStatusLabel);
        p.add(Box.createVerticalStrut(16));
        p.add(loginBtn);

        return p;
    }

    private void handleLogin() {
        String p12  = getFieldText(loginP12Field, "Putanja do .p12 fajla...");
        String user = getFieldText(loginUserField, "Korisničko ime");
        String pass = new String(loginPassField.getPassword());

        if (p12.isEmpty() || user.isEmpty() || pass.isEmpty()) {
            showStatus(loginStatusLabel, "Sva polja su obavezna.", DANGER);
            return;
        }

        showStatus(loginStatusLabel, "Validacija sertifikata...", WARNING);

        // Pokreni u background threadu da GUI ne zamrzne
        SwingWorker<LoginManager.UserLoginResult, Void> worker = new SwingWorker<>() {
            protected LoginManager.UserLoginResult doInBackground() {
                return LoginManager.login(p12, user, pass);
            }
            protected void done() {
                try {
                    LoginManager.UserLoginResult result = get();
                    if (result == null) {
                        showStatus(loginStatusLabel, "Prijava neuspješna. Provjeri podatke.", DANGER);
                    } else {
                        showStatus(loginStatusLabel, "✓ Prijava uspješna!", SUCCESS);
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
                    showStatus(loginStatusLabel, "Greška: " + ex.getMessage(), DANGER);
                }
            }
        };
        worker.execute();
    }

    // ── REG ORGANIZATOR PANEL ────────────────────────────────────

    private JPanel buildRegOrgPanel() {
        JPanel p = new JPanel();
        p.setOpaque(false);
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));

        JLabel title = headerLabel("Registracija Organizatora");
        title.setAlignmentX(LEFT_ALIGNMENT);
        p.add(title);
        p.add(Box.createVerticalStrut(4));
        JLabel sub = bodyLabel("Kreira digitalni sertifikat potpisan Organizator-CA");
        sub.setAlignmentX(LEFT_ALIGNMENT);
        p.add(sub);
        p.add(Box.createVerticalStrut(20));

        regOrgNameField = styledField("Naziv organizacije");
        regOrgNameField.setAlignmentX(LEFT_ALIGNMENT);
        regOrgNameField.setMaximumSize(new Dimension(9999, 38));

        regOrgIdField = styledField("ID broj (korisničko ime za prijavu)");
        regOrgIdField.setAlignmentX(LEFT_ALIGNMENT);
        regOrgIdField.setMaximumSize(new Dimension(9999, 38));

        regOrgPassField = styledPassword("Lozinka");
        regOrgPassField.setAlignmentX(LEFT_ALIGNMENT);
        regOrgPassField.setMaximumSize(new Dimension(9999, 38));

        JLabel regOrgStatus = new JLabel(" ");
        regOrgStatus.setFont(FONT_SMALL);
        regOrgStatus.setAlignmentX(LEFT_ALIGNMENT);

        StyledButton regBtn = successButton("Registruj Organizatora");
        regBtn.setAlignmentX(LEFT_ALIGNMENT);
        regBtn.setMaximumSize(new Dimension(9999, 42));
        regBtn.addActionListener(e -> {
            String name = getFieldText(regOrgNameField, "Naziv organizacije");
            String id   = getFieldText(regOrgIdField,   "ID broj (korisničko ime za prijavu)");
            String pass = new String(regOrgPassField.getPassword());
            if (name.isEmpty() || id.isEmpty() || pass.isEmpty()) {
                showStatus(regOrgStatus, "Sva polja su obavezna.", DANGER); return;
            }
            try {
                UserRegistration.register(name, id, pass, "ORGANIZER");
                showStatus(regOrgStatus, "✓ Organizator registrovan! Fajl: " + id + ".p12", SUCCESS);
                regOrgNameField.setText(""); regOrgIdField.setText(""); regOrgPassField.setText("");
            } catch (Exception ex) {
                showStatus(regOrgStatus, "Greška: " + ex.getMessage(), DANGER);
            }
        });

        p.add(mutedLabel("Naziv organizacije"));
        p.add(Box.createVerticalStrut(6));
        p.add(regOrgNameField);
        p.add(Box.createVerticalStrut(12));
        p.add(mutedLabel("ID broj (korisničko ime)"));
        p.add(Box.createVerticalStrut(6));
        p.add(regOrgIdField);
        p.add(Box.createVerticalStrut(12));
        p.add(mutedLabel("Lozinka"));
        p.add(Box.createVerticalStrut(6));
        p.add(regOrgPassField);
        p.add(Box.createVerticalStrut(8));
        p.add(regOrgStatus);
        p.add(Box.createVerticalStrut(16));
        p.add(regBtn);
        return p;
    }

    // ── REG GLASAČ PANEL ─────────────────────────────────────────

    private JPanel buildRegVotPanel() {
        JPanel p = new JPanel();
        p.setOpaque(false);
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));

        JLabel title = headerLabel("Registracija Glasača");
        title.setAlignmentX(LEFT_ALIGNMENT);
        p.add(title);
        p.add(Box.createVerticalStrut(4));
        JLabel sub = bodyLabel("Kreira digitalni sertifikat potpisan Glasač-CA");
        sub.setAlignmentX(LEFT_ALIGNMENT);
        p.add(sub);
        p.add(Box.createVerticalStrut(20));

        regVoterNameField = styledField("Ime i prezime");
        regVoterNameField.setAlignmentX(LEFT_ALIGNMENT);
        regVoterNameField.setMaximumSize(new Dimension(9999, 38));

        regVoterUserField = styledField("Korisničko ime");
        regVoterUserField.setAlignmentX(LEFT_ALIGNMENT);
        regVoterUserField.setMaximumSize(new Dimension(9999, 38));

        regVoterPassField = styledPassword("Lozinka");
        regVoterPassField.setAlignmentX(LEFT_ALIGNMENT);
        regVoterPassField.setMaximumSize(new Dimension(9999, 38));

        JLabel regVotStatus = new JLabel(" ");
        regVotStatus.setFont(FONT_SMALL);
        regVotStatus.setAlignmentX(LEFT_ALIGNMENT);

        StyledButton regBtn = successButton("Registruj Glasača");
        regBtn.setAlignmentX(LEFT_ALIGNMENT);
        regBtn.setMaximumSize(new Dimension(9999, 42));
        regBtn.addActionListener(e -> {
            String name = getFieldText(regVoterNameField, "Ime i prezime");
            String user = getFieldText(regVoterUserField, "Korisničko ime");
            String pass = new String(regVoterPassField.getPassword());
            if (name.isEmpty() || user.isEmpty() || pass.isEmpty()) {
                showStatus(regVotStatus, "Sva polja su obavezna.", DANGER); return;
            }
            try {
                UserRegistration.register(name, user, pass, "VOTER");
                showStatus(regVotStatus, "✓ Glasač registrovan! Fajl: " + user + ".p12", SUCCESS);
                regVoterNameField.setText(""); regVoterUserField.setText(""); regVoterPassField.setText("");
            } catch (Exception ex) {
                showStatus(regVotStatus, "Greška: " + ex.getMessage(), DANGER);
            }
        });

        p.add(mutedLabel("Ime i prezime"));
        p.add(Box.createVerticalStrut(6));
        p.add(regVoterNameField);
        p.add(Box.createVerticalStrut(12));
        p.add(mutedLabel("Korisničko ime"));
        p.add(Box.createVerticalStrut(6));
        p.add(regVoterUserField);
        p.add(Box.createVerticalStrut(12));
        p.add(mutedLabel("Lozinka"));
        p.add(Box.createVerticalStrut(6));
        p.add(regVoterPassField);
        p.add(Box.createVerticalStrut(8));
        p.add(regVotStatus);
        p.add(Box.createVerticalStrut(16));
        p.add(regBtn);
        return p;
    }

    // ── POMOĆNE METODE ───────────────────────────────────────────

    private JButton makeTab(String text, boolean active) {
        JButton b = new JButton(text) {
            boolean isActive = active;
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                if (isActive) {
                    g2.setColor(ACCENT_BLUE);
                    g2.fill(new RoundRectangle2D.Float(0,0,getWidth(),getHeight(),6,6));
                } else {
                    g2.setColor(new Color(45,65,95));
                    g2.fill(new RoundRectangle2D.Float(0,0,getWidth(),getHeight(),6,6));
                }
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
        b.setPreferredSize(new Dimension(160, 32));
        return b;
    }

    private void setTabActive(JButton active, JButton... others) {
        // Jednostavno repainto — u realnoj implementaciji bismo pratili state
        active.setForeground(Color.WHITE);
        for (JButton o : others) o.setForeground(TEXT_SECONDARY);
    }

    private void showStatus(JLabel label, String text, Color color) {
        SwingUtilities.invokeLater(() -> {
            label.setText(text);
            label.setForeground(color);
        });
    }
}
