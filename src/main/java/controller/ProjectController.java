package controller;

import model.Project;
import model.storage.ProjectStorage;
import java.util.ArrayList;

public class ProjectController {
    private ArrayList<Project> history = new ArrayList<>();
    private model.storage.ProjectStorage storage;
    private Project currentProject;

    public ProjectController(ProjectStorage storage) {
        this.storage = storage;
    }

    public void saveProject(Project project, String filename) throws Exception {
        storage.saveProject(project, filename);
        if (!history.contains(project)) history.add(project);
    }

    public void addProjectToHistory(Project project) {
        if (!history.contains(project)) history.add(project);
    }

    public void setCurrentProject(Project project) {
        this.currentProject = project;
    }

    public Project getCurrentProject() {
        return currentProject;
    }

    public ArrayList<Project> getHistory() {
        return history;
    }
}
