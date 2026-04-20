package gui;

import utility.LoginManager;
import user.UserRegistration;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.io.File;

/**
 * Glavna GUI aplikacija za e-Voting sistem.
 * Zamjenjuje MainMenu.java konzolnu aplikaciju.
 *
 * Pokretanje:
 *   1. SetupPKI mora biti pokrenut jednom (konzolno) prije ovoga
 *   2. Pokrenuti AppGUI.main()
 */
public class AppGUI extends JFrame {

    // ── Paleta boja ──────────────────────────────────────────────
    public static final Color BG_DARK       = new Color(18,  26,  38);
    public static final Color BG_PANEL      = new Color(26,  38,  56);
    public static final Color BG_CARD       = new Color(32,  48,  70);
    public static final Color ACCENT_BLUE   = new Color(41, 128, 210);
    public static final Color ACCENT_LIGHT  = new Color(72, 161, 255);
    public static final Color TEXT_PRIMARY  = new Color(220, 230, 245);
    public static final Color TEXT_SECONDARY= new Color(140, 160, 185);
    public static final Color TEXT_MUTED    = new Color(80,  100, 130);
    public static final Color SUCCESS       = new Color(39,  174,  96);
    public static final Color DANGER        = new Color(192,  57,  43);
    public static final Color WARNING       = new Color(211, 152,  34);
    public static final Color BORDER_COLOR  = new Color(45,  65,  95);

    // ── Fontovi ───────────────────────────────────────────────────
    public static final Font FONT_TITLE  = new Font("SansSerif", Font.BOLD,  22);
    public static final Font FONT_HEADER = new Font("SansSerif", Font.BOLD,  15);
    public static final Font FONT_BODY   = new Font("SansSerif", Font.PLAIN, 13);
    public static final Font FONT_SMALL  = new Font("SansSerif", Font.PLAIN, 11);
    public static final Font FONT_MONO   = new Font("Monospaced",Font.PLAIN, 12);

    private static AppGUI instance;
    private JPanel contentPanel;

    public static AppGUI getInstance() { return instance; }

    public AppGUI() {
        instance = this;
        setTitle("E-Voting Sistem — Sigurno Online Glasanje");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(900, 620);
        setMinimumSize(new Dimension(800, 550));
        setLocationRelativeTo(null);
        getContentPane().setBackground(BG_DARK);

        // Registruj Bouncy Castle
        if (java.security.Security.getProvider("BC") == null) {
            java.security.Security.addProvider(
                    new org.bouncycastle.jce.provider.BouncyCastleProvider());
        }

        // Postavi content panel (CardLayout za navigaciju između ekrana)
        contentPanel = new JPanel(new BorderLayout());
        contentPanel.setBackground(BG_DARK);
        setContentPane(contentPanel);

        // Pokaži početni ekran
        showScreen(new StartupScreen(this));
        setVisible(true);
    }

    /** Zamijeni trenutni ekran novim */
    public void showScreen(JPanel screen) {
        SwingUtilities.invokeLater(() -> {
            contentPanel.removeAll();
            contentPanel.add(screen, BorderLayout.CENTER);
            contentPanel.revalidate();
            contentPanel.repaint();
        });
    }

    public static void main(String[] args) {
        // Postavi sistem izgled
        try {
            UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
        } catch (Exception ignored) {}

        // Globalni UIManager stilovi
        UIManager.put("OptionPane.background",        BG_PANEL);
        UIManager.put("Panel.background",             BG_PANEL);
        UIManager.put("OptionPane.messageForeground", TEXT_PRIMARY);

        SwingUtilities.invokeLater(AppGUI::new);
    }
}
