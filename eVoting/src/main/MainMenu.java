package main;

import java.util.Scanner;
import user.UserRegistration;
import utility.LoginManager;
import utility.ElectionManager;

import model.Election;

import java.util.List;
import java.util.ArrayList;

public class MainMenu {
    private static Scanner scanner = new Scanner(System.in);
    
    public static void showOrganizerMenu(String username) {
        Scanner sc = new Scanner(System.in);
        while (true) {
            System.out.println("\n--- MENI ZA ORGANIZATORE (" + username + ") ---");
            System.out.println("1. Kreiraj novo glasanje");
            System.out.println("2. Pregled rezultata");
            System.out.println("3. Odjava");
            System.out.print("Izbor: ");
            
            int choice = Integer.parseInt(sc.nextLine());
            if (choice == 1) {
                System.out.print("Naslov: "); String title = sc.nextLine();
                System.out.print("Opis: "); String desc = sc.nextLine();
                System.out.print("Kandidati (odvojeni zarezom): ");
                String[] parts = sc.nextLine().split(",");
                List<String> candidates = new ArrayList<>();
                for(String p : parts) candidates.add(p.trim());
                
                try {
                    Election e = new Election(title, desc, candidates);
                    ElectionManager.saveElection(e);
                    System.out.println("SISTEM: Glasanje uspješno sačuvano u folder 'elections/'!");
                } catch (Exception ex) {
                    System.out.println("Greška: " + ex.getMessage());
                }
            }
	        else if (choice == 2) {
	            List<Election> allElections = ElectionManager.loadAllElections();
	            if (allElections.isEmpty()) {
	                System.out.println("Nema kreiranih glasanja.");
	                continue;
	            }
	
	            System.out.println("\n--- VAŠA GLASANJA ---");
	            for (int i = 0; i < allElections.size(); i++) {
	                Election e = allElections.get(i);
	                String status = e.isActive() ? "[AKTIVNO]" : "[ZATVORENO]";
	                System.out.println((i + 1) + ". " + e.getTitle() + " " + status);
	            }
	
	            System.out.print("Izaberite broj za detalje/zatvaranje (ili 0 za nazad): ");
	            int idx = Integer.parseInt(sc.nextLine()) - 1;
	            if (idx >= 0 && idx < allElections.size()) {
	                Election selected = allElections.get(idx);
	                
	                System.out.println("\nRezultati za: " + selected.getTitle());
	                selected.getResults().forEach((kand, glasovi) -> 
	                    System.out.println("- " + kand + ": " + glasovi + " glasova"));
	
	                if (selected.isActive()) {
	                    System.out.print("\nŽelite li ZATVORITI ovo glasanje? (da/ne): ");
	                    if (sc.nextLine().equalsIgnoreCase("da")) {
	                        selected.setActive(false);
	                        try {
	                            ElectionManager.saveElection(selected);
	                            System.out.println("SISTEM: Glasanje je uspješno zatvoreno.");
	                        } catch (Exception ex) {
	                            System.out.println("Greška: " + ex.getMessage());
	                        }
	                    }
	                }
	            }
	        }
            else if (choice == 3) break;
        }
    }
    
    /*
    public static void showVoterMenu(String username) {
        Scanner sc = new Scanner(System.in);
        while (true) {
            System.out.println("\n--- GLASAČKI PANEL ---");
            System.out.println("1. Pregledaj dostupna glasanja");
            System.out.println("2. Glasaj");
            System.out.println("3. Odjava");
            System.out.print("Izbor: ");
            
            int choice = Integer.parseInt(sc.nextLine());
            if (choice == 1) {
                // Ovdje ćemo kasnije ispisati listu iz fajla koji je organizator napravio
                System.out.println("Trenutno nema aktivnih glasanja.");
            } else if (choice == 2) {
                System.out.print("Unesite naziv glasanja: ");
                String electionName = sc.nextLine();
                System.out.print("Vaš glas (ime kandidata): ");
                String vote = sc.nextLine();
                
                // OVDJE IMPLEMENTIRAMO KRIPTOGRAFIJU:
                // Glas se mora digitalno potpisati privatnim ključem korisnika!
                System.out.println("Glas je potpisan vašim ključem i poslat.");
            } else break;
        }
    }
    */

    public static void main(String[] args) {
        while (true) {
            System.out.println("\n--- SISTEM ZA ONLINE GLASANJE ---");
            System.out.println("1. Registracija Organizatora");
            System.out.println("2. Registracija Glasača");
            System.out.println("3. Login");
            System.out.print("Izbor: ");
            
            int choice = Integer.parseInt(scanner.nextLine());
            try {
                if (choice == 1) {
                    System.out.print("Naziv organizacije: "); String org = scanner.nextLine();
                    System.out.print("ID broj: "); String id = scanner.nextLine();
                    System.out.print("Lozinka: "); String pw = scanner.nextLine();
                    UserRegistration.register(org, id, pw, "ORGANIZER");
                } else if (choice == 2) {
                    System.out.print("Ime i prezime: "); String name = scanner.nextLine();
                    System.out.print("Korisničko ime: "); String user = scanner.nextLine();
                    System.out.print("Lozinka: "); String pw = scanner.nextLine();
                    UserRegistration.register(name, user, pw, "VOTER");
                } // Dodaj u MainMenu.java u switch/if strukturu:
                else if (choice == 3) {
                    System.out.print("Putanja do vašeg .p12 fajla (npr. marko123.p12): ");
                    String path = scanner.nextLine();
                    System.out.print("Korisničko ime: ");
                    String user = scanner.nextLine();
                    System.out.print("Lozinka: ");
                    String pw = scanner.nextLine();
                    
                    boolean success = LoginManager.login(path, user, pw);
                    if (success) {
                        // Ovdje otvaramo interfejs za glasanje ili organizaciju
                        System.out.println("Preusmjeravanje na glavni sistem...");
                        showOrganizerMenu(user);
                    }
                }
                else {
                	System.out.println("Exit ..");
                }
            } catch (Exception e) {
                System.out.println("Greška: " + e.getMessage());
            }
        }
    }
}