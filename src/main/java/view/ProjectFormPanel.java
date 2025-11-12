package view;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import controller.FileController;
import java.io.File;
import model.Allocation;
import model.Project;
import model.Resource;
import model.Task;
import model.storage.DatabaseInitializer;
import model.storage.FileProjectStorage;
import model.storage.SqliteDatabaseStorage;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ProjectFormPanel extends JPanel {
    private final Project project;
    private JTable taskTable, resourceTable;
    private JButton addTaskBtn, addResourceBtn, saveBtn, newProjectBtn, uploadTasksBtn, uploadResourcesBtn;
    private JButton editSelectedTaskBtn, deleteSelectedTaskBtn, editSelectedResourceBtn, deleteSelectedResourceBtn;
    private JComboBox<String> storageTypeCombo;
    private FileProjectStorage fileStorage;
    private SqliteDatabaseStorage databaseStorage;
    private boolean useDatabaseStorage = false;

    public ProjectFormPanel(Project project) {
        this.project = project;
        initializeStorage();
        initializeUI();
    }

    private void initializeStorage() {
        DatabaseInitializer.initializeDatabase();
        this.fileStorage = new FileProjectStorage();
        this.databaseStorage = new SqliteDatabaseStorage();
    }

    private void initializeUI() {
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // --- Top controls panel ---
        JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        controlPanel.setBorder(BorderFactory.createTitledBorder("Project Controls"));
        
        // Storage selection
        controlPanel.add(new JLabel("Storage:"));
        storageTypeCombo = new JComboBox<>(new String[]{"File Storage", "Database Storage"});
        storageTypeCombo.addActionListener(e -> {
            useDatabaseStorage = storageTypeCombo.getSelectedIndex() == 1;
            if (useDatabaseStorage) {
                loadProjectFromDatabase();
            }
        });
        controlPanel.add(storageTypeCombo);
        controlPanel.add(Box.createHorizontalStrut(20));

        // Upload buttons
        uploadTasksBtn = new JButton("Upload Tasks");
        uploadResourcesBtn = new JButton("Upload Resources");
        controlPanel.add(uploadTasksBtn);
        controlPanel.add(uploadResourcesBtn);
        controlPanel.add(Box.createHorizontalStrut(20));

        // Basic operation buttons
        addTaskBtn = new JButton("Add Task");
        addResourceBtn = new JButton("Add Resource");
        saveBtn = new JButton("Save");
        newProjectBtn = new JButton("New Project");
        
        controlPanel.add(addTaskBtn);
        controlPanel.add(addResourceBtn);
        controlPanel.add(saveBtn);
        controlPanel.add(newProjectBtn);

        add(controlPanel, BorderLayout.NORTH);

        // --- Tables panel ---
        JPanel tablesPanel = new JPanel(new GridLayout(2, 1, 10, 10));

        // Create tables without edit/delete columns
        taskTable = createTaskTable();
        resourceTable = createResourceTable();

        JScrollPane taskScroll = new JScrollPane(taskTable);
        JScrollPane resourceScroll = new JScrollPane(resourceTable);

        taskScroll.setBorder(BorderFactory.createTitledBorder("Tasks"));
        resourceScroll.setBorder(BorderFactory.createTitledBorder("Resources"));

        tablesPanel.add(taskScroll);
        tablesPanel.add(resourceScroll);

        add(tablesPanel, BorderLayout.CENTER);

        // --- Action buttons panel (for Edit/Delete operations) ---
        JPanel actionPanel = new JPanel(new GridLayout(2, 1, 5, 5));
        
        // Task actions panel
        JPanel taskActionPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 5));
        taskActionPanel.setBorder(BorderFactory.createTitledBorder("Task Operations"));
        
        editSelectedTaskBtn = new JButton("Edit Selected Task");
        deleteSelectedTaskBtn = new JButton("Delete Selected Task");
        
        // Style the buttons
        editSelectedTaskBtn.setBackground(new Color(255, 165, 0));
        editSelectedTaskBtn.setForeground(Color.WHITE);
        deleteSelectedTaskBtn.setBackground(new Color(220, 20, 60));
        deleteSelectedTaskBtn.setForeground(Color.WHITE);
        
        editSelectedTaskBtn.setEnabled(false);
        deleteSelectedTaskBtn.setEnabled(false);
        
        taskActionPanel.add(editSelectedTaskBtn);
        taskActionPanel.add(deleteSelectedTaskBtn);
        
        // Resource actions panel
        JPanel resourceActionPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 5));
        resourceActionPanel.setBorder(BorderFactory.createTitledBorder("Resource Operations"));
        
        editSelectedResourceBtn = new JButton("Edit Selected Resource");
        deleteSelectedResourceBtn = new JButton("Delete Selected Resource");
        
        // Style the buttons
        editSelectedResourceBtn.setBackground(new Color(255, 165, 0));
        editSelectedResourceBtn.setForeground(Color.WHITE);
        deleteSelectedResourceBtn.setBackground(new Color(220, 20, 60));
        deleteSelectedResourceBtn.setForeground(Color.WHITE);
        
        editSelectedResourceBtn.setEnabled(false);
        deleteSelectedResourceBtn.setEnabled(false);
        
        resourceActionPanel.add(editSelectedResourceBtn);
        resourceActionPanel.add(deleteSelectedResourceBtn);
        
        actionPanel.add(taskActionPanel);
        actionPanel.add(resourceActionPanel);

        add(actionPanel, BorderLayout.SOUTH);

        // --- Add selection listeners to tables ---
        taskTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                boolean hasSelection = taskTable.getSelectedRow() >= 0;
                editSelectedTaskBtn.setEnabled(hasSelection);
                deleteSelectedTaskBtn.setEnabled(hasSelection);
            }
        });

        resourceTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                boolean hasSelection = resourceTable.getSelectedRow() >= 0;
                editSelectedResourceBtn.setEnabled(hasSelection);
                deleteSelectedResourceBtn.setEnabled(hasSelection);
            }
        });

        // --- Button Actions ---
        addTaskBtn.addActionListener(e -> showAddTaskDialog());
        addResourceBtn.addActionListener(e -> showAddResourceDialog());
        saveBtn.addActionListener(e -> saveProject());
        newProjectBtn.addActionListener(e -> createOrLoadNewProject());
        uploadTasksBtn.addActionListener(e -> handleFileUpload("tasks"));
        uploadResourcesBtn.addActionListener(e -> handleFileUpload("resources"));
        
        // Edit/Delete button actions
        editSelectedTaskBtn.addActionListener(e -> editSelectedTask());
        deleteSelectedTaskBtn.addActionListener(e -> deleteSelectedTask());
        editSelectedResourceBtn.addActionListener(e -> editSelectedResource());
        deleteSelectedResourceBtn.addActionListener(e -> deleteSelectedResource());

        JButton showTasksBtn = new JButton("Show Task Table");
        controlPanel.add(showTasksBtn);
        showTasksBtn.addActionListener(e -> showFullTaskTableDialog());

        // Load initial data
        refreshTaskTable();
        refreshResourceTable();
    }

    private JTable createTaskTable() {
        String[] columns = {"ID", "Title", "Start", "End", "Dependencies"};
        DefaultTableModel model = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        JTable table = new JTable(model);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setRowHeight(25);
        
        // Set column widths
        table.getColumnModel().getColumn(0).setPreferredWidth(50);
        table.getColumnModel().getColumn(1).setPreferredWidth(200);
        table.getColumnModel().getColumn(2).setPreferredWidth(100);
        table.getColumnModel().getColumn(3).setPreferredWidth(100);
        table.getColumnModel().getColumn(4).setPreferredWidth(150);

        return table;
    }

    private JTable createResourceTable() {
        String[] columns = {"Name", "Allocations"};
        DefaultTableModel model = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        JTable table = new JTable(model);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setRowHeight(25);
        
        // Set column widths
        table.getColumnModel().getColumn(0).setPreferredWidth(150);
        table.getColumnModel().getColumn(1).setPreferredWidth(300);

        return table;
    }

    // === ADD TASK FUNCTIONALITY ===
    private void showAddTaskDialog() {
        JTextField idField = new JTextField();
        JTextField titleField = new JTextField();
        JTextField startField = new JTextField();
        JTextField endField = new JTextField();
        JTextField depsField = new JTextField();

        Object[] fields = {
            "ID (int):", idField,
            "Title:", titleField,
            "Start (yyyy-MM-dd):", startField,
            "End (yyyy-MM-dd):", endField,
            "Dependencies (comma ids):", depsField
        };
        int res = JOptionPane.showConfirmDialog(this, fields, "Add Task", JOptionPane.OK_CANCEL_OPTION);
        if (res == JOptionPane.OK_OPTION) {
            try {
                int id = Integer.parseInt(idField.getText().trim());
                String title = titleField.getText().trim();
                String start = startField.getText().trim();
                String end = endField.getText().trim();
                String deps = depsField.getText().trim();

                // Validation
                if (title.isEmpty() || start.isEmpty() || end.isEmpty()) {
                    JOptionPane.showMessageDialog(this, "All fields except Dependencies are required.", "Input Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                
                // Check unique ID
                if (project.getTasks() != null) {
                    for (Task t : project.getTasks()) {
                        if (t.getId() == id) {
                            JOptionPane.showMessageDialog(this, "Task ID must be unique.", "Input Error", JOptionPane.ERROR_MESSAGE);
                            return;
                        }
                    }
                }
                
                Task t = new Task(id, title, start, end, deps);
                if (project.getTasks() == null) {
                    project.setTasks(new ArrayList<>());
                }
                project.getTasks().add(t);
                
                // Save to database if using database storage
                if (useDatabaseStorage) {
                    try {
                        databaseStorage.saveTaskToDatabase(t, project.getName());
                    } catch (Exception e) {
                        JOptionPane.showMessageDialog(this, "Error saving task to database: " + e.getMessage(),
                                "Database Error", JOptionPane.ERROR_MESSAGE);
                    }
                }
                
                refreshTaskTable();
                JOptionPane.showMessageDialog(this, "Task added successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Invalid input: " + ex.getMessage(), "Input Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    // === ADD RESOURCE FUNCTIONALITY ===
    private void showAddResourceDialog() {
        JTextField nameField = new JTextField();
        JTextField allocsField = new JTextField();

        Object[] fields = {
            "Name:", nameField,
            "Allocations (taskId:load, ...):", allocsField
        };
        int res = JOptionPane.showConfirmDialog(this, fields, "Add Resource", JOptionPane.OK_CANCEL_OPTION);
        if (res == JOptionPane.OK_OPTION) {
            try {
                String name = nameField.getText().trim();
                if (name.isEmpty()) {
                    JOptionPane.showMessageDialog(this, "Name required.", "Input Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                String allocsStr = allocsField.getText().trim();
                List<Allocation> allocList = new ArrayList<>();
                if (!allocsStr.isEmpty()) {
                    String[] allocs = allocsStr.split(",");
                    for (String pair : allocs) {
                        String[] vals = pair.trim().split(":");
                        if (vals.length == 2) {
                            int taskId = Integer.parseInt(vals[0].trim());
                            int load = Integer.parseInt(vals[1].trim());
                            allocList.add(new Allocation(taskId, load));
                        }
                    }
                }
                Resource resObj = new Resource(name, allocList);
                if (project.getResources() == null) {
                    project.setResources(new ArrayList<>());
                }
                project.getResources().add(resObj);
                
                // Save to database if using database storage
                if (useDatabaseStorage) {
                    try {
                        databaseStorage.saveResourceToDatabase(resObj, project.getName());
                    } catch (Exception e) {
                        JOptionPane.showMessageDialog(this, "Error saving resource to database: " + e.getMessage(),
                                "Database Error", JOptionPane.ERROR_MESSAGE);
                    }
                }
                
                refreshResourceTable();
                JOptionPane.showMessageDialog(this, "Resource added successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Invalid input: " + ex.getMessage(), "Input Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    // === NEW PROJECT FUNCTIONALITY ===
    private void createOrLoadNewProject() {
        Object[] options = { "Create New", "Load from File", "Load from Database", "Cancel" };
        int choice = JOptionPane.showOptionDialog(this, 
            "New Project: create new or load from storage?", "New Project", 
            JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, options[0]);
        
        if (choice == 0) {
            // Create New
            String projectName = JOptionPane.showInputDialog("Enter Project Name:");
            if (projectName != null && !projectName.trim().isEmpty()) {
                project.setName(projectName.trim());
                project.setTasks(new ArrayList<>());
                project.setResources(new ArrayList<>());
                storageTypeCombo.setSelectedIndex(0);
                useDatabaseStorage = false;
                refreshTaskTable();
                refreshResourceTable();
                JOptionPane.showMessageDialog(this, "New project created: " + projectName, "Success", JOptionPane.INFORMATION_MESSAGE);
            }
        } else if (choice == 1) {
            // Load from File
            JFileChooser chooser = new JFileChooser();
            int res = chooser.showOpenDialog(this);
            if (res == JFileChooser.APPROVE_OPTION) {
                File file = chooser.getSelectedFile();
                try {
                    Project loaded = fileStorage.loadProjectFromText(file);
                    project.setName(loaded.getName());
                    project.setTasks(loaded.getTasks());
                    project.setResources(loaded.getResources());
                    storageTypeCombo.setSelectedIndex(0);
                    useDatabaseStorage = false;
                    refreshTaskTable();
                    refreshResourceTable();
                    JOptionPane.showMessageDialog(this, "Project loaded from file successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(this, "Failed to load: " + ex.getMessage(), 
                            "Load Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        } else if (choice == 2) {
            // Load from Database
            String projectName = JOptionPane.showInputDialog("Enter Project Name to load from database:");
            if (projectName != null && !projectName.trim().isEmpty()) {
                try {
                    Project loaded = databaseStorage.loadProjectFromDatabase(projectName.trim());
                    project.setName(loaded.getName());
                    project.setTasks(loaded.getTasks());
                    project.setResources(loaded.getResources());
                    storageTypeCombo.setSelectedIndex(1);
                    useDatabaseStorage = true;
                    refreshTaskTable();
                    refreshResourceTable();
                    JOptionPane.showMessageDialog(this, "Project loaded from database successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(this, "Failed to load from database: " + ex.getMessage(), 
                            "Load Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        }
    }

    // === SAVE PROJECT FUNCTIONALITY ===
    private void saveProject() {
        if (useDatabaseStorage) {
            saveProjectToDatabase();
        } else {
            saveProjectToFile();
        }
    }

    private void saveProjectToDatabase() {
        try {
            databaseStorage.saveProjectToDatabase(project);
            JOptionPane.showMessageDialog(this, "Project saved to database successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error saving to database: " + ex.getMessage(),
                    "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void saveProjectToFile() {
        JFileChooser chooser = new JFileChooser();
        chooser.setSelectedFile(new File(project.getName() + ".txt"));
        int res = chooser.showSaveDialog(this);
        if (res == JFileChooser.APPROVE_OPTION) {
            File file = chooser.getSelectedFile();
            try {
                fileStorage.saveProjectAsText(project, file);
                JOptionPane.showMessageDialog(this, "Project saved to " + file.getAbsolutePath(), "Success", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error saving: " + ex.getMessage(), 
                        "Save Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    // === EDIT/DELETE FUNCTIONALITY ===
    private void editSelectedTask() {
        int selectedRow = taskTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Please select a task to edit.", "No Selection", JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        if (project.getTasks() == null || selectedRow < 0 || selectedRow >= project.getTasks().size()) return;
        
        Task task = project.getTasks().get(selectedRow);
        showEditTaskDialog(task, selectedRow);
    }

    private void deleteSelectedTask() {
        int selectedRow = taskTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Please select a task to delete.", "No Selection", JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        if (project.getTasks() == null || selectedRow < 0 || selectedRow >= project.getTasks().size()) return;
        
        Task task = project.getTasks().get(selectedRow);
        int confirm = JOptionPane.showConfirmDialog(this, 
            "Are you sure you want to delete task '" + task.getTitle() + "'?\nThis action cannot be undone.", 
            "Confirm Delete", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        
        if (confirm == JOptionPane.YES_OPTION) {
            // Remove from backend first
            if (useDatabaseStorage) {
                try {
                    databaseStorage.deleteTaskFromDatabase(task.getId());
                } catch (Exception e) {
                    JOptionPane.showMessageDialog(this, "Error deleting task from database: " + e.getMessage(),
                            "Database Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }
            }
            
            // Remove from frontend
            project.getTasks().remove(selectedRow);
            
            // Refresh display
            refreshTaskTable();
            refreshResourceTable();
            
            // Clear selection
            taskTable.clearSelection();
            editSelectedTaskBtn.setEnabled(false);
            deleteSelectedTaskBtn.setEnabled(false);
            
            JOptionPane.showMessageDialog(this, "Task deleted successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    private void editSelectedResource() {
        int selectedRow = resourceTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Please select a resource to edit.", "No Selection", JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        if (project.getResources() == null || selectedRow < 0 || selectedRow >= project.getResources().size()) return;
        
        Resource resource = project.getResources().get(selectedRow);
        showEditResourceDialog(resource, selectedRow);
    }

    private void deleteSelectedResource() {
        int selectedRow = resourceTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Please select a resource to delete.", "No Selection", JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        if (project.getResources() == null || selectedRow < 0 || selectedRow >= project.getResources().size()) return;
        
        Resource resource = project.getResources().get(selectedRow);
        int confirm = JOptionPane.showConfirmDialog(this, 
            "Are you sure you want to delete resource '" + resource.getName() + "'?\nThis action cannot be undone.", 
            "Confirm Delete", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        
        if (confirm == JOptionPane.YES_OPTION) {
            // Remove from backend first
            if (useDatabaseStorage) {
                try {
                    databaseStorage.deleteResourceFromDatabase(resource.getName());
                } catch (Exception e) {
                    JOptionPane.showMessageDialog(this, "Error deleting resource from database: " + e.getMessage(),
                            "Database Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }
            }
            
            // Remove from frontend
            project.getResources().remove(selectedRow);
            
            // Refresh display
            refreshResourceTable();
            
            // Clear selection
            resourceTable.clearSelection();
            editSelectedResourceBtn.setEnabled(false);
            deleteSelectedResourceBtn.setEnabled(false);
            
            JOptionPane.showMessageDialog(this, "Resource deleted successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    private void showEditTaskDialog(Task task, int row) {
        // Create form fields with current data
        JTextField idField = new JTextField(String.valueOf(task.getId()));
        JTextField titleField = new JTextField(task.getTitle());
        JTextField startField = new JTextField(task.getStart());
        JTextField endField = new JTextField(task.getEnd());
        JTextField depsField = new JTextField(task.getDependencies() != null ? task.getDependencies() : "");

        // Make ID field non-editable
        idField.setEditable(false);
        idField.setBackground(Color.LIGHT_GRAY);

        Object[] fields = {
            "ID (cannot be changed):", idField,
            "Title:", titleField,
            "Start (yyyy-MM-dd):", startField,
            "End (yyyy-MM-dd):", endField,
            "Dependencies (comma separated IDs):", depsField
        };

        int result = JOptionPane.showConfirmDialog(this, fields, "Edit Task - " + task.getTitle(), 
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        
        if (result == JOptionPane.OK_OPTION) {
            try {
                // Validate inputs
                String title = titleField.getText().trim();
                String start = startField.getText().trim();
                String end = endField.getText().trim();
                String deps = depsField.getText().trim();

                if (title.isEmpty() || start.isEmpty() || end.isEmpty()) {
                    JOptionPane.showMessageDialog(this, "Title, Start, and End fields are required.", "Validation Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                // Update the task object
                task.setTitle(title);
                task.setStart(start);
                task.setEnd(end);
                task.setDependencies(deps);

                // Save to backend
                if (useDatabaseStorage) {
                    try {
                        databaseStorage.updateTaskInDatabase(task);
                    } catch (Exception e) {
                        JOptionPane.showMessageDialog(this, "Error updating task in database: " + e.getMessage(),
                                "Database Error", JOptionPane.ERROR_MESSAGE);
                    }
                }

                // Refresh display
                refreshTaskTable();
                
                JOptionPane.showMessageDialog(this, "Task updated successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);

            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error updating task: " + ex.getMessage(), 
                        "Update Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void showEditResourceDialog(Resource resource, int row) {
        // Create form fields with current data
        JTextField nameField = new JTextField(resource.getName());
        
        // Build allocations string
        StringBuilder allocsBuilder = new StringBuilder();
        if (resource.getAllocations() != null) {
            for (Allocation alloc : resource.getAllocations()) {
                if (allocsBuilder.length() > 0) allocsBuilder.append(", ");
                allocsBuilder.append(alloc.getTaskId()).append(":").append(alloc.getLoad());
            }
        }
        JTextField allocsField = new JTextField(allocsBuilder.toString());

        Object[] fields = {
            "Name:", nameField,
            "Allocations (format: taskId:load, taskId:load, ...):", allocsField
        };

        int result = JOptionPane.showConfirmDialog(this, fields, "Edit Resource - " + resource.getName(), 
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        
        if (result == JOptionPane.OK_OPTION) {
            try {
                String name = nameField.getText().trim();
                String allocsStr = allocsField.getText().trim();
                
                if (name.isEmpty()) {
                    JOptionPane.showMessageDialog(this, "Resource name is required.", "Validation Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                
                // Parse allocations
                List<Allocation> allocList = new ArrayList<>();
                if (!allocsStr.isEmpty()) {
                    String[] allocs = allocsStr.split(",");
                    for (String pair : allocs) {
                        String[] vals = pair.trim().split(":");
                        if (vals.length == 2) {
                            try {
                                int taskId = Integer.parseInt(vals[0].trim());
                                int load = Integer.parseInt(vals[1].trim());
                                if (load < 0 || load > 100) {
                                    JOptionPane.showMessageDialog(this, "Load must be between 0 and 100.", "Validation Error", JOptionPane.ERROR_MESSAGE);
                                    return;
                                }
                                allocList.add(new Allocation(taskId, load));
                            } catch (NumberFormatException e) {
                                JOptionPane.showMessageDialog(this, "Invalid allocation format: " + pair, "Validation Error", JOptionPane.ERROR_MESSAGE);
                                return;
                            }
                        } else {
                            JOptionPane.showMessageDialog(this, "Invalid allocation format: " + pair, "Validation Error", JOptionPane.ERROR_MESSAGE);
                            return;
                        }
                    }
                }

                // For database, handle name change by deleting old and creating new
                String oldName = resource.getName();
                if (useDatabaseStorage && !oldName.equals(name)) {
                    try {
                        databaseStorage.deleteResourceFromDatabase(oldName);
                    } catch (Exception e) {
                        // Continue anyway
                    }
                }

                // Update the resource object
                resource.setName(name);
                resource.setAllocations(allocList);

                // Save to backend
                if (useDatabaseStorage) {
                    try {
                        databaseStorage.saveResourceToDatabase(resource, project.getName());
                    } catch (Exception e) {
                        JOptionPane.showMessageDialog(this, "Error updating resource in database: " + e.getMessage(),
                                "Database Error", JOptionPane.ERROR_MESSAGE);
                    }
                }

                // Refresh display
                refreshResourceTable();
                
                JOptionPane.showMessageDialog(this, "Resource updated successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);

            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error updating resource: " + ex.getMessage(), 
                        "Update Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    // === FILE UPLOAD FUNCTIONALITY ===
    private void handleFileUpload(String type) {
        JFileChooser chooser = new JFileChooser();
        chooser.setCurrentDirectory(new File(System.getProperty("user.dir")));
        int res = chooser.showOpenDialog(this);
        if (res != JFileChooser.APPROVE_OPTION) return;

        String path = chooser.getSelectedFile().getAbsolutePath();

        try {
            if (type.equalsIgnoreCase("tasks")) {
                FileController.ParseResult<Task> parsed = FileController.loadTasksWithErrors(path);

                if (!parsed.errors.isEmpty()) {
                    JOptionPane.showMessageDialog(this,
                            String.join("\n", parsed.errors),
                            "Task File Warnings", JOptionPane.WARNING_MESSAGE);
                }

                if (parsed.items.isEmpty()) {
                    JOptionPane.showMessageDialog(this,
                            "No valid tasks found. Please check your file format.",
                            "Upload Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                project.setTasks(parsed.items);
                JOptionPane.showMessageDialog(this, "Tasks uploaded successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);

                refreshTaskTable();

                // Save to database if using database storage
                if (useDatabaseStorage) {
                    for (Task task : parsed.items) {
                        try {
                            databaseStorage.saveTaskToDatabase(task, project.getName());
                        } catch (Exception e) {
                            JOptionPane.showMessageDialog(this, "Error saving task to database: " + e.getMessage(),
                                    "Database Error", JOptionPane.ERROR_MESSAGE);
                        }
                    }
                }

                // Prompt for resources upload
                int next = JOptionPane.showConfirmDialog(this,
                        "Do you want to upload Resources now?",
                        "Next Step",
                        JOptionPane.YES_NO_OPTION);
                if (next == JOptionPane.YES_OPTION) {
                    handleFileUpload("resources");
                }

            } else if (type.equalsIgnoreCase("resources")) {
                if (project.getTasks() == null || project.getTasks().isEmpty()) {
                    JOptionPane.showMessageDialog(this,
                            "Please upload tasks before uploading resources.",
                            "Missing Tasks", JOptionPane.WARNING_MESSAGE);
                    return;
                }

                List<Resource> resources = FileController.loadResources(path);

                // Validate resource allocations
                Set<Integer> validTaskIds = new HashSet<>();
                for (Task t : project.getTasks()) validTaskIds.add(t.getId());

                FileController.ValidationResult validation =
                        FileController.validateResources(resources, validTaskIds);

                if (!validation.errors.isEmpty()) {
                    JOptionPane.showMessageDialog(this,
                            String.join("\n", validation.errors),
                            "Resource File Warnings", JOptionPane.WARNING_MESSAGE);
                }

                project.setResources(resources);
                JOptionPane.showMessageDialog(this, "Resources uploaded and linked successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);

                refreshResourceTable();

                // Save to database if using database storage
                if (useDatabaseStorage) {
                    for (Resource resource : resources) {
                        try {
                            databaseStorage.saveResourceToDatabase(resource, project.getName());
                        } catch (Exception e) {
                            JOptionPane.showMessageDialog(this, "Error saving resource to database: " + e.getMessage(),
                                    "Database Error", JOptionPane.ERROR_MESSAGE);
                        }
                    }
                }
            }

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                    "Error processing file:\n" + ex.getMessage(),
                    "Upload Error", JOptionPane.ERROR_MESSAGE);
            ex.printStackTrace();
        }
    }

    // === SHOW FULL TASK TABLE ===
    private void showFullTaskTableDialog() {
        List<Task> tasks = project.getTasks();
        List<Resource> resources = project.getResources();
        if (tasks == null || tasks.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No tasks available.", "Info", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        
        String[] cols = {"Id", "Task", "Start", "End", "Dependencies", "Resources"};
        Object[][] data = new Object[tasks.size()][cols.length];

        for (int i = 0; i < tasks.size(); i++) {
            Task t = tasks.get(i);
            data[i][0] = t.getId();
            data[i][1] = t.getTitle();
            data[i][2] = t.getStart();
            data[i][3] = t.getEnd();
            data[i][4] = t.getDependencies();
            data[i][5] = getResourcesForTask(t, resources);
        }

        JTable table = new JTable(data, cols);
        table.setPreferredScrollableViewportSize(new Dimension(800, 300));
        JScrollPane scroll = new JScrollPane(table);
        JOptionPane.showMessageDialog(this, scroll, "Tasks Overview", JOptionPane.PLAIN_MESSAGE);
    }

    private String getResourcesForTask(Task t, List<Resource> resources) {
        List<String> resNames = new ArrayList<>();
        if (resources != null) {
            for (Resource r : resources) {
                if (r.getAllocations() != null) {
                    for (Allocation alloc : r.getAllocations()) {
                        if (alloc.getTaskId() == t.getId()) {
                            String suffix = (alloc.getLoad() < 100) ? "*" : "";
                            resNames.add(r.getName() + suffix);
                        }
                    }
                }
            }
        }
        return String.join(" ", resNames);
    }

    // === REFRESH METHODS ===
    private void refreshTaskTable() {
        List<Task> tasks = project.getTasks();
        if (tasks == null) return;

        String[] cols = {"ID", "Title", "Start", "End", "Dependencies"};
        Object[][] data = new Object[tasks.size()][cols.length];

        for (int i = 0; i < tasks.size(); i++) {
            Task t = tasks.get(i);
            data[i][0] = t.getId();
            data[i][1] = t.getTitle();
            data[i][2] = t.getStart();
            data[i][3] = t.getEnd();
            data[i][4] = t.getDependencies() != null ? t.getDependencies() : "";
        }

        taskTable.setModel(new DefaultTableModel(data, cols) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        });
    }

    private void refreshResourceTable() {
        List<Resource> resources = project.getResources();
        if (resources == null) return;

        String[] cols = {"Name", "Allocations"};
        Object[][] data = new Object[resources.size()][cols.length];

        for (int i = 0; i < resources.size(); i++) {
            Resource r = resources.get(i);
            data[i][0] = r.getName();
            data[i][1] = r.getAllocations() != null ? formatAllocations(r.getAllocations()) : "";
        }

        resourceTable.setModel(new DefaultTableModel(data, cols) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        });
    }

    private String formatAllocations(List<Allocation> allocations) {
        if (allocations == null || allocations.isEmpty()) return "No allocations";
        
        StringBuilder sb = new StringBuilder();
        for (Allocation alloc : allocations) {
            if (sb.length() > 0) sb.append(", ");
            sb.append("Task ").append(alloc.getTaskId()).append(" (").append(alloc.getLoad()).append("%)");
        }
        return sb.toString();
    }

    // === DATABASE LOAD METHOD ===
    private void loadProjectFromDatabase() {
        try {
            Project dbProject = databaseStorage.loadProjectFromDatabase(project.getName());
            if (dbProject.getTasks() != null && !dbProject.getTasks().isEmpty()) {
                project.setTasks(dbProject.getTasks());
                project.setResources(dbProject.getResources());
                refreshTaskTable();
                refreshResourceTable();
                JOptionPane.showMessageDialog(this, "Project loaded from database!", "Success", JOptionPane.INFORMATION_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(this, 
                    "No project data found in database. Starting with empty project.", 
                    "Info", JOptionPane.INFORMATION_MESSAGE);
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, 
                "No project found in database with name: " + project.getName() + 
                "\nCreating new project in database.", 
                "Info", JOptionPane.INFORMATION_MESSAGE);
        }
    }
}