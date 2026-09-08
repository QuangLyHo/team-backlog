package com.example.database_normalization.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.database_normalization.entity.Task;
import com.example.database_normalization.entity.TaskStatus;
import com.example.database_normalization.entity.User;

@Repository
public interface TaskRepository extends JpaRepository<Task, Long> {

    // @Query("SELECT DISTINCT t FROM Task t LEFT JOIN FETCH t.assignees")
    // List<Task> findAllWithAssignees();

    @Query("SELECT DISTINCT t FROM Task t LEFT JOIN FETCH t.assignees WHERE t.project.id = :projectId")
    List<Task> findByProjectIdWithAssignees(@Param("projectId") Long projectId);

    Page<Task> findByStatus(TaskStatus status, Pageable pageable);

    Page<Task> findByProject_Team_MembersContaining(User user, Pageable pageable);

    Page<Task> findByProject_Team_MembersContainingAndStatus(User user, TaskStatus status, Pageable pageable);

}
