import java.time.*;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.stream.Collectors;

public class CalendarController {

    // State Management
    public enum ViewMode { CALENDAR, LIST }
    public enum TimeScale { DAY, WEEK, MONTH }

    private ViewMode currentMode = ViewMode.CALENDAR;
    private TimeScale currentScale = TimeScale.MONTH;
    private LocalDate referenceDate = LocalDate.now();

    private List<Event> events = new ArrayList<>();
    private FileHandler fileHandler = new FileHandler();

    public CalendarController() {
        try {
            events = fileHandler.loadEvents();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // --- State Getters/Setters ---
    public ViewMode getMode() { return currentMode; }
    public void setMode(ViewMode mode) { this.currentMode = mode; }

    public TimeScale getScale() { return currentScale; }
    public void setScale(TimeScale scale) { this.currentScale = scale; }

    public LocalDate getReferenceDate() { return referenceDate; }
    public void setReferenceDate(LocalDate date) { this.referenceDate = date; }

    // --- Date Navigation Logic ---
    public void navigate(int direction) {
        switch (currentScale) {
            case DAY: referenceDate = referenceDate.plusDays(direction); break;
            case WEEK: referenceDate = referenceDate.plusWeeks(direction); break;
            case MONTH: referenceDate = referenceDate.plusMonths(direction); break;
        }
    }

    // --- Event Logic ---
    public List<Event> getEventsOnDate(LocalDate date) {
        return events.stream().filter(e -> e.occursOn(date)).collect(Collectors.toList());
    }

    public void addOrUpdateEvent(Event event) {
        events.removeIf(e -> e.getId() == event.getId());
        events.add(event);
        save();
    }

    public void deleteEvent(Event event) {
        events.remove(event);
        save();
    }

    public int getNextEventId() {
        return events.stream().mapToInt(Event::getId).max().orElse(0) + 1;
    }

    private void save() {
        try {
            fileHandler.saveEvents(events);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // --- Range Calculations ---
    public LocalDate getStartOfRange() {
        switch (currentScale) {
            case DAY: return referenceDate;
            case WEEK: return referenceDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY));
            default: return referenceDate.withDayOfMonth(1);
        }
    }

    public LocalDate getEndOfRange() {
        switch (currentScale) {
            case DAY: return referenceDate;
            case WEEK: return getStartOfRange().plusDays(6);
            default: return referenceDate.with(TemporalAdjusters.lastDayOfMonth());
        }
    }

    // --- Filter Logic for List View ---
    public List<Event> getEventsInRange() {
        LocalDate start = getStartOfRange();
        LocalDate end = getEndOfRange();
        return events.stream()
                .filter(e -> occursInRange(e, start, end))
                .sorted(Comparator.comparing(Event::getStart))
                .collect(Collectors.toList());
    }

    private boolean occursInRange(Event e, LocalDate start, LocalDate end) {
        // Check original occurrence
        if (!e.getStart().toLocalDate().isBefore(start) &&
                !e.getStart().toLocalDate().isAfter(end)) {
            return true;
        }

        // Check recurring occurrences
        if (!"NONE".equalsIgnoreCase(e.getRecurType())) {
            for (int i = 1; i <= e.getRecurCount(); i++) {
                LocalDate occurrence = e.getOccurrence(i).toLocalDate();
                if (!occurrence.isBefore(start) && !occurrence.isAfter(end)) {
                    return true;
                }
            }
        }
        return false;
    }

    // --- Search Logic ---
    public List<SearchMatch> searchEvents(String query) {
        if (query == null || query.trim().isEmpty()) return new ArrayList<>();
        List<SearchMatch> results = new ArrayList<>();
        String lowQuery = query.toLowerCase();

        for (Event e : events) {
            if (e.getTitle().toLowerCase().contains(lowQuery) ||
                    e.getDescription().toLowerCase().contains(lowQuery)) {

                // Add original occurrence
                results.add(new SearchMatch(e.getStart().toLocalDate(), e));

                // Add all recurring occurrences
                if (!"NONE".equalsIgnoreCase(e.getRecurType())) {
                    for (int i = 1; i <= e.getRecurCount(); i++) {
                        results.add(new SearchMatch(e.getOccurrence(i).toLocalDate(), e));
                    }
                }
            }
        }
        return results;
    }

    public List<SearchMatch> searchEventsByDate(LocalDate start, LocalDate end) {
        List<SearchMatch> results = new ArrayList<>();

        for (Event e : events) {
            LocalDate originalDate = e.getStart().toLocalDate();
            if (!originalDate.isBefore(start) && !originalDate.isAfter(end)) {
                results.add(new SearchMatch(originalDate, e));
            }

            if (!"NONE".equalsIgnoreCase(e.getRecurType())) {
                for (int i = 1; i <= e.getRecurCount(); i++) {
                    LocalDate occurrenceDate = e.getOccurrence(i).toLocalDate();
                    // Check if this specific occurrence falls within the search range
                    if (!occurrenceDate.isBefore(start) && !occurrenceDate.isAfter(end)) {
                        results.add(new SearchMatch(occurrenceDate, e));
                    }
                }
            }
        }

        // Optional: Sort results by date
        results.sort(Comparator.comparing(match -> match.date));
        return results;
    }

    // Fix Search Date for Recurring Event
    public static class SearchMatch {
        public final LocalDate date;
        public final Event event;

        public SearchMatch(LocalDate date, Event event) {
            this.date = date;
            this.event = event;
        }
    }
    // --- Conflict Logic ---
    public boolean hasConflict(Event e) {
        List<Event> sameDay = getEventsOnDate(e.getStart().toLocalDate());
        return checkForConflictOnDate(sameDay);
    }

    public boolean checkForConflictOnDate(List<Event> dayEvents) {
        for (int i = 0; i < dayEvents.size(); i++) {
            for (int j = i + 1; j < dayEvents.size(); j++) {
                Event a = dayEvents.get(i);
                Event b = dayEvents.get(j);
                if (a.getStart().isBefore(b.getEnd()) && a.getEnd().isAfter(b.getStart())) {
                    return true;
                }
            }
        }
        return false;
    }

    // --- Backup/Restore ---
    public void performBackup(String filename) throws Exception {
        fileHandler.backup(filename);
    }

    public void performRestore(String filename, boolean append) throws Exception {
        fileHandler.restore(filename, append);
        events = fileHandler.loadEvents();
    }

    // Overloaded version for backward compatibility
    public void performRestore(String filename) throws Exception {
        performRestore(filename, false);
    }

    // --- Notification Logic ---
    public List<Event> getUpcomingEvents(int hoursAhead) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime future = now.plusHours(hoursAhead);

        return events.stream()
                .filter(e -> {
                    // Check if event starts in the next X hours
                    if (e.getStart().isAfter(now) && e.getStart().isBefore(future)) {
                        return true;
                    }
                    // Check recurring occurrences
                    if (!"NONE".equalsIgnoreCase(e.getRecurType())) {
                        for (int i = 1; i <= e.getRecurCount(); i++) {
                            LocalDateTime occurrence = e.getOccurrence(i);
                            if (occurrence.isAfter(now) && occurrence.isBefore(future)) {
                                return true;
                            }
                        }
                    }
                    return false;
                })
                .sorted(Comparator.comparing(Event::getStart))
                .collect(Collectors.toList());
    }

    // --- Week List View (CLI-style format) ---
    public String generateWeekListView(LocalDate weekStart) {
        LocalDate weekEnd = weekStart.plusDays(6);
        StringBuilder sb = new StringBuilder();

        sb.append("=== Week of ").append(weekStart).append(" ===\n\n");

        String[] dayNames = {"Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"};

        for (int i = 0; i < 7; i++) {
            LocalDate day = weekStart.plusDays(i);
            List<Event> dayEvents = getEventsOnDate(day);

            sb.append(dayNames[i]).append(" ").append(day).append(": ");

            if (dayEvents.isEmpty()) {
                sb.append("No events\n");
            } else {
                sb.append("\n");
                for (Event e : dayEvents) {
                    sb.append("  • ").append(e.getStart().toLocalTime())
                            .append(" - ").append(e.getTitle());
                    if (!"NONE".equals(e.getRecurType())) {
                        sb.append(" [").append(e.getRecurType()).append("]");
                    }
                    sb.append("\n");
                }
            }
        }

        return sb.toString();
    }
}