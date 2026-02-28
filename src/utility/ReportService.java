package utility;

import model.Election;
import model.EncryptedVote;

import java.io.FileWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.cert.X509Certificate;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Generisanje i digitalno potpisivanje izvjestaja o rezultatima glasanja.
 *
 * Implementira zahtjeve zadatka:
 *   [Nakon isteka vremena organizator pokrece proces brojanja]
 *   [Sistem desifriše glasove koristeći privatni kljuc organizatora]
 *   [Rezultati se prikazuju organizatoru]
 *   [Automatski se generise digitalno potpisan izvjestaj]
 */
public class ReportService {

    private static final String SIGN_ALGORITHM = "SHA256withRSA";

    /**
     * Glavni entrypoint — dekriptuje sve glasove, broji ih i generise potpisan izvjestaj.
     *
     * @param election          Glasanje za koje se vrsi brojanje
     * @param organizerUsername Korisnicko ime organizatora (alias u .p12 fajlu)
     * @param p12Password       Lozinka kojom je zasticen organizatorov .p12
     * @return Tekst potpisan izvjestaja (za prikaz i cuvanje)
     */
    public static String countVotesAndGenerateReport(Election election,
                                                      String organizerUsername,
                                                      String p12Password) throws Exception {
        // Ucitaj privatni kljuc i sertifikat organizatora
        KeyStore        ks            = KeyStoreManager.loadKeyStore(organizerUsername + ".p12", p12Password);
        PrivateKey      orgPrivKey    = (PrivateKey)      ks.getKey(organizerUsername, p12Password.toCharArray());
        X509Certificate orgCert       = (X509Certificate) ks.getCertificate(organizerUsername);

        if (orgPrivKey == null) {
            throw new Exception("Nije moguce ucitati privatni kljuc organizatora. " +
                                "Provjerite da je lozinka ispravna.");
        }

        // Ucitaj sve enkriptovane glasove
        List<EncryptedVote> encVotes = VoteStorageManager.loadAllVotes(election.getTitle());

        // Inicijalizuj brojac (svi kandidati na 0)
        Map<String, Integer> tally = new LinkedHashMap<>();
        for (String c : election.getCandidates()) tally.put(c, 0);

        int validCount   = 0;
        int invalidCount = 0;

        System.out.println("\nDekriptovanje i provjera " + encVotes.size() + " glasova...");

        for (EncryptedVote encVote : encVotes) {
            try {
                // Provjeri potpis glasaca — ako nije validan, glas je nevazeci
                if (!VoteEncryptionService.verifyVoteSignature(encVote)) {
                    System.out.println("  [!] Glas ima nevalidan potpis — preskacam.");
                    invalidCount++;
                    continue;
                }

                // Dekripcija glasa privatnim kljucem organizatora
                String candidate = VoteEncryptionService.decryptVote(encVote, orgPrivKey);

                if (tally.containsKey(candidate)) {
                    tally.put(candidate, tally.get(candidate) + 1);
                    validCount++;
                } else {
                    System.out.println("  [!] Nepoznati kandidat u glasu: '" + candidate + "' — preskacam.");
                    invalidCount++;
                }
            } catch (Exception e) {
                System.out.println("  [!] Greska pri dekriptovanju glasa: " + e.getMessage());
                invalidCount++;
            }
        }

        // Izgradi tekst izvjestaja
        String reportText = buildReport(election, tally, validCount, invalidCount);

        // Digitalno potpisi izvjestaj privatnim kljucem organizatora
        Signature signer = Signature.getInstance(SIGN_ALGORITHM);
        signer.initSign(orgPrivKey);
        signer.update(reportText.getBytes(StandardCharsets.UTF_8));
        byte[] sig = signer.sign();

        String signedReport = reportText
                + "\n--- DIGITALNI POTPIS ORGANIZATORA (SHA256withRSA) ---\n"
                + Base64.getEncoder().encodeToString(sig) + "\n"
                + "Sertifikat: " + orgCert.getSubjectX500Principal().getName() + "\n";

        // Sacuvaj u Election i osvjezi na disku
        election.setSignedReport(signedReport);
        election.setActive(false);
        ElectionManager.saveElection(election);

        // Sacuvaj kao tekstualni fajl u reports/
        saveToFile(election.getTitle(), signedReport);

        return signedReport;
    }

    // ---------------------------------------------------------------

    private static String buildReport(Election election, Map<String, Integer> tally,
                                       int validCount, int invalidCount) {
        SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");
        StringBuilder    sb  = new StringBuilder();

        sb.append("============================================\n");
        sb.append("     IZVJESTAJ O REZULTATIMA GLASANJA\n");
        sb.append("============================================\n\n");
        sb.append("Naslov:       ").append(election.getTitle()).append("\n");
        sb.append("Opis:         ").append(election.getDescription()).append("\n");
        sb.append("Organizator:  ").append(election.getOrganizerUsername()).append("\n");
        sb.append("Datum izv.:   ").append(sdf.format(new Date())).append("\n\n");

        sb.append("------------ REZULTATI ------------\n");
        tally.forEach((candidate, votes) -> {
            double pct = validCount > 0 ? votes * 100.0 / validCount : 0;
            sb.append(String.format("  %-30s : %4d  (%.1f%%)\n", candidate, votes, pct));
        });

        sb.append("\n------------------------------------\n");
        sb.append("Validnih glasova:   ").append(validCount).append("\n");
        sb.append("Nevazecih glasova:  ").append(invalidCount).append("\n");
        sb.append("Ukupno obradjeno:   ").append(validCount + invalidCount).append("\n");

        if (validCount > 0) {
            String winner = tally.entrySet().stream()
                    .max(Map.Entry.comparingByValue())
                    .map(Map.Entry::getKey).orElse("N/A");
            sb.append("\nPOBJEDNIK: ").append(winner).append("\n");
        }

        return sb.toString();
    }

    private static void saveToFile(String electionTitle, String content) {
        try {
            java.io.File dir = new java.io.File("reports/");
            if (!dir.exists()) dir.mkdirs();

            String safeName = electionTitle.replace(" ", "_").replaceAll("[^a-zA-Z0-9_\\-]", "");
            try (PrintWriter pw = new PrintWriter(new FileWriter("reports/" + safeName + "_report.txt"))) {
                pw.print(content);
            }
            System.out.println("Izvjestaj sacuvan u: reports/" + safeName + "_report.txt");
        } catch (Exception e) {
            System.err.println("Upozorenje: Nije moguce sacuvati izvjestaj: " + e.getMessage());
        }
    }
}