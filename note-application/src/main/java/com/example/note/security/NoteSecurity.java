package com.example.note.security;

import com.example.note.model.entity.Note;
import com.example.note.model.entity.User;
import com.example.note.repository.NoteRepository;
import com.example.note.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * Helper for SpEL @PreAuthorize ownership checks.
 * Bean name: noteSecurity
 */
@Component("noteSecurity")
@RequiredArgsConstructor
public class NoteSecurity {

    private final UserRepository userRepository;
    private final NoteRepository noteRepository;

    /** True if the given userId belongs to the currently authenticated user. */
    public boolean isOwner(Long userId) {
        User current = getCurrentUser();
        return current != null && current.getId().equals(userId);
    }

    /** Alias for isOwner — used by UserController. */
    public boolean isSelf(Long userId) {
        return isOwner(userId);
    }

    /** True if the note with given id belongs to the currently authenticated user. */
    public boolean isNoteOwner(Long noteId) {
        User current = getCurrentUser();
        if (current == null) {
            return false;
        }
        return noteRepository.findById(noteId)
                .map(Note::getUser)
                .map(User::getId)
                .map(id -> id.equals(current.getId()))
                .orElse(false);
    }

    private User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return null;
        }
        return userRepository.findByUsername(auth.getName()).orElse(null);
    }
}
