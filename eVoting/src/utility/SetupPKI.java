package utility;
import java.security.KeyPair;
import java.security.cert.X509Certificate;

public class SetupPKI {
    public static void main(String[] args) {
        try {
            String pass = "sigurnost2026"; // Lozinka za zaštitu ključeva 

            // 1. Kreiraj Root CA
            KeyPair rootKP = KeyUtils.generateRSAKeyPair();
            X509Certificate rootCert = RootCACreator.createRootCA(rootKP);
            KeyStoreManager.saveToKeyStore("root_ca.p12", "root", rootKP.getPrivate(), pass, rootCert, null);
            System.out.println("Root CA kreiran i sačuvan.");

            // 2. Kreiraj Organizator CA (potpisan od Root CA) 
            KeyPair orgKP = KeyUtils.generateRSAKeyPair();
            X509Certificate orgCert = IntermediateCACreator.createIntermediateCA(
                    "Organizator-CA", orgKP, rootCert, rootKP.getPrivate());
            KeyStoreManager.saveToKeyStore("organizer_ca.p12", "org_ca", orgKP.getPrivate(), pass, orgCert, rootCert);
            System.out.println("Organizator CA kreiran i sačuvan.");

            // 3. Kreiraj Glasač CA (potpisan od Root CA) [cite: 23]
            KeyPair voterKP = KeyUtils.generateRSAKeyPair();
            X509Certificate voterCert = IntermediateCACreator.createIntermediateCA(
                    "Glasac-CA", voterKP, rootCert, rootKP.getPrivate());
            KeyStoreManager.saveToKeyStore("voter_ca.p12", "voter_ca", voterKP.getPrivate(), pass, voterCert, rootCert);
            System.out.println("Glasač CA kreiran i sačuvan.");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}