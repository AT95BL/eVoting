package utility;

import java.io.FileInputStream;
import java.security.*;
import java.security.cert.X509Certificate;

import org.bouncycastle.jce.provider.BouncyCastleProvider;

/**
 * Utility class for cryptographic key operations.
 * This class provides methods to generate security keys using the 
 * Bouncy Castle security provider.
 * * @author AT95BL
 * @version 1.0
 */
public class KeyUtils {
	/**
     * Registers the Bouncy Castle security provider.
     * This static block ensures that the "BC" provider is available 
     * before any cryptographic methods are called.
     */
    static {
        Security.addProvider(new BouncyCastleProvider());
    }
    
    /**
     * Generates an RSA key pair with a strength of 2048 bits.
     * This method explicitly uses the Bouncy Castle ("BC") provider.
     * * @return A {@link KeyPair} containing the generated public and private keys.
     * @throws NoSuchAlgorithmException If the RSA algorithm is not available.
     * @throws NoSuchProviderException If the Bouncy Castle provider is not registered.
     * @throws Exception If any other error occurs during key pair generation.
     */
    public static KeyPair generateRSAKeyPair() throws Exception {
        KeyPairGenerator g = KeyPairGenerator.getInstance("RSA", "BC");
        g.initialize(2048);
        return g.generateKeyPair();
    }
    
    /*
    * Izvlači javni sertifikat iz .p12 fajla bez poznavanja lozinke.
    * Koristi se isključivo za potrebe revokacije (CRL) nakon neuspješnog logina.
    */
   public static X509Certificate getCertificateWithoutPassword(String p12Path, String alias) {
       try (FileInputStream fis = new FileInputStream(p12Path)) {
           // Koristimo standardni "PKCS12" bez navođenja "BC" provajdera
           KeyStore ks = KeyStore.getInstance("PKCS12");
           
           // Neki provajderi dozvoljavaju null lozinku za čitanje sertifikata
           ks.load(fis, null); 
           
           return (X509Certificate) ks.getCertificate(alias);
       } catch (Exception e) {
           // Ako i ovo ne uspe, znači da je fajl potpuno zaključan
           return null;
       }
   }
}
