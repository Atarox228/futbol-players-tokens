package com.desapp.futbolplayerstokens.exception;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class ExceptionTest {

    @Test
    void insufficientBalanceException_message() {
        InsufficientBalanceException ex = new InsufficientBalanceException(
                new BigDecimal("150"), new BigDecimal("100"));
        assertTrue(ex.getMessage().contains("150"));
        assertTrue(ex.getMessage().contains("100"));
        assertTrue(ex.getMessage().contains("Insufficient balance"));
    }

    @Test
    void insufficientBalanceException_httpStatus() {
        InsufficientBalanceException ex = new InsufficientBalanceException(
                new BigDecimal("50"), new BigDecimal("30"));
        assertEquals(HttpStatus.CONFLICT, ex.getHttpStatus());
    }

    @Test
    void insufficientTokensException_message() {
        InsufficientTokensException ex = new InsufficientTokensException(10, 5);
        assertTrue(ex.getMessage().contains("10"));
        assertTrue(ex.getMessage().contains("5"));
    }

    @Test
    void insufficientTokensException_httpStatus() {
        InsufficientTokensException ex = new InsufficientTokensException(3, 1);
        assertEquals(HttpStatus.CONFLICT, ex.getHttpStatus());
    }

    @Test
    void insufficientStockException_message() {
        InsufficientStockException ex = new InsufficientStockException("Messi", 10, 5);
        assertTrue(ex.getMessage().contains("Messi"));
        assertTrue(ex.getMessage().contains("10"));
        assertTrue(ex.getMessage().contains("5"));
    }

    @Test
    void insufficientStockException_httpStatus() {
        InsufficientStockException ex = new InsufficientStockException("Ronaldo", 5, 2);
        assertEquals(HttpStatus.CONFLICT, ex.getHttpStatus());
    }

    @Test
    void resourceNotFoundException_message() {
        ResourceNotFoundException ex = new ResourceNotFoundException("Player not found");
        assertEquals("Player not found", ex.getMessage());
    }

    @Test
    void resourceNotFoundException_httpStatus() {
        ResourceNotFoundException ex = new ResourceNotFoundException("test");
        assertEquals(HttpStatus.NOT_FOUND, ex.getHttpStatus());
    }

    @Test
    void resourceNotFoundException_cause() {
        Throwable cause = new RuntimeException("db error");
        ResourceNotFoundException ex = new ResourceNotFoundException("not found", cause);
        assertSame(cause, ex.getCause());
    }

    @Test
    void validationException_message() {
        ValidationException ex = new ValidationException("Invalid input");
        assertEquals("Invalid input", ex.getMessage());
    }

    @Test
    void validationException_httpStatus() {
        ValidationException ex = new ValidationException("bad");
        assertEquals(HttpStatus.BAD_REQUEST, ex.getHttpStatus());
    }

    @Test
    void validationException_cause() {
        Throwable cause = new IllegalArgumentException("wrong type");
        ValidationException ex = new ValidationException("invalid", cause);
        assertSame(cause, ex.getCause());
    }

    @Test
    void scrapingException_message() {
        ScrapingException ex = new ScrapingException("Scraping failed");
        assertEquals("Scraping failed", ex.getMessage());
    }

    @Test
    void scrapingException_httpStatus() {
        ScrapingException ex = new ScrapingException("error");
        assertEquals(HttpStatus.BAD_REQUEST, ex.getHttpStatus());
    }

    @Test
    void scrapingException_cause() {
        Throwable cause = new RuntimeException("connection timeout");
        ScrapingException ex = new ScrapingException("scrape error", cause);
        assertSame(cause, ex.getCause());
    }

    @Test
    void dataUpdateException_message() {
        DataUpdateException ex = new DataUpdateException("Update failed");
        assertEquals("Update failed", ex.getMessage());
    }

    @Test
    void dataUpdateException_httpStatus() {
        DataUpdateException ex = new DataUpdateException("error");
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, ex.getHttpStatus());
    }

    @Test
    void dataUpdateException_cause() {
        Throwable cause = new RuntimeException("disk full");
        DataUpdateException ex = new DataUpdateException("update error", cause);
        assertSame(cause, ex.getCause());
    }

    @Test
    void configurationException_message() {
        ConfigurationException ex = new ConfigurationException("Config error");
        assertEquals("Config error", ex.getMessage());
    }

    @Test
    void configurationException_httpStatus() {
        ConfigurationException ex = new ConfigurationException("error");
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, ex.getHttpStatus());
    }

    @Test
    void configurationException_cause() {
        Throwable cause = new RuntimeException("missing property");
        ConfigurationException ex = new ConfigurationException("config error", cause);
        assertSame(cause, ex.getCause());
    }
}
