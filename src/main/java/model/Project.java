package model;

import java.util.List;
import java.util.ArrayList;

public class Project {
    private String name;
    private List<Task> tasks;
    private List<Resource> resources;

    private String tasksFilePath;
    private String resourcesFilePath;
    
    public Project(String name) {
        this.name = name;
        this.tasks = new java.util.ArrayList<>();
        this.resources = new java.util.ArrayList<>();
    }

    // --- Getters & Setters ---
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public List<Task> getTasks() { return tasks; }
    public void setTasks(List<Task> tasks) { this.tasks = tasks; }

    public List<Resource> getResources() { return resources; }
    public void setResources(List<Resource> resources) { this.resources = resources; }

    public String getTasksFilePath() { return tasksFilePath; }
    public void setTasksFilePath(String path) { this.tasksFilePath = path; }

    public String getResourcesFilePath() { return resourcesFilePath; }
    public void setResourcesFilePath(String path) { this.resourcesFilePath = path; }

    // ===== TEST METHODS FOR OVERLAPPING TASKS & COMPLETION TIME =====

    /**
     * Finds all overlapping tasks in the project using Task.overlapsWith()
     * @return List of overlap descriptions
     */
    public List<String> findOverlappingTasks() {
        List<String> overlaps = new ArrayList<>();
        List<Task> tasks = getTasks();
        
        if (tasks == null || tasks.size() < 2) {
            return overlaps;
        }
        
        for (int i = 0; i < tasks.size(); i++) {
            for (int j = i + 1; j < tasks.size(); j++) {
                Task task1 = tasks.get(i);
                Task task2 = tasks.get(j);
                
                if (task1.overlapsWith(task2)) {
                    overlaps.add(String.format("OVERLAP: '%s' (%s-%s) and '%s' (%s-%s) overlap",
                        task1.getTitle(), task1.getStart(), task1.getEnd(),
                        task2.getTitle(), task2.getStart(), task2.getEnd()));
                }
            }
        }
        
        return overlaps;
    }
    
    /**
     * Calculates the project completion time (latest end date of all tasks)
     * @return Latest end date as string, or null if no tasks
     */
    public String calculateProjectCompletionTime() {
        List<Task> tasks = getTasks();
        if (tasks == null || tasks.isEmpty()) {
            return null;
        }
        
        String latestEnd = tasks.get(0).getEnd();
        
        for (int i = 1; i < tasks.size(); i++) {
            String currentEnd = tasks.get(i).getEnd();
            if (currentEnd.compareTo(latestEnd) > 0) {
                latestEnd = currentEnd;
            }
        }
        
        return latestEnd;
    }
    
    /**
     * Enhanced completion time calculation that considers dependencies
     * @return Project completion date considering task dependencies
     */
    public String calculateCriticalPathCompletionTime() {
        List<Task> tasks = getTasks();
        if (tasks == null || tasks.isEmpty()) {
            return null;
        }
        
        // For now, using simple latest end date
        // You can enhance this later to consider dependency chains
        return calculateProjectCompletionTime();
    }
    
    /**
     * Finds resource overallocations during overlapping task periods
     * @return List of overallocation warnings
     */
    public List<String> findResourceOverallocations() {
        List<String> overallocations = new ArrayList<>();
        
        if (resources == null || tasks == null) {
            return overallocations;
        }
        
        // Check each resource's allocations during overlapping periods
        for (Resource resource : resources) {
            if (resource.getAllocations() != null) {
                for (Allocation alloc1 : resource.getAllocations()) {
                    for (Allocation alloc2 : resource.getAllocations()) {
                        if (alloc1 != alloc2 && areTasksOverlapping(alloc1.getTaskId(), alloc2.getTaskId())) {
                            int totalLoad = alloc1.getLoad() + alloc2.getLoad();
                            if (totalLoad > 100) {
                                overallocations.add(String.format(
                                    "OVERALLOCATION: %s is allocated %d%% during overlapping tasks %d and %d",
                                    resource.getName(), totalLoad, alloc1.getTaskId(), alloc2.getTaskId()));
                            }
                        }
                    }
                }
            }
        }
        
        return overallocations;
    }
    
    private boolean areTasksOverlapping(int taskId1, int taskId2) {
        Task task1 = findTaskById(taskId1);
        Task task2 = findTaskById(taskId2);
        
        if (task1 != null && task2 != null) {
            return task1.overlapsWith(task2);
        }
        
        return false;
    }
    
    private Task findTaskById(int taskId) {
        if (tasks != null) {
            for (Task task : tasks) {
                if (task.getId() == taskId) {
                    return task;
                }
            }
        }
        return null;
    }

    /**
     * Utility method to check if a specific task overlaps with any other tasks
     * @param taskId The task ID to check
     * @return List of overlapping task descriptions
     */
    public List<String> findOverlapsForTask(int taskId) {
        List<String> overlaps = new ArrayList<>();
        Task targetTask = findTaskById(taskId);
        
        if (targetTask == null || tasks == null) {
            return overlaps;
        }
        
        for (Task otherTask : tasks) {
            if (otherTask.getId() != taskId && targetTask.overlapsWith(otherTask)) {
                overlaps.add(String.format("Task %d '%s' overlaps with Task %d '%s'",
                    taskId, targetTask.getTitle(), otherTask.getId(), otherTask.getTitle()));
            }
        }
        
        return overlaps;
    }

    /**
     * Gets the earliest start date in the project
     * @return Earliest start date or null if no tasks
     */
    public String getProjectStartDate() {
        List<Task> tasks = getTasks();
        if (tasks == null || tasks.isEmpty()) {
            return null;
        }
        
        String earliestStart = tasks.get(0).getStart();
        
        for (int i = 1; i < tasks.size(); i++) {
            String currentStart = tasks.get(i).getStart();
            if (currentStart.compareTo(earliestStart) < 0) {
                earliestStart = currentStart;
            }
        }
        
        return earliestStart;
    }

    /**
     * Calculates project duration in days (simplified)
     * @return Number of days between start and completion, or -1 if cannot calculate
     */
    public int calculateProjectDuration() {
        String start = getProjectStartDate();
        String end = calculateProjectCompletionTime();
        
        if (start == null || end == null) {
            return -1;
        }
        
        // Simple calculation - in real implementation, you'd parse dates and calculate difference
        try {
            // This is a simplified version - you'd need proper date parsing
            return Math.abs(Integer.parseInt(end.replace("-", "")) - Integer.parseInt(start.replace("-", "")));
        } catch (NumberFormatException e) {
            return -1;
        }
    }
}