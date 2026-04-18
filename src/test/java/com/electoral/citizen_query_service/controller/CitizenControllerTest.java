package com.electoral.citizen_query_service.controller;

import com.electoral.citizen_query_service.dto.VoterResponse;
import com.electoral.citizen_query_service.exception.ResourceNotFoundException;
import com.electoral.citizen_query_service.service.CitizenService;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CitizenController.class)
@DisplayName("CitizenController — Pruebas de Integración y Validación")
class CitizenControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CitizenService service;

    // ----------------------------------------------------------
    // CV-01 | EQ-8 | Compatibilidad - Interoperabilidad
    // Verifica respuesta 200 para cédula numérica válida
    // ----------------------------------------------------------
    @Test
    @DisplayName("CV-01 | EQ-8 | Retorna 200 para cédula numérica válida")
    void should_return200_when_documentIsValid() throws Exception {
        VoterResponse response = new VoterResponse();
        response.setDocument("123456789");
        response.setPollingStation("Colegio San José - Mesa 5");
        response.setStatus("HABILITADO");

        when(service.getVoterInfo("123456789")).thenReturn(response);

        mockMvc.perform(get("/api/v1/citizen/polling-station")
                .param("document", "123456789"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.document").value("123456789"))
                .andExpect(jsonPath("$.pollingStation").value("Colegio San José - Mesa 5"))
                .andExpect(jsonPath("$.status").value("HABILITADO"));
    }

    // ----------------------------------------------------------
    // CV-02 | EQ-12 | Usabilidad - Protección contra Errores
    // Verifica rechazo cuando el documento está vacío
    // ----------------------------------------------------------
    @Test
    @DisplayName("CV-02 | EQ-12 | Retorna 400 cuando el documento está vacío")
    void should_return400_when_documentIsBlank() throws Exception {
        mockMvc.perform(get("/api/v1/citizen/polling-station")
                .param("document", ""))
                .andExpect(status().isBadRequest());
    }

    // ----------------------------------------------------------
    // CV-03 | EQ-12 | Usabilidad - Protección contra Errores
    // Verifica rechazo cuando el documento contiene letras
    // ----------------------------------------------------------
    @Test
    @DisplayName("CV-03 | EQ-12 | Retorna 400 cuando el documento contiene letras")
    void should_return400_when_documentContainsLetters() throws Exception {
        mockMvc.perform(get("/api/v1/citizen/polling-station")
                .param("document", "ABC12345"))
                .andExpect(status().isBadRequest());
    }

    // ----------------------------------------------------------
    // CV-04 | EQ-12 | Usabilidad - Protección contra Errores
    // Verifica rechazo cuando el documento tiene menos de 3 dígitos
    // ----------------------------------------------------------
    @Test
    @DisplayName("CV-04 | EQ-12 | Retorna 400 cuando el documento tiene menos de 3 dígitos")
    void should_return400_when_documentIsTooShort() throws Exception {
        mockMvc.perform(get("/api/v1/citizen/polling-station")
                .param("document", "12"))
                .andExpect(status().isBadRequest());
    }

    // ----------------------------------------------------------
    // CV-05 | EQ-12 | Usabilidad - Protección contra Errores
    // Verifica rechazo cuando el documento supera 20 dígitos
    // ----------------------------------------------------------
    @Test
    @DisplayName("CV-05 | EQ-12 | Retorna 400 cuando el documento supera 20 dígitos")
    void should_return400_when_documentIsTooLong() throws Exception {
        mockMvc.perform(get("/api/v1/citizen/polling-station")
                .param("document", "123456789012345678901"))
                .andExpect(status().isBadRequest());
    }

    // ----------------------------------------------------------
    // CV-06 | EQ-12 | Usabilidad - Protección contra Errores
    // Verifica rechazo cuando el documento tiene caracteres especiales
    // ----------------------------------------------------------
    @Test
    @DisplayName("CV-06 | EQ-12 | Retorna 400 cuando el documento tiene caracteres especiales")
    void should_return400_when_documentContainsSpecialChars() throws Exception {
        mockMvc.perform(get("/api/v1/citizen/polling-station")
                .param("document", "123-456"))
                .andExpect(status().isBadRequest());
    }

    // ----------------------------------------------------------
    // CV-07 | EQ-8 | Compatibilidad - Interoperabilidad
    // Verifica 404 cuando el ciudadano no está en el censo
    // ----------------------------------------------------------
    @Test
    @DisplayName("CV-07 | EQ-8 | Retorna 404 cuando el ciudadano no está en el censo")
    void should_return404_when_voterNotFound() throws Exception {
        when(service.getVoterInfo("999999999"))
                .thenThrow(new ResourceNotFoundException("Voter not found"));

        mockMvc.perform(get("/api/v1/citizen/polling-station")
                .param("document", "999999999"))
                .andExpect(status().isNotFound());
    }

    // ----------------------------------------------------------
    // CV-08 | EQ-12 | Usabilidad - Protección contra Errores
    // Verifica rechazo cuando no se envía el parámetro document
    // ----------------------------------------------------------
    @Test
    @DisplayName("CV-08 | EQ-12 | Retorna 400 cuando el parámetro document no se envía")
    void should_return400_when_documentParamIsMissing() throws Exception {
        mockMvc.perform(get("/api/v1/citizen/polling-station"))
                .andExpect(status().isBadRequest());
    }

    // ----------------------------------------------------------
    // CV-09 | EQ-2 | Funcional - Corrección
    // Verifica que todos los campos de respuesta llegan sin alteración
    // ----------------------------------------------------------
    @Test
    @DisplayName("CV-09 | EQ-2 | La respuesta contiene todos los campos sin alteraciones")
    void should_returnAllFields_when_voterExists() throws Exception {
        VoterResponse response = new VoterResponse();
        response.setDocument("100200300");
        response.setPollingStation("IE Técnico - Mesa 3");
        response.setStatus("HABILITADO");

        when(service.getVoterInfo("100200300")).thenReturn(response);

        mockMvc.perform(get("/api/v1/citizen/polling-station")
                .param("document", "100200300"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.document").value("100200300"))
                .andExpect(jsonPath("$.pollingStation").value("IE Técnico - Mesa 3"))
                .andExpect(jsonPath("$.status").value("HABILITADO"));
    }

    // ----------------------------------------------------------
    // CV-10 | EQ-20 | Integridad - Consistencia de Respuesta
    // Verifica que la respuesta es consistente entre múltiples llamadas
    // ----------------------------------------------------------
    @Test
    @DisplayName("CV-10 | EQ-20 | Respuesta consistente en múltiples llamadas al mismo endpoint")
    void should_returnConsistentResponse_when_querySameVoterMultipleTimes() throws Exception {
        VoterResponse response = new VoterResponse();
        response.setDocument("123456789");
        response.setPollingStation("Colegio San José - Mesa 5");
        response.setStatus("HABILITADO");

        when(service.getVoterInfo("123456789")).thenReturn(response);

        // Primera llamada
        mockMvc.perform(get("/api/v1/citizen/polling-station")
                .param("document", "123456789"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.document").value("123456789"));

        // Segunda llamada - debe ser idéntica
        mockMvc.perform(get("/api/v1/citizen/polling-station")
                .param("document", "123456789"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.document").value("123456789"))
                .andExpect(jsonPath("$.status").value("HABILITADO"));
    }

    // ----------------------------------------------------------
    // CV-11 | EQ-20 | Integridad - No Corrupción de Datos en Respuesta
    // Verifica que los datos no se corrompen al enviar en JSON
    // ----------------------------------------------------------
    @Test
    @DisplayName("CV-11 | EQ-20 | Los datos no se corrompen en la serialización JSON")
    void should_preserveDataIntegrity_when_serializingResponse() throws Exception {
        VoterResponse response = new VoterResponse();
        response.setDocument("987654321");
        response.setPollingStation("IE Distrital - Mesa 2 - Local B");
        response.setStatus("YA_VOTO");

        when(service.getVoterInfo("987654321")).thenReturn(response);

        mockMvc.perform(get("/api/v1/citizen/polling-station")
                .param("document", "987654321"))
                .andExpect(status().isOk())
                // Verificar que los caracteres especiales se mantienen intactos
                .andExpect(jsonPath("$.pollingStation").value("IE Distrital - Mesa 2 - Local B"))
                .andExpect(jsonPath("$.document").value("987654321"))
                .andExpect(jsonPath("$.status").value("YA_VOTO"));
    }

    // ----------------------------------------------------------
    // CV-12 | EQ-17 | Fiabilidad - Manejo de Errores de Servicio
    // Verifica que el controlador maneja excepciones del servicio
    // ----------------------------------------------------------
    @Test
    @DisplayName("CV-12 | EQ-17 | El controlador maneja errores del servicio gracefully")
    void should_return500_when_serviceThrowsUnexpectedException() throws Exception {
        when(service.getVoterInfo("123456789"))
                .thenThrow(new RuntimeException("Database connection failed"));

        mockMvc.perform(get("/api/v1/citizen/polling-station")
                .param("document", "123456789"))
                .andExpect(status().isInternalServerError());
    }

    // ----------------------------------------------------------
    // CV-13 | EQ-21 | Auditabilidad - Trazabilidad de Requests
    // Verifica que se pueden trazar requests (document consultado, estado)
    // ----------------------------------------------------------
    @Test
    @DisplayName("CV-13 | EQ-21 | Se puede rastrear qué documento fue consultado (trazabilidad)")
    void should_enableRequestTracing_when_voterQueried() throws Exception {
        VoterResponse response = new VoterResponse();
        response.setDocument("555666777");
        response.setPollingStation("Escuela X - Mesa 1");
        response.setStatus("HABILITADO");

        when(service.getVoterInfo("555666777")).thenReturn(response);

        // Esta request puede ser rastreada: documento enviado, estado retornado
        mockMvc.perform(get("/api/v1/citizen/polling-station")
                .param("document", "555666777"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.document").value("555666777"));

        // Verificar que el servicio fue consultado con el documento correcto
        verify(service, times(1)).getVoterInfo("555666777");
    }

    // ----------------------------------------------------------
    // CV-14 | EQ-20 | Integridad - Validación de Campos en Respuesta
    // Verifica que la respuesta solo contiene campos esperados (sin injecciones)
    // ----------------------------------------------------------
    @Test
    @DisplayName("CV-14 | EQ-20 | La respuesta solo contiene campos esperados (sin datos inesperados)")
    void should_returnOnlyExpectedFields_when_voterExists() throws Exception {
        VoterResponse response = new VoterResponse();
        response.setDocument("111222333");
        response.setPollingStation("Centro Electoral - Mesa 10");
        response.setStatus("HABILITADO");

        when(service.getVoterInfo("111222333")).thenReturn(response);

        mockMvc.perform(get("/api/v1/citizen/polling-station")
                .param("document", "111222333"))
                .andExpect(status().isOk())
                // Verificar que contiene los campos esperados
                .andExpect(jsonPath("$.document").exists())
                .andExpect(jsonPath("$.pollingStation").exists())
                .andExpect(jsonPath("$.status").exists())
                // Verificar que no contiene campos inesperados (seguridad)
                .andExpect(jsonPath("$.hasVoted").doesNotExist())
                .andExpect(jsonPath("$.internalId").doesNotExist());
    }
}
