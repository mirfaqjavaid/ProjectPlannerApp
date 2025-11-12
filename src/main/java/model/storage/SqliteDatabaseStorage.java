package model.storage;

import java.sql.*;
import java.util.*;
import java.io.File;
import java.io.IOException;
import model.*;

public class SqliteDatabaseStorage implements DatabaseStorage {
    
    private Connection connect() throws SQLException {
        
       String url = "jdbc:sqlite:project_planner.db";


        return DriverManager.getConnection(url);
    }

    @Override
    public void saveProjectToDatabase(Project project) throws Exception {
        try (Connection conn = connect()) {
            conn.setAutoCommit(false);
            
            // Save project metadata
            String projectSql = "INSERT OR REPLACE INTO projects(name) VALUES(?)";
            try (PreparedStatement pstmt = conn.prepareStatement(projectSql)) {
                pstmt.setString(1, project.getName());
                pstmt.executeUpdate();
            }
            
            // Clear existing tasks and resources for this project
            String clearTasksSql = "DELETE FROM tasks WHERE project_name = ?";
            String clearResourcesSql = "DELETE FROM resources WHERE project_name = ?";
            String clearAllocationsSql = "DELETE FROM allocations WHERE project_name = ?";
            
            try (PreparedStatement pstmt = conn.prepareStatement(clearAllocationsSql)) {
                pstmt.setString(1, project.getName());
                pstmt.executeUpdate();
            }
            try (PreparedStatement pstmt = conn.prepareStatement(clearTasksSql)) {
                pstmt.setString(1, project.getName());
                pstmt.executeUpdate();
            }
            try (PreparedStatement pstmt = conn.prepareStatement(clearResourcesSql)) {
                pstmt.setString(1, project.getName());
                pstmt.executeUpdate();
            }
            
            // Save tasks
            String taskSql = "INSERT INTO tasks(id, title, start_date, end_date, dependencies, project_name) VALUES(?,?,?,?,?,?)";
            try (PreparedStatement pstmt = conn.prepareStatement(taskSql)) {
                for (Task task : project.getTasks()) {
                    pstmt.setInt(1, task.getId());
                    pstmt.setString(2, task.getTitle());
                    pstmt.setString(3, task.getStart());
                    pstmt.setString(4, task.getEnd());
                    pstmt.setString(5, task.getDependencies());
                    pstmt.setString(6, project.getName());
                    pstmt.addBatch();
                }
                pstmt.executeBatch();
            }
            
            // Save resources and allocations
            String resourceSql = "INSERT INTO resources(name, project_name) VALUES(?,?)";
            String allocationSql = "INSERT INTO allocations(resource_name, task_id, load_percentage, project_name) VALUES(?,?,?,?)";
            
            try (PreparedStatement resourceStmt = conn.prepareStatement(resourceSql);
                 PreparedStatement allocStmt = conn.prepareStatement(allocationSql)) {
                
                for (Resource resource : project.getResources()) {
                    resourceStmt.setString(1, resource.getName());
                    resourceStmt.setString(2, project.getName());
                    resourceStmt.addBatch();
                    
                    for (Allocation alloc : resource.getAllocations()) {
                        allocStmt.setString(1, resource.getName());
                        allocStmt.setInt(2, alloc.getTaskId());
                        allocStmt.setInt(3, alloc.getLoad());
                        allocStmt.setString(4, project.getName());
                        allocStmt.addBatch();
                    }
                }
                resourceStmt.executeBatch();
                allocStmt.executeBatch();
            }
            
            conn.commit();
        }
    }

    @Override
    public Project loadProjectFromDatabase(String projectName) throws Exception {
        Project project = new Project(projectName);
        
        try (Connection conn = connect()) {
            // Load tasks
            String taskSql = "SELECT * FROM tasks WHERE project_name = ?";
            try (PreparedStatement pstmt = conn.prepareStatement(taskSql)) {
                pstmt.setString(1, projectName);
                ResultSet rs = pstmt.executeQuery();
                
                List<Task> tasks = new ArrayList<>();
                while (rs.next()) {
                    Task task = new Task(
                        rs.getInt("id"),
                        rs.getString("title"),
                        rs.getString("start_date"),
                        rs.getString("end_date"),
                        rs.getString("dependencies")
                    );
                    tasks.add(task);
                }
                project.setTasks(tasks);
            }
            
            // Load resources with allocations
            String resourceSql = "SELECT r.name, a.task_id, a.load_percentage " +
                               "FROM resources r LEFT JOIN allocations a ON r.name = a.resource_name AND r.project_name = a.project_name " +
                               "WHERE r.project_name = ?";
            try (PreparedStatement pstmt = conn.prepareStatement(resourceSql)) {
                pstmt.setString(1, projectName);
                ResultSet rs = pstmt.executeQuery();
                
                Map<String, List<Allocation>> resourceMap = new HashMap<>();
                while (rs.next()) {
                    String resourceName = rs.getString("name");
                    int taskId = rs.getInt("task_id");
                    int load = rs.getInt("load_percentage");
                    
                    if (!resourceMap.containsKey(resourceName)) {
                        resourceMap.put(resourceName, new ArrayList<>());
                    }
                    if (taskId > 0) {
                        resourceMap.get(resourceName).add(new Allocation(taskId, load));
                    }
                }
                
                List<Resource> resources = new ArrayList<>();
                for (Map.Entry<String, List<Allocation>> entry : resourceMap.entrySet()) {
                    resources.add(new Resource(entry.getKey(), entry.getValue()));
                }
                project.setResources(resources);
            }
        }
        
        return project;
    }

