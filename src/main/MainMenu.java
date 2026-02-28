package main;

import model.Election;
import user.UserRegistration;
import utility.ElectionManager;
import utility.LoginManager;
import utility.ReportService;
import utility.VoteStorageManager;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Scanner;

public class MainMenu {

    private static final Scanner sc = new Scanner(System.in);

    public static void main(String[] args) {
        if (java.security.Security.getProvider("BC") == null) {
            java.security.Security.addProvider(
                    new org.bouncycastle.jce.provider.BouncyCastleProvider());
        }

        System.out.println("+========================================+");
        System.out.println("|   SISTEM ZA SIGURNO ONLINE GLASANJE    |");
        System.out.println("+========================================+");

        // CA lozinka je potrebna za registraciju — unosi se jednom pri pokretanju
        System.out.print("Unesite CA lozinku (postavljena pri SetupPKI): ");
        String caPass = sc.nextLine();
        UserRegistration.setCAPassword(caPass);

        while (true) {
            System.out.println("\n1. Registracija Organizatora");
            System.out.println("2. Registracija Glasaca");
            System.out.println("3. Prijava");
            System.out.println("4. Izlaz");
            System.out.print("Izbor: ");

            switch (sc.nextLine().trim()) {
                case "1": handleOrganizerRegistration(); break;
                case "2": handleVoterRegistration();     break;
                case "3": handleLogin();                 break;
                case "4":
                    System.out.println("Dovidjenja!");
                    return;
                default:
                    System.out.println("Neispravan izbor.");
            }
        }
    }

    // ================================================================
    //  REGISTRACIJA
    // ================================================================

    private static void handleOrganizerRegistration() {
        try {
            System.out.println("\n--- REGISTRACIJA ORGANIZATORA ---");
            System.out.print("Naziv organizacije: ");         String org = sc.nextLine();
            System.out.print("ID broj (korisnicko ime): ");   String id  = sc.nextLine();
            System.out.print("Lozinka: ");                    String pw  = sc.nextLine();
            UserRegistration.register(org, id, pw, "ORGANIZER");
            System.out.println("Prijava: korisnicko ime = " + id + ", fajl = " + id + ".p12");
        } catch (Exception e) {
            System.out.println("Greska pri registraciji: " + e.getMessage());
        }
    }

    private static void handleVoterRegistration() {
        try {
            System.out.println("\n--- REGISTRACIJA GLASACA ---");
            System.out.print("Ime i prezime: ");    String name = sc.nextLine();
            System.out.print("Korisnicko ime: ");   String user = sc.nextLine();
            System.out.print("Lozinka: ");          String pw   = sc.nextLine();
            UserRegistration.register(name, user, pw, "VOTER");
            System.out.println("Prijava: korisnicko ime = " + user + ", fajl = " + user + ".p12");
        } catch (Exception e) {
            System.out.println("Greska pri registraciji: " + e.getMessage());
        }
    }

    // ================================================================
    //  PRIJAVA
    // ================================================================

    private static void handleLogin() {
        System.out.println("\n--- PRIJAVA ---");
        System.out.print("Putanja do .p12 fajla (npr. marko.p12): "); String path = sc.nextLine().trim();
        System.out.print("Korisnicko ime: ");                          String user = sc.nextLine().trim();
        System.out.print("Lozinka: ");                                 String pw   = sc.nextLine();

        // login() vraca UserLoginResult (ne boolean) — sadrzi tip korisnika i sve podatke
        LoginManager.UserLoginResult result = LoginManager.login(path, user, pw);

        if (result == null) {
            System.out.println("\nPrijava nije uspjela.");
            return;
        }

        if ("ORGANIZER".equals(result.userType)) {
            showOrganizerMenu(result);
        } else {
            VoterPanel.showVoterMenu(result);
        }
    }

    // ================================================================
    //  ORGANIZATORSKI MENI
    // ================================================================

    public static void showOrganizerMenu(LoginManager.UserLoginResult loginResult) {
        while (true) {
            System.out.println("\n+--------------------------------------+");
            System.out.println("|  ORGANIZATORSKI PANEL - " + loginResult.username);
            System.out.println("+--------------------------------------+");
            System.out.println("| 1. Kreiraj novo glasanje");
            System.out.println("| 2. Pregled glasanja");
            System.out.println("| 3. Pokreni brojanje glasova");
            System.out.println("| 4. Odjava");
            System.out.println("+--------------------------------------+");
            System.out.print("Izbor: ");

            switch (sc.nextLine().trim()) {
                case "1": handleCreateElection(loginResult); break;
                case "2": handleViewElections(loginResult);  break;
                case "3": handleCountVotes(loginResult);     break;
                case "4":
                    System.out.println("Odjava uspjesna.");
                    return;
                default:
                    System.out.println("Neispravan izbor.");
            }
        }
    }

