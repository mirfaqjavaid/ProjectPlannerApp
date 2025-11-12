package model.storage;

import java.sql.*;

public class DatabaseInitializer {
    public static void initializeDatabase() {


   String url = "jdbc:sqlite:project_planner.db";

        try (Connection conn = DriverManager.getConnection(url);
             Statement stmt = conn.createStatement()) {
            
            // Create projects table
            String projectsTable = "CREATE TABLE IF NOT EXISTS projects (" +
                    "name TEXT PRIMARY KEY" +
                    ")";
            
            // Create tasks table
            String tasksTable = "CREATE TABLE IF NOT EXISTS tasks (" +
                    "id INTEGER," +
                    "title TEXT NOT NULL," +
                    "start_date TEXT," +
                    "end_date TEXT," +
                    "dependencies TEXT," +
                    "project_name TEXT," +
                    "PRIMARY KEY (id, project_name)," +
                    "FOREIGN KEY (project_name) REFERENCES projects(name) ON DELETE CASCADE" +
                    ")";
            
            // Create resources table
            String resourcesTable = "CREATE TABLE IF NOT EXISTS resources (" +
                    "name TEXT," +
                    "project_name TEXT," +
                    "PRIMARY KEY (name, project_name)," +
                    "FOREIGN KEY (project_name) REFERENCES projects(name) ON DELETE CASCADE" +
                    ")";
            
            // Create allocations table
            String allocationsTable = "CREATE TABLE IF NOT EXISTS allocations (" +
                    "resource_name TEXT," +
                    "task_id INTEGER," +
                    "load_percentage INTEGER," +
                    "project_name TEXT," +
                    "PRIMARY KEY (resource_name, task_id, project_name)," +
                    "FOREIGN KEY (resource_name, project_name) REFERENCES resources(name, project_name) ON DELETE CASCADE," +
                    "FOREIGN KEY (task_id, project_name) REFERENCES tasks(id, project_name) ON DELETE CASCADE" +
                    ")";
            
            stmt.execute(projectsTable);
            stmt.execute(tasksTable);
            stmt.execute(resourcesTable);
            stmt.execute(allocationsTable);
            
            // Enable foreign keys
            stmt.execute("PRAGMA foreign_keys = ON");
            
        } catch (SQLException e) {
            System.err.println("Error initializing database: " + e.getMessage());
        }
    }
}