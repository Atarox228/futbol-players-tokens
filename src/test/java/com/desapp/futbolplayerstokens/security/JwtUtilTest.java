package com.desapp.futbolplayerstokens.security;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class JwtUtilTest {

    private final JwtUtil jwtUtil = new JwtUtil();

    @Test
    void generateToken_returnsNonNullToken() {
        String token = jwtUtil.generateToken("testuser");
        assertNotNull(token);
        assertFalse(token.isEmpty());
    }

    @Test
    void generateAndValidateToken_valid() {
        String token = jwtUtil.generateToken("testuser");
        assertTrue(jwtUtil.validateToken(token));
    }

    @Test
    void extractUsername_fromGeneratedToken() {
        String token = jwtUtil.generateToken("testuser");
        assertEquals("testuser", jwtUtil.extractUsername(token));
    }

    @Test
    void extractUsername_differentUsernames() {
        String token1 = jwtUtil.generateToken("user1");
        String token2 = jwtUtil.generateToken("user2");
        assertEquals("user1", jwtUtil.extractUsername(token1));
        assertEquals("user2", jwtUtil.extractUsername(token2));
    }

    @Test
    void validateToken_invalidToken_returnsFalse() {
        assertFalse(jwtUtil.validateToken("invalid.jwt.token"));
    }

    @Test
    void validateToken_malformedToken_returnsFalse() {
        assertFalse(jwtUtil.validateToken("not-a-jwt"));
    }

    @Test
    void validateToken_emptyToken_returnsFalse() {
        assertFalse(jwtUtil.validateToken(""));
    }

    @Test
    void validateToken_nullToken_returnsFalse() {
        assertFalse(jwtUtil.validateToken(null));
    }

    @Test
    void generateToken_differentUsers_differentTokens() {
        String token1 = jwtUtil.generateToken("user1");
        String token2 = jwtUtil.generateToken("user2");
        assertNotEquals(token1, token2);
    }

    @Test
    void extractUsername_afterValidation() {
        String token = jwtUtil.generateToken("testuser");
        assertTrue(jwtUtil.validateToken(token));
        assertEquals("testuser", jwtUtil.extractUsername(token));
    }
}
