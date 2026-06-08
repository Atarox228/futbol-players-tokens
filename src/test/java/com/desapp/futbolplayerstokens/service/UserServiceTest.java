package com.desapp.futbolplayerstokens.service;

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

    private User existingUser;

    @BeforeEach
    void setUp() {
        existingUser = User.builder()
                .id(1L)
                .username("existing")
                .email("existing@example.com")
                .password("encoded")
                .balance(new BigDecimal("1000"))
                .build();
    }

    @Test
    void whenRegisterUserWithValidData_thenReturnsSavedUser() {
        when(userRepository.findByUsername("newuser")).thenReturn(Optional.empty());
        when(userRepository.existsByEmail("newuser@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User saved = invocation.getArgument(0);
            saved.setId(2L);
            return saved;
        });

        User created = userService.registerUser("newuser", "password123", "newuser@example.com");

        assertNotNull(created);
        assertEquals(2L, created.getId());
        assertEquals("newuser", created.getUsername());
        assertEquals("newuser@example.com", created.getEmail());
        assertEquals("encodedPassword", created.getPassword());
        assertEquals(User.Role.USER, created.getRole());
        assertEquals(new BigDecimal("1000"), created.getBalance());

        verify(userRepository, times(1)).findByUsername("newuser");
        verify(userRepository, times(1)).existsByEmail("newuser@example.com");
        verify(passwordEncoder, times(1)).encode("password123");
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    void whenRegisterUserAndUsernameExists_thenThrowValidationException() {
        when(userRepository.findByUsername("existing")).thenReturn(Optional.of(existingUser));

        ValidationException exception = assertThrows(ValidationException.class,
                () -> userService.registerUser("existing", "password123", "newemail@example.com"));

        assertEquals("Username already exists", exception.getMessage());
        verify(userRepository, times(1)).findByUsername("existing");
        verify(userRepository, never()).existsByEmail(anyString());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void whenRegisterUserAndEmailExists_thenThrowValidationException() {
        when(userRepository.findByUsername("newuser")).thenReturn(Optional.empty());
        when(userRepository.existsByEmail("existing@example.com")).thenReturn(true);

        ValidationException exception = assertThrows(ValidationException.class,
                () -> userService.registerUser("newuser", "password123", "existing@example.com"));

        assertEquals("Email already exists", exception.getMessage());
        verify(userRepository, times(1)).findByUsername("newuser");
        verify(userRepository, times(1)).existsByEmail("existing@example.com");
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void whenFindByUsername_thenReturnOptionalUser() {
        when(userRepository.findByUsername("existing")).thenReturn(Optional.of(existingUser));

        Optional<User> found = userService.findByUsername("existing");

        assertTrue(found.isPresent());
        assertEquals(existingUser, found.get());
        verify(userRepository, times(1)).findByUsername("existing");
    }
}
