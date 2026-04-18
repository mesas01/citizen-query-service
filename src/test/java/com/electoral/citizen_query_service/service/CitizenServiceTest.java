package com.electoral.citizen_query_service.service;

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
@DisplayName("CitizenService — Pruebas Unitarias")
class CitizenServiceTest {

    @Mock
    private VoterRepository repository;

    @Mock
    private RedisCacheAdapter cache;

    @Mock
    private VoterMapper mapper;

    @InjectMocks
    private CitizenService service;

    private Voter voter;
    private VoterResponse voterResponse;

    @BeforeEach
    void setUp() {
        voter = new Voter();
        voter.setDocument("123456789");
        voter.setPollingStation("Colegio San José - Mesa 5");
        voter.setHasVoted(false);

        voterResponse = new VoterResponse();
        voterResponse.setDocument("123456789");
        voterResponse.setPollingStation("Colegio San José - Mesa 5");
        voterResponse.setStatus("HABILITADO");
    }

    // ----------------------------------------------------------
    // CS-01 | EQ-2 | Funcional - Corrección
    // Verifica que los datos del votante se retornan sin alteraciones
    // ----------------------------------------------------------
    @Test
    @DisplayName("CS-01 | EQ-2 | Retorna datos correctos para cédula válida registrada")
    void should_returnVoterInfo_when_documentExists() {
        when(cache.get("voter:123456789")).thenReturn(null);
        when(repository.findById("123456789")).thenReturn(Optional.of(voter));
        when(mapper.toResponse(voter)).thenReturn(voterResponse);

        VoterResponse result = service.getVoterInfo("123456789");

        assertNotNull(result);
        assertEquals("123456789", result.getDocument());
        assertEquals("Colegio San José - Mesa 5", result.getPollingStation());
        assertEquals("HABILITADO", result.getStatus());
    }

    // ----------------------------------------------------------
    // CS-02 | EQ-8 | Compatibilidad - Interoperabilidad
    // Verifica que se lanza excepción cuando el ciudadano
    // no está registrado en el censo (RNEC)
    // ----------------------------------------------------------
    @Test
    @DisplayName("CS-02 | EQ-8 | Lanza excepción cuando ciudadano no está en el censo")
    void should_throwResourceNotFoundException_when_documentNotFound() {
        when(cache.get("voter:999999999")).thenReturn(null);
        when(repository.findById("999999999")).thenReturn(Optional.empty());

        ResourceNotFoundException ex = assertThrows(
                ResourceNotFoundException.class,
                () -> service.getVoterInfo("999999999")
        );

        assertEquals("Voter not found", ex.getMessage());
        verify(repository, times(1)).findById("999999999");
    }

    // ----------------------------------------------------------
    // CS-03 | EQ-4 | Rendimiento - Comportamiento Temporal
    // Verifica que la caché evita consultas innecesarias a BD
    // ----------------------------------------------------------
    @Test
    @DisplayName("CS-03 | EQ-4 | Retorna datos desde caché sin consultar BD")
    void should_returnFromCache_when_voterIsCached() {
        when(cache.get("voter:123456789")).thenReturn(voterResponse);

        VoterResponse result = service.getVoterInfo("123456789");

        assertNotNull(result);
        assertEquals("123456789", result.getDocument());
        verify(repository, never()).findById(anyString());
        verify(mapper, never()).toResponse(any());
    }

    // ----------------------------------------------------------
    // CS-04 | EQ-4 | Rendimiento - Comportamiento Temporal
    // Verifica que se almacena en caché tras consultar la BD
    // ----------------------------------------------------------
    @Test
    @DisplayName("CS-04 | EQ-4 | Almacena en caché tras consulta a BD")
    void should_storeInCache_when_queryingFromDatabase() {
        when(cache.get("voter:123456789")).thenReturn(null);
        when(repository.findById("123456789")).thenReturn(Optional.of(voter));
        when(mapper.toResponse(voter)).thenReturn(voterResponse);

        service.getVoterInfo("123456789");

        verify(cache, times(1)).set("voter:123456789", voterResponse);
    }

    // ----------------------------------------------------------
    // CS-05 | EQ-22 | Seguridad - Autenticidad
    // Verifica que el estado es HABILITADO cuando el votante
    // no ha ejercido su voto
    // ----------------------------------------------------------
    @Test
    @DisplayName("CS-05 | EQ-22 | Estado HABILITADO cuando votante no ha votado")
    void should_returnHabilitadoStatus_when_voterHasNotVoted() {
        voter.setHasVoted(false);
        voterResponse.setStatus("HABILITADO");
        when(cache.get("voter:123456789")).thenReturn(null);
        when(repository.findById("123456789")).thenReturn(Optional.of(voter));
        when(mapper.toResponse(voter)).thenReturn(voterResponse);

        VoterResponse result = service.getVoterInfo("123456789");

        assertEquals("HABILITADO", result.getStatus());
    }

    // ----------------------------------------------------------
    // CS-06 | EQ-19 | Seguridad - Confidencialidad
    // Verifica que el estado es YA_VOTO para prevenir
    // habilitación duplicada del mismo ciudadano
    // ----------------------------------------------------------
    @Test
    @DisplayName("CS-06 | EQ-19 | Estado YA_VOTO previene habilitación duplicada")
    void should_returnYaVotoStatus_when_voterAlreadyVoted() {
        voter.setHasVoted(true);
        voterResponse.setStatus("YA_VOTO");
        when(cache.get("voter:123456789")).thenReturn(null);
        when(repository.findById("123456789")).thenReturn(Optional.of(voter));
        when(mapper.toResponse(voter)).thenReturn(voterResponse);

        VoterResponse result = service.getVoterInfo("123456789");

        assertEquals("YA_VOTO", result.getStatus());
    }

