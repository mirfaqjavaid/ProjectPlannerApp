package model.storage;

import java.util.List;
import java.io.File;
import java.io.IOException;
import model.Project;

public interface ProjectStorage {
    void saveProject(Project project, String filename) throws Exception;
    Project loadProject(String filename) throws Exception;
    List<Project> loadAllProjects(String directory) throws Exception;
    void saveProjectAsText(Project project, File file) throws IOException;
    Project loadProjectFromText(File file) throws IOException;
}