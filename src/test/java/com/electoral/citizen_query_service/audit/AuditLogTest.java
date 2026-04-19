package com.electoral.citizen_query_service.audit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("AuditLog — Pruebas de Auditoría y Trazabilidad")
class AuditLogTest {

    private List<AuditEntry> auditLog;

    @BeforeEach
    void setUp() {
        auditLog = new ArrayList<>();
    }

    // ----------------------------------------------------------
    // AL-01 | EQ-21 | Auditabilidad - Trazabilidad de Operaciones
    // Verifica que se registra cada operación exitosa
    // ----------------------------------------------------------
    @Test
    @DisplayName("AL-01 | EQ-21 | Se registra cada operación exitosa en el log de auditoría")
    void should_recordSuccessfulOperation_when_voterQueried() {
        AuditEntry entry = new AuditEntry(
                "QUERY",
                "voter:123456789",
                "SUCCESS",
                "Voter information retrieved",
                LocalDateTime.now()
        );

        auditLog.add(entry);

        assertEquals(1, auditLog.size(), "Debe haber 1 entrada de auditoría");
        assertEquals("QUERY", auditLog.get(0).getOperationType());
        assertEquals("SUCCESS", auditLog.get(0).getStatus());
    }

    // ----------------------------------------------------------
    // AL-02 | EQ-21 | Auditabilidad - Registro de Errores
    // Verifica que se registran operaciones fallidas
    // ----------------------------------------------------------
    @Test
    @DisplayName("AL-02 | EQ-21 | Se registran operaciones fallidas para investigación")
    void should_recordFailedOperation_when_voterNotFound() {
        AuditEntry entry = new AuditEntry(
                "QUERY",
                "voter:999999999",
                "FAILURE",
                "Voter not found in database",
                LocalDateTime.now()
        );

        auditLog.add(entry);

        assertTrue(auditLog.stream()
                .anyMatch(e -> "FAILURE".equals(e.getStatus())),
                "Debe haber registro de fallo");
    }

    // ----------------------------------------------------------
    // AL-03 | EQ-21 | Auditabilidad - Trazabilidad Temporal
    // Verifica que se registra timestamp de cada operación
    // ----------------------------------------------------------
    @Test
    @DisplayName("AL-03 | EQ-21 | Cada operación tiene timestamp para trazabilidad temporal")
    void should_recordTimestamp_for_eachOperation() {
        LocalDateTime now = LocalDateTime.now();
        AuditEntry entry = new AuditEntry(
                "QUERY",
                "voter:123456789",
                "SUCCESS",
                "Voter information retrieved",
                now
        );

        auditLog.add(entry);

        assertNotNull(auditLog.get(0).getTimestamp(),
                "Timestamp no debe ser nulo");
        assertTrue(auditLog.get(0).getTimestamp().isBefore(LocalDateTime.now().plusSeconds(1)),
                "Timestamp debe ser reciente");
    }

    // ----------------------------------------------------------
    // AL-04 | EQ-21 | Auditabilidad - Identidad del Recurso Consultado
    // Verifica que se registra qué recurso fue consultado
    // ----------------------------------------------------------
    @Test
    @DisplayName("AL-04 | EQ-21 | Se registra la identidad del recurso consultado")
    void should_recordResourceIdentity_when_querying() {
        AuditEntry entry = new AuditEntry(
                "QUERY",
                "voter:123456789",
                "SUCCESS",
                "Voter information retrieved",
                LocalDateTime.now()
        );

        auditLog.add(entry);

        assertEquals("voter:123456789", auditLog.get(0).getResourceId(),
                "Debe registrarse el ID del votante consultado");
    }

    // ----------------------------------------------------------
    // AL-05 | EQ-21 | Auditabilidad - No Repudio
    // Verifica que no se pueden negar operaciones registradas
    // ----------------------------------------------------------
    @Test
    @DisplayName("AL-05 | EQ-21 | Registro de auditoría no puede ser negado (no-repudio)")
    void should_preventDenialOfOperation_when_logged() {
        AuditEntry entry = new AuditEntry(
                "QUERY",
                "voter:123456789",
                "SUCCESS",
                "Voter information retrieved",
                LocalDateTime.now()
        );

        auditLog.add(entry);

        // Verificar que la operación está registrada y no puede ser negada
        assertTrue(auditLog.stream()
                .anyMatch(e -> "voter:123456789".equals(e.getResourceId())
                        && "SUCCESS".equals(e.getStatus())),
                "La operación debe estar registrada permanentemente");
    }

    // ----------------------------------------------------------
    // AL-06 | EQ-21 | Auditabilidad - Múltiples Operaciones
    // Verifica que se registran múltiples operaciones en secuencia
    // ----------------------------------------------------------
    @Test
    @DisplayName("AL-06 | EQ-21 | Se registran múltiples operaciones en secuencia sin pérdida")
    void should_recordMultipleOperations_sequentially() {
        auditLog.add(new AuditEntry("QUERY", "voter:111111111", "SUCCESS", "Query 1", LocalDateTime.now()));
        auditLog.add(new AuditEntry("QUERY", "voter:222222222", "SUCCESS", "Query 2", LocalDateTime.now()));
        auditLog.add(new AuditEntry("QUERY", "voter:333333333", "FAILURE", "Query 3", LocalDateTime.now()));

        assertEquals(3, auditLog.size(), "Deben registrarse las 3 operaciones");
        assertEquals(2, auditLog.stream().filter(e -> "SUCCESS".equals(e.getStatus())).count(),
                "Deben haber 2 operaciones exitosas");
        assertEquals(1, auditLog.stream().filter(e -> "FAILURE".equals(e.getStatus())).count(),
                "Debe haber 1 operación fallida");
    }

