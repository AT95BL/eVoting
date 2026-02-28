package main;

import model.Election;
import model.EncryptedVote;
import utility.*;

import java.io.FileInputStream;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.List;
import java.util.Scanner;
import java.util.stream.Collectors;

/**
 * Panel za glasace.
 *
 * Implementira zahtjeve zadatka:
 *   [Glasac vidi listu aktivnih glasanja]
 *   [Automatska enkripcija glasa i generisanje digitalnog potpisa]
 *   [Glasac dobija potvrdu da je glas uspjesno upisan]
 *   [U svakom trenutku moze verifikovati da je glas ispravno zabiljezen,
 *    bez otkrivanja sadrzaja]
 */
public class VoterPanel {

    public static void showVoterMenu(LoginManager.UserLoginResult loginResult) {
        Scanner sc       = new Scanner(System.in);
        String  username = loginResult.username;

        while (true) {
            System.out.println("\n+---------------------------------------+");
            System.out.println("|   GLASACKI PANEL - " + username);
            System.out.println("+---------------------------------------+");
            System.out.println("| 1. Pregledaj aktivna glasanja i glasaj");
            System.out.println("| 2. Verifikuj moj glas");
            System.out.println("| 3. Odjava");
            System.out.println("+---------------------------------------+");
            System.out.print("Izbor: ");

            switch (sc.nextLine().trim()) {
                case "1": handleVoting(sc, loginResult);       break;
                case "2": handleVerification(sc, loginResult); break;
                case "3":
                    System.out.println("Odjava uspjesna. Dovidjenja!");
                    return;
                default:
                    System.out.println("Neispravan izbor.");
            }
        }
    }

    // ================================================================
    //  GLASANJE
    // ================================================================

    private static void handleVoting(Scanner sc, LoginManager.UserLoginResult loginResult) {
        // Ucitaj samo aktivna glasanja
        List<Election> aktivna = ElectionManager.loadAllElections()
                .stream()
                .filter(Election::isCurrentlyActive)
                .collect(Collectors.toList());

        if (aktivna.isEmpty()) {
            System.out.println("\nTrenutno nema aktivnih glasanja.");
            return;
        }

        System.out.println("\n--- AKTIVNA GLASANJA ---");
        for (int i = 0; i < aktivna.size(); i++) {
            Election e = aktivna.get(i);
            System.out.printf("  %d. %s%n", i + 1, e.getTitle());
            System.out.printf("     %s%n", e.getDescription());
        }

        System.out.print("\nIzaberite broj glasanja (0 = nazad): ");
        int eIdx;
        try {
            eIdx = Integer.parseInt(sc.nextLine().trim()) - 1;
        } catch (NumberFormatException e) {
            System.out.println("Neispravan unos.");
            return;
        }
        if (eIdx < 0 || eIdx >= aktivna.size()) return;

        Election selected = aktivna.get(eIdx);

        // Provjera duplikata
        try {
            String hash = VoteEncryptionService.hashUsername(loginResult.username);
            if (selected.hasVotedByHash(hash)) {
                System.out.println("\nGRESKA: Vec ste glasali na glasanju '" + selected.getTitle() + "'!");
                System.out.println("Koristite opciju 2 za verifikaciju vaseg glasa.");
                return;
            }
        } catch (Exception e) {
            System.out.println("Greska pri provjeri: " + e.getMessage());
            return;
        }

        // Prikazi kandidate
        List<String> candidates = selected.getCandidates();
        System.out.println("\n--- KANDIDATI ZA: " + selected.getTitle() + " ---");
        for (int j = 0; j < candidates.size(); j++) {
            System.out.printf("  %d. %s%n", j + 1, candidates.get(j));
        }

        System.out.print("\nVas glas (broj kandidata, 0 = odustani): ");
        int cIdx;
        try {
            cIdx = Integer.parseInt(sc.nextLine().trim()) - 1;
        } catch (NumberFormatException e) {
            System.out.println("Neispravan unos.");
            return;
        }
        if (cIdx < 0 || cIdx >= candidates.size()) return;

        String chosenCandidate = candidates.get(cIdx);

        // Potvrda
        System.out.println("\nOdabrali ste: " + chosenCandidate);
        System.out.print("Potvrdite glas (da/ne): ");
        if (!sc.nextLine().trim().equalsIgnoreCase("da")) {
            System.out.println("Glasanje otkazano.");
            return;
        }

        castEncryptedVote(loginResult, selected, chosenCandidate);
    }

