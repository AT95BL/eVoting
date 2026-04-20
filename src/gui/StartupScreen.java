package gui;

import user.UserRegistration;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.io.File;

import static gui.AppGUI.*;
import static gui.UIComponents.*;

/**
 * Početni ekran aplikacije.
 * Traži CA lozinku, a zatim nudi Login / Registracija.
 */
public class StartupScreen extends JPanel {

    private final AppGUI app;
    private JPasswordField caPasswordField;
    private JLabel statusLabel;
    private boolean caUnlocked = false;

    public StartupScreen(AppGUI app) {
        this.app = app;
        setBackground(BG_DARK);
        setLayout(new BorderLayout());
        buildUI();
    }

    private void buildUI() {
        // ── Lijeva strana — branding ─────────────────────────────
        JPanel leftPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                // Gradijent pozadina
                GradientPaint gp = new GradientPaint(
                        0, 0,          new Color(20, 40, 70),
                        0, getHeight(),new Color(10, 20, 40));
                g2.setPaint(gp);
                g2.fillRect(0, 0, getWidth(), getHeight());
                // Dekorativni krug
                g2.setColor(new Color(41, 128, 210, 30));
                g2.fill(new Ellipse2D.Float(-80, -80, 300, 300));
                g2.setColor(new Color(41, 128, 210, 15));
                g2.fill(new Ellipse2D.Float(50, getHeight()-200, 250, 250));
                g2.dispose();
            }
        };
        leftPanel.setPreferredSize(new Dimension(320, 0));
        leftPanel.setLayout(new GridBagLayout());

        JPanel brandContent = new JPanel();
        brandContent.setOpaque(false);
        brandContent.setLayout(new BoxLayout(brandContent, BoxLayout.Y_AXIS));
        brandContent.setBorder(BorderFactory.createEmptyBorder(0, 40, 0, 40));

        // Ikona / logo
        JLabel icon = new JLabel("🔐") {
            { setFont(new Font("SansSerif", Font.PLAIN, 56)); }
        };
        icon.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel appName = new JLabel("<html><b>E-Voting</b></html>");
        appName.setFont(new Font("SansSerif", Font.BOLD, 28));
        appName.setForeground(TEXT_PRIMARY);
        appName.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel subtitle = new JLabel("Sigurno Online Glasanje");
        subtitle.setFont(FONT_BODY);
        subtitle.setForeground(ACCENT_LIGHT);
        subtitle.setAlignmentX(Component.LEFT_ALIGNMENT);

        brandContent.add(icon);
        brandContent.add(Box.createVerticalStrut(16));
        brandContent.add(appName);
        brandContent.add(Box.createVerticalStrut(4));
        brandContent.add(subtitle);
        brandContent.add(Box.createVerticalStrut(40));

        // Bullet points sa feature-ima
        String[] features = {
            "🔑  RSA-2048 kriptografija",
            "🛡️  AES-256 enkripcija glasova",
            "📜  X.509 digitalni sertifikati",
            "✅  SHA256withRSA potpis",
            "🏛️  CA hijerarhija u 2 nivoa"
        };
        for (String f : features) {
            JLabel fl = new JLabel(f);
            fl.setFont(FONT_SMALL);
            fl.setForeground(TEXT_SECONDARY);
            fl.setAlignmentX(Component.LEFT_ALIGNMENT);
            brandContent.add(fl);
            brandContent.add(Box.createVerticalStrut(8));
        }

        leftPanel.add(brandContent);

        // ── Desna strana — forma ─────────────────────────────────
        JPanel rightPanel = new JPanel(new GridBagLayout());
        rightPanel.setBackground(BG_DARK);

        JPanel formWrapper = new JPanel();
        formWrapper.setOpaque(false);
        formWrapper.setLayout(new BoxLayout(formWrapper, BoxLayout.Y_AXIS));
        formWrapper.setMaximumSize(new Dimension(400, 9999));

        // Naslov forme
        JLabel formTitle = titleLabel("Dobrodošli");
        formTitle.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel formSub = bodyLabel("Unesite CA lozinku za pristup sistemu");
        formSub.setAlignmentX(Component.LEFT_ALIGNMENT);

        formWrapper.add(formTitle);
        formWrapper.add(Box.createVerticalStrut(6));
        formWrapper.add(formSub);
        formWrapper.add(Box.createVerticalStrut(30));

        // CA lozinka
        JLabel caLabel = headerLabel("CA Lozinka");
        caLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        caPasswordField = styledPassword("Unesite CA lozinku...");
        caPasswordField.setAlignmentX(Component.LEFT_ALIGNMENT);
        caPasswordField.setMaximumSize(new Dimension(9999, 38));

        JLabel caHint = mutedLabel("Lozinka kreirana pri SetupPKI inicijalizaciji");
        caHint.setAlignmentX(Component.LEFT_ALIGNMENT);

        statusLabel = new JLabel(" ");
        statusLabel.setFont(FONT_SMALL);
        statusLabel.setForeground(DANGER);
        statusLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        formWrapper.add(caLabel);
        formWrapper.add(Box.createVerticalStrut(8));
        formWrapper.add(caPasswordField);
        formWrapper.add(Box.createVerticalStrut(4));
        formWrapper.add(caHint);
        formWrapper.add(Box.createVerticalStrut(4));
        formWrapper.add(statusLabel);
        formWrapper.add(Box.createVerticalStrut(24));

        // Gumbi
        StyledButton unlockBtn = primaryButton("Otključaj sistem");
        unlockBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
        unlockBtn.setMaximumSize(new Dimension(9999, 42));
        unlockBtn.setFont(FONT_HEADER);

        formWrapper.add(unlockBtn);
        formWrapper.add(Box.createVerticalStrut(16));

        // Separator
        JPanel sepPanel = new JPanel(new GridLayout(1, 3, 8, 0));
        sepPanel.setOpaque(false);
        sepPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        sepPanel.setMaximumSize(new Dimension(9999, 20));
        sepPanel.add(separator());
        JLabel orLabel = mutedLabel("ILI");
        orLabel.setHorizontalAlignment(SwingConstants.CENTER);
        sepPanel.add(orLabel);
        sepPanel.add(separator());
        formWrapper.add(sepPanel);
        formWrapper.add(Box.createVerticalStrut(16));

        // Direktan pristup (ako su CA fajlovi već postavljeni)
        JLabel noCALabel = mutedLabel("Nemate CA lozinku? Pokrenite SetupPKI.java");
        noCALabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        formWrapper.add(noCALabel);

        rightPanel.add(formWrapper, new GridBagConstraints());

        // Dodaj panele
        add(leftPanel,  BorderLayout.WEST);
        add(rightPanel, BorderLayout.CENTER);

        // ── Akcije ──────────────────────────────────────────────
        unlockBtn.addActionListener(e -> handleUnlock());
        caPasswordField.addActionListener(e -> handleUnlock());
    }

    private void handleUnlock() {
        String pass = new String(caPasswordField.getPassword());
        if (pass.length() < 4) {
            statusLabel.setText("Lozinka mora imati najmanje 4 znaka.");
            return;
        }

        // Provjeri da li postoje CA fajlovi
        if (!new File("root_ca.p12").exists() ||
            !new File("organizer_ca.p12").exists() ||
            !new File("voter_ca.p12").exists()) {
            statusLabel.setText("CA fajlovi ne postoje. Pokrenite SetupPKI.java prvo.");
            return;
        }

        // Pokušaj otvoriti jedan CA fajl sa datom lozinkom
        try {
            utility.KeyStoreManager.loadKeyStore("organizer_ca.p12", pass);
        } catch (Exception ex) {
            statusLabel.setText("Neispravna CA lozinka. Pokušajte ponovo.");
            caPasswordField.setText("");
            return;
        }

        // Lozinka je ispravna
        UserRegistration.setCAPassword(pass);
        statusLabel.setForeground(SUCCESS);
        statusLabel.setText("✓ Sistem otključan!");

        // Prijeđi na glavni ekran nakon kratke pauze
        Timer t = new Timer(600, ev -> app.showScreen(new MainScreen(app)));
        t.setRepeats(false);
        t.start();
    }
}
