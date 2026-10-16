package com.example.note.repository;

import com.example.note.model.entity.Note;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;

@Repository
public interface NoteRepository extends JpaRepository<Note, Long> {

    List<Note> findByUserId(Long userId);

    Page<Note> findByUserId(Long userId, Pageable pageable);

    @Query("SELECT n FROM Note n WHERE n.user.id = :userId " +
           "AND (:title IS NULL OR LOWER(n.title) LIKE LOWER(CONCAT('%', :title, '%'))) " +
           "AND (:startDate IS NULL OR n.createdAt >= :startDate) " +
           "AND (:endDate IS NULL OR n.createdAt <= :endDate)")
    Page<Note> findByUserIdWithFilters(
            @Param("userId") Long userId,
            @Param("title") String title,
            @Param("startDate") Date startDate,
            @Param("endDate") Date endDate,
            Pageable pageable
    );
}
