package test;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;

class ProjectTest {
    private model.Project project;

    @BeforeEach
    void setUp() {
        project = new model.Project("Test Project");
        project.setTasks(new ArrayList<>());
        project.setResources(new ArrayList<>());
    }

    @Test
    void testNoOverlappingTasks() {
        model.Task task1 = new model.Task(1, "Task 1", "2024-01-01", "2024-01-10", "");
        model.Task task2 = new model.Task(2, "Task 2", "2024-01-11", "2024-01-20", "");

        project.getTasks().add(task1);
        project.getTasks().add(task2);

        List<String> overlaps = project.findOverlappingTasks();
        assertTrue(overlaps.isEmpty(), "Should not find overlaps for non-overlapping tasks");
    }

    @Test
    void testOverlappingTasksSamePeriod() {
        model.Task task1 = new model.Task(1, "Task 1", "2024-01-01", "2024-01-10", "");
        model.Task task2 = new model.Task(2, "Task 2", "2024-01-01", "2024-01-10", "");

        project.getTasks().add(task1);
        project.getTasks().add(task2);

        List<String> overlaps = project.findOverlappingTasks();
        assertEquals(1, overlaps.size(), "Should find one overlapping pair");
        assertTrue(overlaps.get(0).contains("Task 1"));
        assertTrue(overlaps.get(0).contains("Task 2"));
    }

   

    @Test
    void testProjectCompletionTimeSingleTask() {
        model.Task task1 = new model.Task(1, "Task 1", "2024-01-01", "2024-01-10", "");
        project.getTasks().add(task1);

        String completionTime = project.calculateProjectCompletionTime();
        assertEquals("2024-01-10", completionTime);
    }

    @Test
    void testProjectCompletionTimeMultipleTasks() {
        model.Task task1 = new model.Task(1, "Task 1", "2024-01-01", "2024-01-15", "");
        model.Task task2 = new model.Task(2, "Task 2", "2024-01-05", "2024-01-20", "");
        model.Task task3 = new model.Task(3, "Task 3", "2024-01-10", "2024-01-25", "");

        project.getTasks().addAll(Arrays.asList(task1, task2, task3));

        String completionTime = project.calculateProjectCompletionTime();
        assertEquals("2024-01-25", completionTime);
    }

    @Test
    void testProjectCompletionTimeEmptyProject() {
        String completionTime = project.calculateProjectCompletionTime();
        assertNull(completionTime);
    }
}