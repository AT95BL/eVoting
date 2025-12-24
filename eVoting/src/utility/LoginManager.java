package utility;

import java.security.KeyStore;
import java.security.cert.X509Certificate;
import java.util.Date;

import main.MainMenu;
import main.VoterPanel;

public class LoginManager {

    public static boolean login(String p12Path, String username, String password) {
        try {
            // 1. Učitavanje KeyStore-a (Prvi korak - Validacija sertifikata)
            KeyStore ks = KeyStoreManager.loadKeyStore(p12Path, password);
            X509Certificate userCert = (X509Certificate) ks.getCertificate(username);

            if (userCert == null) {
                System.out.println("Greška: Sertifikat nije pronađen u fajlu.");
                return false;
            }
            
            String issuerDN = userCert.getIssuerX500Principal().getName();
            if (issuerDN.contains("Organizator-CA")) {
                System.out.println("SISTEM: Prepoznat nivo pristupa - ORGANIZATOR.");
                MainMenu.showOrganizerMenu(username);
            } else if (issuerDN.contains("Glasac-CA")) {
                System.out.println("SISTEM: Prepoznat nivo pristupa - GLASAČ.");
                // MainMenu.showVoterMenu(username);
                VoterPanel.showVoterMenu(username);
            }

            // A) Provjera vremenske validnosti
            userCert.checkValidity(new Date());

            // B) Provjera izdavača (da li ga je izdao naš CA)
            // U realnom sistemu ovdje bi išla provjera potpisa pomoću javnog ključa CA
            String issuerName = userCert.getIssuerX500Principal().getName();
            if (!issuerName.contains("Organizator-CA") && !issuerName.contains("Glasac-CA")) {
                System.out.println("Greška: Sertifikat nije izdat od strane priznatog CA tijela.");
                return false;
            }

            // C) Provjera CRL liste
            if (CRLManager.isRevoked(userCert)) {
                System.out.println("Greška: Sertifikat je povučen (CRL)!");
                return false;
            }

            // D) Provjera pripadnosti korisniku (username se mora poklapati sa CN ili aliasom)
            // (Ovdje pretpostavljamo da je alias u .p12 isti kao username)
            
            System.out.println("KORAK 1: Sertifikat je validan.");
            System.out.println("KORAK 2: Unesite lozinku za potvrdu identiteta...");
            
            // U ovom sistemu, ako je uspio otključati .p12 fajl istom lozinkom, 
            // smatramo da je drugi korak uspješan (lozinka ključa = lozinka naloga).
            
            System.out.println("PRIJAVA USPJEŠNA! Dobrodošli, " + username);
            return true;

        } catch (Exception e) {
            System.out.println("Prijava neuspješna: " + e.getMessage());
            // Ovdje bi trebali dodati brojač neuspješnih prijava za automatsko povlačenje (zadatak!)
            return false;
        }
    }
}