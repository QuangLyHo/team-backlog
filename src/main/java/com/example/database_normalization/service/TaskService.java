package com.example.database_normalization.service;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.security.core.Authentication;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;

import com.example.database_normalization.dto.TaskRequest;
import com.example.database_normalization.dto.TaskResponse;
import com.example.database_normalization.dto.TaskStatusUpdateRequest;
import com.example.database_normalization.entity.Project;

import com.example.database_normalization.entity.Task;
import com.example.database_normalization.entity.TaskStatus;
import com.example.database_normalization.entity.User;
import com.example.database_normalization.repository.ProjectRepository;
import com.example.database_normalization.repository.TaskRepository;
import com.example.database_normalization.repository.UserRepository;

@Service
public class TaskService {
    
    private final TaskRepository taskRepository;
    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final TeamService teamService;

    public TaskService(TaskRepository taskRepository, ProjectRepository projectRepository, UserRepository userRepository, TeamService teamService) {
        this.taskRepository = taskRepository;
        this.projectRepository = projectRepository;
        this.userRepository = userRepository;
        this.teamService = teamService;
    }

    public Page<TaskResponse> getAllTasks(Pageable pageable, TaskStatus status) {
        User currentUser = getCurrentUser();

        Page<Task> tasks = status != null
                ? taskRepository.findByProject_Team_MembersContainingAndStatus(currentUser, status, pageable)
                : taskRepository.findByProject_Team_MembersContaining(currentUser, pageable);

        return tasks.map(TaskResponse::from);
    }

    public Optional<TaskResponse> getTaskById(Long id) {
        return taskRepository.findById(id).map(task -> {
            checkTaskAccess(task);
            return TaskResponse.from(task);
        });
    }

    public List<TaskResponse> getTasksByProjectId(Long projectId) {
        checkProjectAccess(resolveProject(projectId));

        return taskRepository.findByProjectIdWithAssignees(projectId).stream()
                .map(TaskResponse::from)
                .toList();
    }

    public TaskResponse createTask(TaskRequest request) {
        Project project = resolveProject(request.projectId());
        checkProjectAccess(project);

        Task task = new Task();

        task.setTitle(request.title());
        task.setStatus(request.status());
        task.setProject(project);
        task.setAssignees(resolveAssignees(request.assigneeIds()));

        return TaskResponse.from(taskRepository.save(task));
    }

    public Optional<TaskResponse> updateTask(Long id, TaskRequest request) {
        return taskRepository.findById(id).map(existingTask -> {
            checkTaskAccess(existingTask);

            Project newProject = resolveProject(request.projectId());
            checkProjectAccess(newProject);

            existingTask.setTitle(request.title());
            existingTask.setStatus(request.status());

            existingTask.setProject(newProject);
            existingTask.setAssignees(resolveAssignees(request.assigneeIds()));

            return TaskResponse.from(taskRepository.save(existingTask));
        });
    }

    public Optional<TaskResponse> updateTaskStatus(Long id, TaskStatusUpdateRequest request) {
        return taskRepository.findById(id).map(existingTask -> {
            checkTaskAccess(existingTask);

            existingTask.setStatus(request.status());

            return TaskResponse.from(taskRepository.save(existingTask));
        });
    }

    public boolean deleteTask(Long id) {
        Optional<Task> existingTask = taskRepository.findById(id);

        if (existingTask.isEmpty()) {
            return false;
        }

        checkTaskAccess(existingTask.get());
        taskRepository.deleteById(id);
        return true;
    }

    private Project resolveProject(Long projectId) {
        if (projectId == null) {
            return null;
        }

        return projectRepository.findById(projectId)
                .orElseThrow(() -> new IllegalArgumentException("Project not found: " + projectId));
    }

    private Set<User> resolveAssignees(Set<Long> assigneeIds) {
        if (assigneeIds == null || assigneeIds.isEmpty()) {
            return new HashSet<>();
        }

        return new HashSet<>(userRepository.findAllById(assigneeIds));
    }

    private void checkTaskAccess(Task task) {
        checkProjectAccess(task.getProject());
    }

    private void checkProjectAccess(Project project) {
        if (project == null) {
            if (!isCurrentUserAdmin()) {
                throw new AccessDeniedException("You are not authorized to access a task with no project");
            }
            return;
        }

        teamService.checkMembership(project.getTeam());
    }

    private boolean isCurrentUserAdmin() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN"));
    }

    private User getCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email);
    }
}