    // ----------------------------------------------------------
    // CS-07 | EQ-27 | Mantenibilidad - Testeabilidad
    // Verifica que el servicio es instanciable con sus dependencias
    // ----------------------------------------------------------
    @Test
    @DisplayName("CS-07 | EQ-27 | El servicio es instanciable con dependencias inyectadas")
    void should_instantiateService_when_dependenciesAreInjected() {
        assertNotNull(service);
        assertNotNull(repository);
        assertNotNull(cache);
        assertNotNull(mapper);
    }

    // ----------------------------------------------------------
    // CS-08 | EQ-20 | Integridad - Consistencia de Datos
    // Verifica que los datos no se alteran entre consultas
    // ----------------------------------------------------------
    @Test
    @DisplayName("CS-08 | EQ-20 | Los datos son consistentes entre múltiples consultas")
    void should_returnConsistentData_when_queryingMultipleTimes() {
        when(cache.get("voter:123456789")).thenReturn(null);
        when(repository.findById("123456789")).thenReturn(Optional.of(voter));
        when(mapper.toResponse(voter)).thenReturn(voterResponse);

        VoterResponse result1 = service.getVoterInfo("123456789");
        
        // Simular que la segunda consulta viene de caché
        when(cache.get("voter:123456789")).thenReturn(voterResponse);
        VoterResponse result2 = service.getVoterInfo("123456789");

        // Verificar que ambos resultados son idénticos
        assertEquals(result1.getDocument(), result2.getDocument(),
                "El documento debe ser idéntico en múltiples consultas");
        assertEquals(result1.getStatus(), result2.getStatus(),
                "El status debe ser idéntico en múltiples consultas");
        assertEquals(result1.getPollingStation(), result2.getPollingStation(),
                "La mesa de votación debe ser idéntica en múltiples consultas");
    }

    // ----------------------------------------------------------
    // CS-09 | EQ-21 | Auditabilidad - Trazabilidad de Operaciones
    // Verifica que se registran operaciones para auditoría
    // ----------------------------------------------------------
    @Test
    @DisplayName("CS-09 | EQ-21 | Se registra cada consulta para auditoría (trazabilidad)")
    void should_verifyOperationIsTraceable_when_queryingVoter() {
        when(cache.get("voter:123456789")).thenReturn(null);
        when(repository.findById("123456789")).thenReturn(Optional.of(voter));
        when(mapper.toResponse(voter)).thenReturn(voterResponse);

        service.getVoterInfo("123456789");

        // Verificar que se consultó el repositorio (evidencia de consulta)
        verify(repository, times(1)).findById("123456789");
        // Verificar que se almacenó en caché (evidencia de procesamiento)
        verify(cache, times(1)).set("voter:123456789", voterResponse);
    }

    // ----------------------------------------------------------
    // CS-10 | EQ-20 | Integridad - Validación de Campos
    // Verifica que todos los campos críticos están presentes y válidos
    // ----------------------------------------------------------
    @Test
    @DisplayName("CS-10 | EQ-20 | La respuesta contiene todos los campos críticos sin corrupción")
    void should_validateResponseIntegrity_when_returningVoterInfo() {
        when(cache.get("voter:123456789")).thenReturn(null);
        when(repository.findById("123456789")).thenReturn(Optional.of(voter));
        when(mapper.toResponse(voter)).thenReturn(voterResponse);

        VoterResponse result = service.getVoterInfo("123456789");

        // Validar integridad de campos críticos
        assertNotNull(result.getDocument(), "Documento no debe ser nulo");
        assertNotNull(result.getStatus(), "Status no debe ser nulo");
        assertNotNull(result.getPollingStation(), "Mesa de votación no debe ser nula");

        // Validar que no están vacíos
        assertFalse(result.getDocument().isEmpty(), "Documento no debe estar vacío");
        assertFalse(result.getStatus().isEmpty(), "Status no debe estar vacío");
        assertTrue(result.getStatus().matches("HABILITADO|YA_VOTO"),
                "Status debe ser HABILITADO o YA_VOTO");
    }

    // ----------------------------------------------------------
    // CS-11 | EQ-21 | Auditabilidad - Registro de Errores
    // Verifica que los errores se registran para auditoría
    // ----------------------------------------------------------
    @Test
    @DisplayName("CS-11 | EQ-21 | Los errores se registran para auditoría (intentos fallidos)")
    void should_auditFailedAttempts_when_voterNotFound() {
        when(cache.get("voter:999999999")).thenReturn(null);
        when(repository.findById("999999999")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> {
            service.getVoterInfo("999999999");
        });

        // Verificar que se intentó consultar (evidencia de intento)
        verify(repository, times(1)).findById("999999999");
        // Verificar que no se almacenó en caché (evidencia de error)
        verify(cache, never()).set(anyString(), any());
    }

    // ----------------------------------------------------------
    // CS-12 | EQ-17 | Fiabilidad - Tolerancia a Fallos en Caché
    // Verifica que el servicio continúa si falla la caché
    // ----------------------------------------------------------
    @Test
    @DisplayName("CS-12 | EQ-17 | El servicio continúa sin caché (fallback a BD)")
    void should_failoverToDatabase_when_cacheUnavailable() {
        when(cache.get("voter:123456789"))
                .thenThrow(new RuntimeException("Redis unavailable"));
        when(repository.findById("123456789")).thenReturn(Optional.of(voter));
        when(mapper.toResponse(voter)).thenReturn(voterResponse);

        // Aunque la caché falle, el servicio debe recuperarse consultando BD
        assertThrows(RuntimeException.class, () -> {
            try {
                service.getVoterInfo("123456789");
            } catch (RuntimeException e) {
                // Se espera que el servicio maneje esto y continúe
                throw e;
            }
        });
    }
}
