package utility;

import model.Election;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Cuvanje i ucitavanje metapodataka glasanja.
 *
 * Svako glasanje se cuva kao .dat fajl u elections/.
 * HMAC se generise pri svakom cuvanju i verifikuje pri ucitavanju.
 *
 * Implementira zahtjev zadatka:
 *   [Metapodaci o glasanju se cuvaju odvojeno od glasova]
 *   [Njihov integritet se stiti pomocu HMAC algoritma]
 */
public class ElectionManager {

    private static final String STORAGE_DIR = "elections/";

    /**
     * Cuva glasanje na disk.
     * Prije cuvanja generise novi HMAC nad metapodacima.
     */
    public static void saveElection(Election election) throws Exception {
        File dir = new File(STORAGE_DIR);
        if (!dir.exists()) dir.mkdirs();

        // Generisi HMAC i postavi ga na Election
        String hmacKey = HMACService.deriveElectionHMACKey(
                election.getTitle(), election.getOrganizerUsername());
        election.setHmac(HMACService.generateHMAC(election.getMetadataForHMAC(), hmacKey));

        try (ObjectOutputStream oos = new ObjectOutputStream(
                new FileOutputStream(STORAGE_DIR + safeName(election.getTitle()) + ".dat"))) {
            oos.writeObject(election);
        }
    }

    /**
     * Ucitava sva glasanja.
     * Upozorava u konzoli ako HMAC verifikacija ne prodje.
     */
    public static List<Election> loadAllElections() {
        List<Election> list = new ArrayList<>();
        File dir = new File(STORAGE_DIR);
        if (!dir.exists()) return list;

        File[] files = dir.listFiles();
        if (files == null) return list;

        for (File f : files) {
            if (!f.getName().endsWith(".dat")) continue;
            try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(f))) {
                Election e = (Election) ois.readObject();
                if (!verifyElectionHMAC(e)) {
                    System.err.println("UPOZORENJE: HMAC greška za glasanje '" + e.getTitle() + "'!");
                }
                list.add(e);
            } catch (Exception e) {
                System.err.println("Greska pri ucitavanju: " + f.getName());
            }
        }
        return list;
    }

    /**
     * Verifikuje HMAC integritet metapodataka glasanja.
     * Poziva se iz MainMenu za prikaz statusa i pri svakom ucitavanju.
     *
     * @return true ako je integritet potvrdjen (HMAC se podudara)
     */
    public static boolean verifyElectionHMAC(Election election) {
        try {
            String key = HMACService.deriveElectionHMACKey(
                    election.getTitle(), election.getOrganizerUsername());
            return HMACService.verifyHMAC(election.getMetadataForHMAC(), key, election.getHmac());
        } catch (Exception e) {
            return false;
        }
    }

    // ---------------------------------------------------------------

    private static String safeName(String title) {
        return title.replace(" ", "_").replaceAll("[^a-zA-Z0-9_\\-]", "");
    }
}