package utility;

import java.security.KeyPair;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Scanner;

/**
 * Inicijalizacija PKI hijerarhije.
 *
 * Pokrenuti JEDNOM, prije prvog koristenja sistema.
 * Kreira:
 *   1. Root CA          (root_ca.p12)
 *   2. Organizator CA   (organizer_ca.p12) — potpisuje sertifikate organizatora
 *   3. Glasac CA        (voter_ca.p12)     — potpisuje sertifikate glasaca
 *
 * ISPRAVKA: Lozinka se vise ne hard-koduje kao "sigurnost2026".
 * Cita se interaktivno — ako je pokrenut iz terminala, lozinka
 * je skrivena (Console.readPassword), a ako je pokrenut iz IDE-a
 * (gdje System.console() vraca null), koristi se obicni Scanner.
 */
public class SetupPKI {

    public static void main(String[] args) {
        // Registruj Bouncy Castle
        if (java.security.Security.getProvider("BC") == null) {
            java.security.Security.addProvider(
                    new org.bouncycastle.jce.provider.BouncyCastleProvider());
        }

        System.out.println("+==========================================+");
        System.out.println("|    INITIALIZATION OF THE PKI HIERARC     |");
        System.out.println("+==========================================+");
        System.out.println("This creates a Root CA, an Organizer CA, and a Voter CA.");
        System.out.println("WARNING: Perform only once!\n");

        // --- Unos CA lozinke ---
        String pass = readCAPassword();
        if (pass == null || pass.length() < 8) {
            System.out.println("ERROR: Password must be at least 8 characters long. Giving up.");
            return;
        }

        // Postavi lozinku za UserRegistration da je moze koristiti bez ponovnog unosa
        user.UserRegistration.setCAPassword(pass);

        try {
            // 1. Root CA
            System.out.print("\n[1/3] Creating Root CA... ");
            KeyPair         rootKP   = KeyUtils.generateRSAKeyPair();
            X509Certificate rootCert = RootCACreator.createRootCA(rootKP);
            KeyStoreManager.saveToKeyStore("root_ca.p12", "root", rootKP.getPrivate(), pass, rootCert, null);
            System.out.println("OK  ->  root_ca.p12");

            // 2. Organizator CA — potpisan od Root CA
            System.out.print("[2/3] Creating Organizer CA... ");
            KeyPair         orgKP   = KeyUtils.generateRSAKeyPair();
            X509Certificate orgCert = IntermediateCACreator.createIntermediateCA(
                    "Organizator-CA", orgKP, rootCert, rootKP.getPrivate());
            KeyStoreManager.saveToKeyStore("organizer_ca.p12", "org_ca", orgKP.getPrivate(), pass, orgCert, rootCert);
            System.out.println("OK  ->  organizer_ca.p12");

            // 3. Glasac CA — potpisan od Root CA
            System.out.print("[3/3] Creating Voter CA... ");
            KeyPair         voterKP   = KeyUtils.generateRSAKeyPair();
            X509Certificate voterCert = IntermediateCACreator.createIntermediateCA(
                    "Glasac-CA", voterKP, rootCert, rootKP.getPrivate());
            KeyStoreManager.saveToKeyStore("voter_ca.p12", "voter_ca", voterKP.getPrivate(), pass, voterCert, rootCert);
            System.out.println("OK  ->  voter_ca.p12");

            System.out.println("\n+==========================================+");
            System.out.println("|   PKI HIERARCHY CREATED SUCCESSFULLY!!     |");
            System.out.println("+==========================================+");
            System.out.println("Keep the CA password in a safe place.");
            System.out.println("It is required for registration of new users.");
            System.out.println("\nNext Step: Run AppGUI.");

        } catch (Exception e) {
            System.out.println("\nERROR: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Cita CA lozinku sa konzole.
     *
     * Logika:
     *   - Pravi terminal (npr. cmd, bash): System.console() nije null
     *     => koristimo Console.readPassword() koji SKRIVA unos (kao sudo)
     *   - IDE (IntelliJ, Eclipse): System.console() vraca null
     *     => koristimo Scanner, ali upozoravamo korisnika da je lozinka vidljiva
     */
    private static String readCAPassword() {
        java.io.Console console = System.console();

        if (console != null) {
            // Terminal — lozinka se ne prikazuje na ekranu
            char[] pass1 = console.readPassword("Enter the CA keys password: ");
            char[] pass2 = console.readPassword("Confirm the password: ");

            if (!Arrays.equals(pass1, pass2)) {
                System.out.println("ERROR: Passwords missmatch!");
                Arrays.fill(pass1, '\0');  // Obrisi iz memorije
                Arrays.fill(pass2, '\0');
                return null;
            }

            String pass = new String(pass1);
            Arrays.fill(pass1, '\0');      // Obrisi iz memorije
            Arrays.fill(pass2, '\0');
            return pass;

        } else {
            // IDE environment — System.console() je null
            System.out.println("NOTE: Launched from the IDE. The password will be visible.");
            System.out.println("          For production use run from terminal.");
            System.out.print("Enter password for CA keys (min 8 characters): ");
            return new Scanner(System.in).nextLine();
        }
    }
}
