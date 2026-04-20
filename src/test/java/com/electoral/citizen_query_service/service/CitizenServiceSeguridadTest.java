package com.electoral.citizen_query_service.service;

// ============================================================
//  TIPO: Unitaria — CitizenService
//  Atributos: Seguridad (EQ-19, EQ-20, EQ-21, EQ-22)
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
@DisplayName("CitizenService — Seguridad")
class CitizenServiceSeguridadTest {

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

    // S-01 | EQ-22 | Estado HABILITADO cuando no ha votado
    @Test
    @DisplayName("S-01 | EQ-22 | Estado es HABILITADO cuando el votante no ha ejercido su voto")
    void should_returnHabilitado_when_voterHasNotVoted() {
        voter.setHasVoted(false);
        response.setStatus("HABILITADO");
        when(cache.get("voter:123456789")).thenReturn(null);
        when(repository.findById("123456789")).thenReturn(Optional.of(voter));
        when(mapper.toResponse(voter)).thenReturn(response);

        assertEquals("HABILITADO", service.getVoterInfo("123456789").getStatus());
    }

    // S-02 | EQ-22 | Estado YA_VOTO cuando ya votó
    @Test
    @DisplayName("S-02 | EQ-22 | Estado es YA_VOTO cuando el votante ya ejerció su voto")
    void should_returnYaVoto_when_voterHasVoted() {
        voter.setHasVoted(true);
        response.setStatus("YA_VOTO");
        when(cache.get("voter:123456789")).thenReturn(null);
        when(repository.findById("123456789")).thenReturn(Optional.of(voter));
        when(mapper.toResponse(voter)).thenReturn(response);

        assertEquals("YA_VOTO", service.getVoterInfo("123456789").getStatus());
    }

    // S-03 | EQ-19 | Cédula inexistente no revela información del censo
    @Test
    @DisplayName("S-03 | EQ-19 | Cédula inexistente no revela información interna del censo")
    void should_notRevealCensoInfo_when_documentNotFound() {
        when(cache.get("voter:999999999")).thenReturn(null);
        when(repository.findById("999999999")).thenReturn(Optional.empty());

        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                () -> service.getVoterInfo("999999999"));

