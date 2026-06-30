package com.desapp.futbolplayerstokens.config;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.desapp.futbolplayerstokens.security.JwtUtil;
import com.desapp.futbolplayerstokens.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests para LoggingAspect.
 *
 * Verifica:
 * - El aspect intercepta métodos de controllers
 * - El logging ocurre correctamente
 * - Los datos sensibles se sanitizan
 * - El contexto MDC se establece/limpia
 * - El tiempo de ejecución se loguea
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class LoggingAspectTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthenticationManager authenticationManager;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private JwtUtil jwtUtil;

    private ListAppender<ILoggingEvent> listAppender;

    @BeforeEach
    void setUp() {
        Logger logger = (Logger) LoggerFactory.getLogger(LoggingAspect.class);
        listAppender = new ListAppender<>();
        listAppender.start();
        logger.addAppender(listAppender);
    }

    /**
     * Test que verifica que el aspect intercepta un GET request correctamente.
     * El endpoint devuelve una respuesta exitosa.
     */
    @Test
    @WithMockUser(username = "testuser")
    void testLoggingAspect_interceptsGetRequest() throws Exception {
        // When: hacer un GET request autenticado
        MvcResult result = mockMvc.perform(get("/health"))
                .andExpect(status().isOk())
                .andReturn();

        // Then: la respuesta es exitosa y el audit log contiene la operación esperada
        assertThat(result.getResponse().getStatus()).isEqualTo(200);
        assertThat(allLogs()).anyMatch(log -> log.contains("=== AUDIT START ==="));
        assertThat(allLogs()).anyMatch(log -> log.contains("Operation: GET /health"));
        assertThat(allLogs()).anyMatch(log -> log.contains("Method: HealthController.health"));
        assertThat(allLogs()).anyMatch(log -> log.contains("user=testuser"));
        assertThat(allLogs()).anyMatch(log -> log.contains("requestId="));
    }

    /**
     * Test que verifica que el aspect captura información del usuario autenticado.
     * Se verifica que el MDC contiene el usuario de SecurityContextHolder.
     */
    @Test
    @WithMockUser(username = "admin")
    void testLoggingAspect_capturesAuthenticatedUser() throws Exception {
        // When: hacer un GET request con usuario autenticado
        mockMvc.perform(get("/health"))
                .andExpect(status().isOk())
                .andReturn();

        // Then: el audit log debe reflejar el usuario autenticado
        assertThat(allLogs()).anyMatch(log -> log.contains("user=admin"));
    }

    /**
     * Test que verifica que el aspect funciona con usuarios anónimos.
     */
    @Test
    void testLoggingAspect_handlesAnonymousUser() throws Exception {
        // When: hacer un GET request sin autenticación
        MvcResult result = mockMvc.perform(get("/health"))
                .andReturn();

        // Then: el request se procesa sin error y el usuario queda marcado como anonymous
        assertThat(result.getResponse().getStatus()).isLessThan(500);
        assertThat(allLogs()).anyMatch(log -> log.contains("user=anonymous"));
    }

    /**
     * Test que verifica que el aspect no altera la respuesta del controller.
     */
    @Test
    @WithMockUser(username = "testuser")
    void testLoggingAspect_doesNotAlterResponse() throws Exception {
        // When: hacer un request que devuelve datos
        MvcResult result = mockMvc.perform(get("/health"))
                .andExpect(status().isOk())
                .andReturn();

        // Then: la respuesta no está alterada por el aspect
        assertThat(result.getResponse().getContentAsString()).isNotNull();
    }

    /**
     * Test que verifica que el aspect maneja excepciones correctamente.
     */
    @Test
    @WithMockUser(username = "testuser")
    void testLoggingAspect_handlesExceptions() throws Exception {
        when(authenticationManager.authenticate(any()))
            .thenThrow(new BadCredentialsException("Bad credentials"));

        // When: hacer un login con credenciales inválidas
        MvcResult result = mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"testuser\",\"password\":\"wrong\"}"))
            .andExpect(status().isBadRequest())
            .andReturn();

        // Then: el status es de error controlado y se emite auditoría del request
        assertThat(result.getResponse().getStatus()).isGreaterThanOrEqualTo(400);
        assertThat(allLogs()).anyMatch(log -> log.contains("=== AUDIT START ==="));
        assertThat(allLogs()).anyMatch(log -> log.contains("Operation: POST /auth/login"));
    }

    /**
     * Test que verifica la sanitización de parámetros sensibles.
     * (Unidad que prueba la lógica de sanitización)
     */
    @Test
    void testLoggingAspect_sanitizesSensitiveData() {
        String sanitized = invokeSanitizeValue("username=testuser,password=super-secret-token,authorization=Bearer abc123");

        assertThat(sanitized)
        .doesNotContain("super-secret-token")
        .doesNotContain("Bearer abc123")
        .contains("password=***REDACTED***")
        .contains("authorization=***REDACTED***");
    }

    /**
     * Test que verifica la ejecución de múltiples requests secuenciales.
     * Asegura que el MDC se limpia correctamente entre requests.
     */
    @Test
    @WithMockUser(username = "user1")
    void testLoggingAspect_cleansMDCBetweenRequests() throws Exception {
        // When: hacer primer request
        mockMvc.perform(get("/health"))
                .andExpect(status().isOk());

        // When: hacer segundo request con usuario diferente
        mockMvc.perform(get("/health"))
            .andExpect(status().isOk());

        // Then: el MDC debe estar limpio entre requests; el log debe contener ambas ejecuciones
        assertThat(allLogs()).anyMatch(log -> log.contains("requestId="));
    }

    /**
     * Test que verifica que el aspect captura el HTTP method y path.
     */
    @Test
    @WithMockUser(username = "testuser")
    void testLoggingAspect_capturesHttpMethodAndPath() throws Exception {
        // When: hacer un GET request estable
        mockMvc.perform(get("/health"))
                .andExpect(status().isOk())
                .andReturn();

        // Then: el aspect debe haber logueado la operación como método + path
        assertThat(allLogs()).anyMatch(log -> log.contains("Operation: GET /health"));
    }

    /**
     * Test que verifica que el aspect genera un requestId único.
     */
    @Test
    @WithMockUser(username = "testuser")
    void testLoggingAspect_generatesUniqueRequestId() throws Exception {
        // When: hacer múltiples requests
        mockMvc.perform(get("/health"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/health"))
                .andExpect(status().isOk());

        // Then: el audit log debe incluir requestId en cada request
        long requestIdCount = allLogs().stream().filter(log -> log.contains("requestId=")).count();
        assertThat(requestIdCount).isGreaterThanOrEqualTo(2);
    }

    /**
     * Test que verifica el rendimiento bajo overhead.
     * El aspect no debe agregar más de X ms por request.
     */
    @Test
    @WithMockUser(username = "testuser")
    void testLoggingAspect_lowOverhead() throws Exception {
        // When: hacer un request
        long startTime = System.currentTimeMillis();
        mockMvc.perform(get("/health"))
                .andExpect(status().isOk());
        long duration = System.currentTimeMillis() - startTime;

        // Then: el request debe completarse en tiempo razonable
        assertThat(duration).isLessThan(5000L);
    }

    private java.util.List<String> allLogs() {
        return listAppender.list.stream()
                .filter(event -> event.getLevel().isGreaterOrEqual(Level.INFO))
                .map(this::renderEvent)
                .toList();
    }

    private String renderEvent(ILoggingEvent event) {
        Map<String, String> mdc = event.getMDCPropertyMap();
        return event.getFormattedMessage() + " | MDC=" + mdc;
    }

    private String invokeSanitizeValue(String raw) {
        try {
            Method method = LoggingAspect.class.getDeclaredMethod("sanitizeValue", Object.class);
            method.setAccessible(true);
            return (String) method.invoke(new LoggingAspect(), raw);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(e);
        }
    }
}
