package user;

import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import java.math.BigInteger;
import java.security.*;
import java.security.cert.X509Certificate;
import java.util.Date;

import utility.*;

public class UserRegistration {
    public static void register(String cn, String username, String password, String type) throws Exception {
        KeyPair keyPair = KeyUtils.generateRSAKeyPair();
        
        // Odabir CA tijela na osnovu tipa korisnika 
        String caFile = type.equals("VOTER") ? "voter_ca.p12" : "organizer_ca.p12";
        String caAlias = type.equals("VOTER") ? "voter_ca" : "org_ca";
        
        KeyStore ks = KeyStoreManager.loadKeyStore(caFile, "sigurnost2026");
        PrivateKey caPrivKey = (PrivateKey) ks.getKey(caAlias, "sigurnost2026".toCharArray());
        X509Certificate caCert = (X509Certificate) ks.getCertificate(caAlias);

        X500Name subject = new X500Name("CN=" + cn);
        X500Name issuer = X500Name.getInstance(caCert.getSubjectX500Principal().getEncoded());

        JcaX509v3CertificateBuilder certBuilder = new JcaX509v3CertificateBuilder(
            issuer, BigInteger.valueOf(System.currentTimeMillis()), new Date(),
            new Date(System.currentTimeMillis() + 365L*24*60*60*1000), subject, keyPair.getPublic());

        ContentSigner signer = new JcaContentSignerBuilder("SHA256WithRSAEncryption").setProvider("BC").build(caPrivKey);
        X509Certificate userCert = new JcaX509CertificateConverter().setProvider("BC").getCertificate(certBuilder.build(signer));

        // Čuvanje sertifikata i ključa zaštićenog lozinkom [cite: 9]
        String fileName = username + ".p12";
        KeyStoreManager.saveToKeyStore(fileName, username, keyPair.getPrivate(), password, userCert, caCert);
        System.out.println("\nUSPJEH: Kreiran sertifikat " + fileName);
    }
}