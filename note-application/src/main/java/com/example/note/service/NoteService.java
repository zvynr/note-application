package com.example.note.service;

import com.example.note.model.dto.NoteDto;
import com.example.note.exception.ResourceNotFoundException;

import java.util.Date;
import java.util.List;
import java.util.Map;

public interface NoteService {

    NoteDto createNote(NoteDto noteDto, Long idUser) throws ResourceNotFoundException;

    List<NoteDto> getAllNotes(Long idUser);

    NoteDto getNoteById(Long idNote) throws ResourceNotFoundException;

    NoteDto updateNote(NoteDto noteDto, Long idNote) throws ResourceNotFoundException;

    void deleteNote(Long idNote) throws ResourceNotFoundException;

    Map<String, Object> getAllNotesPaginated(
            Long idUser,
            String title,
            int pageNumber,
            int pageSize,
            String sortBy,
            String sortDirection,
            Date startDate,
            Date endDate,
            int daysBefore
    );

    /** Admin: get all notes from all users */
    List<NoteDto> getAllNotesAdmin();
}
