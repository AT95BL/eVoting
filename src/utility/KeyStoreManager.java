package utility;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;

public class KeyStoreManager {

    private static final String KEYSTORE_TYPE = "PKCS12";

    /**
     * Snima privatni ključ i sertifikat u .p12 fajl zaštićen lozinkom.
     */
    public static void saveToKeyStore(String fileName, String alias, PrivateKey privateKey, 
            String password, X509Certificate certificate, 
            X509Certificate issuerCert) throws Exception {

		KeyStore keyStore = KeyStore.getInstance(KEYSTORE_TYPE);
		keyStore.load(null, null);
		
		Certificate[] chain;
		if (issuerCert != null) {
		// Lanac MORA početi sa sertifikatom koji pripada privatnom ključu
		chain = new Certificate[]{certificate, issuerCert};
		} else {
		chain = new Certificate[]{certificate};
		}
		
		// Dodajemo provjeru lanca prije snimanja
		keyStore.setKeyEntry(alias, privateKey, password.toCharArray(), chain);
		
		try (FileOutputStream fos = new FileOutputStream(fileName)) {
		keyStore.store(fos, password.toCharArray());
		}
    }

    /**
     * Učitava KeyStore iz fajla.
     */
    public static KeyStore loadKeyStore(String fileName, String password) throws Exception {
        KeyStore keyStore = KeyStore.getInstance(KEYSTORE_TYPE);
        try (FileInputStream fis = new FileInputStream(fileName)) {
            keyStore.load(fis, password.toCharArray());
        }
        return keyStore;
    }
}