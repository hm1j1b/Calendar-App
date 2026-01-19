import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.zip.*;

public class FileHandler {
    private final String DATA_DIR = "data";
    private final String EVENT_FILE = "data/event.csv";
    private final String RECUR_FILE = "data/recurrent.csv";

    public FileHandler() {
        File dir = new File(DATA_DIR);
        if (!dir.exists()) dir.mkdir();
    }

    public void saveEvents(List<Event> events) throws IOException {
        // 1. Save Basic Data to event.csv (with header)
        try (PrintWriter pw = new PrintWriter(new FileWriter(EVENT_FILE))) {
            pw.println("eventId,title,description,startDateTime,endDateTime");
            for (Event e : events) {
                pw.printf("%d,%s,%s,%s,%s%n",
                    e.getId(),
                    escapeCSV(e.getTitle()),
                    escapeCSV(e.getDescription()),
                    e.getStart().format(Event.FMT), // <--- NEW: Forces correct format
                    e.getEnd().format(Event.FMT)    // <--- NEW: Forces correct format
                );
            }
        }

        // 2. Save Recurring Data to recurrent.csv (with header)
        try (PrintWriter pw = new PrintWriter(new FileWriter(RECUR_FILE))) {
            pw.println("eventId,recurrentInterval,recurrentTimes,recurrentEndDate");
            for (Event e : events) {
                if (!"NONE".equalsIgnoreCase(e.getRecurType())) {
                    String shortCode = convertToPdfFormat(e.getRecurType());
                    pw.printf("%d,%s,%d,%s%n",
                        e.getId(),
                        shortCode,
                        e.getRecurCount(),
                        "0"
                    );
                }
            }
        }
    }

    public List<Event> loadEvents() throws IOException {
        List<Event> list = new ArrayList<>();
        File eFile = new File(EVENT_FILE);
        File rFile = new File(RECUR_FILE);

        if (!eFile.exists()) return list;

        // 1. Read Basic Events
        try (BufferedReader br = new BufferedReader(new FileReader(eFile))) {
            String line = br.readLine(); // Skip header
            while ((line = br.readLine()) != null) {
                if (line.trim().isEmpty()) continue;
                Event e = Event.fromCSV(line);
                if (e != null) list.add(e);
            }
        }

        // 2. Read Recurring Data and merge
        if (rFile.exists()) {
            try (BufferedReader br = new BufferedReader(new FileReader(rFile))) {
                String line = br.readLine(); // Skip header
                while ((line = br.readLine()) != null) {
                    if (line.trim().isEmpty()) continue;
                    String[] parts = line.split(",");
                    if (parts.length >= 3) {
                        int id = Integer.parseInt(parts[0].trim());
                        String interval = parts[1].trim();
                        int count = Integer.parseInt(parts[2].trim());

                        for (Event e : list) {
                            if (e.getId() == id) {
                                e.setRecurType(convertFromPdfFormat(interval));
                                e.setRecurCount(count);
                                break;
                            }
                        }
                    }
                }
            }
        }
        return list;
    }

    // --- ZIP BACKUP ---
    public void backup(String dest) throws IOException {
        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(dest))) {
            addToZip(EVENT_FILE, zos);
            addToZip(RECUR_FILE, zos);
        }
    }

    private void addToZip(String path, ZipOutputStream zos) throws IOException {
        File file = new File(path);
        if (!file.exists()) return;
        
        ZipEntry entry = new ZipEntry(file.getName());
        zos.putNextEntry(entry);
        Files.copy(file.toPath(), zos);
        zos.closeEntry();
    }

    // --- RESTORE with append option ---
    public void restore(String zipPath, boolean append) throws IOException {
        // Load existing events if appending
        List<Event> existingEvents = append ? loadEvents() : new ArrayList<>();
        
        // Temporary lists for imported data
        List<Event> importedEvents = new ArrayList<>();
        Map<Integer, String[]> importedRecurrences = new HashMap<>();
        
        // Extract from ZIP
        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(zipPath))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                String fileName = entry.getName();
                
                if (fileName.equals("event.csv")) {
                    BufferedReader br = new BufferedReader(new InputStreamReader(zis));
                    String line = br.readLine(); // Skip header
                    while ((line = br.readLine()) != null) {
                        if (line.trim().isEmpty()) continue;
                        Event e = Event.fromCSV(line);
                        if (e != null) importedEvents.add(e);
                    }
                } else if (fileName.equals("recurrent.csv")) {
                    BufferedReader br = new BufferedReader(new InputStreamReader(zis));
                    String line = br.readLine(); // Skip header
                    while ((line = br.readLine()) != null) {
                        if (line.trim().isEmpty()) continue;
                        String[] parts = line.split(",");
                        if (parts.length >= 3) {
                            int id = Integer.parseInt(parts[0].trim());
                            importedRecurrences.put(id, parts);
                        }
                    }
                }
                zis.closeEntry();
            }
        }
        
        // Apply recurrence data to imported events
        for (Event e : importedEvents) {
            if (importedRecurrences.containsKey(e.getId())) {
                String[] recurData = importedRecurrences.get(e.getId());
                String interval = recurData[1].trim();
                int count = Integer.parseInt(recurData[2].trim());
                e.setRecurType(convertFromPdfFormat(interval));
                e.setRecurCount(count);
            }
        }
        
        // Merge or replace
        if (append) {
            existingEvents.addAll(importedEvents);
            saveEvents(existingEvents);
        } else {
            saveEvents(importedEvents);
        }
    }
    
    // Overloaded version for backward compatibility
    public void restore(String zipPath) throws IOException {
        restore(zipPath, false);
    }

    // --- Helper Methods ---
    private String escapeCSV(String data) {
        if (data == null) return "";
        return data.replace(",", ";");
    }

    private String convertToPdfFormat(String uiType) {
        switch (uiType.toUpperCase()) {
            case "DAILY": return "1d";
            case "WEEKLY": return "1w";
            case "MONTHLY": return "1m";
            default: return "0";
        }
    }

    private String convertFromPdfFormat(String pdfType) {
        if (pdfType.contains("d")) return "DAILY";
        if (pdfType.contains("w")) return "WEEKLY";
        if (pdfType.contains("m")) return "MONTHLY";
        return "NONE";
    }
}