package utility;

import model.EncryptedVote;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Cuvanje i ucitavanje enkriptovanih glasova.
 *
 * Implementira zahtjev zadatka:
 *   [Metapodaci o glasanju se cuvaju odvojeno od glasova]
 *
 * Struktura na disku:
 *   elections/
 *     naziv.dat                  <- Election metapodaci (HMAC zasticeni)
 *     naziv_votes/
 *       <hash_korisnika>.vote    <- jedan EncryptedVote po glasacu
 *
 * Naziv fajla glasa je SHA-256 hash korisnickog imena (Base64, URL-safe).
 * Na taj nacin ne mozemo odmah vidjeti ko je glasao samo gledanjem fajlova,
 * ali mozemo provjeriti duplikate i pronaci glas odredjenog glasaca.
 */
public class VoteStorageManager {

    private static final String ELECTIONS_DIR = "elections/";
    private static final String VOTES_SUFFIX  = "_votes";
    private static final String VOTE_EXT      = ".vote";

    /**
     * Cuva enkriptovani glas na disk.
     *
     * @param electionTitle Naslov glasanja (koristi se za ime foldera)
     * @param vote          Enkriptovani glas za cuvanje
     */
    public static void saveVote(String electionTitle, EncryptedVote vote) throws Exception {
        File dir = votesDir(electionTitle);
        if (!dir.exists()) dir.mkdirs();

        String safeHash = urlSafeHash(vote.getVoterUsernameHash());
        File   voteFile = new File(dir, safeHash + VOTE_EXT);

        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(voteFile))) {
            oos.writeObject(vote);
        }
    }

    /**
     * Ucitava sve glasove za dato glasanje.
     * Poziva ReportService tokom procesa brojanja.
     *
     * @param electionTitle Naslov glasanja
     * @return Lista enkriptovanih glasova (moze biti prazna)
     */
    public static List<EncryptedVote> loadAllVotes(String electionTitle) {
        List<EncryptedVote> votes = new ArrayList<>();
        File dir = votesDir(electionTitle);

        if (!dir.exists()) return votes;
        File[] files = dir.listFiles();
        if (files == null) return votes;

        for (File f : files) {
            if (!f.getName().endsWith(VOTE_EXT)) continue;
            try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(f))) {
                votes.add((EncryptedVote) ois.readObject());
            } catch (Exception e) {
                System.err.println("Upozorenje: Nije moguce ucitati glas: " + f.getName());
            }
        }
        return votes;
    }

    /**
     * Pronalazi glas odredjenog glasaca po hash-u korisnickog imena.
     * Glasac koristi ovo za verifikaciju svog glasa.
     *
     * @param electionTitle  Naslov glasanja
     * @param usernameHash   Base64 hash korisnickog imena (iz VoteEncryptionService.hashUsername())
     * @return EncryptedVote ako postoji, null ako glasac nije glasao
     */
    public static EncryptedVote findVoteByUsernameHash(String electionTitle, String usernameHash) {
        File voteFile = new File(votesDir(electionTitle), urlSafeHash(usernameHash) + VOTE_EXT);
        if (!voteFile.exists()) return null;

        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(voteFile))) {
            return (EncryptedVote) ois.readObject();
        } catch (Exception e) {
            System.err.println("Greska pri ucitavanju glasa: " + e.getMessage());
            return null;
        }
    }

    /**
     * Broji sacuvane glasove za dato glasanje.
     * Koristi se u MainMenu za prikaz statusa.
     *
     * @param electionTitle Naslov glasanja
     * @return Broj glasova (0 ako nema nijednog)
     */
    public static int countVotes(String electionTitle) {
        File   dir   = votesDir(electionTitle);
        File[] files = dir.exists() ? dir.listFiles() : null;
        if (files == null) return 0;

        int count = 0;
        for (File f : files) {
            if (f.getName().endsWith(VOTE_EXT)) count++;
        }
        return count;
    }

    // ---------------------------------------------------------------

    private static File votesDir(String electionTitle) {
        return new File(ELECTIONS_DIR + safeName(electionTitle) + VOTES_SUFFIX);
    }

    private static String safeName(String title) {
        return title.replace(" ", "_").replaceAll("[^a-zA-Z0-9_\\-]", "");
    }

    /** Base64 moze sadrzati '/' i '+' koji nisu sigurni kao naziv fajla. */
    private static String urlSafeHash(String base64Hash) {
        return base64Hash.replace("/", "_").replace("+", "-").replace("=", "");
    }
}