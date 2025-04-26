package igoat.server;

import igoat.Timer;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Simple highscore manager for the game
 */
public class HighscoreManager {
    private static final Logger logger = LoggerFactory.getLogger(HighscoreManager.class);
    private static final String HIGHSCORE_DIR = "highscores";
    private static final String GUARD_HIGHSCORE_FILE = HIGHSCORE_DIR + "/guard_highscores.txt";
    private static final String GOAT_HIGHSCORE_FILE = HIGHSCORE_DIR + "/goat_highscores.txt";
    private static final int MAX_HIGHSCORES = 10;
    
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    /**
     * Initialize the highscore system
     */
    public static void initialize() {
        try {
            Path dirPath = Paths.get(HIGHSCORE_DIR);
            if (!Files.exists(dirPath)) {
                Files.createDirectories(dirPath);
                logger.info("Created highscores directory");
            }
            
            File guardFile = new File(GUARD_HIGHSCORE_FILE);
            if (!guardFile.exists()) {
                guardFile.createNewFile();
                logger.info("Created guard highscores file");
            }
            
            File goatFile = new File(GOAT_HIGHSCORE_FILE);
            if (!goatFile.exists()) {
                goatFile.createNewFile();
                logger.info("Created goat highscores file");
            }
        } catch (IOException e) {
            logger.error("Error initializing highscore system", e);
        }
    }
    
    /**
     * Add a guard highscore
     */
    public static void addGuardHighscore(String guardName, long timeInMs) {
        try {
            String entry = guardName + "," + timeInMs + "," + 
            LocalDateTime.now().format(DATE_FORMAT);
            
            List<String> entries = new ArrayList<>();
            File file = new File(GUARD_HIGHSCORE_FILE);
            if (file.exists()) {
                try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        entries.add(line);
                    }
                }
            }
            
            entries.add(entry);
            
            Collections.sort(entries, (a, b) -> {
                long timeA = Long.parseLong(a.split(",")[1]);
                long timeB = Long.parseLong(b.split(",")[1]);
                return Long.compare(timeA, timeB);
            });
            
