package com.electoral.citizen_query_service.controller;

// ============================================================
//  TIPO: Integración (MockMvc) — CitizenController
//  Atributos: Compatibilidad (EQ-8) | Usabilidad (EQ-12)
// ============================================================

import com.electoral.citizen_query_service.dto.VoterResponse;
import com.electoral.citizen_query_service.exception.ResourceNotFoundException;
import com.electoral.citizen_query_service.service.CitizenService;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CitizenController.class)
@DisplayName("CitizenController — Integración MockMvc")
class CitizenControllerTest {

    @Autowired private MockMvc mockMvc;
    @MockBean  private CitizenService service;

    // C-01 | EQ-8 | Compatibilidad - Interoperabilidad
    @Test
    @DisplayName("C-01 | EQ-8 | Retorna 200 con JSON válido para cédula registrada")
    void should_return200WithValidJson_when_documentIsValid() throws Exception {
        VoterResponse response = new VoterResponse();
        response.setDocument("123456789");
        response.setPollingStation("Colegio San José - Mesa 5");
        response.setStatus("HABILITADO");

        when(service.getVoterInfo("123456789")).thenReturn(response);

        mockMvc.perform(get("/api/v1/citizen/polling-station")
                .param("document", "123456789"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json"))
                .andExpect(jsonPath("$.document").value("123456789"))
                .andExpect(jsonPath("$.pollingStation").value("Colegio San José - Mesa 5"))
                .andExpect(jsonPath("$.status").value("HABILITADO"));
    }

    // C-02 | EQ-8 | Compatibilidad - Interoperabilidad
    @Test
    @DisplayName("C-02 | EQ-8 | Retorna 404 cuando la cédula no está en el censo")
    void should_return404_when_documentNotInCenso() throws Exception {
        when(service.getVoterInfo("999999999"))
                .thenThrow(new ResourceNotFoundException("Voter not found"));

        mockMvc.perform(get("/api/v1/citizen/polling-station")
                .param("document", "999999999"))
                .andExpect(status().isNotFound());
    }

    // U-01 | EQ-12 | Usabilidad - Protección contra Errores
    @ParameterizedTest
    @ValueSource(strings = {"123-456", "123.456", "123 456", "123@456", "<script>"})
    @DisplayName("U-01 | EQ-12 | Rechaza documentos con caracteres especiales")
    void should_return400_when_documentHasSpecialChars(String document) throws Exception {
        mockMvc.perform(get("/api/v1/citizen/polling-station")
                .param("document", document))
                .andExpect(status().isBadRequest());
    }

    // U-02 | EQ-12 | Usabilidad - Protección contra Errores
    @Test
    @DisplayName("U-02 | EQ-12 | Rechaza documentos vacíos")
    void should_return400_when_documentIsEmpty() throws Exception {
        mockMvc.perform(get("/api/v1/citizen/polling-station")
                .param("document", ""))
                .andExpect(status().isBadRequest());
    }

    // U-03 | EQ-12 | Usabilidad - Protección contra Errores
    @Test
    @DisplayName("U-03 | EQ-12 | Rechaza documentos con menos de 3 dígitos")
    void should_return400_when_documentIsTooShort() throws Exception {
        mockMvc.perform(get("/api/v1/citizen/polling-station")
                .param("document", "12"))
                .andExpect(status().isBadRequest());
    }

    // U-04 | EQ-12 | Usabilidad - Protección contra Errores
    @Test
    @DisplayName("U-04 | EQ-12 | Rechaza documentos con más de 20 dígitos")
    void should_return400_when_documentIsTooLong() throws Exception {
        mockMvc.perform(get("/api/v1/citizen/polling-station")
                .param("document", "123456789012345678901"))
                .andExpect(status().isBadRequest());
    }

    // U-05 | EQ-12 | Usabilidad - Protección contra Errores
    @Test
    @DisplayName("U-05 | EQ-12 | Retorna 400 cuando no se envía el parámetro document")
    void should_return400_when_paramIsMissing() throws Exception {
        mockMvc.perform(get("/api/v1/citizen/polling-station"))
                .andExpect(status().isBadRequest());
    }

    // U-06 | EQ-22 | Seguridad - Autenticidad
    @Test
    @DisplayName("U-06 | EQ-22 | Rechaza intento de inyección SQL en el parámetro")
    void should_return400_when_documentContainsSQLInjection() throws Exception {
        mockMvc.perform(get("/api/v1/citizen/polling-station")
                .param("document", "1 OR 1=1"))
                .andExpect(status().isBadRequest());
    }

    // U-07 | EQ-22 | Seguridad - Autenticidad
    @Test
    @DisplayName("U-07 | EQ-22 | El endpoint no acepta método POST")
    void should_return405_when_postMethodIsUsed() throws Exception {
        mockMvc.perform(post("/api/v1/citizen/polling-station")
                .param("document", "123456789"))
                .andExpect(status().isMethodNotAllowed());
    }
}