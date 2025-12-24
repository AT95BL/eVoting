package utility;

import java.security.*;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

public class KeyUtils {
    static {
        Security.addProvider(new BouncyCastleProvider());
    }

    public static KeyPair generateRSAKeyPair() throws Exception {
        KeyPairGenerator g = KeyPairGenerator.getInstance("RSA", "BC");
        g.initialize(2048);
        return g.generateKeyPair();
    }
}
