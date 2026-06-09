package com.desapp.futbolplayerstokens.service;

import com.desapp.futbolplayerstokens.exception.ResourceNotFoundException;
import com.desapp.futbolplayerstokens.exception.ValidationException;
import com.desapp.futbolplayerstokens.modelo.User;
import com.desapp.futbolplayerstokens.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .username("testuser")
                .password("encoded-pass")
                .email("test@example.com")
                .role(User.Role.USER)
                .balance(new BigDecimal("1000"))
                .build();
    }

    @Test
    void registerUser_createsUserSuccessfully() {
        when(userRepository.findByUsername("newuser")).thenReturn(Optional.empty());
        when(userRepository.existsByEmail("new@example.com")).thenReturn(false);
        when(passwordEncoder.encode("rawpass")).thenReturn("encoded-pass");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User u = invocation.getArgument(0);
            u.setId(2L);
            return u;
        });

        User result = userService.registerUser("newuser", "rawpass", "new@example.com");

        assertNotNull(result);
        assertEquals("newuser", result.getUsername());
        assertEquals("encoded-pass", result.getPassword());
        assertEquals("new@example.com", result.getEmail());
        assertEquals(User.Role.USER, result.getRole());
        assertEquals(new BigDecimal("1000"), result.getBalance());
        verify(userRepository).save(any(User.class));
    }

    @Test
    void registerUser_duplicateUsername_throwsException() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));

        assertThrows(ValidationException.class,
                () -> userService.registerUser("testuser", "pass", "other@example.com"));
        verify(userRepository, never()).save(any());
    }

    @Test
    void registerUser_duplicateEmail_throwsException() {
        when(userRepository.findByUsername("newuser")).thenReturn(Optional.empty());
        when(userRepository.existsByEmail("test@example.com")).thenReturn(true);

        assertThrows(ValidationException.class,
                () -> userService.registerUser("newuser", "pass", "test@example.com"));
        verify(userRepository, never()).save(any());
    }

    @Test
    void findByUsername_userExists() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));

        Optional<User> result = userService.findByUsername("testuser");

        assertTrue(result.isPresent());
        assertEquals("testuser", result.get().getUsername());
    }

    @Test
    void findByUsername_userNotFound() {
        when(userRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());

        Optional<User> result = userService.findByUsername("nonexistent");

        assertTrue(result.isEmpty());
    }

    @Test
    void findById_userExists() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        User result = userService.findById(1L);

        assertNotNull(result);
        assertEquals(1L, result.getId());
    }

    @Test
    void findById_userNotFound_throwsException() {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> userService.findById(999L));
    }

    @Test
    void registerUser_encodesPassword() {
        when(userRepository.findByUsername("newuser")).thenReturn(Optional.empty());
        when(userRepository.existsByEmail("new@example.com")).thenReturn(false);
        when(passwordEncoder.encode("rawpass")).thenReturn("strong-encoded-pass");
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        userService.registerUser("newuser", "rawpass", "new@example.com");

        verify(passwordEncoder).encode("rawpass");
    }
}
