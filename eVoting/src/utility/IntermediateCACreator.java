package utility;

import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.BasicConstraints;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.KeyUsage;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;

import java.math.BigInteger;
import java.security.*;
import java.security.cert.X509Certificate;
import java.util.Date;

public class IntermediateCACreator {

    /**
     * Kreira podređeno CA tijelo (Organizaciono ili Glasačko)
     */
    public static X509Certificate createIntermediateCA(
            String commonName, 
            KeyPair intermediateKeyPair, 
            X509Certificate rootCert, 
            PrivateKey rootPrivateKey) throws Exception {

        X500Name subject = new X500Name("CN=" + commonName + ", O=ETF-BL, C=BA");
        X500Name issuer = X500Name.getInstance(rootCert.getSubjectX500Principal().getEncoded());
        //X500Name issuer = new X500Name(rootCert.getSubjectX500Principal().getName());
        
        BigInteger serial = BigInteger.valueOf(System.currentTimeMillis());
        Date notBefore = new Date();
        Date notAfter = new Date(System.currentTimeMillis() + (5L * 365 * 24 * 60 * 60 * 1000)); // 5 godina

        X509v3CertificateBuilder certBuilder = new JcaX509v3CertificateBuilder(
                issuer, 
                serial, 
                notBefore, 
                notAfter, 
                subject, 
                intermediateKeyPair.getPublic()
        );

        // Ekstenzije: Ovo je i dalje CA, ali podređen (Intermediate)
        certBuilder.addExtension(Extension.basicConstraints, true, new BasicConstraints(true));
        
        // KeyUsage: Može potpisivati sertifikate korisnika i CRL liste [cite: 24, 25]
        certBuilder.addExtension(Extension.keyUsage, true, new KeyUsage(KeyUsage.keyCertSign | KeyUsage.cRLSign));

        // Potpisivanje se vrši PRIVATNIM KLJUČEM ROOT CA
        ContentSigner signer = new JcaContentSignerBuilder("SHA256WithRSAEncryption")
                .setProvider("BC")
                .build(rootPrivateKey);

        X509CertificateHolder holder = certBuilder.build(signer);
        
        return new JcaX509CertificateConverter().setProvider("BC").getCertificate(holder);
    }
}
