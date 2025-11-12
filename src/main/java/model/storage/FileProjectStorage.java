package model.storage;

import java.io.*;
import model.*;
import java.util.*;

public class FileProjectStorage implements ProjectStorage {
    @Override
    public void saveProject(Project project, String filename) throws Exception {
        try (ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(filename))) {
            out.writeObject(project);
        }
    }

    @Override
    public Project loadProject(String filename) throws Exception {
        try (ObjectInputStream in = new ObjectInputStream(new FileInputStream(filename))) {
            return (Project) in.readObject();
        }
    }

    @Override
    public List<Project> loadAllProjects(String directory) throws Exception {
        List<Project> projects = new ArrayList<>();
        File dir = new File(directory);
        if (dir.exists() && dir.isDirectory()) {
            for (File file : dir.listFiles()) {
                if (file.getName().endsWith(".dat")) {
                    projects.add(loadProject(file.getAbsolutePath()));
                }
            }
        }
        return projects;
    }

    @Override
    public void saveProjectAsText(Project project, File file) throws IOException {
        try (PrintWriter out = new PrintWriter(file)) {
            // Save tasks
            for (Task t : project.getTasks()) {
                out.println(t.getId() + ", " + t.getTitle() + ", " + t.getStart() + ", " + t.getEnd() + 
                    (t.getDependencies().isEmpty() ? "" : ", " + t.getDependencies()));
            }
            out.println("---RESOURCES---");
            // Save resources
            for (Resource r : project.getResources()) {
                StringBuilder allocStr = new StringBuilder();
                for (Allocation a : r.getAllocations()) {
                    if (allocStr.length() > 0) allocStr.append(", ");
                    allocStr.append(a.getTaskId()).append(":").append(a.getLoad());
                }
                out.println(r.getName() + (allocStr.length() > 0 ? ", " + allocStr.toString() : ""));
            }
        }
    }

    @Override
    public Project loadProjectFromText(File file) throws IOException {
        Project project = new Project(file.getName().replaceFirst("\\.txt$", ""));
        List<Task> tasks = new ArrayList<>();
        List<Resource> resources = new ArrayList<>();
        
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            boolean inResourcesSection = false;
            
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;
                
                if (line.equals("---RESOURCES---")) {
                    inResourcesSection = true;
                    continue;
                }
                
                if (!inResourcesSection) {
                    // Parse task
                    String[] parts = line.split(",", -1);
                    if (parts.length >= 4) {
                        try {
                            int id = Integer.parseInt(parts[0].trim());
                            String title = parts[1].trim();
                            String start = parts[2].trim();
                            String end = parts[3].trim();
                            String deps = parts.length > 4 ? parts[4].trim() : "";
                            Task t = new Task(id, title, start, end, deps);
                            tasks.add(t);
                        } catch (NumberFormatException e) {
                            System.err.println("Invalid task ID in line: " + line);
                        }
                    }
                } else {
                    // Parse resource
                    String[] parts = line.split(",", -1);
                    if (parts.length >= 1) {
                        String name = parts[0].trim();
                        List<Allocation> allocations = new ArrayList<>();
                        
                        for (int i = 1; i < parts.length; i++) {
                            String allocStr = parts[i].trim();
                            if (!allocStr.isEmpty()) {
                                String[] pair = allocStr.split(":");
                                if (pair.length == 2) {
                                    try {
                                        int taskId = Integer.parseInt(pair[0].trim());
                                        int load = Integer.parseInt(pair[1].trim());
                                        allocations.add(new Allocation(taskId, load));
                                    } catch (NumberFormatException e) {
                                        System.err.println("Invalid allocation format: " + allocStr);
                                    }
                                }
                            }
                        }
                        resources.add(new Resource(name, allocations));
                    }
                }
            }
        }
        
        project.setTasks(tasks);
        project.setResources(resources);
        return project;
    }

    public void saveTasksToFile(List<Task> tasks, String filePath) throws IOException {
        try (PrintWriter out = new PrintWriter(filePath)) {
            for (Task t : tasks) {
                out.println(t.getId() + ", " + t.getTitle() + ", " + t.getStart() + ", " + t.getEnd() + 
                    (t.getDependencies().isEmpty() ? "" : ", " + t.getDependencies()));
            }
        }
    }

    public void saveResourcesToFile(List<Resource> resources, String filePath) throws IOException {
        try (PrintWriter out = new PrintWriter(filePath)) {
            for (Resource r : resources) {
                StringBuilder allocStr = new StringBuilder();
                for (Allocation a : r.getAllocations()) {
                    if (allocStr.length() > 0) allocStr.append(", ");
                    allocStr.append(a.getTaskId()).append(":").append(a.getLoad());
                }
                out.println(r.getName() + (allocStr.length() > 0 ? ", " + allocStr.toString() : ""));
            }
        }
    }
}