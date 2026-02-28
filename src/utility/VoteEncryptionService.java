package utility;

import model.EncryptedVote;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Base64;  // <-- java.util.Base64, standardno od Java 8

public class VoteEncryptionService {

    private static final String AES_ALGORITHM = "AES";
    private static final String AES_TRANSFORM = "AES/CBC/PKCS5Padding";
    private static final String RSA_TRANSFORM = "RSA/ECB/OAEPWithSHA-256AndMGF1Padding";
    private static final String SIGN_ALGORITHM = "SHA256withRSA";
    private static final int    AES_KEY_BITS  = 256;
    private static final int    AES_IV_BYTES  = 16;

    public static EncryptedVote encryptAndSign(String candidateName,
                                               String electionTitle,
                                               String voterUsername,
                                               PrivateKey voterPrivateKey,
                                               X509Certificate voterCert,
                                               PublicKey organizerPublicKey) throws Exception {

        // KORAK 1: Nasumicni AES-256 kljuc
        KeyGenerator keyGen = KeyGenerator.getInstance(AES_ALGORITHM);
        keyGen.init(AES_KEY_BITS, new SecureRandom());
        SecretKey aesKey = keyGen.generateKey();

        // KORAK 2: Enkripcija glasa AES/CBC
        Cipher aesCipher = Cipher.getInstance(AES_TRANSFORM);
        aesCipher.init(Cipher.ENCRYPT_MODE, aesKey, new SecureRandom());
        byte[] iv            = aesCipher.getIV();
        byte[] encryptedVote = aesCipher.doFinal(candidateName.getBytes(StandardCharsets.UTF_8));

        // Spoji [IV(16B) | ciphertext]
        byte[] ivAndCiphertext = new byte[iv.length + encryptedVote.length];
        System.arraycopy(iv,            0, ivAndCiphertext, 0,         iv.length);
        System.arraycopy(encryptedVote, 0, ivAndCiphertext, iv.length, encryptedVote.length);

        // KORAK 3: Enkripcija AES kljuca javnim kljucem organizatora (RSA/OAEP)
        Cipher rsaCipher = Cipher.getInstance(RSA_TRANSFORM);
        rsaCipher.init(Cipher.ENCRYPT_MODE, organizerPublicKey);
        byte[] encryptedAesKey = rsaCipher.doFinal(aesKey.getEncoded());

        // KORAK 4: Digitalni potpis glasaca (SHA256withRSA)
        Signature signer = Signature.getInstance(SIGN_ALGORITHM);
        signer.initSign(voterPrivateKey);
        signer.update(ivAndCiphertext);
        byte[] signature = signer.sign();

        // Hash korisnickog imena — java.util.Base64, dostupan od Java 8
        MessageDigest md           = MessageDigest.getInstance("SHA-256");
        String        usernameHash = Base64.getEncoder().encodeToString(
                md.digest(voterUsername.getBytes(StandardCharsets.UTF_8)));

        return new EncryptedVote(
                ivAndCiphertext,
                encryptedAesKey,
                signature,
                voterCert.getEncoded(),
                usernameHash,
                electionTitle
        );
    }

    public static boolean verifyVoteSignature(EncryptedVote vote) throws Exception {
        CertificateFactory cf        = CertificateFactory.getInstance("X.509");
        X509Certificate    voterCert = (X509Certificate) cf.generateCertificate(
                new ByteArrayInputStream(vote.getVoterCertEncoded()));

        Signature verifier = Signature.getInstance(SIGN_ALGORITHM);
        verifier.initVerify(voterCert.getPublicKey());
        verifier.update(vote.getEncryptedVoteData());
        return verifier.verify(vote.getDigitalSignature());
    }

    public static String decryptVote(EncryptedVote vote, PrivateKey organizerPrivateKey) throws Exception {
        // Dekriptuj AES kljuc privatnim kljucem organizatora
        Cipher rsaCipher = Cipher.getInstance(RSA_TRANSFORM);
        rsaCipher.init(Cipher.DECRYPT_MODE, organizerPrivateKey);
        byte[]    aesKeyBytes = rsaCipher.doFinal(vote.getEncryptedSymmetricKey());
        SecretKey aesKey      = new SecretKeySpec(aesKeyBytes, AES_ALGORITHM);

        // Izdvoji IV i ciphertext
        byte[] ivAndCiphertext = vote.getEncryptedVoteData();
        byte[] iv         = Arrays.copyOfRange(ivAndCiphertext, 0,           AES_IV_BYTES);
        byte[] ciphertext = Arrays.copyOfRange(ivAndCiphertext, AES_IV_BYTES, ivAndCiphertext.length);

        // Dekriptuj glas
        Cipher aesCipher = Cipher.getInstance(AES_TRANSFORM);
        aesCipher.init(Cipher.DECRYPT_MODE, aesKey, new IvParameterSpec(iv));
        byte[] decryptedBytes = aesCipher.doFinal(ciphertext);

        return new String(decryptedBytes, StandardCharsets.UTF_8);
    }

    public static String hashUsername(String username) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        return Base64.getEncoder().encodeToString(
                md.digest(username.getBytes(StandardCharsets.UTF_8)));
    }
}