package gui;

import model.Election;
import model.EncryptedVote;
import utility.*;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.io.FileInputStream;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import static gui.AppGUI.*;
import static gui.UIComponents.*;

/**
 * Panel za glasača — pregled glasanja, glasanje i verifikacija.
 */
public class VoterScreen extends JPanel {

    private final AppGUI                      app;
    private final LoginManager.UserLoginResult loginResult;

    private JPanel electionsListPanel;
    private JLabel statusBar;

    public VoterScreen(AppGUI app, LoginManager.UserLoginResult loginResult) {
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

        JLabel logo = new JLabel("🗳️  Glasački Panel");
        logo.setFont(FONT_HEADER);
        logo.setForeground(TEXT_PRIMARY);

        JPanel userInfo = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        userInfo.setOpaque(false);
        userInfo.add(badge("GLASAČ", SUCCESS));
        userInfo.add(bodyLabel(loginResult.username));

        StyledButton logoutBtn = dangerButton("Odjava");
        logoutBtn.setPreferredSize(new Dimension(90, 30));
        logoutBtn.addActionListener(e -> app.showScreen(new MainScreen(app)));
        userInfo.add(logoutBtn);

        topBar.add(logo,    BorderLayout.WEST);
        topBar.add(userInfo,BorderLayout.EAST);
        add(topBar, BorderLayout.NORTH);

        // ── Sadržaj ──────────────────────────────────────────────
        JPanel content = new JPanel(new BorderLayout(0, 16));
        content.setBackground(BG_DARK);
        content.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // Info kartica
        JPanel infoCard = card();
        infoCard.setLayout(new BorderLayout(16, 0));
        infoCard.setPreferredSize(new Dimension(0, 80));

        JPanel infoText = new JPanel();
        infoText.setOpaque(false);
        infoText.setLayout(new BoxLayout(infoText, BoxLayout.Y_AXIS));
        infoText.add(headerLabel("Dobrodošli, " + loginResult.username));
        infoText.add(Box.createVerticalStrut(4));
        infoText.add(bodyLabel("Vaš glas je zaštićen AES-256/CBC enkripcijom i RSA digitalnim potpisom."));

        // Refresh gumb
        StyledButton refreshBtn = ghostButton("↺ Osvježi liste");
        refreshBtn.setPreferredSize(new Dimension(130, 34));
        refreshBtn.addActionListener(e -> refreshElections());

        infoCard.add(infoText,   BorderLayout.CENTER);
        infoCard.add(refreshBtn, BorderLayout.EAST);
        content.add(infoCard, BorderLayout.NORTH);

        // Lista glasanja
        electionsListPanel = new JPanel();
        electionsListPanel.setLayout(new BoxLayout(electionsListPanel, BoxLayout.Y_AXIS));
        electionsListPanel.setOpaque(false);

        JScrollPane scroll = new JScrollPane(electionsListPanel);
        scroll.setBackground(BG_DARK);
        scroll.getViewport().setBackground(BG_DARK);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        content.add(scroll, BorderLayout.CENTER);

        add(content, BorderLayout.CENTER);

        // ── Status bar ───────────────────────────────────────────
        JPanel sb = new JPanel(new BorderLayout());
        sb.setBackground(BG_PANEL);
        sb.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1,0,0,0, BORDER_COLOR),
                BorderFactory.createEmptyBorder(6, 24, 6, 24)));
        statusBar = new JLabel("Pregledajte dostupna glasanja.");
        statusBar.setFont(FONT_SMALL);
        statusBar.setForeground(TEXT_MUTED);
        sb.add(statusBar, BorderLayout.WEST);
        add(sb, BorderLayout.SOUTH);

        refreshElections();
    }

    // ── LISTA GLASANJA ───────────────────────────────────────────

    private void refreshElections() {
        SwingUtilities.invokeLater(() -> {
            electionsListPanel.removeAll();

            List<Election> all = ElectionManager.loadAllElections();
            List<Election> active = all.stream()
                    .filter(Election::isCurrentlyActive)
                    .collect(Collectors.toList());

            if (active.isEmpty()) {
                JPanel empty = new JPanel(new GridBagLayout());
                empty.setOpaque(false);
                empty.setPreferredSize(new Dimension(0, 200));
                JLabel msg = bodyLabel("Trenutno nema aktivnih glasanja.");
                empty.add(msg);
                electionsListPanel.add(empty);
            } else {
                for (Election e : active) {
                    electionsListPanel.add(buildElectionCard(e));
                    electionsListPanel.add(Box.createVerticalStrut(12));
                }
            }

            // Sekcija za verifikaciju
            electionsListPanel.add(Box.createVerticalStrut(8));
            electionsListPanel.add(buildVerifySection(all));

            electionsListPanel.revalidate();
            electionsListPanel.repaint();
        });
    }

    private JPanel buildElectionCard(Election election) {
        JPanel card = card();
        card.setLayout(new BorderLayout(16, 0));
        card.setMaximumSize(new Dimension(9999, 140));
        card.setAlignmentX(LEFT_ALIGNMENT);

        // Lijeva info strana
        JPanel info = new JPanel();
        info.setOpaque(false);
        info.setLayout(new BoxLayout(info, BoxLayout.Y_AXIS));

        JLabel titleLbl = headerLabel(election.getTitle());
        titleLbl.setAlignmentX(LEFT_ALIGNMENT);
        JLabel descLbl  = bodyLabel(election.getDescription());
        descLbl.setAlignmentX(LEFT_ALIGNMENT);

        String candStr = String.join("  •  ", election.getCandidates());
        JLabel candLbl  = mutedLabel("Kandidati: " + candStr);
        candLbl.setAlignmentX(LEFT_ALIGNMENT);

        int votes = VoteStorageManager.countVotes(election.getTitle());
        JLabel voteLbl = mutedLabel("Ukupno glasova: " + votes);
        voteLbl.setAlignmentX(LEFT_ALIGNMENT);

        info.add(titleLbl);
        info.add(Box.createVerticalStrut(4));
        info.add(descLbl);
        info.add(Box.createVerticalStrut(8));
        info.add(candLbl);
        info.add(Box.createVerticalStrut(2));
        info.add(voteLbl);

        // Desna strana — status + gumb za glasanje
        JPanel right = new JPanel();
        right.setOpaque(false);
        right.setLayout(new BoxLayout(right, BoxLayout.Y_AXIS));
        right.setPreferredSize(new Dimension(160, 0));

        // Provjeri da li je glasač već glasao
        boolean alreadyVoted = false;
        try {
            String hash = VoteEncryptionService.hashUsername(loginResult.username);
            alreadyVoted = election.hasVotedByHash(hash);
        } catch (Exception ignored) {}

        if (alreadyVoted) {
            JLabel doneLabel = badge("✓ Glasali ste", SUCCESS);
            doneLabel.setAlignmentX(CENTER_ALIGNMENT);
            right.add(Box.createVerticalGlue());
            right.add(doneLabel);
            right.add(Box.createVerticalGlue());
        } else {
            right.add(Box.createVerticalGlue());
            StyledButton voteBtn = primaryButton("Glasaj");
            voteBtn.setAlignmentX(CENTER_ALIGNMENT);
            voteBtn.setMaximumSize(new Dimension(9999, 38));
            voteBtn.addActionListener(e -> showVoteDialog(election));
            right.add(voteBtn);
            right.add(Box.createVerticalGlue());
        }

        card.add(info,  BorderLayout.CENTER);
        card.add(right, BorderLayout.EAST);
        return card;
    }

    // ── DIJALOG ZA GLASANJE ──────────────────────────────────────

    private void showVoteDialog(Election election) {
        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this),
                "Glasanje — " + election.getTitle(), true);
        dialog.setSize(480, 420);
        dialog.setLocationRelativeTo(this);
        dialog.getContentPane().setBackground(BG_DARK);
        dialog.setLayout(new BorderLayout());

        JPanel content = new JPanel();
        content.setBackground(BG_DARK);
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBorder(BorderFactory.createEmptyBorder(24, 32, 24, 32));

        JLabel title = headerLabel(election.getTitle());
        title.setAlignmentX(LEFT_ALIGNMENT);
        JLabel desc  = bodyLabel(election.getDescription());
        desc.setAlignmentX(LEFT_ALIGNMENT);

        content.add(title);
        content.add(Box.createVerticalStrut(4));
        content.add(desc);
        content.add(Box.createVerticalStrut(20));
        content.add(separator());
        content.add(Box.createVerticalStrut(20));

        JLabel chooseLabel = headerLabel("Odaberite kandidata:");
        chooseLabel.setAlignmentX(LEFT_ALIGNMENT);
        content.add(chooseLabel);
        content.add(Box.createVerticalStrut(12));

        // Radio gumbi za kandidate
        ButtonGroup group = new ButtonGroup();
        List<JRadioButton> radios = new java.util.ArrayList<>();
        for (String candidate : election.getCandidates()) {
            JRadioButton rb = new JRadioButton(candidate);
            rb.setFont(FONT_BODY);
            rb.setForeground(TEXT_PRIMARY);
            rb.setBackground(BG_DARK);
            rb.setOpaque(false);
            rb.setAlignmentX(LEFT_ALIGNMENT);
            rb.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            group.add(rb);
            radios.add(rb);
            content.add(rb);
            content.add(Box.createVerticalStrut(10));
        }

        // Kripto info
        content.add(Box.createVerticalStrut(10));
        JPanel cryptoInfo = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        cryptoInfo.setOpaque(false);
        cryptoInfo.add(badge("AES-256", ACCENT_BLUE));
        cryptoInfo.add(badge("RSA/OAEP", ACCENT_BLUE));
        cryptoInfo.add(badge("SHA256withRSA", ACCENT_BLUE));
        cryptoInfo.setAlignmentX(LEFT_ALIGNMENT);
        content.add(cryptoInfo);

        JLabel progressLabel = new JLabel(" ");
        progressLabel.setFont(FONT_SMALL);
        progressLabel.setAlignmentX(LEFT_ALIGNMENT);

        // Gumbi
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        btnPanel.setBackground(BG_DARK);
        btnPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1,0,0,0, BORDER_COLOR),
                BorderFactory.createEmptyBorder(12, 24, 12, 24)));

        StyledButton cancelBtn = ghostButton("Odustani");
        cancelBtn.addActionListener(e -> dialog.dispose());

        StyledButton confirmBtn = primaryButton("Potvrdi glas");
        confirmBtn.addActionListener(e -> {
            String chosen = null;
            for (JRadioButton rb : radios) {
                if (rb.isSelected()) { chosen = rb.getText(); break; }
            }
            if (chosen == null) {
                progressLabel.setText("Odaberite kandidata!");
                progressLabel.setForeground(DANGER);
                return;
            }

            final String finalChosen = chosen;
            confirmBtn.setEnabled(false);
            cancelBtn.setEnabled(false);
            progressLabel.setForeground(WARNING);

            SwingWorker<Boolean, String> worker = new SwingWorker<>() {
                protected Boolean doInBackground() throws Exception {
                    publish("[1/4] Generisanje AES-256 ključa...");
                    KeyStore ks = KeyStoreManager.loadKeyStore(
                            loginResult.p12Path, loginResult.password);
                    PrivateKey privKey = (PrivateKey) ks.getKey(
                            loginResult.username, loginResult.password.toCharArray());
                    X509Certificate cert = (X509Certificate) ks.getCertificate(loginResult.username);

                    PublicKey orgPubKey = loadOrganizerPublicKey(election.getOrganizerUsername());
                    if (orgPubKey == null) throw new Exception(
                            "Javni ključ organizatora nije dostupan u public_certs/");

                    publish("[2/4] Enkripcija glasa (AES/CBC) i RSA/OAEP enkripcija ključa...");
                    EncryptedVote encVote = VoteEncryptionService.encryptAndSign(
                            finalChosen, election.getTitle(),
                            loginResult.username, privKey, cert, orgPubKey);

                    publish("[3/4] Čuvanje enkriptovanog glasa...");
                    VoteStorageManager.saveVote(election.getTitle(), encVote);

                    publish("[3/4] Ažuriranje metapodataka i HMAC...");
                    String hash = VoteEncryptionService.hashUsername(loginResult.username);
                    election.registerVoterHash(hash);
                    ElectionManager.saveElection(election);

                    publish("[4/4] Verifikacija digitalnog potpisa...");
                    boolean valid = VoteEncryptionService.verifyVoteSignature(encVote);
                    if (!valid) throw new Exception("Verifikacija potpisa nije uspjela!");

                    return true;
                }

                protected void process(List<String> chunks) {
                    progressLabel.setText(chunks.get(chunks.size()-1));
                }

                protected void done() {
                    try {
                        get();
                        dialog.dispose();
                        showVoteSuccessDialog(finalChosen, election.getTitle());
                        refreshElections();
                        setStatus("Glas za '" + finalChosen + "' uspješno zabilježen.");
                    } catch (Exception ex) {
                        progressLabel.setForeground(DANGER);
                        progressLabel.setText("Greška: " + ex.getMessage());
                        confirmBtn.setEnabled(true);
                        cancelBtn.setEnabled(true);
                    }
                }
            };
            worker.execute();
        });

        content.add(Box.createVerticalStrut(8));
        content.add(progressLabel);

        btnPanel.add(cancelBtn);
        btnPanel.add(confirmBtn);

        dialog.add(new JScrollPane(content) {{
            setBorder(BorderFactory.createEmptyBorder());
            getViewport().setBackground(BG_DARK);
            setBackground(BG_DARK);
        }}, BorderLayout.CENTER);
        dialog.add(btnPanel, BorderLayout.SOUTH);
        dialog.setVisible(true);
    }

    private void showVoteSuccessDialog(String candidate, String electionTitle) {
        JDialog d = new JDialog((Frame)SwingUtilities.getWindowAncestor(this),
                "Glas zabilježen!", true);
        d.setSize(420, 280);
        d.setLocationRelativeTo(this);
        d.getContentPane().setBackground(BG_DARK);
        d.setLayout(new BorderLayout());

        JPanel content = new JPanel();
        content.setBackground(BG_DARK);
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBorder(BorderFactory.createEmptyBorder(32, 40, 24, 40));

        JLabel icon = new JLabel("✅");
        icon.setFont(new Font("SansSerif", Font.PLAIN, 48));
        icon.setAlignmentX(CENTER_ALIGNMENT);

        JLabel title = headerLabel("Glas uspješno zabilježen!");
        title.setAlignmentX(CENTER_ALIGNMENT);
        title.setForeground(SUCCESS);

        JLabel info1 = bodyLabel("Glasanje: " + electionTitle);
        info1.setAlignmentX(CENTER_ALIGNMENT);

        JLabel crypto = mutedLabel("AES-256/CBC + RSA/OAEP + SHA256withRSA potpis");
        crypto.setAlignmentX(CENTER_ALIGNMENT);

        JLabel remind = bodyLabel("Koristite 'Verifikuj glas' za provjeru.");
        remind.setAlignmentX(CENTER_ALIGNMENT);

        content.add(icon);
        content.add(Box.createVerticalStrut(12));
        content.add(title);
        content.add(Box.createVerticalStrut(8));
        content.add(info1);
        content.add(Box.createVerticalStrut(4));
        content.add(crypto);
        content.add(Box.createVerticalStrut(16));
        content.add(remind);

        JPanel btn = new JPanel(new FlowLayout(FlowLayout.CENTER));
        btn.setBackground(BG_DARK);
        StyledButton ok = primaryButton("U redu");
        ok.addActionListener(e -> d.dispose());
        btn.add(ok);

        d.add(content, BorderLayout.CENTER);
        d.add(btn, BorderLayout.SOUTH);
        d.setVisible(true);
    }

    // ── SEKCIJA ZA VERIFIKACIJU ──────────────────────────────────

    private JPanel buildVerifySection(List<Election> all) {
        JPanel outer = card();
        outer.setLayout(new BorderLayout(0, 12));
        outer.setMaximumSize(new Dimension(9999, 160));
        outer.setAlignmentX(LEFT_ALIGNMENT);

        JLabel title = headerLabel("🔍  Verifikacija glasa");
        JLabel sub   = bodyLabel("Provjerite da li je vaš glas ispravno zabilježen, bez otkrivanja sadržaja.");

        JPanel top = new JPanel();
        top.setOpaque(false);
        top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));
        top.add(title);
        top.add(Box.createVerticalStrut(4));
        top.add(sub);

        // Combo za odabir glasanja
        JComboBox<String> combo = new JComboBox<>();
        combo.setBackground(BG_CARD);
        combo.setForeground(TEXT_PRIMARY);
        combo.setFont(FONT_BODY);
        all.forEach(e -> combo.addItem(e.getTitle()));

        JLabel verifyStatus = new JLabel(" ");
        verifyStatus.setFont(FONT_BODY);

        StyledButton verifyBtn = primaryButton("Verifikuj");
        verifyBtn.addActionListener(e -> {
            String selected = (String) combo.getSelectedItem();
            if (selected == null) return;
            Election election = all.stream()
                    .filter(el -> el.getTitle().equals(selected))
                    .findFirst().orElse(null);
            if (election == null) return;
            handleVerify(election, verifyStatus);
        });

        JPanel bottom = new JPanel(new BorderLayout(8, 0));
        bottom.setOpaque(false);
        bottom.add(combo,     BorderLayout.CENTER);
        bottom.add(verifyBtn, BorderLayout.EAST);

        outer.add(top,         BorderLayout.NORTH);
        outer.add(bottom,      BorderLayout.CENTER);
        outer.add(verifyStatus,BorderLayout.SOUTH);
        return outer;
    }

    private void handleVerify(Election election, JLabel statusLbl) {
        SwingWorker<String, Void> worker = new SwingWorker<>() {
            protected String doInBackground() throws Exception {
                String hash   = VoteEncryptionService.hashUsername(loginResult.username);
                EncryptedVote myVote = VoteStorageManager.findVoteByUsernameHash(
                        election.getTitle(), hash);
                if (myVote == null) return "NEPRONADJEN";
                boolean valid = VoteEncryptionService.verifyVoteSignature(myVote);
                return valid ? "VALIDAN|" + new Date(myVote.getTimestamp()) : "NEVALIDAN";
            }
            protected void done() {
                try {
                    String result = get();
                    if ("NEPRONADJEN".equals(result)) {
                        statusLbl.setText("Niste glasali na ovom glasanju.");
                        statusLbl.setForeground(TEXT_MUTED);
                    } else if (result.startsWith("VALIDAN")) {
                        String ts = result.split("\\|")[1];
                        statusLbl.setText("✓ Glas je VALIDAN i nije izmijenjen. Glasano: " + ts);
                        statusLbl.setForeground(SUCCESS);
                    } else {
                        statusLbl.setText("✗ Glas je NEVALIDAN — moguća izmjena!");
                        statusLbl.setForeground(DANGER);
                    }
                } catch (Exception ex) {
                    statusLbl.setText("Greška: " + ex.getMessage());
                    statusLbl.setForeground(DANGER);
                }
            }
        };
        worker.execute();
    }

    // ── POMOĆNE METODE ───────────────────────────────────────────

    private void setStatus(String text) {
        SwingUtilities.invokeLater(() -> statusBar.setText(text));
    }

    private PublicKey loadOrganizerPublicKey(String orgUsername) {
        try {
            java.io.File f = new java.io.File("public_certs/" + orgUsername + ".cer");
            if (!f.exists()) return null;
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            try (FileInputStream fis = new FileInputStream(f)) {
                return ((X509Certificate) cf.generateCertificate(fis)).getPublicKey();
            }
        } catch (Exception e) { return null; }
    }
}
