package model;

public class Allocation {
    private int taskId;
    private int resourceId;
    private String resourceName;
    private int load; 

    public Allocation(int taskId, int resourceId, String resourceName, int load) {
        this.taskId = taskId;
        this.resourceId = resourceId;
        this.resourceName = resourceName;
        this.load = load;
    }

    // Alternative constructor when only taskId and load are known (for backward compatibility)
    public Allocation(int taskId, int load) {
        this(taskId, -1, "Unassigned", load);
    }

    // Getters
    public int getTaskId() { return taskId; }
    public int getResourceId() { return resourceId; }
    public String getResourceName() { return resourceName; }
    public int getLoad() { return load; }

    // Setters
    public void setTaskId(int taskId) { this.taskId = taskId; }
    public void setResourceId(int resourceId) { this.resourceId = resourceId; }
    public void setResourceName(String resourceName) { this.resourceName = resourceName; }
    public void setLoad(int load) { this.load = load; }

    @Override
    public String toString() {
        String resPart = (resourceId == -1 || resourceName == null)
                ? "Unassigned Resource"
                : "Resource " + resourceName + " (ID: " + resourceId + ")";
        return String.format("Task %d → %s | Load: %d%%", taskId, resPart, load);
    }

    // Validation helper (optional, useful for upload handling)
    public boolean isValid() {
        return taskId >= 0 && load >= 0 && load <= 100;
    }
}