package utility;

import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.BasicConstraints;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.KeyUsage;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;

import java.math.BigInteger;
import java.security.*;
import java.security.cert.X509Certificate;
import java.util.Date;

public class RootCACreator {

    static {
        // Registracija Bouncy Castle providera
        Security.addProvider(new BouncyCastleProvider());
    }

    public static X509Certificate createRootCA(KeyPair keyPair) throws Exception {
        // 1. Podaci o izdavaču i subjektu (za Root CA su isti)
        X500Name issuer = new X500Name("CN=Root-Voting-CA, O=ETF-BL, C=BA");
        
        // 2. Serijski broj sertifikata
        BigInteger serial = BigInteger.valueOf(System.currentTimeMillis());
        
        // 3. Period važenja
        Date notBefore = new Date();
        Date notAfter = new Date(System.currentTimeMillis() + (10L * 365 * 24 * 60 * 60 * 1000)); // 10 godina

        // 4. Kreiranje buildera za X509 v3 sertifikat
        X509v3CertificateBuilder certBuilder = new JcaX509v3CertificateBuilder(
                issuer, 
                serial, 
                notBefore, 
                notAfter, 
                issuer, 
                keyPair.getPublic()
        );

        // 5. Dodavanje ekstenzija (Kritično za CA hijerarhiju [cite: 24])
        // BasicConstraints(true) označava da je ovo CA sertifikat
        certBuilder.addExtension(Extension.basicConstraints, true, new BasicConstraints(true));
        
        // KeyUsage: Digitalni potpis i potpisivanje sertifikata (KeyCertSign) i CRL-ova (CRLSign)
        certBuilder.addExtension(Extension.keyUsage, true, new KeyUsage(KeyUsage.keyCertSign | KeyUsage.cRLSign));

        // 6. Potpisivanje sertifikata privatnim ključem Root CA (Self-signed)
        ContentSigner signer = new JcaContentSignerBuilder("SHA256WithRSAEncryption")
                .setProvider("BC")
                .build(keyPair.getPrivate());

        X509CertificateHolder holder = certBuilder.build(signer);
        
        // Konverzija u standardni Java X509Certificate format
        return new JcaX509CertificateConverter()
                .setProvider("BC")
                .getCertificate(holder);
    }
    
    /*
    public static void main(String[] args) {
        try {
            // Testiranje kreiranja
            KeyPair rootKeyPair = KeyUtils.generateRSAKeyPair();
            X509Certificate rootCert = createRootCA(rootKeyPair);
            
            System.out.println("Root CA uspješno kreiran!");
            System.out.println("Issuer: " + rootCert.getIssuerX500Principal());
            System.out.println("Validan do: " + rootCert.getNotAfter());
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    */
}