            if (entries.size() > MAX_HIGHSCORES) {
                entries = entries.subList(0, MAX_HIGHSCORES);
            }
            
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
                for (String line : entries) {
                    writer.write(line);
                    writer.newLine();
                }
            }
            
            logger.info("Added new highscore for {}: {}", guardName, timeInMs);
        } catch (Exception e) {
            logger.error("Error adding guard highscore", e);
        }
    }
    
    /**
     * Add a goat highscore
     */
    public static void addGoatHighscore(String goatNames, long timeInMs) {
        try {
            String entry = goatNames + "," + timeInMs + "," + 
            LocalDateTime.now().format(DATE_FORMAT);
            
            List<String> entries = new ArrayList<>();
            File file = new File(GOAT_HIGHSCORE_FILE);
            if (file.exists()) {
                try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        entries.add(line);
                    }
                }
            }
            
            entries.add(entry);
            
            Collections.sort(entries, (a, b) -> {
                try {
                    int lastCommaA = a.lastIndexOf(",");
                    int lastCommaB = b.lastIndexOf(",");
                    
                    String withoutDateA = a.substring(0, lastCommaA);
                    String withoutDateB = b.substring(0, lastCommaB);
                    
                    int timeCommaA = withoutDateA.lastIndexOf(",");
                    int timeCommaB = withoutDateB.lastIndexOf(",");
                    
                    long timeA = Long.parseLong(withoutDateA.substring(timeCommaA + 1).trim());
                    long timeB = Long.parseLong(withoutDateB.substring(timeCommaB + 1).trim());
                    
                    return Long.compare(timeA, timeB);
                } catch (Exception e) {
                    logger.warn("Error comparing goat highscore entries", e);
                    return 0;
                }
            });
            
            if (entries.size() > MAX_HIGHSCORES) {
                entries = entries.subList(0, MAX_HIGHSCORES);
            }
            
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
                for (String line : entries) {
                    writer.write(line);
                    writer.newLine();
                }
            }
            
            logger.info("Added new highscore for {}: {}", goatNames, timeInMs);
        } catch (Exception e) {
            logger.error("Error adding goat highscore", e);
        }
    }
    
    /**
     * Get all highscores as a formatted string
     */
    public static String getHighscores() {
        StringBuilder sb = new StringBuilder();
        
        sb.append("=== GUARD HIGHSCORES (Fastest Wins) ===\n");
        sb.append("----------------------------------------\n");
        
        List<String> guardEntries = new ArrayList<>();
        try {
            File file = new File(GUARD_HIGHSCORE_FILE);
            if (file.exists()) {
                try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        guardEntries.add(line);
                    }
                }
            }
            
            Collections.sort(guardEntries, (a, b) -> {
                try {
                    long timeA = Long.parseLong(a.split(",")[1]);
                    long timeB = Long.parseLong(b.split(",")[1]);
                    return Long.compare(timeA, timeB);
                } catch (Exception e) {
                    return 0;
                }
            });
            
            if (guardEntries.isEmpty()) {
                sb.append("No guard highscores yet.\n");
            } else {
                for (int i = 0; i < Math.min(guardEntries.size(), 10); i++) {
                    try {
                        String[] parts = guardEntries.get(i).split(",");
                        if (parts.length >= 3) {
                            String name = parts[0];
                            long time = Long.parseLong(parts[1]);
                            String date = parts[2];
                            
                            double[] minSec = Timer.convertToMinSec(time);
                            String formattedTime = String.format("%.0f:%05.2f", minSec[0], minSec[1]);
                            
                            sb.append(String.format("%d. %s - Time: %s - Date: %s\n", i + 1, name, formattedTime, date));
                        }
                    } catch (Exception e) {
                        logger.warn("Error parsing guard highscore entry: {}", guardEntries.get(i));
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Error reading guard highscores", e);
            sb.append("Error reading guard highscores.\n");
        }
        
        sb.append("\n=== GOAT HIGHSCORES (Fastest Wins) ===\n");
        sb.append("---------------------------------------\n");
        
        List<String> goatEntries = new ArrayList<>();
        try {
            File file = new File(GOAT_HIGHSCORE_FILE);
            if (file.exists()) {
                try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        goatEntries.add(line);
                    }
                }
            }
            
            Collections.sort(goatEntries, (a, b) -> {
                try {
                    long timeA = Long.parseLong(a.split(",")[1]);
                    long timeB = Long.parseLong(b.split(",")[1]);
                    return Long.compare(timeA, timeB);
                } catch (Exception e) {
                    return 0;
                }
            });
            
            if (goatEntries.isEmpty()) {
                sb.append("No goat highscores yet.\n");
            } else {
                for (int i = 0; i < Math.min(goatEntries.size(), 10); i++) {
                    try {
                        String entry = goatEntries.get(i);
                        
                        int lastCommaIndex = entry.lastIndexOf(",");
                        String date = entry.substring(lastCommaIndex + 1).trim();
                        
                        String withoutDate = entry.substring(0, lastCommaIndex);
                        int timeCommaIndex = withoutDate.lastIndexOf(",");
                        
                        if (timeCommaIndex > 0) {
                            String playerNames = withoutDate.substring(0, timeCommaIndex).trim();
                            String timeStr = withoutDate.substring(timeCommaIndex + 1).trim();
                            long time = Long.parseLong(timeStr);
                            
                            double[] minSec = Timer.convertToMinSec(time);
                            String formattedTime = String.format("%.0f:%05.2f", minSec[0], minSec[1]);
                            
                            sb.append(String.format("%d. %s - Time: %s - Date: %s\n", i + 1, playerNames, formattedTime, date));
                        }
                    } catch (Exception e) {
                        logger.warn("Error parsing goat highscore entry: {}", goatEntries.get(i), e);
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Error reading goat highscores", e);
            sb.append("Error reading goat highscores.\n");
        }
        
        return sb.toString();
    }
}
