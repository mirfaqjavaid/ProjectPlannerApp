package model;

public class Task {
    private int id;
    private String title;
    private String start;
    private String end;
    private String dependencies;
    private String team;

    public Task(int id, String title, String start, String end, String dependencies) {
        this.id = id;
        this.title = title;
        this.start = start;
        this.end = end;
        this.dependencies = dependencies;
        this.team = "";
    }

    // Getters
    public int getId() { return id; }
    public String getTitle() { return title; }
    public String getStart() { return start; }
    public String getEnd() { return end; }
    public String getDependencies() { return dependencies; }
    public String getTeam() { return team; }

    // Setters
    public void setId(int id) { this.id = id; }
    public void setTitle(String title) { this.title = title; }
    public void setStart(String start) { this.start = start; }
    public void setEnd(String end) { this.end = end; }
    public void setDependencies(String dependencies) { this.dependencies = dependencies; }
    public void setTeam(String team) { this.team = team; }

    public boolean overlapsWith(Task other) {
        return this.start.compareTo(other.getEnd()) <= 0 &&
               this.end.compareTo(other.getStart()) >= 0;
    }
}