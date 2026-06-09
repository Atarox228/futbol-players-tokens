package com.desapp.futbolplayerstokens.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private UserDetailsService userDetailsService;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    @InjectMocks
    private JwtAuthenticationFilter filter;

    @Test
    void doFilter_noToken_continuesChain() throws Exception {
        when(request.getHeader("Authorization")).thenReturn(null);
        when(request.getCookies()).thenReturn(null);

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verifyNoInteractions(jwtUtil);
        verifyNoInteractions(userDetailsService);
    }

    @Test
    void doFilter_emptyCookies_continuesChain() throws Exception {
        when(request.getHeader("Authorization")).thenReturn(null);
        when(request.getCookies()).thenReturn(new Cookie[]{});

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilter_validBearerToken_authenticates() throws Exception {
        when(request.getHeader("Authorization")).thenReturn("Bearer valid-token");
        when(jwtUtil.validateToken("valid-token")).thenReturn(true);
        when(jwtUtil.extractUsername("valid-token")).thenReturn("testuser");

        UserDetails userDetails = org.springframework.security.core.userdetails.User.builder()
                .username("testuser")
                .password("pass")
                .roles("USER")
                .build();
        when(userDetailsService.loadUserByUsername("testuser")).thenReturn(userDetails);

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verify(jwtUtil).validateToken("valid-token");
        verify(userDetailsService).loadUserByUsername("testuser");
    }

    @Test
    void doFilter_invalidBearerToken_skipsAuth() throws Exception {
        when(request.getHeader("Authorization")).thenReturn("Bearer invalid-token");
        when(jwtUtil.validateToken("invalid-token")).thenReturn(false);

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verify(userDetailsService, never()).loadUserByUsername(any());
    }

    @Test
    void doFilter_validCookieToken_authenticates() throws Exception {
        when(request.getHeader("Authorization")).thenReturn(null);
        Cookie cookie = new Cookie("authToken", "cookie-token");
        when(request.getCookies()).thenReturn(new Cookie[]{cookie});
        when(jwtUtil.validateToken("cookie-token")).thenReturn(true);
        when(jwtUtil.extractUsername("cookie-token")).thenReturn("cookieuser");

        UserDetails userDetails = org.springframework.security.core.userdetails.User.builder()
                .username("cookieuser")
                .password("pass")
                .roles("USER")
                .build();
        when(userDetailsService.loadUserByUsername("cookieuser")).thenReturn(userDetails);

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verify(jwtUtil).validateToken("cookie-token");
        verify(userDetailsService).loadUserByUsername("cookieuser");
    }

    @Test
    void doFilter_cookieIgnoredWhenBearerPresent() throws Exception {
        when(request.getHeader("Authorization")).thenReturn("Bearer bearer-token");
        when(jwtUtil.validateToken("bearer-token")).thenReturn(true);
        when(jwtUtil.extractUsername("bearer-token")).thenReturn("beareruser");

        UserDetails userDetails = org.springframework.security.core.userdetails.User.builder()
                .username("beareruser")
                .password("pass")
                .roles("USER")
                .build();
        when(userDetailsService.loadUserByUsername("beareruser")).thenReturn(userDetails);

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verify(jwtUtil).validateToken("bearer-token");
        verify(jwtUtil, never()).validateToken("cookie-token");
    }

    @Test
    void doFilter_userNotFound_continuesWithoutAuth() throws Exception {
        when(request.getHeader("Authorization")).thenReturn("Bearer valid-token");
        when(jwtUtil.validateToken("valid-token")).thenReturn(true);
        when(jwtUtil.extractUsername("valid-token")).thenReturn("nonexistent");
        when(userDetailsService.loadUserByUsername("nonexistent"))
                .thenThrow(new RuntimeException("User not found"));

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilter_wrongCookieName_ignored() throws Exception {
        when(request.getHeader("Authorization")).thenReturn(null);
        Cookie wrong = new Cookie("otherCookie", "some-value");
        when(request.getCookies()).thenReturn(new Cookie[]{wrong});

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verifyNoInteractions(jwtUtil);
    }

    @Test
    void doFilter_authorizationHeaderWithoutBearer_ignored() throws Exception {
        when(request.getHeader("Authorization")).thenReturn("Basic somecreds");
        when(request.getCookies()).thenReturn(null);

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verifyNoInteractions(jwtUtil);
    }
}
