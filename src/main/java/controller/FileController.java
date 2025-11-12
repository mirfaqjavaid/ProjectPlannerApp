package controller;

import java.io.*;
import model.*;
import java.util.*;

public class FileController {
    public static class ParseResult<T> {
        public final List<T> items = new ArrayList<>();
        public final List<String> errors = new ArrayList<>();
    }

    // --- Load tasks from file (with parsing errors captured) ---
    public static ParseResult<Task> loadTasksWithErrors(String filePath) {
        ParseResult<Task> res = new ParseResult<>();
        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String line;
            int ln = 0;
            while ((line = br.readLine()) != null) {
                ln++;
                String trimmed = line.trim();
                if (trimmed.isEmpty()) continue; // skip empty lines
                if (!Character.isDigit(trimmed.charAt(0))) continue;
                List<String> parts = parseCsvLine(line);
                if (parts.size() < 4) {
                    res.errors.add("Line " + ln + ": not enough fields (expected id,title,start,end,...)");
                    continue;
                }
                try {
                    int id = Integer.parseInt(parts.get(0).trim());
                    String title = parts.get(1).trim();
                    String start = parts.get(2).trim();
                    String end = parts.get(3).trim();
                    StringBuilder deps = new StringBuilder();
                    for (int i = 4; i < parts.size(); i++) {
                        String tok = parts.get(i).trim();
                        if (!tok.isEmpty()) {
                            if (deps.length() > 0) deps.append(", ");
                            deps.append(tok);
                        }
                    }
                    res.items.add(new Task(id, title, start, end, deps.toString()));
                } catch (NumberFormatException nfe) {
                    res.errors.add("Line " + ln + ": invalid id '" + parts.get(0).trim() + "'");
                } catch (Exception ex) {
                    res.errors.add("Line " + ln + ": unexpected parse error: " + ex.getMessage());
                }
            }
        } catch (Exception e) {
            res.errors.add("Error reading tasks file: " + e.getMessage());
        }
        return res;
    }

    public static List<Task> loadTasks(String filePath) {
        ParseResult<Task> pr = loadTasksWithErrors(filePath);
        if (!pr.errors.isEmpty()) {
            System.err.println("FileController: task parse warnings/errors:");
            for (String e : pr.errors) System.err.println("  " + e);
        }
        return pr.items;
    }

    // Parse a CSV line into fields supporting quoted fields with commas
    private static List<String> parseCsvLine(String line) {
        List<String> out = new ArrayList<>();
        if (line == null || line.isEmpty()) return out;
        StringBuilder cur = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                // toggle inQuotes (handle escaped quotes "")
                if (inQuotes && i+1 < line.length() && line.charAt(i+1) == '"') {
                    cur.append('"');
                    i++; // skip escaped quote
                } else {
                    inQuotes = !inQuotes;
                }
            } else if (c == ',' && !inQuotes) {
                out.add(cur.toString()); cur.setLength(0);
            } else {
                cur.append(c);
            }
        }
        out.add(cur.toString());
        return out;
    }

    // --- Load resources from file ---
    public static List<Resource> loadResources(String filePath) {
        List<Resource> resources = new ArrayList<>();

        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String line;

            while ((line = br.readLine()) != null) {
                String trimmed = line.trim();
                if (trimmed.isEmpty()) continue;
                // skip header/comment lines
                if (trimmed.startsWith("-") || trimmed.startsWith("#") || trimmed.startsWith("//")) continue;

                // split only first comma for name, rest are allocations
                String[] parts = line.split(",", 2);
                String name = parts[0].trim();
                    String allocationsStr = (parts.length > 1) ? parts[1].trim() : "";
                    List<Allocation> allocations = new ArrayList<>();
                    if (!allocationsStr.isEmpty()) {
                        String[] allocParts = allocationsStr.split(",");
                        for (String alloc : allocParts) {
                            alloc = alloc.trim();
                            if (!alloc.isEmpty()) {
                                String[] pair = alloc.split(":");
                                if (pair.length == 2) {
                                    try {
                                        int taskId = Integer.parseInt(pair[0].trim());
                                        int load = Integer.parseInt(pair[1].trim());
                                        allocations.add(new Allocation(taskId, load));
                                    } catch (NumberFormatException e) {
                                        System.err.println("Invalid allocation value: " + alloc);
                                    }
                                } else {
                                    System.err.println("Invalid allocation format: " + alloc);
                                }
                            }
                        }
                    }
                    resources.add(new Resource(name, allocations));
            }

        } catch (Exception e) {
            System.err.println(" Error reading resources file: " + e.getMessage());
            e.printStackTrace();
        }

        return resources;
    }

    // --- Validation helpers ---
    public static class ValidationResult {
        public final List<String> errors = new ArrayList<>();
        public final Set<Integer> invalidTaskIds = new HashSet<>();
        public final Set<Integer> validTaskIds = new HashSet<>();
    }

    // Parse various date formats to epoch millis. Returns -1 on failure.
    public static long parseToMillis(String dateStr) {
        if (dateStr == null) return -1;
        dateStr = dateStr.trim();
        try {
            java.util.regex.Matcher m = java.util.regex.Pattern.compile("^(\\d{8})(\\d{4})?([+-]\\d{4})?$")
                    .matcher(dateStr);
            if (m.matches()) {
                String dpart = m.group(1);
                String tpart = m.group(2);
                String off = m.group(3);
                java.time.LocalDate d = java.time.LocalDate.parse(dpart, java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd"));
                java.time.LocalDateTime ldt;
                if (tpart != null) {
                    int hh = Integer.parseInt(tpart.substring(0,2));
                    int mm = Integer.parseInt(tpart.substring(2,4));
                    ldt = d.atTime(hh, mm);
                } else {
                    ldt = d.atStartOfDay();
                }
                if (off != null) {
                    java.time.ZoneOffset zo = java.time.ZoneOffset.of(off);
                    java.time.OffsetDateTime odt = java.time.OffsetDateTime.of(ldt, zo);
                    return odt.toInstant().toEpochMilli();
                } else {
                    return ldt.atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli();
                }
            }
        } catch (Exception ex) {}
        String[] patterns = new String[]{"yyyyMMddHHmm","yyyyMMddHH","yyyy-MM-dd HH:mm","yyyy-MM-dd"};
        for (String p : patterns) {
            try {
                java.time.format.DateTimeFormatter f = java.time.format.DateTimeFormatter.ofPattern(p);
                if (p.equals("yyyy-MM-dd")) {
                    java.time.LocalDate ld = java.time.LocalDate.parse(dateStr, f);
                    return ld.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli();
                } else {
                    java.time.LocalDateTime ldt = java.time.LocalDateTime.parse(dateStr, f);
                    return ldt.atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli();
                }
            } catch (Exception e) {}
        }
        return -1;
    }

    // Validate tasks: check dates, end>start, dependencies format and existence.
    public static ValidationResult validateTasks(List<model.Task> tasks) {
        ValidationResult vr = new ValidationResult();
        if (tasks == null) return vr;
        // Collect ids
        Set<Integer> ids = new HashSet<>();
        for (model.Task t : tasks) ids.add(t.getId());
        // Validate each task
        for (model.Task t : tasks) {
            int id = t.getId();
            boolean ok = true;
            if (t.getTitle() == null || t.getTitle().trim().isEmpty()) {
                vr.errors.add("Task " + id + ": title is empty");
            }
            long s = parseToMillis(t.getStart());
            long e = parseToMillis(t.getEnd());
            if (s <= 0) { vr.errors.add("Task " + id + ": invalid start date '" + t.getStart() + "'"); ok = false; }
            if (e <= 0) { vr.errors.add("Task " + id + ": invalid end date '" + t.getEnd() + "'"); ok = false; }
            if (s > 0 && e > 0 && e <= s) { vr.errors.add("Task " + id + ": end date must be after start date"); ok = false; }
            // dependencies: comma separated ids (optional)
            String deps = t.getDependencies();
            if (deps != null && !deps.trim().isEmpty()) {
                String[] parts = deps.split(",");
                for (String p : parts) {
                    p = p.trim();
                    if (p.isEmpty()) continue;
                    try {
                        int did = Integer.parseInt(p);
                        if (!ids.contains(did)) {
                            vr.errors.add("Task " + id + ": dependency '" + did + "' not found among tasks");
                            ok = false;
                        }
                    } catch (NumberFormatException ex) {
                        vr.errors.add("Task " + id + ": invalid dependency token '" + p + "'"); ok = false;
                    }
                }
            }
            if (ok) vr.validTaskIds.add(id); else vr.invalidTaskIds.add(id);
        }
        return vr;
    }

    // Validate resources: check allocations reference existing tasks and loads are numbers
    public static ValidationResult validateResources(List<model.Resource> resources, Set<Integer> knownTaskIds) {
        ValidationResult vr = new ValidationResult();
        if (resources == null) return vr;
        int idx = 0;
        for (model.Resource r : resources) {
            idx++;
            if (r.getName() == null || r.getName().trim().isEmpty()) {
                vr.errors.add("Resource #" + idx + ": name is empty");
            }
            for (model.Allocation a : r.getAllocations()) {
                int tid = a.getTaskId();
                int load = a.getLoad();
                if (!knownTaskIds.contains(tid)) {
                    vr.errors.add("Resource '" + (r.getName()==null?"<unnamed>":r.getName()) + "' allocation: task id " + tid + " not found");
                }
                if (load < 0 || load > 1000) {
                    vr.errors.add("Resource '" + (r.getName()==null?"<unnamed>":r.getName()) + "' allocation: load " + load + " seems invalid");
                }
            }
        }
        return vr;
    }
}
