package main;

import model.Election;
import utility.ElectionManager;
import java.util.List;
import java.util.Scanner;

public class VoterPanel {
    public static void showVoterMenu(String username) {
        Scanner sc = new Scanner(System.in);
        while (true) {
            System.out.println("\n--- GLASAČKI PANEL (Korisnik: " + username + ") ---");
            System.out.println("1. Pregledaj dostupna glasanja i glasaj");
            System.out.println("2. Odjava");
            System.out.print("Izbor: ");

            String choice = sc.nextLine();
            if (choice.equals("1")) {
                List<Election> allElections = ElectionManager.loadAllElections();
                if (allElections.isEmpty()) {
                    System.out.println("Trenutno nema aktivnih glasanja.");
                    continue;
                }

                System.out.println("\nDOSTUPNA GLASANJA:");
                for (int i = 0; i < allElections.size(); i++) {
                    System.out.println((i + 1) + ". " + allElections.get(i).getTitle());
                }

                System.out.print("Izaberite broj glasanja (ili 0 za nazad): ");
                int eIdx = Integer.parseInt(sc.nextLine()) - 1;
                if (eIdx < 0) continue;

                Election selected = allElections.get(eIdx);

                // PROVJERA: Da li je korisnik već glasao?
                if (selected.hasVoted(username)) {
                    System.out.println("GREŠKA: Već ste glasali na ovom glasanju!");
                    continue;
                }

                System.out.println("\nKandidati za '" + selected.getTitle() + "':");
                List<String> candidates = selected.getCandidates();
                for (int j = 0; j < candidates.size(); j++) {
                    System.out.println((j + 1) + ". " + candidates.get(j));
                }

                System.out.print("Vaš glas (unesite broj kandidata): ");
                int cIdx = Integer.parseInt(sc.nextLine()) - 1;
                
                if (cIdx >= 0 && cIdx < candidates.size()) {
                    String chosenCandidate = candidates.get(cIdx);
                    
                    // Registrovanje glasa
                    selected.addVote(chosenCandidate, username);
                    
                    try {
                        // Čuvanje ažuriranog stanja glasanja
                        ElectionManager.saveElection(selected);
                        System.out.println("USPJEH: Vaš glas za '" + chosenCandidate + "' je zabilježen!");
                    } catch (Exception ex) {
                        System.out.println("Greška pri čuvanju glasa: " + ex.getMessage());
                    }
                }
            } else break;
        }
    }
}