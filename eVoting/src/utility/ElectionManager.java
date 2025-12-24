package utility;

import model.Election;
import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class ElectionManager {
    private static final String STORAGE_DIR = "elections/";

    public static void saveElection(Election election) throws Exception {
        File dir = new File(STORAGE_DIR);
        if (!dir.exists()) dir.mkdir();

        try (ObjectOutputStream oos = new ObjectOutputStream(
                new FileOutputStream(STORAGE_DIR + election.getTitle().replace(" ", "_") + ".dat"))) {
            oos.writeObject(election);
        }
    }

    public static List<Election> loadAllElections() {
        List<Election> elections = new ArrayList<>();
        File dir = new File(STORAGE_DIR);
        if (dir.exists() && dir.listFiles() != null) {
            for (File file : dir.listFiles()) {
                if (file.getName().endsWith(".dat")) {
                    try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file))) {
                        elections.add((Election) ois.readObject());
                    } catch (Exception e) {
                        System.err.println("Greška pri učitavanju glasanja: " + file.getName());
                    }
                }
            }
        }
        return elections;
    }
}