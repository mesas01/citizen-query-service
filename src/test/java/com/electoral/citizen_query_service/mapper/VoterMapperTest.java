package com.electoral.citizen_query_service.mapper;

import com.electoral.citizen_query_service.dto.VoterResponse;
import com.electoral.citizen_query_service.entity.Voter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("VoterMapper — Pruebas Unitarias")
class VoterMapperTest {

    private VoterMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new VoterMapper();
    }

    // ----------------------------------------------------------
    // VM-01 | EQ-2 | Funcional - Corrección
    // Verifica que el mapper convierte correctamente
    // todos los campos de Voter a VoterResponse
    // ----------------------------------------------------------
    @Test
    @DisplayName("VM-01 | EQ-2 | Mapea todos los campos de Voter a VoterResponse correctamente")
    void should_mapAllFields_when_voterIsValid() {
        Voter voter = new Voter();
        voter.setDocument("123456789");
        voter.setPollingStation("Colegio San José - Mesa 5");
        voter.setHasVoted(false);

        VoterResponse response = mapper.toResponse(voter);

        assertNotNull(response);
        assertEquals("123456789", response.getDocument());
        assertEquals("Colegio San José - Mesa 5", response.getPollingStation());
        assertNotNull(response.getStatus(),
                "El status no debe ser nulo tras el mapeo");
    }

    // ----------------------------------------------------------
    // VM-02 | EQ-20 | Seguridad - Integridad
    // Verifica que el status HABILITADO se asigna correctamente
    // cuando el votante no ha votado
    // ----------------------------------------------------------
    @Test
    @DisplayName("VM-02 | EQ-20 | Asigna status HABILITADO cuando el votante no ha votado")
    void should_setHabilitadoStatus_when_hasVotedIsFalse() {
        Voter voter = new Voter();
        voter.setDocument("123456789");
        voter.setPollingStation("Colegio San José - Mesa 5");
        voter.setHasVoted(false);

        VoterResponse response = mapper.toResponse(voter);

        assertEquals("HABILITADO", response.getStatus(),
                "El votante que no ha votado debe tener status HABILITADO");
    }

    // ----------------------------------------------------------
    // VM-03 | EQ-20 | Seguridad - Integridad
    // Verifica que el status YA_VOTO se asigna correctamente
    // para prevenir doble habilitación
    // ----------------------------------------------------------
    @Test
    @DisplayName("VM-03 | EQ-20 | Asigna status YA_VOTO cuando el votante ya ejerció su voto")
    void should_setYaVotoStatus_when_hasVotedIsTrue() {
        Voter voter = new Voter();
        voter.setDocument("123456789");
        voter.setPollingStation("Colegio San José - Mesa 5");
        voter.setHasVoted(true);

        VoterResponse response = mapper.toResponse(voter);

        assertEquals("YA_VOTO", response.getStatus(),
                "El votante que ya votó debe tener status YA_VOTO");
    }

    // ----------------------------------------------------------
    // VM-04 | EQ-2 | Funcional - Corrección
    // Verifica que el mapper no altera el documento original
    // ----------------------------------------------------------
    @Test
    @DisplayName("VM-04 | EQ-2 | El mapper no altera el documento del votante")
    void should_preserveDocument_when_mapping() {
        Voter voter = new Voter();
        voter.setDocument("987654321");
        voter.setPollingStation("IE Distrital - Mesa 2");
        voter.setHasVoted(false);

        VoterResponse response = mapper.toResponse(voter);

        assertEquals("987654321", response.getDocument(),
                "El documento no debe ser alterado durante el mapeo");
    }

    // ----------------------------------------------------------
    // VM-05 | EQ-27 | Mantenibilidad - Testeabilidad
    // Verifica que el mapper es instanciable sin dependencias externas
    // ----------------------------------------------------------
    @Test
    @DisplayName("VM-05 | EQ-27 | El mapper es instanciable sin dependencias externas")
    void should_instantiate_when_noExternalDependencies() {
        assertNotNull(mapper,
                "El mapper debe poder instanciarse directamente");
    }
}
