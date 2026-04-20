package gui;

import user.UserRegistration;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.io.File;

import static gui.AppGUI.*;
import static gui.UIComponents.*;

/**
 * Startup screen — prompts for the CA password and validates it
 * before allowing access to the rest of the application.
 */
public class StartupScreen extends JPanel {

    private final AppGUI       app;
    private JPasswordField     caPasswordField;
    private JLabel             statusLabel;

    public StartupScreen(AppGUI app) {
        this.app = app;
        setBackground(BG_DARK);
        setLayout(new BorderLayout());
        buildUI();
    }

    private void buildUI() {

        // ── Left panel — branding ────────────────────────────────
        JPanel leftPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                GradientPaint gp = new GradientPaint(
                        0, 0,           new Color(20, 40, 70),
                        0, getHeight(), new Color(10, 20, 40));
                g2.setPaint(gp);
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.setColor(new Color(41, 128, 210, 30));
                g2.fill(new Ellipse2D.Float(-80, -80, 300, 300));
                g2.setColor(new Color(41, 128, 210, 15));
                g2.fill(new Ellipse2D.Float(50, getHeight()-200, 250, 250));
                g2.dispose();
            }
        };
        leftPanel.setPreferredSize(new Dimension(320, 0));
        leftPanel.setLayout(new GridBagLayout());

        JPanel brand = new JPanel();
        brand.setOpaque(false);
        brand.setLayout(new BoxLayout(brand, BoxLayout.Y_AXIS));
        brand.setBorder(BorderFactory.createEmptyBorder(0, 40, 0, 40));

        JLabel icon = new JLabel("🔐");
        icon.setFont(new Font("SansSerif", Font.PLAIN, 56));
        icon.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel appName = new JLabel("<html><b>E-Voting</b></html>");
        appName.setFont(new Font("SansSerif", Font.BOLD, 28));
        appName.setForeground(TEXT_PRIMARY);
        appName.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel subtitle = new JLabel("Secure Online Voting System");
        subtitle.setFont(FONT_BODY);
        subtitle.setForeground(ACCENT_LIGHT);
        subtitle.setAlignmentX(Component.LEFT_ALIGNMENT);

        brand.add(icon);
        brand.add(Box.createVerticalStrut(16));
        brand.add(appName);
        brand.add(Box.createVerticalStrut(4));
        brand.add(subtitle);
        brand.add(Box.createVerticalStrut(40));

        String[] features = {
            "🔑  RSA-2048 key pairs",
            "🛡️  AES-256/CBC vote encryption",
            "📜  X.509 digital certificates",
            "✅  SHA256withRSA signatures",
            "🏛️  Two-level CA hierarchy",
            "🔒  HmacSHA256 integrity checks"
        };
        for (String f : features) {
            JLabel fl = new JLabel(f);
            fl.setFont(FONT_SMALL);
            fl.setForeground(TEXT_SECONDARY);
            fl.setAlignmentX(Component.LEFT_ALIGNMENT);
            brand.add(fl);
            brand.add(Box.createVerticalStrut(8));
        }

        leftPanel.add(brand);

        // ── Right panel — form ───────────────────────────────────
        JPanel rightPanel = new JPanel(new GridBagLayout());
        rightPanel.setBackground(BG_DARK);

        JPanel form = new JPanel();
        form.setOpaque(false);
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));
        form.setMaximumSize(new Dimension(400, 9999));

        JLabel formTitle = titleLabel("Welcome");
        formTitle.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel formSub = bodyLabel("Enter the CA password to access the system");
        formSub.setAlignmentX(Component.LEFT_ALIGNMENT);

        form.add(formTitle);
        form.add(Box.createVerticalStrut(6));
        form.add(formSub);
        form.add(Box.createVerticalStrut(30));

        JLabel caLabel = headerLabel("CA Password");
        caLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        caPasswordField = styledPassword("Enter CA password...");
        caPasswordField.setAlignmentX(Component.LEFT_ALIGNMENT);
        caPasswordField.setMaximumSize(new Dimension(9999, 38));

        JLabel caHint = mutedLabel("Created during SetupPKI initialization");
        caHint.setAlignmentX(Component.LEFT_ALIGNMENT);

        statusLabel = new JLabel(" ");
        statusLabel.setFont(FONT_SMALL);
        statusLabel.setForeground(DANGER);
        statusLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        StyledButton unlockBtn = primaryButton("Unlock System");
        unlockBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
        unlockBtn.setMaximumSize(new Dimension(9999, 42));
        unlockBtn.setFont(FONT_HEADER);

        form.add(caLabel);
        form.add(Box.createVerticalStrut(8));
        form.add(caPasswordField);
        form.add(Box.createVerticalStrut(4));
        form.add(caHint);
        form.add(Box.createVerticalStrut(4));
        form.add(statusLabel);
        form.add(Box.createVerticalStrut(24));
        form.add(unlockBtn);
        form.add(Box.createVerticalStrut(16));

        JPanel sepRow = new JPanel(new GridLayout(1, 3, 8, 0));
        sepRow.setOpaque(false);
        sepRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        sepRow.setMaximumSize(new Dimension(9999, 20));
        sepRow.add(separator());
        JLabel orLbl = mutedLabel("OR");
        orLbl.setHorizontalAlignment(SwingConstants.CENTER);
        sepRow.add(orLbl);
        sepRow.add(separator());
        form.add(sepRow);
        form.add(Box.createVerticalStrut(16));

        JLabel hint = mutedLabel("No CA password? Run SetupPKI.java first.");
        hint.setAlignmentX(Component.LEFT_ALIGNMENT);
        form.add(hint);

        rightPanel.add(form, new GridBagConstraints());

        add(leftPanel,  BorderLayout.WEST);
        add(rightPanel, BorderLayout.CENTER);

        unlockBtn.addActionListener(e -> handleUnlock());
        caPasswordField.addActionListener(e -> handleUnlock());
    }

    private void handleUnlock() {
        String pass = new String(caPasswordField.getPassword());

        if (pass.length() < 4) {
            setStatus("Password must be at least 4 characters.", DANGER);
            return;
        }

        if (!new File("root_ca.p12").exists() ||
            !new File("organizer_ca.p12").exists() ||
            !new File("voter_ca.p12").exists()) {
            setStatus("CA files not found. Please run SetupPKI.java first.", DANGER);
            return;
        }

        try {
            utility.KeyStoreManager.loadKeyStore("organizer_ca.p12", pass);
        } catch (Exception ex) {
            setStatus("Incorrect CA password. Please try again.", DANGER);
            caPasswordField.setText("");
            return;
        }

        UserRegistration.setCAPassword(pass);
        setStatus("✓ System unlocked!", SUCCESS);

        Timer t = new Timer(600, ev -> app.showScreen(new MainScreen(app)));
        t.setRepeats(false);
        t.start();
    }

    private void setStatus(String text, Color color) {
        SwingUtilities.invokeLater(() -> {
            statusLabel.setText(text);
            statusLabel.setForeground(color);
        });
    }
}
