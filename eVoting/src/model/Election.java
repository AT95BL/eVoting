package model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Election implements Serializable {
    private static final long serialVersionUID = 1L; // Dobra praksa za Serializable klase
    
    private String title;
    private String description;
    private List<String> candidates;
    private Map<String, Integer> results; // Kandidat -> Broj glasova
    private boolean isOpen;

    public Election(String title, String description, List<String> candidates) {
        this.title = title;
        this.description = description;
        this.candidates = candidates;
        this.results = new HashMap<>();
        for (String c : candidates) {
            results.put(c, 0);
        }
        this.isOpen = true;
    }
    
    // Dodaj ovo polje u model.Election klasu
    private List<String> votedUsers = new ArrayList<>();

    public boolean hasVoted(String username) {
        return votedUsers.contains(username);
    }

    public void addVote(String candidate, String username) {
        results.put(candidate, results.get(candidate) + 1);
        votedUsers.add(username);
    }

    // Noviteti:
    // U klasi model.Election dodaj:
    private boolean active = true;

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    // public Map<String, Integer> getResults() { return results; }

    /**
     * Registruje glas za određenog kandidata ako su izbori otvoreni.
     */
    public synchronized boolean castVote(String candidateName) {
        if (isOpen && results.containsKey(candidateName)) {
            results.put(candidateName, results.get(candidateName) + 1);
            return true;
        }
        return false;
    }

    // --- GETTERI I SETTERI ---

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<String> getCandidates() {
        return new ArrayList<>(candidates); // Vraća kopiju liste radi sigurnosti
    }

    public void setCandidates(List<String> candidates) {
        this.candidates = candidates;
    }

    public Map<String, Integer> getResults() {
        return new HashMap<>(results); // Vraća kopiju mape
    }

    public boolean isOpen() {
        return isOpen;
    }

    public void setIsOpen(boolean open) {
        this.isOpen = open;
    }
    
    @Override
    public String toString() {
        return "Election: " + title + " (Status: " + (isOpen ? "Open" : "Closed") + ")";
    }
}