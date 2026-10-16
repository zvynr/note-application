package com.example.note.service.impl;

import com.example.note.model.dto.NoteDto;
import com.example.note.model.entity.Note;
import com.example.note.model.entity.User;
import com.example.note.exception.ResourceNotFoundException;
import com.example.note.repository.NoteRepository;
import com.example.note.repository.UserRepository;
import com.example.note.service.NoteService;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class NoteServiceImpl implements NoteService {

    private final ModelMapper modelMapper;
    private final NoteRepository noteRepository;
    private final UserRepository userRepository;

    @CacheEvict(value = "notesByUser", key = "#idUser")
    @Override
    public NoteDto createNote(NoteDto noteDto, Long idUser) throws ResourceNotFoundException {
        Note note = modelMapper.map(noteDto, Note.class);
        User user = userRepository.findById(idUser)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", idUser));
        note.setUser(user);
        noteRepository.save(note);
        return modelMapper.map(note, NoteDto.class);
    }

    @Cacheable(value = "notesByUser", key = "#idUser")
    @Override
    public List<NoteDto> getAllNotes(Long idUser) {
        List<Note> notes = noteRepository.findByUserId(idUser);
        return notes.stream()
                .map(note -> modelMapper.map(note, NoteDto.class))
                .collect(Collectors.toList());
    }

    @Override
    public NoteDto getNoteById(Long idNote) throws ResourceNotFoundException {
        Note note = noteRepository.findById(idNote)
                .orElseThrow(() -> new ResourceNotFoundException("Note", "id", idNote));
        return modelMapper.map(note, NoteDto.class);
    }

    @CacheEvict(value = "notesByUser", allEntries = true)
    @Override
    public NoteDto updateNote(NoteDto noteDto, Long idNote) throws ResourceNotFoundException {
        Note note = noteRepository.findById(idNote)
                .orElseThrow(() -> new ResourceNotFoundException("Note", "id", idNote));

        if (noteDto.getTitle() != null) {
            note.setTitle(noteDto.getTitle());
        }
        if (noteDto.getContent() != null) {
            note.setContent(noteDto.getContent());
        }

        noteRepository.save(note);
        return modelMapper.map(note, NoteDto.class);
    }

    @CacheEvict(value = "notesByUser", allEntries = true)
    @Override
    public void deleteNote(Long idNote) throws ResourceNotFoundException {
        Note note = noteRepository.findById(idNote)
                .orElseThrow(() -> new ResourceNotFoundException("Note", "id", idNote));
        noteRepository.delete(note);
    }

    @Override
    public Map<String, Object> getAllNotesPaginated(
            Long idUser,
            String title,
            int pageNumber,
            int pageSize,
            String sortBy,
            String sortDirection,
            Date startDate,
            Date endDate,
            int daysBefore
    ) {
        Objects.requireNonNull(idUser, "ID User cannot be null");

        if (daysBefore > 0) {
            endDate = new Date();
            Calendar cal = Calendar.getInstance();
            cal.setTime(endDate);
            cal.add(Calendar.DAY_OF_MONTH, -daysBefore);
            startDate = cal.getTime();
        }

        Sort.Direction direction = "asc".equalsIgnoreCase(sortDirection)
                ? Sort.Direction.ASC
                : Sort.Direction.DESC;

        String sortField = (sortBy == null || sortBy.isEmpty()) ? "createdAt" : sortBy;
        Pageable pageable = PageRequest.of(pageNumber, pageSize, Sort.by(direction, sortField));

        Page<Note> notePage = noteRepository.findByUserIdWithFilters(
                idUser,
                (title == null || title.isEmpty()) ? null : title,
                startDate,
                endDate,
                pageable
        );

        List<NoteDto> notes = notePage.getContent().stream()
                .map(n -> modelMapper.map(n, NoteDto.class))
                .collect(Collectors.toList());

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("Notes", notes);
        response.put("current_page", notePage.getNumber());
        response.put("total_items", notePage.getTotalElements());
        response.put("total_page", notePage.getTotalPages());
        return response;
    }

    @Override
    public List<NoteDto> getAllNotesAdmin() {
        return noteRepository.findAll().stream()
                .map(n -> modelMapper.map(n, NoteDto.class))
                .collect(Collectors.toList());
    }
}
