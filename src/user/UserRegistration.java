package user;

import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.BasicConstraints;
import org.bouncycastle.asn1.x509.ExtendedKeyUsage;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.KeyPurposeId;
import org.bouncycastle.asn1.x509.KeyUsage;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509ExtensionUtils;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import utility.KeyStoreManager;
import utility.KeyUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.math.BigInteger;
import java.security.KeyPair;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.Date;

public class UserRegistration {

    // CA lozinka — postavlja jednom SetupPKI ili MainMenu pri pokretanju.
    // Ovo eliminise potrebu za hard-kodovanom lozinkom.
    private static String caPassword = null;

    /**
     * Postavlja lozinku kojom su zasticeni CA keystoreovi.
     * Mora biti pozvano PRIJE register() — tipicno u SetupPKI.main()
     * ili u MainMenu.main() pri prvom pokretanju.
     */
    public static void setCAPassword(String password) {
        caPassword = password;
    }

    /**
     * Registruje novog korisnika: generiše RSA par kljuceva,
     * kreira X.509 sertifikat potpisan odgovarajucim CA,
     * cuva .p12 fajl zasticen korisnickovom lozinkom,
     * i eksportuje javni sertifikat u public_certs/.
     *
     * @param cn       Common Name (naziv organizacije ili ime glasaca)
     * @param username Korisnicko ime (alias u KeyStore-u, ime .p12 fajla)
     * @param password Lozinka kojom se stiti korisnikov .p12 fajl
     * @param type     "VOTER" ili "ORGANIZER"
     */
    public static void register(String cn, String username, String password, String type)
            throws Exception {

        if (caPassword == null) {
            throw new IllegalStateException(
                "CA lozinka nije postavljena!\n" +
                "Rjesenje: Pozovite UserRegistration.setCAPassword(pass) " +
                "prije registracije korisnika.");
        }

        // Odaberi odgovarajuci CA na osnovu tipa korisnika
        String caFile  = type.equals("VOTER") ? "voter_ca.p12"    : "organizer_ca.p12";
        String caAlias = type.equals("VOTER") ? "voter_ca"        : "org_ca";

        // Ucitaj CA keystoroe i izvuci privatni kljuc + sertifikat
        KeyStore        caKS      = KeyStoreManager.loadKeyStore(caFile, caPassword);
        PrivateKey      caPrivKey = (PrivateKey) caKS.getKey(caAlias, caPassword.toCharArray());
        X509Certificate caCert    = (X509Certificate) caKS.getCertificate(caAlias);

        if (caPrivKey == null) {
            throw new Exception("Nije moguce ucitati CA privatni kljuc. " +
                                "Provjerite da je CA lozinka ispravna.");
        }

        // Generiši RSA-2048 par kljuceva za korisnika
        KeyPair keyPair = KeyUtils.generateRSAKeyPair();

        // Izgradi DN (Distinguished Name) subjekta
        // OU sadrzi username — koristimo ga pri validaciji sertifikata
        X500Name subject = new X500Name("CN=" + cn + ", OU=" + username);
        X500Name issuer  = X500Name.getInstance(caCert.getSubjectX500Principal().getEncoded());

        // Nasumican serijski broj (sigurniji od System.currentTimeMillis())
        BigInteger serial    = new BigInteger(128, new SecureRandom());
        Date       notBefore = new Date();
        Date       notAfter  = new Date(System.currentTimeMillis() + 365L * 24 * 60 * 60 * 1000);

        JcaX509v3CertificateBuilder certBuilder = new JcaX509v3CertificateBuilder(
                issuer, serial, notBefore, notAfter, subject, keyPair.getPublic());

        JcaX509ExtensionUtils extUtils = new JcaX509ExtensionUtils();

        // Standardne ekstenzije
        certBuilder.addExtension(Extension.subjectKeyIdentifier, false,
                extUtils.createSubjectKeyIdentifier(keyPair.getPublic()));
        certBuilder.addExtension(Extension.authorityKeyIdentifier, false,
                extUtils.createAuthorityKeyIdentifier(caCert));
        certBuilder.addExtension(Extension.basicConstraints, true,
                new BasicConstraints(false));  // nije CA sertifikat

        // KeyUsage — razlicit za organizatora i glasaca (zahtjev zadatka)
        if (type.equals("ORGANIZER")) {
            // Organizator: prima enkriptovane glasove (keyEncipherment) i potpisuje izvjestaje
            certBuilder.addExtension(Extension.keyUsage, true,
                    new KeyUsage(KeyUsage.digitalSignature | KeyUsage.keyEncipherment));
            certBuilder.addExtension(Extension.extendedKeyUsage, false,
                    new ExtendedKeyUsage(KeyPurposeId.id_kp_emailProtection));
        } else {
            // Glasac: potpisuje glasove (digitalSignature + nonRepudiation)
            certBuilder.addExtension(Extension.keyUsage, true,
                    new KeyUsage(KeyUsage.digitalSignature | KeyUsage.nonRepudiation));
            certBuilder.addExtension(Extension.extendedKeyUsage, false,
                    new ExtendedKeyUsage(KeyPurposeId.id_kp_clientAuth));
        }

        // Potpiši sertifikat privatnim kljucem CA
        ContentSigner   signer   = new JcaContentSignerBuilder("SHA256WithRSAEncryption")
                .setProvider("BC").build(caPrivKey);
        X509Certificate userCert = new JcaX509CertificateConverter()
                .setProvider("BC").getCertificate(certBuilder.build(signer));

        // Sačuvaj korisnikov .p12 (privatni kljuc + sertifikat, zasticeno lozinkom)
        String p12FileName = username + ".p12";
        KeyStoreManager.saveToKeyStore(p12FileName, username, keyPair.getPrivate(),
                password, userCert, caCert);
        System.out.println("USPJEH: Kljucevi i sertifikat sacuvani u: " + p12FileName);

        // Eksportuj javni sertifikat u public_certs/<username>.cer
        // Ovaj fajl je "javan" — glasaci ga citaju da bi enkriptovali glasove organizatoru
        exportPublicCert(username, userCert);
    }

    /**
     * Eksportuje javni sertifikat u DER format u folder public_certs/.
     * Javni sertifikat nije tajna — svi ga mogu citati.
     */
    private static void exportPublicCert(String username, X509Certificate cert) throws Exception {
        File dir = new File("public_certs/");
        if (!dir.exists()) dir.mkdirs();

        File certFile = new File(dir, username + ".cer");
        try (FileOutputStream fos = new FileOutputStream(certFile)) {
            fos.write(cert.getEncoded());  // DER format
        }
        System.out.println("SISTEM: Javni sertifikat eksportovan u: public_certs/" + username + ".cer");
    }
}