package utility;

import java.security.cert.X509Certificate;
import java.util.HashSet;
import java.util.Set;

public class CRLManager {
    // Simulacija liste povučenih serijskih brojeva sertifikata
    private static Set<String> revokedSerials = new HashSet<>();

    public static void revoke(X509Certificate cert) {
        revokedSerials.add(cert.getSerialNumber().toString());
    }

    public static boolean isRevoked(X509Certificate cert) {
        return revokedSerials.contains(cert.getSerialNumber().toString());
    }
}