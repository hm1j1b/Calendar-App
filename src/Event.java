import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Event {
    private int id;
    private String title;
    private String description;
    private LocalDateTime start;
    private LocalDateTime end;
    
    private String recurType; // "NONE", "DAILY", "WEEKLY", "MONTHLY"
    private int recurCount;   // How many times it repeats

    // Date formatter for CSV
    public static final DateTimeFormatter FMT = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    // Full Constructor
    public Event(int id, String title, String description, LocalDateTime start, LocalDateTime end, String recurType, int recurCount) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.start = start;
        this.end = end;
        this.recurType = recurType;
        this.recurCount = recurCount;
    }

    // Simplified Constructor (for non-recurring events)
    public Event(int id, String title, String description, LocalDateTime start, LocalDateTime end) {
        this(id, title, description, start, end, "NONE", 0);
    }

    // --- GETTERS ---
    public int getId() { return id; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public LocalDateTime getStart() { return start; }
    public LocalDateTime getEnd() { return end; }
    public String getRecurType() { return recurType; }
    public int getRecurCount() { return recurCount; }

    // --- SETTERS ---
    public void setTitle(String title) { this.title = title; }
    public void setDescription(String description) { this.description = description; }
    public void setStart(LocalDateTime start) { this.start = start; }
    public void setEnd(LocalDateTime end) { this.end = end; }
    public void setRecurType(String recurType) { this.recurType = recurType; }
    public void setRecurCount(int recurCount) { this.recurCount = recurCount; }

    // --- LOGIC ---
    public boolean occursOn(java.time.LocalDate date) {
        // Check original date
        if (start.toLocalDate().equals(date)) return true;
        
        // Check recurrences
        if (!"NONE".equalsIgnoreCase(recurType)) {
            for (int i = 1; i <= recurCount; i++) {
                LocalDateTime next = getOccurrence(i);
                if (next.toLocalDate().equals(date)) return true;
            }
        }
        return false;
    }

    public LocalDateTime getOccurrence(int index) {
        switch (recurType.toUpperCase()) {
            case "DAILY": return start.plusDays(index);
            case "WEEKLY": return start.plusWeeks(index);
            case "MONTHLY": return start.plusMonths(index);
            default: return start;
        }
    }

    // CSV Parsing Helper (reads from event.csv format)
    public static Event fromCSV(String csvLine) {
        try {
            String[] parts = csvLine.split(",", 5); // Limit to 5 parts
            if (parts.length < 5) return null;
            
            int id = Integer.parseInt(parts[0].trim());
            String title = parts[1].trim();
            String desc = parts[2].trim();
            LocalDateTime start = LocalDateTime.parse(parts[3].trim(), FMT);
            LocalDateTime end = LocalDateTime.parse(parts[4].trim(), FMT);
            
            return new Event(id, title, desc, start, end, "NONE", 0);
        } catch (Exception e) {
            System.err.println("Error parsing CSV line: " + csvLine);
            e.printStackTrace();
            return null;
        }
    }
}