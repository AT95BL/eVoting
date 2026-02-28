package utility;

import java.io.*;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;

/**
 * Upravljanje CRL (Certificate Revocation List) listama.
 *
 * ISPRAVKE u odnosu na originalnu verziju:
 *   - Dodata metoda revokeCertificate() (LoginManager je poziva)
 *   - Originalna revoke() je zadrzana kao alias radi kompatibilnosti
 *   - Dvije odvojene CRL liste (po jedna za svako CA tijelo)
 *   - Liste se cuvaju na disk (trajnost kroz restartove)
 *
 * Implementira zahtjev zadatka:
 *   [Provjera u odnosu na CRL listu — posebna za svako CA tijelo]
 *   [Sertifikati se automatski povlace u slucaju tri neuspjesne prijave]
 */
public class CRLManager {

    private static final String ORGANIZER_CRL = "organizer_ca_crl.dat";
    private static final String VOTER_CRL     = "voter_ca_crl.dat";
    private static final String ROOT_CRL      = "root_ca_crl.dat";

    /**
     * Provjerava da li je sertifikat na CRL listi.
     * Automatski bira ispravnu listu na osnovu izdavaca.
     */
    public static boolean isRevoked(X509Certificate cert) {
        try {
            String crlFile = resolveCRLFile(cert);
            List<String> revoked = load(crlFile);
            String serial = cert.getSerialNumber().toString(16).toUpperCase();
            return revoked.contains(serial);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Povlaci sertifikat — dodaje ga na odgovarajucu CRL listu.
     * Ovo je metoda koju poziva LoginManager.
     */
    public static void revokeCertificate(X509Certificate cert) {
        try {
            String crlFile = resolveCRLFile(cert);
            List<String> revoked = load(crlFile);
            String serial = cert.getSerialNumber().toString(16).toUpperCase();
            if (!revoked.contains(serial)) {
                revoked.add(serial);
                save(crlFile, revoked);
                System.out.println("  CRL: Sertifikat (serial: " + serial + ") dodat na listu povucenih.");
            }
        } catch (Exception e) {
            System.err.println("  CRL greska pri povlacenju: " + e.getMessage());
        }
    }

    /**
     * Alias za revokeCertificate() — zadrzano zbog eventualne upotrebe
     * stare metode revoke() negdje u projektu.
     */
    public static void revoke(X509Certificate cert) {
        revokeCertificate(cert);
    }

    // ---------------------------------------------------------------

    private static String resolveCRLFile(X509Certificate cert) {
        String issuer = cert.getIssuerX500Principal().getName();
        if (issuer.contains("Organizator-CA")) return ORGANIZER_CRL;
        if (issuer.contains("Glasac-CA"))      return VOTER_CRL;
        return ROOT_CRL;
    }

    @SuppressWarnings("unchecked")
    private static List<String> load(String fileName) {
        File file = new File(fileName);
        if (!file.exists()) return new ArrayList<>();
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file))) {
            return (List<String>) ois.readObject();
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    private static void save(String fileName, List<String> serials) throws Exception {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(fileName))) {
            oos.writeObject(serials);
        }
    }
}