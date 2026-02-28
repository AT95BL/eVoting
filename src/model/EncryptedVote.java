package model;

import java.io.Serializable;

/**
 * Jedan enkriptovani glas.
 *
 * ISPRAVKA: Dodat timestamp field i getTimestamp() getter
 * koje VoterPanel koristi pri verifikaciji glasa.
 */
public class EncryptedVote implements Serializable {
    private static final long serialVersionUID = 1L;

    private final byte[] encryptedVoteData;     // [IV(16B) | AES ciphertext]
    private final byte[] encryptedSymmetricKey; // AES kljuc enkriptovan RSA/OAEP jav. kljucem org.
    private final byte[] digitalSignature;      // SHA256withRSA potpis glasaca nad encryptedVoteData
    private final byte[] voterCertEncoded;      // DER sertifikat glasaca (za verifikaciju potpisa)
    private final String voterUsernameHash;     // SHA-256 hash korisnickog imena (djelimicna anonimnost)
    private final String electionTitle;
    private final long   timestamp;             // Vrijeme glasanja (System.currentTimeMillis())

    public EncryptedVote(byte[] encryptedVoteData,
                         byte[] encryptedSymmetricKey,
                         byte[] digitalSignature,
                         byte[] voterCertEncoded,
                         String voterUsernameHash,
                         String electionTitle) {
        this.encryptedVoteData     = encryptedVoteData;
        this.encryptedSymmetricKey = encryptedSymmetricKey;
        this.digitalSignature      = digitalSignature;
        this.voterCertEncoded      = voterCertEncoded;
        this.voterUsernameHash     = voterUsernameHash;
        this.electionTitle         = electionTitle;
        this.timestamp             = System.currentTimeMillis(); // automatski pri kreiranju
    }

    public byte[] getEncryptedVoteData()      { return encryptedVoteData; }
    public byte[] getEncryptedSymmetricKey()  { return encryptedSymmetricKey; }
    public byte[] getDigitalSignature()       { return digitalSignature; }
    public byte[] getVoterCertEncoded()       { return voterCertEncoded; }
    public String getVoterUsernameHash()      { return voterUsernameHash; }
    public String getElectionTitle()          { return electionTitle; }
    public long   getTimestamp()              { return timestamp; } // <-- VoterPanel.handleVerification() treba ovo
}