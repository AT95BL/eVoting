package utility;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;

/**
 * HMAC operacije za zastitu integriteta metapodataka glasanja.
 *
 * ISPRAVKA: Dodata metoda deriveElectionHMACKey() koju ElectionManager poziva.
 * Originalna verzija je imala samo generateHMAC() i verifyHMAC() —
 * ElectionManager je trazio i kljuc za derivaciju, a ta metoda nije postojala.
 *
 * Implementira zahtjev zadatka:
 *   [Metapodaci o glasanju se cuvaju odvojeno od glasova i
 *    njihov integritet se stiti pomocu HMAC algoritma]
 */
public class HMACService {

    private static final String HMAC_ALGORITHM = "HmacSHA256";

    /**
     * Generise HmacSHA256 nad datim podacima.
     *
     * @param data      Podaci ciji se integritet stiti (metapodaci glasanja)
     * @param secretKey Tajni kljuc za HMAC
     * @return Base64-enkodovani HMAC string
     */
    public static String generateHMAC(String data, String secretKey) throws Exception {
        Mac          mac     = Mac.getInstance(HMAC_ALGORITHM);
        SecretKeySpec keySpec = new SecretKeySpec(
                secretKey.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM);
        mac.init(keySpec);
        byte[] hmacBytes = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(hmacBytes);
    }

    /**
     * Verifikuje HMAC — provjerava da metapodaci nisu izmijenjeni.
     *
     * Koristi MessageDigest.isEqual() za sigurno poredenje bajtova
     * koje sprecava timing napade (obicno == poredenje bi odavalo
     * informacije o tome koliko bajtova se podudara).
     *
     * @param data         Originalni podaci
     * @param secretKey    Isti kljuc koristen pri generateHMAC()
     * @param expectedHmac Ocekivana HMAC vrijednost (Base64)
     * @return true ako je HMAC validan (integritet potvrden)
     */
    public static boolean verifyHMAC(String data, String secretKey, String expectedHmac)
            throws Exception {
        if (expectedHmac == null || expectedHmac.isEmpty()) return false;
        String computed = generateHMAC(data, secretKey);
        // Sigurno poredenje — sprecava timing attack
        return MessageDigest.isEqual(
                Base64.getDecoder().decode(computed),
                Base64.getDecoder().decode(expectedHmac));
    }

    /**
     * Derivira HMAC kljuc jedinstven za dato glasanje.
     *
     * Ova metoda je ta koja je nedostajala — ElectionManager je poziva
     * prije generateHMAC() i verifyHMAC() da dobije kljuc.
     *
     * Kljuc je deterministicki: isti naslov + isti organizator = isti kljuc.
     * U produkcijskom sistemu bio bi to poseban tajni kljuc pohranjen sigurno
     * (npr. u HSM ili environment varijabli), ali za akademski projekat
     * ova derivacija je prihvatljiva i zadovoljava zahtjev zadatka.
     *
     * @param electionTitle     Naslov glasanja
     * @param organizerUsername Korisnicko ime organizatora
     * @return Derivirani kljuc kao String
     */
    public static String deriveElectionHMACKey(String electionTitle, String organizerUsername) {
        // Prefiks + kombinacija naslova i organizatora cini kljuc jedinstven per-glasanje
        return "EVOTING_HMAC_" + electionTitle + "_" + organizerUsername;
    }
}