    @Override
    public List<Project> loadAllProjectsFromDatabase() throws Exception {
        List<Project> projects = new ArrayList<>();
        try (Connection conn = connect()) {
            String sql = "SELECT DISTINCT name FROM projects";
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(sql)) {
                while (rs.next()) {
                    projects.add(loadProjectFromDatabase(rs.getString("name")));
                }
            }
        }
        return projects;
    }

    @Override
    public void updateTaskInDatabase(Task task) throws Exception {
        String sql = "UPDATE tasks SET title = ?, start_date = ?, end_date = ?, dependencies = ? WHERE id = ?";
        try (Connection conn = connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, task.getTitle());
            pstmt.setString(2, task.getStart());
            pstmt.setString(3, task.getEnd());
            pstmt.setString(4, task.getDependencies());
            pstmt.setInt(5, task.getId());
            pstmt.executeUpdate();
        }
    }

    @Override
    public void deleteTaskFromDatabase(int taskId) throws Exception {
        try (Connection conn = connect()) {
            conn.setAutoCommit(false);
            
            // Delete allocations first
            String allocSql = "DELETE FROM allocations WHERE task_id = ?";
            try (PreparedStatement pstmt = conn.prepareStatement(allocSql)) {
                pstmt.setInt(1, taskId);
                pstmt.executeUpdate();
            }
            
            // Delete task
            String taskSql = "DELETE FROM tasks WHERE id = ?";
            try (PreparedStatement pstmt = conn.prepareStatement(taskSql)) {
                pstmt.setInt(1, taskId);
                pstmt.executeUpdate();
            }
            
            conn.commit();
        }
    }

    @Override
    public void updateResourceInDatabase(Resource resource) throws Exception {
        // For simplicity, we delete and reinsert
        deleteResourceFromDatabase(resource.getName());
        // Note: Need project name context - this will be handled in the controller
    }

    @Override
    public void deleteResourceFromDatabase(String resourceName) throws Exception {
        try (Connection conn = connect()) {
            conn.setAutoCommit(false);
            
            // Delete allocations first
            String allocSql = "DELETE FROM allocations WHERE resource_name = ?";
            try (PreparedStatement pstmt = conn.prepareStatement(allocSql)) {
                pstmt.setString(1, resourceName);
                pstmt.executeUpdate();
            }
            
            // Delete resource
            String resourceSql = "DELETE FROM resources WHERE name = ?";
            try (PreparedStatement pstmt = conn.prepareStatement(resourceSql)) {
                pstmt.setString(1, resourceName);
                pstmt.executeUpdate();
            }
            
            conn.commit();
        }
    }

    @Override
    public void saveTaskToDatabase(Task task, String projectName) throws Exception {
        String sql = "INSERT OR REPLACE INTO tasks(id, title, start_date, end_date, dependencies, project_name) VALUES(?,?,?,?,?,?)";
        try (Connection conn = connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, task.getId());
            pstmt.setString(2, task.getTitle());
            pstmt.setString(3, task.getStart());
            pstmt.setString(4, task.getEnd());
            pstmt.setString(5, task.getDependencies());
            pstmt.setString(6, projectName);
            pstmt.executeUpdate();
        }
    }

    @Override
    public void saveResourceToDatabase(Resource resource, String projectName) throws Exception {
        try (Connection conn = connect()) {
            conn.setAutoCommit(false);
            
            // Save resource
            String resourceSql = "INSERT OR REPLACE INTO resources(name, project_name) VALUES(?,?)";
            try (PreparedStatement pstmt = conn.prepareStatement(resourceSql)) {
                pstmt.setString(1, resource.getName());
                pstmt.setString(2, projectName);
                pstmt.executeUpdate();
            }
            
            // Save allocations
            String allocSql = "INSERT OR REPLACE INTO allocations(resource_name, task_id, load_percentage, project_name) VALUES(?,?,?,?)";
            try (PreparedStatement pstmt = conn.prepareStatement(allocSql)) {
                for (Allocation alloc : resource.getAllocations()) {
                    pstmt.setString(1, resource.getName());
                    pstmt.setInt(2, alloc.getTaskId());
                    pstmt.setInt(3, alloc.getLoad());
                    pstmt.setString(4, projectName);
                    pstmt.addBatch();
                }
                pstmt.executeBatch();
            }
            
            conn.commit();
        }
    }

    // Implement ProjectStorage interface methods
    @Override
    public void saveProject(Project project, String filename) throws Exception {
        saveProjectToDatabase(project);
    }

    @Override
    public Project loadProject(String projectName) throws Exception {
        return loadProjectFromDatabase(projectName);
    }

    @Override
    public List<Project> loadAllProjects(String directory) throws Exception {
        return loadAllProjectsFromDatabase();
    }

    @Override
    public void saveProjectAsText(Project project, File file) throws IOException {
        try {
            saveProjectToDatabase(project);
        } catch (Exception e) {
            throw new IOException("Failed to save to database: " + e.getMessage(), e);
        }
    }

    @Override
    public Project loadProjectFromText(File file) throws IOException {
        try {
            return loadProjectFromDatabase(file.getName().replaceFirst("[.][^.]+$", ""));
        } catch (Exception e) {
            throw new IOException("Failed to load from database: " + e.getMessage(), e);
        }
    }
}