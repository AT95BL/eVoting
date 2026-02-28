package model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Metapodaci jednog glasanja.
 *
 * VAZNO: Ova klasa NE cuva glasove niti rezultate u plaintextu!
 *   - Glasovi se cuvaju odvojeno kao EncryptedVote objekti
 *     u folderu elections/<naziv>_votes/
 *   - Integritet metapodataka stiti se HMAC algoritmom
 *   - Rezultati se racunaju tek kad organizator pokrene brojanje
 *
 * Zadovoljeni zahtjevi:
 *   [Metapodaci o glasanju se cuvaju odvojeno od glasova]
 *   [Njihov integritet se stiti pomocu HMAC algoritma]
 */
public class Election implements Serializable {
    private static final long serialVersionUID = 2L;

    private String       title;
    private String       description;
    private Date         startDate;
    private Date         endDate;
    private List<String> candidates;          // 2-5 opcija

    // Korisnicko ime organizatora - potrebno da glasac moze
    // ucitati javni kljuc organizatora za enkripciju glasa
    private String       organizerUsername;

    // Hashevi korisnickih imena glasaca koji su vec glasali
    // Cuva se hash (SHA-256), ne plaintext - djelimicna anonimnost
    private List<String> votedUserHashes;

    private boolean      active;

    // HmacSHA256 nad getMetadataForHMAC() - stiti integritet
    private String       hmac;

    // Digitalno potpisan izvjestaj organizatora nakon brojanja
    private String       signedReport;

    public Election(String title, String description, Date startDate, Date endDate,
                    List<String> candidates, String organizerUsername) {
        this.title             = title;
        this.description       = description;
        this.startDate         = startDate;
        this.endDate           = endDate;
        this.candidates        = new ArrayList<>(candidates);
        this.organizerUsername = organizerUsername;
        this.votedUserHashes   = new ArrayList<>();
        this.active            = true;
    }

    /**
     * String koji se koristi za racunanje i provjeru HMAC-a.
     * Ako se bilo koji metapodatak promijeni, HMAC verifikacija ce otkriti izmjenu.
     */
    public String getMetadataForHMAC() {
        return title + "|" +
               description + "|" +
               (startDate != null ? startDate.getTime() : "null") + "|" +
               (endDate   != null ? endDate.getTime()   : "null") + "|" +
               String.join(",", candidates) + "|" +
               organizerUsername;
    }

    /**
     * Provjera da li je glasac (ciji je hash) vec glasao.
     */
    public boolean hasVotedByHash(String usernameHash) {
        return votedUserHashes.contains(usernameHash);
    }

    /**
     * Registruje glasaca kao onog koji je glasao (cuva hash).
     * Poziva se NAKON sto je enkriptovani glas uspjesno sacuvan.
     */
    public void registerVoterHash(String usernameHash) {
        if (!votedUserHashes.contains(usernameHash)) {
            votedUserHashes.add(usernameHash);
        }
    }

    /**
     * Provjera da li je glasanje trenutno aktivno.
     * Uzima u obzir i vremenski period i active flag.
     */
    public boolean isCurrentlyActive() {
        if (!active) return false;
        Date now = new Date();
        if (startDate != null && now.before(startDate)) return false;
        if (endDate   != null && now.after(endDate))    return false;
        return true;
    }

    // ---------- getteri i setteri ----------

    public String       getTitle()                        { return title; }
    public void         setTitle(String t)                { this.title = t; }

    public String       getDescription()                  { return description; }
    public void         setDescription(String d)          { this.description = d; }

    public Date         getStartDate()                    { return startDate; }
    public void         setStartDate(Date d)              { this.startDate = d; }

    public Date         getEndDate()                      { return endDate; }
    public void         setEndDate(Date d)                { this.endDate = d; }

    public List<String> getCandidates()                   { return new ArrayList<>(candidates); }
    public void         setCandidates(List<String> c)     { this.candidates = c; }

    public String       getOrganizerUsername()            { return organizerUsername; }
    public void         setOrganizerUsername(String u)    { this.organizerUsername = u; }

    public boolean      isActive()                        { return active; }
    public void         setActive(boolean a)              { this.active = a; }

    public String       getHmac()                         { return hmac; }
    public void         setHmac(String hmac)              { this.hmac = hmac; }

    public String       getSignedReport()                 { return signedReport; }
    public void         setSignedReport(String r)         { this.signedReport = r; }

    public List<String> getVotedUserHashes()              { return new ArrayList<>(votedUserHashes); }

    @Override
    public String toString() {
        String status = isCurrentlyActive() ? "AKTIVNO" : (active ? "JOS NIJE POCELO/ISTEKLO" : "ZATVORENO");
        return "Glasanje: " + title + " [" + status + "]";
    }
}