package com.desapp.futbolplayerstokens.controller;

import com.desapp.futbolplayerstokens.exception.ValidationException;
import com.desapp.futbolplayerstokens.modelo.User;
import com.desapp.futbolplayerstokens.security.JwtUtil;
import com.desapp.futbolplayerstokens.service.UserService;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private UserService userService;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private HttpServletResponse response;

    @InjectMocks
    private AuthController authController;

    @SuppressWarnings("unchecked")
    @Test
    void register_success() {
        AuthController.RegisterRequest req = new AuthController.RegisterRequest();
        req.setUsername("newuser");
        req.setPassword("password");
        req.setEmail("new@example.com");

        when(userService.registerUser("newuser", "password", "new@example.com"))
                .thenReturn(User.builder().id(2L).username("newuser").build());

        ResponseEntity<Map<String, Object>> result = authController.register(req);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals("User registered successfully", result.getBody().get("message"));
    }

    @SuppressWarnings("unchecked")
    @Test
    void register_duplicateUser_returnsBadRequest() {
        AuthController.RegisterRequest req = new AuthController.RegisterRequest();
        req.setUsername("existing");
        req.setPassword("password");
        req.setEmail("existing@example.com");

        when(userService.registerUser("existing", "password", "existing@example.com"))
                .thenThrow(new ValidationException("Username already exists"));

        ResponseEntity<Map<String, Object>> result = authController.register(req);

        assertEquals(HttpStatus.BAD_REQUEST, result.getStatusCode());
        assertEquals("Username already exists", result.getBody().get("error"));
    }

    @SuppressWarnings("unchecked")
    @Test
    void register_duplicateEmail_returnsBadRequest() {
        AuthController.RegisterRequest req = new AuthController.RegisterRequest();
        req.setUsername("newuser");
        req.setPassword("password");
        req.setEmail("used@example.com");

        when(userService.registerUser("newuser", "password", "used@example.com"))
                .thenThrow(new ValidationException("Email already exists"));

        ResponseEntity<Map<String, Object>> result = authController.register(req);

        assertEquals(HttpStatus.BAD_REQUEST, result.getStatusCode());
        assertEquals("Email already exists", result.getBody().get("error"));
    }

    @Test
    void login_success() {
        AuthController.LoginRequest req = new AuthController.LoginRequest();
        req.setUsername("testuser");
        req.setPassword("password");

        Authentication authentication = mock(Authentication.class);
        when(authenticationManager.authenticate(any())).thenReturn(authentication);
        when(jwtUtil.generateToken("testuser")).thenReturn("jwt-token");
        when(userService.findByUsername("testuser"))
                .thenReturn(Optional.of(User.builder().id(1L).username("testuser").build()));

        ResponseEntity<?> result = authController.login(req, response);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertInstanceOf(Map.class, result.getBody());
        Map<?, ?> body = (Map<?, ?>) result.getBody();
        assertEquals("jwt-token", body.get("token"));
        assertEquals("Login successful", body.get("message"));
        assertEquals(1L, body.get("userId"));
        verify(response).addCookie(argThat(c -> c.getName().equals("authToken") && c.getValue().equals("jwt-token")));
    }

    @Test
    void login_invalidCredentials_returnsBadRequest() {
        AuthController.LoginRequest req = new AuthController.LoginRequest();
        req.setUsername("testuser");
        req.setPassword("wrongpass");

        when(authenticationManager.authenticate(any()))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        ResponseEntity<Map<String, Object>> result = authController.login(req, response);

        assertEquals(HttpStatus.BAD_REQUEST, result.getStatusCode());
        assertEquals("Invalid credentials", result.getBody().get("error"));
    }

    @Test
    void logout_setsCookieToNull() {
        ResponseEntity<?> result = authController.logout(response);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertInstanceOf(Map.class, result.getBody());
        assertEquals("Logout successful", ((Map<?, ?>) result.getBody()).get("message"));
        verify(response).addCookie(argThat(c ->
                c.getName().equals("authToken") && c.getValue() == null && c.getMaxAge() == 0));
    }

    @Test
    void register_requestSettersAndGetters() {
        AuthController.RegisterRequest req = new AuthController.RegisterRequest();
        req.setUsername("u");
        req.setPassword("p");
        req.setEmail("e");
        assertEquals("u", req.getUsername());
        assertEquals("p", req.getPassword());
        assertEquals("e", req.getEmail());
    }

    @Test
    void login_requestSettersAndGetters() {
        AuthController.LoginRequest req = new AuthController.LoginRequest();
        req.setUsername("u");
        req.setPassword("p");
        assertEquals("u", req.getUsername());
        assertEquals("p", req.getPassword());
    }
}
