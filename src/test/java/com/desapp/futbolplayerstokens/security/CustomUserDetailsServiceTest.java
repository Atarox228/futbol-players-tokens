package com.desapp.futbolplayerstokens.security;

import com.desapp.futbolplayerstokens.modelo.User;
import com.desapp.futbolplayerstokens.service.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CustomUserDetailsServiceTest {

    @Mock
    private UserService userService;

    @InjectMocks
    private CustomUserDetailsService customUserDetailsService;

    @Test
    void loadUserByUsername_userExists() {
        User user = User.builder()
                .username("testuser")
                .password("encoded-pass")
                .role(User.Role.USER)
                .build();
        when(userService.findByUsername("testuser")).thenReturn(Optional.of(user));

        UserDetails details = customUserDetailsService.loadUserByUsername("testuser");

        assertNotNull(details);
        assertEquals("testuser", details.getUsername());
        assertEquals("encoded-pass", details.getPassword());
        assertTrue(details.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_USER")));
    }

    @Test
    void loadUserByUsername_adminRole() {
        User user = User.builder()
                .username("admin")
                .password("encoded-pass")
                .role(User.Role.ADMIN)
                .build();
        when(userService.findByUsername("admin")).thenReturn(Optional.of(user));

        UserDetails details = customUserDetailsService.loadUserByUsername("admin");

        assertTrue(details.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN")));
    }

    @Test
    void loadUserByUsername_superuserRole() {
        User user = User.builder()
                .username("super")
                .password("encoded-pass")
                .role(User.Role.SUPERUSER)
                .build();
        when(userService.findByUsername("super")).thenReturn(Optional.of(user));

        UserDetails details = customUserDetailsService.loadUserByUsername("super");

        assertTrue(details.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_SUPERUSER")));
    }

    @Test
    void loadUserByUsername_userNotFound_throwsException() {
        when(userService.findByUsername("nonexistent")).thenReturn(Optional.empty());

        assertThrows(UsernameNotFoundException.class,
                () -> customUserDetailsService.loadUserByUsername("nonexistent"));
    }
}
