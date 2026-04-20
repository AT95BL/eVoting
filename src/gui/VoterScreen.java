package gui;

import model.Election;
import model.EncryptedVote;
import utility.*;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
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
 * Voter panel — browse active elections, cast encrypted votes, verify vote integrity.
 */
public class VoterScreen extends JPanel {

    private final AppGUI                       app;
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
                BorderFactory.createMatteBorder(0, 0, 1, 0, BORDER_COLOR),
                BorderFactory.createEmptyBorder(12, 24, 12, 24)));

        JLabel logo = new JLabel("🗳️  Voter Panel");
        logo.setFont(FONT_HEADER);
        logo.setForeground(TEXT_PRIMARY);

        JPanel userInfo = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        userInfo.setOpaque(false);
        userInfo.add(badge("VOTER", SUCCESS));
        userInfo.add(bodyLabel(loginResult.username));
        StyledButton logoutBtn = dangerButton("Sign Out");
        logoutBtn.setPreferredSize(new Dimension(90, 30));
        logoutBtn.addActionListener(e -> app.showScreen(new MainScreen(app)));
        userInfo.add(logoutBtn);

        topBar.add(logo,    BorderLayout.WEST);
        topBar.add(userInfo,BorderLayout.EAST);
        add(topBar, BorderLayout.NORTH);

        // ── Content ──────────────────────────────────────────────
        JPanel content = new JPanel(new BorderLayout(0, 16));
        content.setBackground(BG_DARK);
        content.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // Info card
        JPanel infoCard = card();
        infoCard.setLayout(new BorderLayout(16, 0));
        infoCard.setPreferredSize(new Dimension(0, 80));

        JPanel infoText = new JPanel();
        infoText.setOpaque(false);
        infoText.setLayout(new BoxLayout(infoText, BoxLayout.Y_AXIS));
        infoText.add(headerLabel("Welcome, " + loginResult.username));
        infoText.add(Box.createVerticalStrut(4));
        infoText.add(bodyLabel("Your vote is protected with AES-256/CBC encryption and RSA digital signature."));

        StyledButton refreshBtn = ghostButton("↺ Refresh");
        refreshBtn.setPreferredSize(new Dimension(100, 34));
        refreshBtn.addActionListener(e -> refresh());

        infoCard.add(infoText,   BorderLayout.CENTER);
        infoCard.add(refreshBtn, BorderLayout.EAST);
        content.add(infoCard, BorderLayout.NORTH);

        // Elections list
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
                BorderFactory.createMatteBorder(1, 0, 0, 0, BORDER_COLOR),
                BorderFactory.createEmptyBorder(6, 24, 6, 24)));
        statusBar = new JLabel("Browse available elections below.");
        statusBar.setFont(FONT_SMALL);
        statusBar.setForeground(TEXT_MUTED);
        sb.add(statusBar, BorderLayout.WEST);
        add(sb, BorderLayout.SOUTH);

        refresh();
    }

    // ── ELECTIONS LIST ───────────────────────────────────────────

    private void refresh() {
        SwingUtilities.invokeLater(() -> {
            electionsListPanel.removeAll();

            List<Election> all    = ElectionManager.loadAllElections();
            List<Election> active = all.stream()
                    .filter(Election::isCurrentlyActive)
                    .collect(Collectors.toList());

            if (active.isEmpty()) {
                JPanel empty = new JPanel(new GridBagLayout());
                empty.setOpaque(false);
                empty.setPreferredSize(new Dimension(0, 200));
                empty.add(bodyLabel("No active elections at this time."));
                electionsListPanel.add(empty);
            } else {
                for (Election e : active) {
                    electionsListPanel.add(buildElectionCard(e));
                    electionsListPanel.add(Box.createVerticalStrut(12));
                }
            }

            // Verification section always visible
            electionsListPanel.add(Box.createVerticalStrut(8));
            electionsListPanel.add(buildVerifySection(all));

            electionsListPanel.revalidate();
            electionsListPanel.repaint();
        });
    }

    private JPanel buildElectionCard(Election election) {
        JPanel c = card();
        c.setLayout(new BorderLayout(16, 0));
        c.setMaximumSize(new Dimension(9999, 140));
        c.setAlignmentX(LEFT_ALIGNMENT);

        // Left — election info
        JPanel info = new JPanel();
        info.setOpaque(false);
        info.setLayout(new BoxLayout(info, BoxLayout.Y_AXIS));

        JLabel titleLbl = headerLabel(election.getTitle());
        titleLbl.setAlignmentX(LEFT_ALIGNMENT);
        JLabel descLbl  = bodyLabel(election.getDescription());
        descLbl.setAlignmentX(LEFT_ALIGNMENT);
        JLabel candLbl  = mutedLabel("Candidates: " + String.join("  •  ", election.getCandidates()));
        candLbl.setAlignmentX(LEFT_ALIGNMENT);
        JLabel voteLbl  = mutedLabel("Total votes cast: " + VoteStorageManager.countVotes(election.getTitle()));
        voteLbl.setAlignmentX(LEFT_ALIGNMENT);

        info.add(titleLbl);
        info.add(Box.createVerticalStrut(4));
        info.add(descLbl);
        info.add(Box.createVerticalStrut(8));
        info.add(candLbl);
        info.add(Box.createVerticalStrut(2));
        info.add(voteLbl);

        // Right — vote button or already-voted badge
        JPanel right = new JPanel();
        right.setOpaque(false);
        right.setLayout(new BoxLayout(right, BoxLayout.Y_AXIS));
        right.setPreferredSize(new Dimension(150, 0));

        boolean alreadyVoted = false;
        try {
            alreadyVoted = election.hasVotedByHash(
                    VoteEncryptionService.hashUsername(loginResult.username));
        } catch (Exception ignored) {}

        if (alreadyVoted) {
            JLabel done = badge("✓ Vote Cast", SUCCESS);
            done.setAlignmentX(CENTER_ALIGNMENT);
            right.add(Box.createVerticalGlue());
            right.add(done);
            right.add(Box.createVerticalGlue());
        } else {
            right.add(Box.createVerticalGlue());
            StyledButton voteBtn = primaryButton("Vote");
            voteBtn.setAlignmentX(CENTER_ALIGNMENT);
            voteBtn.setMaximumSize(new Dimension(9999, 38));
            voteBtn.addActionListener(e -> showVoteDialog(election));
            right.add(voteBtn);
            right.add(Box.createVerticalGlue());
        }

        c.add(info,  BorderLayout.CENTER);
        c.add(right, BorderLayout.EAST);
        return c;
    }

    // ── VOTE DIALOG ──────────────────────────────────────────────

    private void showVoteDialog(Election election) {
        JDialog dialog = new JDialog(
                (Frame) SwingUtilities.getWindowAncestor(this),
                "Cast Vote — " + election.getTitle(), true);
        dialog.setSize(480, 430);
        dialog.setLocationRelativeTo(this);
        dialog.getContentPane().setBackground(BG_DARK);
        dialog.setLayout(new BorderLayout());

        JPanel content = new JPanel();
        content.setBackground(BG_DARK);
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBorder(BorderFactory.createEmptyBorder(24, 32, 16, 32));

        JLabel titleLbl = headerLabel(election.getTitle());
        titleLbl.setAlignmentX(LEFT_ALIGNMENT);
        JLabel descLbl  = bodyLabel(election.getDescription());
        descLbl.setAlignmentX(LEFT_ALIGNMENT);
        content.add(titleLbl);
        content.add(Box.createVerticalStrut(4));
        content.add(descLbl);
        content.add(Box.createVerticalStrut(16));
        content.add(separator());
        content.add(Box.createVerticalStrut(16));

        JLabel choose = headerLabel("Select a candidate:");
        choose.setAlignmentX(LEFT_ALIGNMENT);
        content.add(choose);
        content.add(Box.createVerticalStrut(12));

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

        content.add(Box.createVerticalStrut(8));
        JPanel cryptoBadges = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        cryptoBadges.setOpaque(false);
        cryptoBadges.add(badge("AES-256/CBC", ACCENT_BLUE));
        cryptoBadges.add(badge("RSA/OAEP",   ACCENT_BLUE));
        cryptoBadges.add(badge("SHA256withRSA", ACCENT_BLUE));
        cryptoBadges.setAlignmentX(LEFT_ALIGNMENT);
        content.add(cryptoBadges);
        content.add(Box.createVerticalStrut(8));

        JLabel progressLabel = new JLabel(" ");
        progressLabel.setFont(FONT_SMALL);
        progressLabel.setAlignmentX(LEFT_ALIGNMENT);
        content.add(progressLabel);

        // Button row
        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        btnRow.setBackground(BG_DARK);
        btnRow.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, BORDER_COLOR),
                BorderFactory.createEmptyBorder(12, 24, 12, 24)));

        StyledButton cancelBtn  = ghostButton("Cancel");
        StyledButton confirmBtn = primaryButton("Confirm Vote");

        cancelBtn .addActionListener(e -> dialog.dispose());
        confirmBtn.addActionListener(e -> {
            String chosen = null;
            for (JRadioButton rb : radios) if (rb.isSelected()) { chosen = rb.getText(); break; }
            if (chosen == null) {
                progressLabel.setText("Please select a candidate first.");
                progressLabel.setForeground(DANGER);
                return;
            }

            final String finalChosen = chosen;
            confirmBtn.setEnabled(false);
            cancelBtn .setEnabled(false);
            progressLabel.setForeground(WARNING);

            SwingWorker<Boolean, String> worker = new SwingWorker<>() {
                protected Boolean doInBackground() throws Exception {
                    publish("[1/4] Generating AES-256 key...");
                    KeyStore ks = KeyStoreManager.loadKeyStore(
                            loginResult.p12Path, loginResult.password);
                    PrivateKey      privKey = (PrivateKey)      ks.getKey(loginResult.username, loginResult.password.toCharArray());
                    X509Certificate cert    = (X509Certificate) ks.getCertificate(loginResult.username);

                    PublicKey orgPubKey = loadOrganizerPublicKey(election.getOrganizerUsername());
                    if (orgPubKey == null)
                        throw new Exception("Organizer's public key not found in public_certs/");

                    publish("[2/4] Encrypting vote (AES/CBC) + wrapping key (RSA/OAEP)...");
                    EncryptedVote encVote = VoteEncryptionService.encryptAndSign(
                            finalChosen, election.getTitle(),
                            loginResult.username, privKey, cert, orgPubKey);

                    publish("[3/4] Saving encrypted vote + updating HMAC...");
                    VoteStorageManager.saveVote(election.getTitle(), encVote);
                    election.registerVoterHash(
                            VoteEncryptionService.hashUsername(loginResult.username));
                    ElectionManager.saveElection(election);

                    publish("[4/4] Verifying digital signature...");
                    if (!VoteEncryptionService.verifyVoteSignature(encVote))
                        throw new Exception("Signature verification failed!");

                    return true;
                }

                protected void process(List<String> chunks) {
                    progressLabel.setText(chunks.get(chunks.size()-1));
                    progressLabel.setForeground(WARNING);
                }

                protected void done() {
                    try {
                        get();
                        dialog.dispose();
                        showSuccessDialog(finalChosen, election.getTitle());
                        refresh();
                        setStatus("Vote for '" + finalChosen + "' successfully recorded.");
                    } catch (Exception ex) {
                        progressLabel.setText("Error: " + ex.getMessage());
                        progressLabel.setForeground(DANGER);
                        confirmBtn.setEnabled(true);
                        cancelBtn .setEnabled(true);
                    }
                }
            };
            worker.execute();
        });

        btnRow.add(cancelBtn);
        btnRow.add(confirmBtn);

        JScrollPane sp = new JScrollPane(content);
        sp.setBorder(BorderFactory.createEmptyBorder());
        sp.getViewport().setBackground(BG_DARK);
        sp.setBackground(BG_DARK);

        dialog.add(sp,    BorderLayout.CENTER);
        dialog.add(btnRow,BorderLayout.SOUTH);
        dialog.setVisible(true);
    }

    private void showSuccessDialog(String candidate, String electionTitle) {
        JDialog d = new JDialog(
                (Frame) SwingUtilities.getWindowAncestor(this), "Vote Recorded!", true);
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

        JLabel title = headerLabel("Vote Successfully Recorded!");
        title.setAlignmentX(CENTER_ALIGNMENT);
        title.setForeground(SUCCESS);

        JLabel info = bodyLabel("Election: " + electionTitle);
        info.setAlignmentX(CENTER_ALIGNMENT);

        JLabel crypto = mutedLabel("AES-256/CBC + RSA/OAEP + SHA256withRSA signature");
        crypto.setAlignmentX(CENTER_ALIGNMENT);

        JLabel remind = bodyLabel("Use 'Verify My Vote' to check your vote at any time.");
        remind.setAlignmentX(CENTER_ALIGNMENT);

        content.add(icon);          content.add(Box.createVerticalStrut(12));
        content.add(title);         content.add(Box.createVerticalStrut(8));
        content.add(info);          content.add(Box.createVerticalStrut(4));
        content.add(crypto);        content.add(Box.createVerticalStrut(16));
        content.add(remind);

        JPanel btn = new JPanel(new FlowLayout(FlowLayout.CENTER));
        btn.setBackground(BG_DARK);
        StyledButton ok = primaryButton("OK");
        ok.addActionListener(e -> d.dispose());
        btn.add(ok);

        d.add(content, BorderLayout.CENTER);
        d.add(btn,     BorderLayout.SOUTH);
        d.setVisible(true);
    }

    // ── VERIFICATION SECTION ─────────────────────────────────────

    private JPanel buildVerifySection(List<Election> all) {
        JPanel p = card();
        p.setLayout(new BorderLayout(0, 12));
        p.setMaximumSize(new Dimension(9999, 160));
        p.setAlignmentX(LEFT_ALIGNMENT);

        JPanel top = new JPanel();
        top.setOpaque(false);
        top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));
        top.add(headerLabel("🔍  Verify My Vote"));
        top.add(Box.createVerticalStrut(4));
        top.add(bodyLabel("Check that your vote is correctly recorded — without revealing its content."));

        JComboBox<String> combo = new JComboBox<>();
        combo.setBackground(BG_CARD);
        combo.setForeground(TEXT_PRIMARY);
        combo.setFont(FONT_BODY);
        all.forEach(e -> combo.addItem(e.getTitle()));

        JLabel verifyStatus = new JLabel(" ");
        verifyStatus.setFont(FONT_BODY);

        StyledButton verifyBtn = primaryButton("Verify");
        verifyBtn.addActionListener(e -> {
            String selected = (String) combo.getSelectedItem();
            if (selected == null) return;
            all.stream().filter(el -> el.getTitle().equals(selected))
               .findFirst().ifPresent(el -> handleVerify(el, verifyStatus));
        });

        JPanel bottom = new JPanel(new BorderLayout(8, 0));
        bottom.setOpaque(false);
        bottom.add(combo,     BorderLayout.CENTER);
        bottom.add(verifyBtn, BorderLayout.EAST);

        p.add(top,         BorderLayout.NORTH);
        p.add(bottom,      BorderLayout.CENTER);
        p.add(verifyStatus,BorderLayout.SOUTH);
        return p;
    }

    private void handleVerify(Election election, JLabel statusLbl) {
        SwingWorker<String, Void> worker = new SwingWorker<>() {
            protected String doInBackground() throws Exception {
                String hash  = VoteEncryptionService.hashUsername(loginResult.username);
                EncryptedVote v = VoteStorageManager.findVoteByUsernameHash(
                        election.getTitle(), hash);
                if (v == null) return "NOT_FOUND";
                return VoteEncryptionService.verifyVoteSignature(v)
                        ? "VALID|" + new Date(v.getTimestamp())
                        : "INVALID";
            }
            protected void done() {
                try {
                    String result = get();
                    if ("NOT_FOUND".equals(result)) {
                        statusLbl.setText("You have not voted in this election.");
                        statusLbl.setForeground(TEXT_MUTED);
                    } else if (result.startsWith("VALID")) {
                        statusLbl.setText("✓ Vote is VALID and has not been altered. Cast: "
                                + result.split("\\|")[1]);
                        statusLbl.setForeground(SUCCESS);
                    } else {
                        statusLbl.setText("✗ Vote is INVALID — possible tampering detected!");
                        statusLbl.setForeground(DANGER);
                    }
                } catch (Exception ex) {
                    statusLbl.setText("Error: " + ex.getMessage());
                    statusLbl.setForeground(DANGER);
                }
            }
        };
        worker.execute();
    }

    // ── HELPERS ──────────────────────────────────────────────────

    private void setStatus(String text) {
        SwingUtilities.invokeLater(() -> statusBar.setText(text));
    }

    private PublicKey loadOrganizerPublicKey(String username) {
        try {
            java.io.File f = new java.io.File("public_certs/" + username + ".cer");
            if (!f.exists()) return null;
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            try (FileInputStream fis = new FileInputStream(f)) {
                return ((X509Certificate) cf.generateCertificate(fis)).getPublicKey();
            }
        } catch (Exception e) { return null; }
    }
}
