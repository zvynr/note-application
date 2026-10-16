package com.example.note.controller;

import com.example.note.model.dto.NoteDto;
import com.example.note.exception.ResourceNotFoundException;
import com.example.note.service.NoteService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.Date;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notes")
@RequiredArgsConstructor
public class NoteController {

    private final NoteService noteService;

    /**
     * USER: create note only for themselves (userId must match current user).
     * ADMIN: can create note for any user.
     */
    @PreAuthorize("hasRole('ADMIN') or @noteSecurity.isOwner(#userId)")
    @PostMapping("/user/{userId}")
    public ResponseEntity<NoteDto> createNote(
            @Valid @RequestBody NoteDto noteDto,
            @PathVariable Long userId
    ) throws ResourceNotFoundException {
        NoteDto created = noteService.createNote(noteDto, userId);
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    /**
     * USER: get own notes only (userId must match current user).
     * ADMIN: can get notes of any user.
     */
    @PreAuthorize("hasRole('ADMIN') or @noteSecurity.isOwner(#userId)")
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<NoteDto>> getAllNotes(@PathVariable Long userId) {
        return ResponseEntity.ok(noteService.getAllNotes(userId));
    }

    /**
     * USER: paginated own notes only.
     * ADMIN: can paginate any user's notes.
     */
    @PreAuthorize("hasRole('ADMIN') or @noteSecurity.isOwner(#userId)")
    @GetMapping("/user/{userId}/paginated")
    public ResponseEntity<Map<String, Object>> getAllNotesPaginated(
            @PathVariable Long userId,
            @RequestParam(required = false) String title,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDirection,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) Date startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) Date endDate,
            @RequestParam(defaultValue = "0") int daysBefore
    ) {
        Map<String, Object> response = noteService.getAllNotesPaginated(
                userId, title, page, size, sortBy, sortDirection, startDate, endDate, daysBefore
        );
        return ResponseEntity.ok(response);
    }

    /** Admin only: get all notes from all users — must be declared BEFORE /{id} */
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/all")
    public ResponseEntity<List<NoteDto>> getAllNotesAdmin() {
        return ResponseEntity.ok(noteService.getAllNotesAdmin());
    }

    /**
     * USER: get own note only.
     * ADMIN: can get any note.
     */
    @PreAuthorize("hasRole('ADMIN') or @noteSecurity.isNoteOwner(#id)")
    @GetMapping("/{id}")
    public ResponseEntity<NoteDto> getNoteById(@PathVariable Long id) throws ResourceNotFoundException {
        return ResponseEntity.ok(noteService.getNoteById(id));
    }

    /**
     * USER: update own note only.
     * ADMIN: can update any note.
     */
    @PreAuthorize("hasRole('ADMIN') or @noteSecurity.isNoteOwner(#id)")
    @PutMapping("/{id}")
    public ResponseEntity<NoteDto> updateNote(
            @Valid @RequestBody NoteDto noteDto,
            @PathVariable Long id
    ) throws ResourceNotFoundException {
        return ResponseEntity.ok(noteService.updateNote(noteDto, id));
    }

    /**
     * USER: delete own note only.
     * ADMIN: can delete any note.
     */
    @PreAuthorize("hasRole('ADMIN') or @noteSecurity.isNoteOwner(#id)")
    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteNote(@PathVariable Long id) throws ResourceNotFoundException {
        noteService.deleteNote(id);
        return ResponseEntity.ok("Note deleted successfully");
    }
}
