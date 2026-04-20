package gui;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.RoundRectangle2D;

import static gui.AppGUI.*;

/**
 * Kolekcija stilizovanih Swing komponenti koje se koriste kroz cijelu aplikaciju.
 */
public class UIComponents {

    // ════════════════════════════════════════════════════════
    //  STILIZOVANI GUMB
    // ════════════════════════════════════════════════════════

    public static class StyledButton extends JButton {
        private Color baseColor;
        private Color hoverColor;
        private boolean hovered = false;

        public StyledButton(String text, Color base) {
            super(text);
            this.baseColor  = base;
            this.hoverColor = base.brighter();
            setOpaque(false);
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setFont(FONT_BODY);
            setForeground(Color.WHITE);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setPreferredSize(new Dimension(180, 38));

            addMouseListener(new MouseAdapter() {
                public void mouseEntered(MouseEvent e) { hovered = true;  repaint(); }
                public void mouseExited (MouseEvent e) { hovered = false; repaint(); }
            });
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(hovered ? hoverColor : baseColor);
            g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 8, 8));
            super.paintComponent(g2);
            g2.dispose();
        }
    }

    /** Primarni plavi gumb */
    public static StyledButton primaryButton(String text) {
        return new StyledButton(text, ACCENT_BLUE);
    }

    /** Zeleni gumb (akcija uspjeha) */
    public static StyledButton successButton(String text) {
        return new StyledButton(text, SUCCESS);
    }

    /** Crveni gumb (destruktivna akcija) */
    public static StyledButton dangerButton(String text) {
        return new StyledButton(text, DANGER);
    }

    /** Sivi gumb (sekundarna akcija) */
    public static StyledButton ghostButton(String text) {
        return new StyledButton(text, new Color(55, 75, 105));
    }

    // ════════════════════════════════════════════════════════
    //  STILIZOVANO TEKSTUALNO POLJE
    // ════════════════════════════════════════════════════════

    public static JTextField styledField(String placeholder) {
        JTextField field = new JTextField() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(BG_DARK);
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 8, 8));
                super.paintComponent(g2);
                g2.dispose();
            }
        };
        field.setOpaque(false);
        field.setBackground(BG_DARK);
        field.setForeground(TEXT_PRIMARY);
        field.setCaretColor(ACCENT_LIGHT);
        field.setFont(FONT_BODY);
        field.setBorder(BorderFactory.createCompoundBorder(
                new RoundBorder(BORDER_COLOR, 8),
                BorderFactory.createEmptyBorder(8, 12, 8, 12)));
        field.setPreferredSize(new Dimension(0, 38));

        // Placeholder efekat
        field.setText(placeholder);
        field.setForeground(TEXT_MUTED);
        field.addFocusListener(new FocusAdapter() {
            public void focusGained(FocusEvent e) {
                if (field.getText().equals(placeholder)) {
                    field.setText("");
                    field.setForeground(TEXT_PRIMARY);
                }
            }
            public void focusLost(FocusEvent e) {
                if (field.getText().isEmpty()) {
                    field.setText(placeholder);
                    field.setForeground(TEXT_MUTED);
                }
            }
        });
        return field;
    }

    /** Pomoćna metoda — vraća pravi tekst (bez placeholder-a) */
    public static String getFieldText(JTextField field, String placeholder) {
        String t = field.getText();
        return t.equals(placeholder) ? "" : t;
    }

    // ════════════════════════════════════════════════════════
    //  STILIZOVANO POLJE ZA LOZINKU
    // ════════════════════════════════════════════════════════

    public static JPasswordField styledPassword(String placeholder) {
        JPasswordField field = new JPasswordField();
        field.setOpaque(true);
        field.setBackground(BG_DARK);
        field.setForeground(TEXT_PRIMARY);
        field.setCaretColor(ACCENT_LIGHT);
        field.setFont(FONT_BODY);
        field.setBorder(BorderFactory.createCompoundBorder(
                new RoundBorder(BORDER_COLOR, 8),
                BorderFactory.createEmptyBorder(8, 12, 8, 12)));
        field.setPreferredSize(new Dimension(0, 38));
        field.setEchoChar('●');
        return field;
    }

    // ════════════════════════════════════════════════════════
    //  LABEL STILOVI
    // ════════════════════════════════════════════════════════

    public static JLabel titleLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(FONT_TITLE);
        l.setForeground(TEXT_PRIMARY);
        return l;
    }

    public static JLabel headerLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(FONT_HEADER);
        l.setForeground(TEXT_PRIMARY);
        return l;
    }

    public static JLabel bodyLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(FONT_BODY);
        l.setForeground(TEXT_SECONDARY);
        return l;
    }

    public static JLabel mutedLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(FONT_SMALL);
        l.setForeground(TEXT_MUTED);
        return l;
    }

    public static JLabel statusLabel(String text, Color color) {
        JLabel l = new JLabel(text);
        l.setFont(FONT_BODY);
        l.setForeground(color);
        return l;
    }

    // ════════════════════════════════════════════════════════
    //  KARTICA (CARD) PANEL
    // ════════════════════════════════════════════════════════

    public static JPanel card() {
        JPanel p = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(BG_CARD);
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 12, 12));
                // Suptilna bordura
                g2.setColor(BORDER_COLOR);
                g2.setStroke(new BasicStroke(1f));
                g2.draw(new RoundRectangle2D.Float(0.5f, 0.5f, getWidth()-1, getHeight()-1, 12, 12));
                g2.dispose();
            }
        };
        p.setOpaque(false);
        p.setBorder(BorderFactory.createEmptyBorder(20, 24, 20, 24));
        return p;
    }

    // ════════════════════════════════════════════════════════
    //  SEPARATOR
    // ════════════════════════════════════════════════════════

    public static JSeparator separator() {
        JSeparator sep = new JSeparator();
        sep.setForeground(BORDER_COLOR);
        sep.setBackground(BORDER_COLOR);
        return sep;
    }

    // ════════════════════════════════════════════════════════
    //  BADGE (STATUS OZNAKA)
    // ════════════════════════════════════════════════════════

    public static JLabel badge(String text, Color bg) {
        JLabel l = new JLabel(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(bg.getRed(), bg.getGreen(), bg.getBlue(), 40));
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 20, 20));
                g2.dispose();
                super.paintComponent(g);
            }
        };
        l.setOpaque(false);
        l.setFont(FONT_SMALL);
        l.setForeground(bg);
        l.setBorder(BorderFactory.createEmptyBorder(3, 10, 3, 10));
        return l;
    }

    // ════════════════════════════════════════════════════════
    //  OKRUGLA BORDURA (pomoćna klasa)
    // ════════════════════════════════════════════════════════

    public static class RoundBorder extends AbstractBorder {
        private final Color color;
        private final int   radius;

        public RoundBorder(Color color, int radius) {
            this.color  = color;
            this.radius = radius;
        }

        @Override
        public void paintBorder(Component c, Graphics g, int x, int y, int w, int h) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(color);
            g2.setStroke(new BasicStroke(1f));
            g2.draw(new RoundRectangle2D.Float(x+0.5f, y+0.5f, w-1, h-1, radius, radius));
            g2.dispose();
        }

        @Override
        public Insets getBorderInsets(Component c) { return new Insets(1,1,1,1); }
    }

    // ════════════════════════════════════════════════════════
    //  DIJALOG ZA PORUKE
    // ════════════════════════════════════════════════════════

    public static void showSuccess(Component parent, String message) {
        JOptionPane.showMessageDialog(parent, message, "Uspjeh", JOptionPane.INFORMATION_MESSAGE);
    }

    public static void showError(Component parent, String message) {
        JOptionPane.showMessageDialog(parent, message, "Greška", JOptionPane.ERROR_MESSAGE);
    }

    public static boolean confirm(Component parent, String message) {
        return JOptionPane.showConfirmDialog(parent, message, "Potvrda",
                JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION;
    }
}
