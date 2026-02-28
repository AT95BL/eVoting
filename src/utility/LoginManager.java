package utility;

import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Upravlja dvostepenom prijavom korisnika.
 *
 * Korak 1: Validacija digitalnog sertifikata iz .p12 fajla
 *   A) Vremenska validnost
 *   B) Provjera izdavaca (Organizator-CA ili Glasac-CA)
 *   C) CRL lista
 *   D) Dostupnost privatnog kljuca (potvrda lozinke)
 *
 * Korak 2: Provjera lozinke
 *   — U PKCS#12 sistemu, uspjesno otvaranje keystora = ispravna lozinka.
 *     Dodatno provjeravamo da je privatni kljuc dostupan.
 *
 * Vracа UserLoginResult koji sadrži sve podatke o prijavljenom korisniku.
 * Ovaj objekat se prosljedjuje VoterPanel-u ili OrganizerMenu-u i eliminise
 * potrebu za ponovnim ucitavanjem .p12 fajla.
 *
 * ISPRAVKE u odnosu na originalnu verziju:
 *   - Validacija se vrsi PRIJE prikaza menija (ne poslije!)
 *   - Vise ne vraca boolean nego UserLoginResult (nosi tip korisnika i lozinku)
 *   - Prati neuspjesne pokusaje i automatski povlaci sertifikat na 3. gresku
 *   - Uklonjen cirkularni import (LoginManager vise ne zna za MainMenu/VoterPanel)
 */
public class LoginManager {

    private static final int MAX_FAILED = 3;

    // username -> broj neuspjesnih pokusaja (u memoriji, za trajanje sesije)
    private static final Map<String, Integer> failedAttempts = new HashMap<>();

    /**
     * Pokusaj prijave.
     *
     * @param p12Path  Putanja do korisnickog .p12 fajla
     * @param username Korisnicko ime (alias u keystoru)
     * @param password Lozinka za otkljucavanje .p12
     * @return UserLoginResult ako je prijava uspjesna, null ako nije
     */
    public static UserLoginResult login(String p12Path, String username, String password) {

        System.out.println("\n=== KORAK 1: Validacija digitalnog sertifikata ===");

        // Blokirani nalog?
        if (failedAttempts.getOrDefault(username, 0) >= MAX_FAILED) {
            System.out.println("  [x] NALOG BLOKIRAN: Previse neuspjesnih pokusaja prijave.");
            return null;
        }

        KeyStore        ks;
        X509Certificate userCert;

        // --- 1a: Otvori .p12 fajl ---
        try {
            ks = KeyStoreManager.loadKeyStore(p12Path, password);
        } catch (Exception e) {
            System.out.println("  [x] Nije moguce otvoriti .p12 fajl.");
            System.out.println("      Razlog: " + e.getMessage());
            registerFailed(username, null);
            return null;
        }

        // --- 1b: Pronađi sertifikat po aliasom (username) ---
        try {
            userCert = (X509Certificate) ks.getCertificate(username);
            if (userCert == null) {
                System.out.println("  [x] Sertifikat za korisnika '" + username +
                                   "' nije pronadjen u fajlu.");
                registerFailed(username, null);
                return null;
            }
        } catch (Exception e) {
            System.out.println("  [x] Greska pri citanju sertifikata: " + e.getMessage());
            registerFailed(username, null);
            return null;
        }

        // --- 1c: Provjera A — vremenska validnost ---
        try {
            userCert.checkValidity(new Date());
            System.out.println("  [ok] Sertifikat je vremenski validan.");
        } catch (Exception e) {
            System.out.println("  [x] Sertifikat je istekao ili jos nije aktivan.");
            registerFailed(username, userCert);
            return null;
        }

        // --- 1d: Provjera B — izdavac mora biti nas CA ---
        String issuerDN = userCert.getIssuerX500Principal().getName();
        String userType;
        if (issuerDN.contains("Organizator-CA")) {
            userType = "ORGANIZER";
            System.out.println("  [ok] Izdat od Organizator-CA. Tip: ORGANIZATOR.");
        } else if (issuerDN.contains("Glasac-CA")) {
            userType = "VOTER";
            System.out.println("  [ok] Izdat od Glasac-CA. Tip: GLASAC.");
        } else {
            System.out.println("  [x] Sertifikat nije izdat od priznatog CA tijela.");
            System.out.println("      Izdavac: " + issuerDN);
            registerFailed(username, userCert);
            return null;
        }

        // --- 1e: Provjera C — CRL lista ---
        if (CRLManager.isRevoked(userCert)) {
            System.out.println("  [x] Sertifikat je POVUCEN (nalazi se na CRL listi)!");
            return null;  // Ne registrujemo pokusaj, sertifikat je vec povucen
        }
        System.out.println("  [ok] Sertifikat nije na CRL listi.");

        System.out.println("\n=== KORAK 2: Provjera lozinke ===");

        // --- 2: Provjera D — privatni kljuc dostupan = lozinka ispravna ---
        try {
            PrivateKey privKey = (PrivateKey) ks.getKey(username, password.toCharArray());
            if (privKey == null) {
                System.out.println("  [x] Privatni kljuc nije dostupan za alias '" + username + "'.");
                registerFailed(username, userCert);
                return null;
            }
            System.out.println("  [ok] Lozinka ispravna, privatni kljuc ucitan.");
        } catch (Exception e) {
            System.out.println("  [x] Neispravna lozinka.");
            registerFailed(username, userCert);
            return null;
        }

        // Prijava uspjesna — resetuj brojac
        failedAttempts.remove(username);
        System.out.println("\n  PRIJAVA USPJESNA! Dobrodosli, " + username + " (" + userType + ")");

        return new UserLoginResult(username, userType, userCert, password, p12Path);
    }

    // ----------------------------------------------------------------

    private static void registerFailed(String username, X509Certificate cert) {
        int count = failedAttempts.getOrDefault(username, 0) + 1;
        failedAttempts.put(username, count);
        System.out.println("  Neuspjesnih pokusaja: " + count + "/" + MAX_FAILED);

        if (count >= MAX_FAILED) {
            System.out.println("  *** Dostignut maksimum neuspjesnih pokusaja! ***");
            if (cert != null) {
                CRLManager.revokeCertificate(cert);
                System.out.println("  SISTEM: Sertifikat je AUTOMATSKI POVUCEN.");
            } else {
                System.out.println("  SISTEM: Nalog je blokiran (sertifikat nije bio dostupan).");
            }
        }
    }

    // ----------------------------------------------------------------

    /**
     * Sadrzi sve podatke o uspjesno prijavljenom korisniku.
     *
     * Prosljedjuje se panelima (VoterPanel, showOrganizerMenu) da ne bi
     * morali ponovo ucitavati .p12 fajl pri svakoj operaciji.
     */
    public static class UserLoginResult {
        public final String          username;
        public final String          userType;     // "VOTER" ili "ORGANIZER"
        public final X509Certificate certificate;
        public final String          password;     // Cuva se za kasniji pristup privatnom kljucu
        public final String          p12Path;

        public UserLoginResult(String username, String userType,
                               X509Certificate certificate, String password, String p12Path) {
            this.username    = username;
            this.userType    = userType;
            this.certificate = certificate;
            this.password    = password;
            this.p12Path     = p12Path;
        }
    }
}