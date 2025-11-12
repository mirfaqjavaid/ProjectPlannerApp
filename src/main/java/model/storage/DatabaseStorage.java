package model.storage;

import model.Project;
import java.util.List;

public interface DatabaseStorage extends ProjectStorage {
    // Database-specific methods
    void saveProjectToDatabase(Project project) throws Exception;
    Project loadProjectFromDatabase(String projectName) throws Exception;
    List<Project> loadAllProjectsFromDatabase() throws Exception;
    void updateTaskInDatabase(model.Task task) throws Exception;
    void deleteTaskFromDatabase(int taskId) throws Exception;
    void updateResourceInDatabase(model.Resource resource) throws Exception;
    void deleteResourceFromDatabase(String resourceName) throws Exception;
    void saveTaskToDatabase(model.Task task, String projectName) throws Exception;
    void saveResourceToDatabase(model.Resource resource, String projectName) throws Exception;
}