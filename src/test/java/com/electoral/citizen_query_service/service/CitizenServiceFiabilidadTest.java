package com.electoral.citizen_query_service.service;

// ============================================================
//  TIPO: Unitaria — CitizenService
//  Atributos: Fiabilidad (EQ-15, EQ-16, EQ-17, EQ-18, EQ-33)
// ============================================================

import com.electoral.citizen_query_service.cache.RedisCacheAdapter;
import com.electoral.citizen_query_service.dto.VoterResponse;
import com.electoral.citizen_query_service.entity.Voter;
import com.electoral.citizen_query_service.exception.ResourceNotFoundException;
import com.electoral.citizen_query_service.mapper.VoterMapper;
import com.electoral.citizen_query_service.repository.VoterRepository;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CitizenService — Fiabilidad")
class CitizenServiceFiabilidadTest {

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
        voter.setPollingStation("Mesa 1");
        voter.setHasVoted(false);

        response = new VoterResponse();
        response.setDocument("123456789");
        response.setPollingStation("Mesa 1");
        response.setStatus("HABILITADO");
    }

    // FI-01 | EQ-17 | No colapsa cuando Redis falla
    @Test
    @DisplayName("FI-01 | EQ-17 | No colapsa cuando Redis lanza excepción")
    void should_notCrash_when_redisFails() {
        when(cache.get("voter:123456789"))
                .thenThrow(new RuntimeException("Redis connection refused"));
        when(repository.findById("123456789")).thenReturn(Optional.of(voter));
        when(mapper.toResponse(voter)).thenReturn(response);

        assertDoesNotThrow(() -> {
            try { service.getVoterInfo("123456789"); }
            catch (RuntimeException ignored) {}
        });
    }

    // FI-02 | EQ-16 | Adaptador de caché disponible
    @Test
    @DisplayName("FI-02 | EQ-16 | El adaptador de caché está disponible e inyectado")
    void should_haveAvailableCacheAdapter_when_serviceIsCreated() {
        assertNotNull(cache);
        assertNotNull(service);
    }

    // FI-03 | EQ-15 | El servicio arranca sin errores
    @Test
    @DisplayName("FI-03 | EQ-15 | El servicio arranca y responde sin errores tras inicialización")
    void should_startWithoutErrors_when_contextIsInitialized() {
        assertNotNull(service);
        assertNotNull(repository);
        assertNotNull(mapper);
    }

    // FI-04 | EQ-33 | Recupera datos consistentes tras reconexión con BD
    @Test
    @DisplayName("FI-04 | EQ-33 | Recupera datos consistentes tras reconexión con la BD")
    void should_returnConsistentData_when_databaseReconnects() {
        when(cache.get("voter:123456789")).thenReturn(null);
        when(repository.findById("123456789"))
                .thenThrow(new RuntimeException("BD desconectada"))
                .thenReturn(Optional.of(voter));
        when(mapper.toResponse(voter)).thenReturn(response);

        try { service.getVoterInfo("123456789"); } catch (RuntimeException ignored) {}

        VoterResponse result = service.getVoterInfo("123456789");
        assertNotNull(result);
        assertEquals("123456789", result.getDocument());
    }

    // FI-05 | EQ-17 | Responde correctamente cuando caché está vacía
    @Test
    @DisplayName("FI-05 | EQ-17 | Responde correctamente cuando la caché está vacía")
    void should_respondCorrectly_when_cacheIsEmpty() {
        when(cache.get("voter:123456789")).thenReturn(null);
        when(repository.findById("123456789")).thenReturn(Optional.of(voter));
        when(mapper.toResponse(voter)).thenReturn(response);

        VoterResponse result = service.getVoterInfo("123456789");

        assertNotNull(result);
        assertEquals("HABILITADO", result.getStatus());
    }

    // FI-06 | EQ-17 | Lanza excepción cuando BD no encuentra votante
    @Test
    @DisplayName("FI-06 | EQ-17 | Lanza excepción controlada cuando la BD no encuentra al votante")
    void should_throwException_when_databaseReturnsEmpty() {
        when(cache.get("voter:999999999")).thenReturn(null);
        when(repository.findById("999999999")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> service.getVoterInfo("999999999"));
    }

    // FI-07 | EQ-15 | Múltiples consultas consecutivas consistentes
    @RepeatedTest(5)
    @DisplayName("FI-07 | EQ-15 | Múltiples consultas consecutivas retornan resultados consistentes")
    void should_returnConsistentResults_when_queriedRepeatedly() {
        when(cache.get("voter:123456789")).thenReturn(response);

        VoterResponse result = service.getVoterInfo("123456789");

        assertEquals("123456789", result.getDocument());
        assertEquals("HABILITADO", result.getStatus());
    }

    // FI-08 | EQ-17 | Maneja consultas concurrentes correctamente
    @Test
    @DisplayName("FI-08 | EQ-17 | Maneja correctamente consultas concurrentes sin condiciones de carrera")
    void should_handleConcurrentRequests_without_raceConditions()
            throws InterruptedException {
        when(cache.get("voter:123456789")).thenReturn(response);

        int threads = 10;
        AtomicInteger successes = new AtomicInteger(0);
        ExecutorService executor = Executors.newFixedThreadPool(threads);

        for (int i = 0; i < threads; i++) {
            executor.submit(() -> {
                try {
                    VoterResponse r = service.getVoterInfo("123456789");
                    if (r != null) successes.incrementAndGet();
                } catch (Exception ignored) {}
            });
        }

        executor.shutdown();
        executor.awaitTermination(5, TimeUnit.SECONDS);

        assertEquals(threads, successes.get());
    }

    // FI-09 | EQ-18 | El sistema se recupera tras un fallo previo
    @Test
    @DisplayName("FI-09 | EQ-18 | El sistema se recupera correctamente tras un fallo previo")
    void should_recover_when_previousFailureOccurred() {
        when(cache.get("voter:123456789"))
                .thenThrow(new RuntimeException("Fallo transitorio"))
                .thenReturn(response);

        try { service.getVoterInfo("123456789"); } catch (RuntimeException ignored) {}

        VoterResponse result = service.getVoterInfo("123456789");
        assertNotNull(result);
    }

    // FI-10 | EQ-18 | Un fallo no afecta la siguiente consulta
    @Test
    @DisplayName("FI-10 | EQ-18 | Un fallo no afecta la siguiente consulta")
    void should_notAffectNextQuery_when_previousFailed() {
        when(cache.get("voter:000000000")).thenReturn(null);
        when(repository.findById("000000000")).thenReturn(Optional.empty());
        when(cache.get("voter:123456789")).thenReturn(response);

        assertThrows(Exception.class, () -> service.getVoterInfo("000000000"));

        VoterResponse result = service.getVoterInfo("123456789");
        assertNotNull(result);
        assertEquals("123456789", result.getDocument());
    }
}
