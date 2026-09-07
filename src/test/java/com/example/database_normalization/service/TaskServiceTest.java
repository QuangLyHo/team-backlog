package com.example.database_normalization.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import java.util.Optional;
import java.util.Set;

import com.example.database_normalization.dto.TaskRequest;
import com.example.database_normalization.dto.TaskResponse;
import com.example.database_normalization.entity.Project;
import com.example.database_normalization.entity.Task;
import com.example.database_normalization.entity.Team;
import com.example.database_normalization.entity.User;
import com.example.database_normalization.repository.ProjectRepository;
import com.example.database_normalization.repository.TaskRepository;
import com.example.database_normalization.repository.UserRepository;
import com.example.database_normalization.entity.TaskStatus;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
public class TaskServiceTest {

    @Mock
    private TaskRepository taskRepository;

    @Mock
    ProjectRepository projectRepository;

    @Mock
    UserRepository userRepository;

    @Mock
    TeamService teamService;

    @InjectMocks
    private TaskService taskService;

    @BeforeEach
    void authenticateAsAdmin() {
        UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                "admin@example.com", null, List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
        SecurityContextHolder.getContext().setAuthentication(authToken);
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void getAllTasks_delegatesToTeamScopedRepositoryAndReturnsResult() {
        User currentUser = new User();
        currentUser.setEmail("admin@example.com");

        Task task = new Task();
        task.setTitle("Fix login bug");

        PageRequest pageable = PageRequest.of(0, 10);
        Page<Task> taskPage = new PageImpl<>(List.of(task), pageable, 1);

        when(userRepository.findByEmail("admin@example.com")).thenReturn(currentUser);
        when(taskRepository.findByProject_Team_MembersContaining(currentUser, pageable)).thenReturn(taskPage);

        Page<TaskResponse> result = taskService.getAllTasks(pageable, null);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).title()).isEqualTo("Fix login bug");
        verify(taskRepository).findByProject_Team_MembersContaining(currentUser, pageable);
    }

    @Test
    void getAllTasks_withStatusFilter_delegatesToTeamScopedFindByStatus() {
        User currentUser = new User();
        currentUser.setEmail("admin@example.com");

        Task task = new Task();
        task.setTitle("In progress task");
        task.setStatus(TaskStatus.in_progress);

        PageRequest pageable = PageRequest.of(0, 10);
        Page<Task> taskPage = new PageImpl<>(List.of(task), pageable, 1);

        when(userRepository.findByEmail("admin@example.com")).thenReturn(currentUser);
        when(taskRepository.findByProject_Team_MembersContainingAndStatus(currentUser, TaskStatus.in_progress, pageable))
                .thenReturn(taskPage);

        Page<TaskResponse> result = taskService.getAllTasks(pageable, TaskStatus.in_progress);

        assertThat(result.getContent()).hasSize(1);
        verify(taskRepository).findByProject_Team_MembersContainingAndStatus(currentUser, TaskStatus.in_progress, pageable);
        verify(taskRepository, never()).findAll(any(Pageable.class));
    }

    @Test
    void getAllTaskById_whenTaskExists_returnsTasks() {
        Task task = new Task();
        task.setId(1L);
        task.setTitle("New title");

        when(taskRepository.findById(1L)).thenReturn(Optional.of(task));

        Optional<TaskResponse> result = taskService.getTaskById(1L);

        assertThat(result).isPresent();
        assertThat(result.get().title()).isEqualTo("New title");
        verify(taskRepository).findById(1L);
    }

    @Test
    void getTaskById_whenTaskDoesNotExist_returnsEmptyOptional() {
        when(taskRepository.findById(99L)).thenReturn(Optional.empty());

        Optional<TaskResponse> result = taskService.getTaskById(99L);

        assertThat(result).isEmpty();
        verify(taskRepository).findById(99L);
    }

    @Test
    void createTask_savesAndReturnsTask() {
        TaskRequest request = new TaskRequest("New Title", TaskStatus.todo, null, Set.of());

        when(taskRepository.save(any(Task.class))).thenAnswer(invocation -> {
            Task savedTask = invocation.getArgument(0);
            savedTask.setId(1L);
            return savedTask;
        });

        TaskResponse result = taskService.createTask(request);

        assertThat(result.id()).isEqualTo(1L);
        assertThat(result.title()).isEqualTo("New Title");
        verify(taskRepository).save(any(Task.class));
    }
    
    @Test
    void updateTask_whenTaskExists_updatesAndReturnsTask() {
        Task existingTask = new Task();
        existingTask.setId(1L);
        existingTask.setTitle("Old title");
        existingTask.setStatus(TaskStatus.todo);

        TaskRequest request = new TaskRequest("New title", TaskStatus.todo, null, Set.of());

        when(taskRepository.findById(1L)).thenReturn(Optional.of(existingTask));
        when(taskRepository.save(existingTask)).thenReturn(existingTask);

        Optional<TaskResponse> result = taskService.updateTask(1L, request);

        assertThat(result).isPresent();
        assertThat(result.get().title()).isEqualTo("New title");
        assertThat(result.get().status()).isEqualTo(TaskStatus.todo);
        verify(taskRepository).save(existingTask);
    }

    @Test
    void updateTask_whenTaskDoesNotExist_returnsEmptyOptional() {
        TaskRequest request = new TaskRequest("New title", TaskStatus.done, null, Set.of());
        
        when(taskRepository.findById(99L)).thenReturn(Optional.empty());


        Optional<TaskResponse> result = taskService.updateTask(99L, request);

        assertThat(result).isEmpty();
        verify(taskRepository, never()).save(any());
    }

    @Test
    void deleteTask_whenTaskExists_deletesAndReturnsTrue() {
        Task existingTask = new Task();
        existingTask.setId(1L);

        when(taskRepository.findById(1L)).thenReturn(Optional.of(existingTask));

        boolean result = taskService.deleteTask(1L);

        assertThat(result).isTrue();
        verify(taskRepository).deleteById(1L);
    }

    @Test
    void deleteTask_whenTaskDoesNotExist_returnsFalseWithoutDeleting() {
        when(taskRepository.findById(99L)).thenReturn(Optional.empty());

        boolean result = taskService.deleteTask(99L);

        assertThat(result).isFalse();
        verify(taskRepository, never()).deleteById(any());
    }

    @Test
    void updateTask_whenUserIsTeamMemberOfTasksProject_succeeds() {
        Team team = new Team("Platform");
        Project project = new Project();
        project.setId(1L);
        project.setTeam(team);

        Task existingTask = new Task();
        existingTask.setId(1L);
        existingTask.setTitle("Old title");
        existingTask.setProject(project);

        TaskRequest request = new TaskRequest("New title", TaskStatus.todo, 1L, Set.of());

        UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                "member@example.com", null, List.of(new SimpleGrantedAuthority("ROLE_USER")));
        SecurityContextHolder.getContext().setAuthentication(authToken);

        when(taskRepository.findById(1L)).thenReturn(Optional.of(existingTask));
        when(projectRepository.findById(1L)).thenReturn(Optional.of(project));
        when(taskRepository.save(existingTask)).thenReturn(existingTask);

        Optional<TaskResponse> result = taskService.updateTask(1L, request);

        assertThat(result).isPresent();
        assertThat(result.get().title()).isEqualTo("New title");
        verify(teamService, times(2)).checkMembership(team);
    }

    @Test
    void updateTask_whenUserIsNotTeamMemberOfTasksProject_throwsAccessDeniedException() {
        Team team = new Team("Platform");
        Project project = new Project();
        project.setId(1L);
        project.setTeam(team);

        Task existingTask = new Task();
        existingTask.setId(1L);
        existingTask.setProject(project);

        TaskRequest request = new TaskRequest("New title", TaskStatus.todo, null, Set.of());

        UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                "outsider@example.com", null, List.of(new SimpleGrantedAuthority("ROLE_USER")));
        SecurityContextHolder.getContext().setAuthentication(authToken);

        when(taskRepository.findById(1L)).thenReturn(Optional.of(existingTask));
        doThrow(new AccessDeniedException("You are not a member of this team"))
                .when(teamService).checkMembership(team);

        assertThatThrownBy(() -> taskService.updateTask(1L, request))
                .isInstanceOf(AccessDeniedException.class);

        verify(taskRepository, never()).save(any());
    }

    @Test
    void updateTask_whenTaskHasNoProjectAndUserIsNotAdmin_throwsAccessDeniedException() {
        Task existingTask = new Task();
        existingTask.setId(1L);

        TaskRequest request = new TaskRequest("New title", TaskStatus.todo, null, Set.of());

        UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                "someone-else@example.com", null, List.of(new SimpleGrantedAuthority("ROLE_USER")));
        SecurityContextHolder.getContext().setAuthentication(authToken);

        when(taskRepository.findById(1L)).thenReturn(Optional.of(existingTask));

        assertThatThrownBy(() -> taskService.updateTask(1L, request))
                .isInstanceOf(AccessDeniedException.class);

        verify(taskRepository, never()).save(any());
    }

    @Test
    void createTask_withProjectId_associatesProject() {
        Project project = new Project();
        project.setId(1L);
        project.setName("Mobile App v1");
        project.setBudget(new BigDecimal("10000.00"));

        TaskRequest request = new TaskRequest("New Task", TaskStatus.todo, 1L, Set.of());
        
        when(projectRepository.findById(1L)).thenReturn(Optional.of(project));
        when(taskRepository.save(any(Task.class))).thenAnswer(invocation -> {
            Task savedTask = invocation.getArgument(0);
            savedTask.setId(1L);
            
            return savedTask;
        });

        TaskResponse result = taskService.createTask(request);

        assertThat(result.project()).isNotNull();
        assertThat(result.project().id()).isEqualTo(1L);
        assertThat(result.project().name()).isEqualTo("Mobile App v1");
        verify(projectRepository).findById(1L);
    }

    @Test
    void getTasksByProjectId_whenUserIsNotTeamMember_throwsAccessDeniedException() {
        Team team = new Team("Platform");
        Project project = new Project();
        project.setId(1L);
        project.setTeam(team);

        UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                "outsider@example.com", null, List.of(new SimpleGrantedAuthority("ROLE_USER")));
        SecurityContextHolder.getContext().setAuthentication(authToken);

        when(projectRepository.findById(1L)).thenReturn(Optional.of(project));
        doThrow(new AccessDeniedException("You are not a member of this team"))
                .when(teamService).checkMembership(team);

        assertThatThrownBy(() -> taskService.getTasksByProjectId(1L))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void createTask_withNonExistentProjectId_throwsIllegalArgumentException() {
        TaskRequest request = new TaskRequest("New Task", TaskStatus.todo, 99L, Set.of());

        when(projectRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> taskService.createTask(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Project not found");
    }
}