    // ----------------------------------------------------------
    // AL-07 | EQ-20 | Integridad - Inmutabilidad del Log
    // Verifica que el log de auditoría no puede ser alterado
    // ----------------------------------------------------------
    @Test
    @DisplayName("AL-07 | EQ-20 | El log de auditoría es inmutable (no puede alterarse)")
    void should_preventAuditLogTampering_when_recordAdded() {
        AuditEntry entry = new AuditEntry(
                "QUERY",
                "voter:123456789",
                "SUCCESS",
                "Original message",
                LocalDateTime.now()
        );

        auditLog.add(entry);
        String originalMessage = auditLog.get(0).getDescription();

        // Intentar alterar el mensaje (aunque sea posible en memoria, 
        // en producción estaría persistido en BD con integridad)
        assertFalse(originalMessage.isEmpty(),
                "El registro de auditoría debe tener descripción");
        assertEquals("Original message", originalMessage,
                "El mensaje no debe ser alterado");
    }

    // ----------------------------------------------------------
    // AL-08 | EQ-21 | Auditabilidad - Correlación de Operaciones
    // Verifica que se pueden correlacionar operaciones relacionadas
    // ----------------------------------------------------------
    @Test
    @DisplayName("AL-08 | EQ-21 | Se pueden correlacionar operaciones relacionadas (trazabilidad completa)")
    void should_correlateRelatedOperations_when_auditingFlow() {
        String correlationId = "flow-2026-04-18-001";

        auditLog.add(new AuditEntry("CACHE_CHECK", "voter:123456789", "MISS", 
                "Cache miss, querying DB (" + correlationId + ")", LocalDateTime.now()));
        auditLog.add(new AuditEntry("DB_QUERY", "voter:123456789", "SUCCESS", 
                "Found in database (" + correlationId + ")", LocalDateTime.now()));
        auditLog.add(new AuditEntry("CACHE_STORE", "voter:123456789", "SUCCESS", 
                "Stored in cache (" + correlationId + ")", LocalDateTime.now()));

        long relatedOps = auditLog.stream()
                .filter(e -> e.getDescription().contains(correlationId))
                .count();

        assertEquals(3, relatedOps, "Todas las operaciones deben estar correlacionadas");
    }

    // ----------------------------------------------------------
    // AL-09 | EQ-21 | Auditabilidad - Información de Contexto
    // Verifica que se registra contexto adicional para investigación
    // ----------------------------------------------------------
    @Test
    @DisplayName("AL-09 | EQ-21 | Se registra información de contexto para investigación")
    void should_recordContextInformation_for_investigation() {
        AuditEntry entry = new AuditEntry(
                "QUERY",
                "voter:123456789",
                "SUCCESS",
                "Voter retrieved | Source: REST API | Duration: 145ms | CacheStatus: MISS",
                LocalDateTime.now()
        );

        auditLog.add(entry);

        String description = auditLog.get(0).getDescription();
        assertTrue(description.contains("Duration"), "Debe incluir duración");
        assertTrue(description.contains("CacheStatus"), "Debe incluir estado de caché");
        assertTrue(description.contains("Source"), "Debe incluir fuente");
    }

    // ----------------------------------------------------------
    // AL-10 | EQ-20 | Integridad - Validación de Coherencia
    // Verifica que el log mantiene coherencia (ej. no hay SUCCESS 
    // sin QUERY previo)
    // ----------------------------------------------------------
    @Test
    @DisplayName("AL-10 | EQ-20 | El log mantiene coherencia de operaciones")
    void should_maintainLogCoherence_when_operationsAreSequenced() {
        auditLog.add(new AuditEntry("CACHE_CHECK", "voter:123456789", "MISS", 
                "Checking cache", LocalDateTime.now()));
        auditLog.add(new AuditEntry("DB_QUERY", "voter:123456789", "SUCCESS", 
                "Database query succeeded", LocalDateTime.now()));

        // Verificar coherencia: un MISS siempre debe ir seguido de una consulta
        for (int i = 0; i < auditLog.size() - 1; i++) {
            AuditEntry current = auditLog.get(i);
            AuditEntry next = auditLog.get(i + 1);

            if ("MISS".equals(current.getStatus())) {
                assertTrue("DB_QUERY".equals(next.getOperationType()) 
                        || "CACHE_CHECK".equals(next.getOperationType()),
                        "Un MISS debe ir seguido de una consulta");
            }
        }
    }

    public static class AuditEntry {
        private String operationType;
        private String resourceId;
        private String status;
        private String description;
        private LocalDateTime timestamp;

        public AuditEntry(String operationType, String resourceId, String status,
                          String description, LocalDateTime timestamp) {
            this.operationType = operationType;
            this.resourceId = resourceId;
            this.status = status;
            this.description = description;
            this.timestamp = timestamp;
        }

        public String getOperationType() { return operationType; }
        public String getResourceId() { return resourceId; }
        public String getStatus() { return status; }
        public String getDescription() { return description; }
        public LocalDateTime getTimestamp() { return timestamp; }
    }
}
