package controller;

import model.*;
import java.util.*;

public class AnalysisController {

    public static String getProjectCompletionDate(List<Task> tasks) {
        if (tasks == null || tasks.isEmpty()) return "No tasks available.";
        Task last = tasks.get(0);
        for (Task t : tasks) {
            if (t.getEnd().compareTo(last.getEnd()) > 0) last = t;
        }
        return last.getEnd();
    }

    public static List<String> getOverlappingTasks(List<Task> tasks) {
        List<String> overlaps = new ArrayList<>();
        if (tasks == null) return overlaps;
        
        for (int i = 0; i < tasks.size(); i++) {
            Task t1 = tasks.get(i);
            for (int j = i + 1; j < tasks.size(); j++) {
                Task t2 = tasks.get(j);
                if (t1.overlapsWith(t2)) {
                    overlaps.add(t1.getTitle() + " ↔ " + t2.getTitle());
                }
            }
        }
        return overlaps;
    }

    public static List<String> getTeamForTask(int taskId, List<Resource> resources) {
        List<String> team = new ArrayList<>();
        if (resources == null) return team;
        
        for (Resource r : resources) {
            for (Allocation alloc : r.getAllocations()) {
                if (alloc.getTaskId() == taskId) {
                    team.add(r.getName());
                    break;
                }
            }
        }
        return team;
    }

    public static Map<String, Long> getEffortHoursPerResource(List<Resource> resources, List<Task> tasks) {
        Map<String, Long> map = new HashMap<>();
        if (resources == null) return map;
        
        for (Resource r : resources) {
            long hrs = r.getAllocations().size() * 8; // each task = 8 hrs
            map.put(r.getName(), hrs);
        }
        return map;
    }
}