    /**
     * Kompletni kriptografski tok glasanja:
     *   1. Ucitava kljuceve glasaca i javni kljuc organizatora
     *   2. encryptAndSign() — AES enkripcija + RSA enkripcija kljuca + potpis
     *   3. Cuva enkriptovani glas odvojeno od metapodataka
     *   4. Azurira Election (registruje hash glasaca, cuva HMAC)
     *   5. Verifikuje potpis i prikazuje potvrdu glasacu
     */
    private static void castEncryptedVote(LoginManager.UserLoginResult loginResult,
                                           Election election,
                                           String chosenCandidate) {
        System.out.println("\nProcesiranje glasa...");
        try {
            // --- Privatni kljuc i sertifikat glasaca ---
            KeyStore        voterKS      = KeyStoreManager.loadKeyStore(loginResult.p12Path, loginResult.password);
            PrivateKey      voterPrivKey = (PrivateKey)      voterKS.getKey(loginResult.username, loginResult.password.toCharArray());
            X509Certificate voterCert   = (X509Certificate) voterKS.getCertificate(loginResult.username);

            if (voterPrivKey == null || voterCert == null) {
                System.out.println("GRESKA: Nije moguce ucitati kljuceve glasaca.");
                return;
            }

            // --- Javni kljuc organizatora (iz public_certs/) ---
            PublicKey organizerPubKey = loadOrganizerPublicKey(election.getOrganizerUsername());
            if (organizerPubKey == null) {
                System.out.println("GRESKA: Nije moguce ucitati javni kljuc organizatora.");
                System.out.println("Savjet: Provjerite da postoji fajl public_certs/"
                        + election.getOrganizerUsername() + ".cer");
                return;
            }

            // --- [1/4] AES enkripcija + RSA enkripcija kljuca + potpis ---
            System.out.print("  [1/4] Enkripcija glasa i generisanje potpisa... ");
            EncryptedVote encryptedVote = VoteEncryptionService.encryptAndSign(
                    chosenCandidate,
                    election.getTitle(),
                    loginResult.username,
                    voterPrivKey,
                    voterCert,
                    organizerPubKey
            );
            System.out.println("OK");

            // --- [2/4] Cuva enkriptovani glas odvojeno ---
            System.out.print("  [2/4] Cuvanje enkriptovanog glasa... ");
            VoteStorageManager.saveVote(election.getTitle(), encryptedVote);
            System.out.println("OK");

            // --- [3/4] Azurira Election metapodatke (registruje hash, osvjezava HMAC) ---
            System.out.print("  [3/4] Azuriranje metapodataka glasanja (HMAC)... ");
            election.registerVoterHash(encryptedVote.getVoterUsernameHash());
            ElectionManager.saveElection(election);
            System.out.println("OK");

            // --- [4/4] Verifikacija potpisa ---
            System.out.print("  [4/4] Verifikacija digitalnog potpisa... ");
            boolean signatureValid = VoteEncryptionService.verifyVoteSignature(encryptedVote);
            System.out.println(signatureValid ? "OK" : "UPOZORENJE - potpis nije validan!");

            // --- Potvrda glasacu ---
            System.out.println("\n+------------------------------------------+");
            System.out.println("|     GLAS JE USPJESNO ZABILJEZENH         |");
            System.out.println("+------------------------------------------+");
            System.out.println("| Glasanje : " + election.getTitle());
            System.out.println("| Enkrip.  : AES-256/CBC + RSA/OAEP");
            System.out.println("| Potpis   : " + (signatureValid ? "VALIDAN - glas nije izmijenjen" : "GRESKA!"));
            System.out.println("| Sadrzaj  : [ENKRIPTOVAN - samo org. moze vidjeti]");
            System.out.println("+------------------------------------------+");
            System.out.println("Koristite opciju 2 za kasniju verifikaciju.");

        } catch (Exception e) {
            System.out.println("\nGRESKA pri glasanju: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ================================================================
    //  VERIFIKACIJA GLASA
    // ================================================================

    private static void handleVerification(Scanner sc, LoginManager.UserLoginResult loginResult) {
        List<Election> elections = ElectionManager.loadAllElections();

        if (elections.isEmpty()) {
            System.out.println("\nNema glasanja za verifikaciju.");
            return;
        }

        System.out.println("\n--- GLASANJA ---");
        for (int i = 0; i < elections.size(); i++) {
            System.out.printf("  %d. %s%n", i + 1, elections.get(i).getTitle());
        }

        System.out.print("Izaberite glasanje (0 = nazad): ");
        int idx;
        try {
            idx = Integer.parseInt(sc.nextLine().trim()) - 1;
        } catch (NumberFormatException e) {
            return;
        }
        if (idx < 0 || idx >= elections.size()) return;

        Election election = elections.get(idx);

        try {
            String        usernameHash = VoteEncryptionService.hashUsername(loginResult.username);
            EncryptedVote myVote       = VoteStorageManager.findVoteByUsernameHash(
                    election.getTitle(), usernameHash);

            if (myVote == null) {
                System.out.println("\nNiste glasali na ovom glasanju.");
                return;
            }

            // Verifikuj potpis — bez dekriptovanja (sadrzaj ostaje tajna)
            boolean valid = VoteEncryptionService.verifyVoteSignature(myVote);

            System.out.println("\n--- VERIFIKACIJA GLASA ---");
            System.out.println("Glasanje  : " + election.getTitle());
            System.out.println("Status    : Glas pronadjen u sistemu");
            System.out.println("Potpis    : " + (valid
                    ? "VALIDAN - glas nije izmijenjen od glasanja"
                    : "NEVALIDAN - moguca izmjena glasa!"));
            System.out.println("Sadrzaj   : [ENKRIPTOVAN - tajnost je zasticena]");
            System.out.println("Timestamp : " + new Date(myVote.getTimestamp()));

        } catch (Exception e) {
            System.out.println("Greska pri verifikaciji: " + e.getMessage());
        }
    }

    // ================================================================
    //  POMOCNE METODE
    // ================================================================

    /**
     * Ucitava javni kljuc organizatora iz public_certs/<username>.cer
     *
     * Javni sertifikati su dostupni svima - kreira ih UserRegistration
     * automatski tokom registracije organizatora.
     */
    private static PublicKey loadOrganizerPublicKey(String organizerUsername) {
        try {
            java.io.File certFile = new java.io.File("public_certs/" + organizerUsername + ".cer");
            if (!certFile.exists()) {
                return null;
            }
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            try (FileInputStream fis = new FileInputStream(certFile)) {
                X509Certificate cert = (X509Certificate) cf.generateCertificate(fis);
                return cert.getPublicKey();
            }
        } catch (Exception e) {
            System.err.println("Greska pri ucitavanju javnog kljuca: " + e.getMessage());
            return null;
        }
    }
}