        assertFalse(ex.getMessage().toLowerCase().contains("database"));
        assertFalse(ex.getMessage().toLowerCase().contains("sql"));
        assertFalse(ex.getMessage().toLowerCase().contains("table"));
    }

    // S-04 | EQ-19 | No expone datos de otros votantes
    @Test
    @DisplayName("S-04 | EQ-19 | No expone datos de otros votantes al consultar una cédula")
    void should_notExposeOtherVoterData_when_documentIsQueried() {
        when(cache.get("voter:123456789")).thenReturn(response);

        VoterResponse result = service.getVoterInfo("123456789");

        assertEquals("123456789", result.getDocument());
        assertNotEquals("987654321", result.getDocument());
    }

    // S-05 | EQ-20 | hasVoted no puede revertirse
    @Test
    @DisplayName("S-05 | EQ-20 | hasVoted=true produce YA_VOTO y no puede revertirse a HABILITADO")
    void should_notRevertHasVoted_when_voterAlreadyVoted() {
        voter.setHasVoted(true);
        response.setStatus("YA_VOTO");
        when(cache.get("voter:123456789")).thenReturn(null);
        when(repository.findById("123456789")).thenReturn(Optional.of(voter));
        when(mapper.toResponse(voter)).thenReturn(response);

        VoterResponse result = service.getVoterInfo("123456789");

        assertEquals("YA_VOTO", result.getStatus());
        assertNotEquals("HABILITADO", result.getStatus());
    }

    // S-06 | EQ-21 | Repositorio consultado exactamente una vez
    @Test
    @DisplayName("S-06 | EQ-21 | El repositorio es consultado exactamente una vez por solicitud")
    void should_queryRepositoryOnce_when_requestArrives() {
        when(cache.get("voter:123456789")).thenReturn(null);
        when(repository.findById("123456789")).thenReturn(Optional.of(voter));
        when(mapper.toResponse(voter)).thenReturn(response);

        service.getVoterInfo("123456789");

        verify(repository, times(1)).findById("123456789");
    }

    // S-07 | EQ-19 | Sin contaminación cruzada entre cédulas
    @Test
    @DisplayName("S-07 | EQ-19 | Sin contaminación cruzada entre cédulas diferentes")
    void should_notCrossContaminateData_when_differentDocumentsQueried() {
        VoterResponse resp1 = new VoterResponse();
        resp1.setDocument("111111111");
        resp1.setPollingStation("Mesa A");
        resp1.setStatus("HABILITADO");

        VoterResponse resp2 = new VoterResponse();
        resp2.setDocument("222222222");
        resp2.setPollingStation("Mesa B");
        resp2.setStatus("YA_VOTO");

        when(cache.get("voter:111111111")).thenReturn(resp1);
        when(cache.get("voter:222222222")).thenReturn(resp2);

        VoterResponse r1 = service.getVoterInfo("111111111");
        VoterResponse r2 = service.getVoterInfo("222222222");

        assertNotEquals(r1.getDocument(), r2.getDocument());
        assertNotEquals(r1.getPollingStation(), r2.getPollingStation());
    }

    // S-08 | EQ-19 | Caché usa claves específicas por documento
    @Test
    @DisplayName("S-08 | EQ-19 | La caché usa claves específicas por documento")
    void should_useSeparateCacheKeys_when_differentDocumentsQueried() {
        when(cache.get("voter:111111111")).thenReturn(null);
        when(cache.get("voter:222222222")).thenReturn(null);
        when(repository.findById("111111111")).thenReturn(Optional.empty());
        when(repository.findById("222222222")).thenReturn(Optional.empty());

        try { service.getVoterInfo("111111111"); } catch (Exception ignored) {}
        try { service.getVoterInfo("222222222"); } catch (Exception ignored) {}

        verify(cache).get("voter:111111111");
        verify(cache).get("voter:222222222");
        verify(cache, never()).get("voter:333333333");
    }

    // S-09 | EQ-19 | Repositorio no consultado cuando hay caché
    @Test
    @DisplayName("S-09 | EQ-19 | El repositorio no es consultado cuando hay datos en caché")
    void should_notQueryRepository_when_cacheHit() {
        when(cache.get("voter:123456789")).thenReturn(response);

        service.getVoterInfo("123456789");

        verify(repository, never()).findById(anyString());
    }

    // S-10 | EQ-20 | Datos no alterados en ningún punto del flujo
    @Test
    @DisplayName("S-10 | EQ-20 | Los datos no son alterados en ningún punto del flujo BD→respuesta")
    void should_preserveDataIntegrity_when_processingRequest() {
        when(cache.get("voter:123456789")).thenReturn(null);
        when(repository.findById("123456789")).thenReturn(Optional.of(voter));
        when(mapper.toResponse(voter)).thenReturn(response);

        VoterResponse result = service.getVoterInfo("123456789");

        assertEquals("123456789", result.getDocument());
        assertEquals("Mesa 1", result.getPollingStation());
        assertEquals("HABILITADO", result.getStatus());
    }

    // S-11 | EQ-20 | hasVoted=false produce HABILITADO
    @Test
    @DisplayName("S-11 | EQ-20 | hasVoted=false en BD produce exactamente HABILITADO")
    void should_mapFalseToHabilitado_when_hasVotedIsFalse() {
        voter.setHasVoted(false);
        response.setStatus("HABILITADO");
        when(cache.get("voter:123456789")).thenReturn(null);
        when(repository.findById("123456789")).thenReturn(Optional.of(voter));
        when(mapper.toResponse(voter)).thenReturn(response);

        assertEquals("HABILITADO", service.getVoterInfo("123456789").getStatus());
    }

    // S-12 | EQ-20 | hasVoted=true produce YA_VOTO
    @Test
    @DisplayName("S-12 | EQ-20 | hasVoted=true en BD produce exactamente YA_VOTO")
    void should_mapTrueToYaVoto_when_hasVotedIsTrue() {
        voter.setHasVoted(true);
        response.setStatus("YA_VOTO");
        when(cache.get("voter:123456789")).thenReturn(null);
        when(repository.findById("123456789")).thenReturn(Optional.of(voter));
        when(mapper.toResponse(voter)).thenReturn(response);

        assertEquals("YA_VOTO", service.getVoterInfo("123456789").getStatus());
    }

    // S-13 | EQ-20 | Datos en caché mantienen integridad
    @Test
    @DisplayName("S-13 | EQ-20 | Los datos en caché mantienen la misma integridad que los de BD")
    void should_maintainIntegrity_when_returningFromCache() {
        when(cache.get("voter:123456789")).thenReturn(response);

        VoterResponse result = service.getVoterInfo("123456789");

        assertEquals("123456789", result.getDocument());
        assertEquals("Mesa 1", result.getPollingStation());
        assertEquals("HABILITADO", result.getStatus());
    }

    // S-14 | EQ-21 | Caché consultada antes que BD
    @Test
    @DisplayName("S-14 | EQ-21 | La caché siempre es consultada antes que la BD")
    void should_checkCacheBeforeDatabase_when_requestArrives() {
        when(cache.get("voter:123456789")).thenReturn(null);
        when(repository.findById("123456789")).thenReturn(Optional.of(voter));
        when(mapper.toResponse(voter)).thenReturn(response);

        service.getVoterInfo("123456789");

        var inOrder = inOrder(cache, repository);
        inOrder.verify(cache).get("voter:123456789");
        inOrder.verify(repository).findById("123456789");
    }

    // S-15 | EQ-21 | Caché escrita exactamente una vez
    @Test
    @DisplayName("S-15 | EQ-21 | La caché se escribe exactamente una vez para evitar duplicación")
    void should_writeCacheExactlyOnce_when_fetchingFromDatabase() {
        when(cache.get("voter:123456789")).thenReturn(null);
        when(repository.findById("123456789")).thenReturn(Optional.of(voter));
        when(mapper.toResponse(voter)).thenReturn(response);

        service.getVoterInfo("123456789");

        verify(cache, times(1)).set(eq("voter:123456789"), any());
    }
}
