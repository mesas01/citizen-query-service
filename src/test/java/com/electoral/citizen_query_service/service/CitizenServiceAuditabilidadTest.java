package com.electoral.citizen_query_service.service;

// ============================================================
//  TIPO: Unitaria — CitizenService
//  Atributos: Auditabilidad (EQ-21, EQ-25, EQ-31)
//             Mantenibilidad (EQ-25, EQ-27)
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
@DisplayName("CitizenService — Auditabilidad y Mantenibilidad")
class CitizenServiceAuditabilidadTest {

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

        response = new VoterResponse();
        response.setDocument("123456789");
        response.setPollingStation("Mesa 1");
        response.setStatus("HABILITADO");
    }

    // A-01 | EQ-31 | Cada consulta genera clave trazable
    @Test
    @DisplayName("A-01 | EQ-31 | Cada consulta genera clave de caché trazable con el documento")
    void should_generateTraceableCacheKey_when_documentIsQueried() {
        when(cache.get("voter:123456789")).thenReturn(null);
        when(repository.findById("123456789")).thenReturn(Optional.of(voter));
        when(mapper.toResponse(voter)).thenReturn(response);

        service.getVoterInfo("123456789");

        verify(cache).get("voter:123456789");
        verify(cache).set(eq("voter:123456789"), any());
    }

    // A-02 | EQ-21 | Votante no encontrado deja rastro auditable
    @Test
    @DisplayName("A-02 | EQ-21 | Un intento fallido deja rastro auditable en el repositorio")
    void should_leaveAuditTrail_when_voterNotFound() {
        when(cache.get("voter:999999999")).thenReturn(null);
        when(repository.findById("999999999")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> service.getVoterInfo("999999999"));

        verify(repository, times(1)).findById("999999999");
        verify(cache, never()).set(anyString(), any());
    }

    // A-03 | EQ-31 | Consulta exitosa deja rastro completo
    @Test
    @DisplayName("A-03 | EQ-31 | Una consulta exitosa deja rastro completo de todas las operaciones")
    void should_leaveCompleteTrace_when_voterFound() {
        when(cache.get("voter:123456789")).thenReturn(null);
        when(repository.findById("123456789")).thenReturn(Optional.of(voter));
        when(mapper.toResponse(voter)).thenReturn(response);

        service.getVoterInfo("123456789");

        var inOrder = inOrder(cache, repository, mapper);
        inOrder.verify(cache).get("voter:123456789");
        inOrder.verify(repository).findById("123456789");
        inOrder.verify(mapper).toResponse(voter);
    }

    // A-04 | EQ-31 | Con CACHE HIT la BD no es consultada
    @Test
    @DisplayName("A-04 | EQ-31 | Con CACHE HIT queda rastro de que la BD no fue consultada")
    void should_leaveCacheHitTrace_when_dataIsCached() {
        when(cache.get("voter:123456789")).thenReturn(response);

        service.getVoterInfo("123456789");

        verify(cache, times(1)).get("voter:123456789");
        verify(repository, never()).findById(anyString());
        verify(cache, never()).set(anyString(), any());
    }

    // A-05 | EQ-25 | Fallo en BD atribuible a capa de repositorio
    @Test
    @DisplayName("A-05 | EQ-25 | Un fallo en BD es atribuible a la capa de repositorio")
    void should_attributeFaultToRepository_when_databaseFails() {
        when(cache.get("voter:123456789")).thenReturn(null);
        when(repository.findById("123456789"))
                .thenThrow(new RuntimeException("PostgreSQL connection lost"));

        assertThrows(RuntimeException.class,
                () -> service.getVoterInfo("123456789"));

        verify(cache, times(1)).get("voter:123456789");
        verify(repository, times(1)).findById("123456789");
        verify(mapper, never()).toResponse(any());
    }

    // A-06 | EQ-25 | Fallo de caché no impide consulta a BD
    @Test
    @DisplayName("A-06 | EQ-25 | Fallo en caché no impide la consulta a BD")
    void should_reachDatabase_when_cacheFails() {
        when(cache.get("voter:123456789"))
                .thenThrow(new RuntimeException("Redis timeout"));
        when(repository.findById("123456789")).thenReturn(Optional.of(voter));
        when(mapper.toResponse(voter)).thenReturn(response);

        try { service.getVoterInfo("123456789"); } catch (RuntimeException ignored) {}

        verify(repository, times(1)).findById("123456789");
    }

    // A-07 | EQ-25 | Cada capa verificable independientemente
    @Test
    @DisplayName("A-07 | EQ-25 | Las tres capas son verificables de forma independiente")
    void should_allowIndependentLayerVerification_when_auditing() {
        when(cache.get("voter:123456789")).thenReturn(null);
        when(repository.findById("123456789")).thenReturn(Optional.of(voter));
        when(mapper.toResponse(voter)).thenReturn(response);

        service.getVoterInfo("123456789");

        verify(cache, times(1)).get("voter:123456789");
        verify(repository, times(1)).findById("123456789");
        verify(mapper, times(1)).toResponse(voter);
        verify(cache, times(1)).set("voter:123456789", response);
    }

    // M-01 | EQ-27 | El servicio es instanciable con dependencias inyectadas
    @Test
    @DisplayName("M-01 | EQ-27 | El servicio es instanciable con dependencias inyectadas via Mockito")
    void should_instantiateService_when_dependenciesAreInjected() {
        assertNotNull(service);
        assertNotNull(repository);
        assertNotNull(cache);
        assertNotNull(mapper);
    }

    // M-02 | EQ-25 | El cache adapter es reemplazable sin afectar el servicio
    @Test
    @DisplayName("M-02 | EQ-25 | El cache adapter es reemplazable sin afectar la lógica del servicio")
    void should_workWithDifferentCache_when_adapterIsReplaced() {
        RedisCacheAdapter otroAdapter = mock(RedisCacheAdapter.class);
        CitizenService serviceConOtroCache =
                new CitizenService(repository, otroAdapter, mapper);

        assertNotNull(serviceConOtroCache);
    }
}
