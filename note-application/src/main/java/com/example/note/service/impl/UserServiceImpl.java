package com.example.note.service.impl;

import com.example.note.model.dto.UserDto;
import com.example.note.model.entity.User;
import com.example.note.exception.ResourceNotFoundException;
import com.example.note.exception.TokenHasExpiredException;
import com.example.note.repository.UserRepository;
import com.example.note.utility.AttemptLimiter;
import com.example.note.service.UserService;
import com.example.note.utility.TokenHashUtil;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import com.example.note.service.NotificationPublisher;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService, UserDetailsService {

    private final ModelMapper modelMapper;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final NotificationPublisher notificationPublisher;
    private final AttemptLimiter attemptLimiter;

    @Value("${app.upload.dir:./uploads/profile-photos}")
    private String uploadDir;

    /** Email verification link lifetime (minutes) */
    @Value("${security.token.verify-minutes:60}")
    private int verifyTokenMinutes;

    /** Password reset link lifetime (minutes) */
    @Value("${security.token.reset-minutes:15}")
    private int resetTokenMinutes;

    private Path getRoot() {
        return Paths.get(uploadDir);
    }

    private void init() {
        try {
            Files.createDirectories(getRoot());
        } catch (IOException e) {
            throw new RuntimeException("Could not initialize profile photo directory");
        }
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with username: " + username));

        if (!user.isEmailVerified()) {
            throw new UsernameNotFoundException(
                    "Email not verified. Please check your inbox and verify your email before login.");
        }

        return new org.springframework.security.core.userdetails.User(
                user.getUsername(),
                user.getPassword(),
                getAuthority(user)
        );
    }

    private java.util.Set<SimpleGrantedAuthority> getAuthority(User user) {
        java.util.Set<SimpleGrantedAuthority> authorities = new java.util.HashSet<>();
        String role = user.getRole() != null ? user.getRole() : "USER";
        authorities.add(new SimpleGrantedAuthority("ROLE_" + role));
        return authorities;
    }

    @Value("${app.public-url:http://localhost:8080}")
    private String publicUrl;

    @Override
    public UserDto createUser(UserDto userDto, HttpServletRequest request) {
        if (userRepository.existsByUsername(userDto.getUsername())) {
            throw new RuntimeException("Username is already taken");
        }
        if (userRepository.existsByEmail(userDto.getEmail())) {
            throw new RuntimeException("Email is already in use");
        }

        User user = modelMapper.map(userDto, User.class);
        user.setPassword(passwordEncoder.encode(userDto.getPassword()));
        if (userDto.getRole() == null || userDto.getRole().trim().isEmpty()) {
            user.setRole("USER");
        } else {
            user.setRole(userDto.getRole().toUpperCase());
        }

        // Must verify email before login — store HASH only in DB
        user.setEmailVerified(false);
        String rawVerifyToken = TokenHashUtil.generateRawToken();
        user.setVerificationToken(TokenHashUtil.hash(rawVerifyToken));
        user.setVerificationTokenExpiry(
                new Date(System.currentTimeMillis() + 1000L * 60 * verifyTokenMinutes));

        userRepository.save(user);

        String base = publicUrl != null && !publicUrl.isEmpty() ? publicUrl : resolveBaseUrl(request);
        String body = "Welcome! Please verify your email by opening this link (valid "
                + verifyTokenMinutes + " minutes):\n"
                + base + "/api/users/verify?token=" + rawVerifyToken;
        notificationPublisher.publishEmail(user.getEmail(), "Verify your email", body);

        return modelMapper.map(user, UserDto.class);
    }

    @Override
    public void verifyEmail(String token) throws IllegalArgumentException, TokenHasExpiredException {
        if (token == null || token.trim().isEmpty()) {
            throw new IllegalArgumentException("Invalid verification link");
        }
        if (!attemptLimiter.tryConsume("verify", token)) {
            throw new RuntimeException(
                    "Too many verification attempts. Try again in "
                            + attemptLimiter.getWindowMinutes() + " minutes.");
        }

        String tokenHash = TokenHashUtil.hash(token);
        User user = userRepository.findByVerificationToken(tokenHash)
                .orElseThrow(() -> new IllegalArgumentException("Invalid verification link"));

        if (user.getVerificationTokenExpiry() != null
                && new Date(System.currentTimeMillis()).after(user.getVerificationTokenExpiry())) {
            throw new TokenHasExpiredException(
                    "Verification link has expired. Please register again or request a new link.");
        }

        user.setEmailVerified(true);
        user.setVerificationToken(null);
        user.setVerificationTokenExpiry(null);
        userRepository.save(user);
        attemptLimiter.reset("verify", token);
    }

    private String resolveBaseUrl(HttpServletRequest request) {
        return request.getScheme() + "://" + request.getServerName()
                + (request.getServerPort() == 80 || request.getServerPort() == 443
                ? "" : ":" + request.getServerPort());
    }

    @Override
    public List<UserDto> getAllUsers() {
        List<User> users = userRepository.findAll();
        return users.stream()
                .map(user -> modelMapper.map(user, UserDto.class))
                .collect(Collectors.toList());
    }

    @Override
    public UserDto getUserById(Long idUser) throws ResourceNotFoundException {
        User user = userRepository.findById(idUser)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", idUser));
        return modelMapper.map(user, UserDto.class);
    }

    @Override
    public UserDto updateUser(UserDto userDto, Long idUser) throws ResourceNotFoundException {
        User user = userRepository.findById(idUser)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", idUser));

        if (userDto.getUsername() != null) {
            user.setUsername(userDto.getUsername());
        }
        if (userDto.getEmail() != null) {
            user.setEmail(userDto.getEmail());
        }
        if (userDto.getPassword() != null && !userDto.getPassword().isEmpty()) {
            user.setPassword(passwordEncoder.encode(userDto.getPassword()));
        }
        // Only ADMIN can change role
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean isAdmin = auth != null && auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        if (isAdmin && userDto.getRole() != null && !userDto.getRole().trim().isEmpty()) {
            user.setRole(userDto.getRole().toUpperCase());
        }

        userRepository.save(user);
        return modelMapper.map(user, UserDto.class);
    }

    @Override
    public void deleteUser(Long idUser) throws ResourceNotFoundException {
        User user = userRepository.findById(idUser)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", idUser));
        userRepository.delete(user);
    }

    @Override
    public void uploadUserProfile(MultipartFile file) throws IOException {
        init();
        try {
            User user = getCurrentUser();
            String filename = user.getId() + "-" + file.getOriginalFilename().toLowerCase().replaceAll(" ", "");
            String path = getRoot().resolve(Paths.get(filename)).toString();
            user.setProfilePath(path);

            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(inputStream, getRoot().resolve(Paths.get(filename)),
                        java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            }
            userRepository.save(user);
        } catch (IOException e) {
            throw new RuntimeException("Failed to upload profile photo: " + e.getMessage());
        }
    }

    @Override
    public byte[] getCurrentUserPhotoProfile() throws IOException, ResourceNotFoundException {
        User user = getCurrentUser();
        if (user.getProfilePath() == null) {
            throw new ResourceNotFoundException("User doesn't have photo profile");
        }
        return Files.readAllBytes(new File(user.getProfilePath()).toPath());
    }

    @Override
    public void deleteUserProfile() throws ResourceNotFoundException {
        try {
            User user = getCurrentUser();
            if (user.getProfilePath() == null) {
                throw new ResourceNotFoundException("User doesn't have photo profile");
            }
            Path filepath = Paths.get(user.getProfilePath());
            if (Files.deleteIfExists(filepath)) {
                user.setProfilePath(null);
                userRepository.save(user);
            }
        } catch (IOException e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    private User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String currentPrincipalName = auth.getName();
        return userRepository.findByUsername(currentPrincipalName)
                .orElseThrow(() -> new UsernameNotFoundException("Current user not found"));
    }

    // ==================== Forgot Password ====================

    @Override
    public void processForgotPasswordRequest(String email, HttpServletRequest request) {
        String limitKey = (email == null ? "unknown" : email.trim().toLowerCase());
        if (!attemptLimiter.tryConsume("forgot", limitKey)) {
            // Still uniform response — do not process further
            return;
        }

        Optional<User> optionalUser = userRepository.findByEmail(email);
        if (!optionalUser.isPresent()) {
            return;
        }

        User user = optionalUser.get();
        String rawResetToken = TokenHashUtil.generateRawToken();
        user.setResetToken(TokenHashUtil.hash(rawResetToken));
        user.setTokenExpiryDate(
                new Date(System.currentTimeMillis() + 1000L * 60 * resetTokenMinutes));
        userRepository.save(user);

        String base = publicUrl != null && !publicUrl.isEmpty() ? publicUrl : resolveBaseUrl(request);
        String body = "To reset your password, open this link (valid "
                + resetTokenMinutes + " minutes):\n"
                + base + "/api/users/reset?token=" + rawResetToken;

        notificationPublisher.publishEmail(email, "Password Reset Request", body);
    }

    @Override
    public void resetTokenValidation(String token) throws IllegalArgumentException, TokenHasExpiredException {
        if (token == null || token.trim().isEmpty()) {
            throw new IllegalArgumentException("The password reset link is invalid");
        }
        if (!attemptLimiter.tryConsume("reset-validate", token)) {
            throw new RuntimeException(
                    "Too many attempts. Try again in " + attemptLimiter.getWindowMinutes() + " minutes.");
        }

        String tokenHash = TokenHashUtil.hash(token);
        User user = userRepository.findByResetToken(tokenHash)
                .orElseThrow(() -> new IllegalArgumentException("The password reset link is invalid"));

        if (user.getTokenExpiryDate() == null
                || new Date(System.currentTimeMillis()).after(user.getTokenExpiryDate())) {
            throw new TokenHasExpiredException("Token reset password has expired");
        }
    }

    @Override
    public void updateNewPassword(String password, String token) {
        if (token == null || token.trim().isEmpty()) {
            throw new IllegalArgumentException("The password reset link is invalid");
        }
        if (!attemptLimiter.tryConsume("reset-update", token)) {
            throw new RuntimeException(
                    "Too many attempts. Try again in " + attemptLimiter.getWindowMinutes() + " minutes.");
        }

        String tokenHash = TokenHashUtil.hash(token);
        User user = userRepository.findByResetToken(tokenHash)
                .orElseThrow(() -> new IllegalArgumentException("The password reset link is invalid"));

        if (user.getTokenExpiryDate() == null
                || new Date(System.currentTimeMillis()).after(user.getTokenExpiryDate())) {
            throw new TokenHasExpiredException("Token reset password has expired");
        }

        user.setPassword(passwordEncoder.encode(password.replace("\"", "")));
        user.setResetToken(null);
        user.setTokenExpiryDate(null);
        userRepository.save(user);
        attemptLimiter.reset("reset-validate", token);
        attemptLimiter.reset("reset-update", token);
        attemptLimiter.reset("forgot", user.getEmail() != null ? user.getEmail().toLowerCase() : "");
    }
}
