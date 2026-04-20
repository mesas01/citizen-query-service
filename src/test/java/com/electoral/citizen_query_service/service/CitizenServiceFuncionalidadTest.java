package com.electoral.citizen_query_service.service;

// ============================================================
//  TIPO: Unitaria — CitizenService
//  Atributos: Funcionalidad (EQ-1, EQ-2) | Rendimiento (EQ-4)
// ============================================================

import com.electoral.citizen_query_service.cache.RedisCacheAdapter;
import com.electoral.citizen_query_service.dto.VoterResponse;
import com.electoral.citizen_query_service.entity.Voter;
import com.electoral.citizen_query_service.exception.ResourceNotFoundException;
import com.electoral.citizen_query_service.mapper.VoterMapper;
import com.electoral.citizen_query_service.repository.VoterRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CitizenService — Funcionalidad y Rendimiento")
class CitizenServiceFuncionalidadTest {

    @Mock private VoterRepository repository;
    @Mock private RedisCacheAdapter cache;
    @Mock private VoterMapper mapper;
    @InjectMocks private CitizenService service;

    private Voter voter;
    private VoterResponse response;

    @BeforeEach
    void setUp() {
        voter = new Voter();
        voter.setDocument("123456789");
        voter.setPollingStation("Colegio San José - Mesa 5");
        voter.setHasVoted(false);

        response = new VoterResponse();
        response.setDocument("123456789");
        response.setPollingStation("Colegio San José - Mesa 5");
        response.setStatus("HABILITADO");
    }

    // F-01 | EQ-2 | Retorna puesto de votación correcto
    @Test
    @DisplayName("F-01 | EQ-2 | Retorna el puesto de votación correcto para cédula registrada")
    void should_returnCorrectPollingStation_when_documentExists() {
        when(cache.get("voter:123456789")).thenReturn(null);
        when(repository.findById("123456789")).thenReturn(Optional.of(voter));
        when(mapper.toResponse(voter)).thenReturn(response);

        VoterResponse result = service.getVoterInfo("123456789");

        assertEquals("Colegio San José - Mesa 5", result.getPollingStation());
    }

    // F-02 | EQ-2 | Los datos no son alterados entre BD y respuesta
    @Test
    @DisplayName("F-02 | EQ-2 | Los datos del votante no son alterados entre BD y respuesta")
    void should_notAlterData_when_mappingFromDatabase() {
        when(cache.get("voter:123456789")).thenReturn(null);
        when(repository.findById("123456789")).thenReturn(Optional.of(voter));
        when(mapper.toResponse(voter)).thenReturn(response);

        VoterResponse result = service.getVoterInfo("123456789");

        assertEquals("123456789", result.getDocument());
        assertEquals("Colegio San José - Mesa 5", result.getPollingStation());
        assertEquals("HABILITADO", result.getStatus());
    }

    // F-03 | EQ-2 | El documento coincide exactamente con el consultado
    @Test
    @DisplayName("F-03 | EQ-2 | El documento retornado es idéntico al consultado")
    void should_returnExactDocument_when_queried() {
        when(cache.get("voter:123456789")).thenReturn(null);
        when(repository.findById("123456789")).thenReturn(Optional.of(voter));
        when(mapper.toResponse(voter)).thenReturn(response);

        VoterResponse result = service.getVoterInfo("123456789");

        assertEquals("123456789", result.getDocument());
        assertEquals("123456789".length(), result.getDocument().length());
    }

    // F-04 | EQ-1 | La respuesta incluye todos los campos requeridos
    @Test
    @DisplayName("F-04 | EQ-1 | La respuesta incluye todos los campos requeridos")
    void should_includeAllFields_when_voterExists() {
        when(cache.get("voter:123456789")).thenReturn(null);
        when(repository.findById("123456789")).thenReturn(Optional.of(voter));
        when(mapper.toResponse(voter)).thenReturn(response);

        VoterResponse result = service.getVoterInfo("123456789");

        assertNotNull(result.getDocument());
        assertNotNull(result.getPollingStation());
        assertNotNull(result.getStatus());
    }

    // F-05 | EQ-1 | Ningún campo llega nulo
    @Test
    @DisplayName("F-05 | EQ-1 | Ningún campo llega nulo para un votante registrado")
    void should_haveNoNullFields_when_voterIsRegistered() {
        when(cache.get("voter:123456789")).thenReturn(null);
        when(repository.findById("123456789")).thenReturn(Optional.of(voter));
        when(mapper.toResponse(voter)).thenReturn(response);

        VoterResponse result = service.getVoterInfo("123456789");

        assertAll(
            () -> assertNotNull(result.getDocument()),
            () -> assertNotNull(result.getPollingStation()),
            () -> assertNotNull(result.getStatus())
        );
    }

    // R-01 | EQ-4 | Responde desde caché sin tocar BD
    @Test
    @DisplayName("R-01 | EQ-4 | Responde desde caché sin tocar la BD")
    void should_returnFromCache_when_dataExists() {
        when(cache.get("voter:123456789")).thenReturn(response);

        service.getVoterInfo("123456789");

        verify(repository, never()).findById(anyString());
        verify(mapper, never()).toResponse(any());
    }

    // R-02 | EQ-4 | Almacena en caché tras primera consulta a BD
    @Test
    @DisplayName("R-02 | EQ-4 | Almacena en caché tras la primera consulta a BD")
    void should_storeInCache_when_queryingDatabase() {
        when(cache.get("voter:123456789")).thenReturn(null);
        when(repository.findById("123456789")).thenReturn(Optional.of(voter));
        when(mapper.toResponse(voter)).thenReturn(response);

        service.getVoterInfo("123456789");

        verify(cache, times(1)).set("voter:123456789", response);
    }

    // R-03 | EQ-4 | Consultas repetidas no generan múltiples accesos a BD
    @Test
    @DisplayName("R-03 | EQ-4 | Consultas repetidas no generan múltiples accesos a BD")
    void should_notHitDatabase_when_cacheIsWarmed() {
        when(cache.get("voter:123456789")).thenReturn(response);

        for (int i = 0; i < 10; i++) service.getVoterInfo("123456789");

        verify(repository, never()).findById(anyString());
    }

    // F-06 | EQ-8 | Lanza excepción para documento no registrado
    @Test
    @DisplayName("F-06 | EQ-8 | Lanza excepción cuando el documento no está en el censo")
    void should_throwException_when_documentNotFound() {
        when(cache.get("voter:999999999")).thenReturn(null);
        when(repository.findById("999999999")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> service.getVoterInfo("999999999"));
    }
}