    private static void handleCreateElection(LoginManager.UserLoginResult loginResult) {
        System.out.println("\n--- KREIRANJE NOVOG GLASANJA ---");
        System.out.print("Naslov: "); String title = sc.nextLine();
        System.out.print("Opis: ");   String desc  = sc.nextLine();

        System.out.println("Datum pocetka (dd.MM.yyyy HH:mm, Enter = odmah): ");
        Date startDate = parseDate(sc.nextLine().trim());
        System.out.println("Datum kraja   (dd.MM.yyyy HH:mm, Enter = bez ogranicenja): ");
        Date endDate = parseDate(sc.nextLine().trim());

        List<String> candidates = new ArrayList<>();
        System.out.println("Kandidati (min 2, max 5) — prazna linija za kraj:");
        while (candidates.size() < 5) {
            System.out.printf("  Kandidat %d: ", candidates.size() + 1);
            String c = sc.nextLine().trim();
            if (c.isEmpty()) {
                if (candidates.size() < 2) { System.out.println("  Potrebna su najmanje 2!"); continue; }
                break;
            }
            candidates.add(c);
        }

        try {
            // Election konstruktor trazi i organizerUsername — glasaci ga trebaju
            // da bi mogli pronaci javni kljuc organizatora za enkripciju glasa
            Election election = new Election(title, desc, startDate, endDate,
                                             candidates, loginResult.username);
            ElectionManager.saveElection(election);
            System.out.println("\nSISTEM: Glasanje '" + title + "' kreirano!");
        } catch (Exception e) {
            System.out.println("Greska: " + e.getMessage());
        }
    }

    private static void handleViewElections(LoginManager.UserLoginResult loginResult) {
        List<Election> elections = ElectionManager.loadAllElections();
        if (elections.isEmpty()) { System.out.println("\nNema glasanja."); return; }

        System.out.println("\n--- LISTA GLASANJA ---");
        for (int i = 0; i < elections.size(); i++) {
            Election e      = elections.get(i);
            int      cnt    = VoteStorageManager.countVotes(e.getTitle());
            String   status = e.isCurrentlyActive() ? "[AKTIVNO]" : "[ZATVORENO]";
            System.out.printf("  %d. %s %s  |  Glasova: %d%n", i + 1, e.getTitle(), status, cnt);
        }

        System.out.print("Izaberite za detalje (0 = nazad): ");
        int idx;
        try { idx = Integer.parseInt(sc.nextLine().trim()) - 1; }
        catch (NumberFormatException e) { return; }
        if (idx < 0 || idx >= elections.size()) return;

        Election sel = elections.get(idx);
        System.out.println("\nNaslov:    " + sel.getTitle());
        System.out.println("Opis:      " + sel.getDescription());
        System.out.println("Kandidati: " + String.join(", ", sel.getCandidates()));
        System.out.println("Glasova:   " + VoteStorageManager.countVotes(sel.getTitle()));
        // verifyElectionHMAC je definisana u ElectionManager
        boolean hmacOk = ElectionManager.verifyElectionHMAC(sel);
        System.out.println("HMAC:      " + (hmacOk ? "Integritet potvrđen [ok]" : "NARUSEN! [x]"));

        if (sel.isActive()) {
            System.out.print("\nZatvoriti ovo glasanje? (da/ne): ");
            if (sc.nextLine().trim().equalsIgnoreCase("da")) {
                sel.setActive(false);
                try { ElectionManager.saveElection(sel); System.out.println("Glasanje zatvoreno."); }
                catch (Exception e) { System.out.println("Greska: " + e.getMessage()); }
            }
        }
    }

    private static void handleCountVotes(LoginManager.UserLoginResult loginResult) {
        List<Election> sva      = ElectionManager.loadAllElections();
        List<Election> zatvorena = new ArrayList<>();
        for (Election e : sva) if (!e.isCurrentlyActive()) zatvorena.add(e);

        if (zatvorena.isEmpty()) { System.out.println("\nNema zatvorenih glasanja."); return; }

        System.out.println("\n--- ZATVORENA GLASANJA ---");
        for (int i = 0; i < zatvorena.size(); i++) {
            Election e = zatvorena.get(i);
            System.out.printf("  %d. %s  (glasova: %d)%n",
                              i + 1, e.getTitle(), VoteStorageManager.countVotes(e.getTitle()));
        }

        System.out.print("Izaberite (0 = nazad): ");
        int idx;
        try { idx = Integer.parseInt(sc.nextLine().trim()) - 1; }
        catch (NumberFormatException e) { return; }
        if (idx < 0 || idx >= zatvorena.size()) return;

        System.out.print("Unesite vasu lozinku: ");
        String pw = sc.nextLine();

        try {
            // ReportService dekriptuje glasove i generise potpisan izvjestaj
            String report = ReportService.countVotesAndGenerateReport(
                    zatvorena.get(idx), loginResult.username, pw);
            System.out.println("\n" + report);
        } catch (Exception e) {
            System.out.println("Greska pri brojanju: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ================================================================
    //  POMOCNE METODE
    // ================================================================

    private static Date parseDate(String s) {
        if (s == null || s.isEmpty()) return null;
        try { return new SimpleDateFormat("dd.MM.yyyy HH:mm").parse(s); }
        catch (Exception e) { System.out.println("  Neispravan datum — koristim null."); return null; }
    }
}