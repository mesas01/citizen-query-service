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

    // VM-01 | EQ-2 | Mapea todos los campos correctamente
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
        assertNotNull(response.getStatus());
    }

    // VM-02 | EQ-20 | hasVoted=false produce HABILITADO
    @Test
    @DisplayName("VM-02 | EQ-20 | Asigna status HABILITADO cuando hasVoted es false")
    void should_setHabilitadoStatus_when_hasVotedIsFalse() {
        Voter voter = new Voter();
        voter.setDocument("123456789");
        voter.setPollingStation("Mesa 1");
        voter.setHasVoted(false);

        VoterResponse response = mapper.toResponse(voter);

        assertEquals("HABILITADO", response.getStatus());
    }

    // VM-03 | EQ-20 | hasVoted=true produce YA_VOTO
    @Test
    @DisplayName("VM-03 | EQ-20 | Asigna status YA_VOTO cuando hasVoted es true")
    void should_setYaVotoStatus_when_hasVotedIsTrue() {
        Voter voter = new Voter();
        voter.setDocument("123456789");
        voter.setPollingStation("Mesa 1");
        voter.setHasVoted(true);

        VoterResponse response = mapper.toResponse(voter);

        assertEquals("YA_VOTO", response.getStatus());
    }

    // VM-04 | EQ-2 | El documento no es alterado durante el mapeo
    @Test
    @DisplayName("VM-04 | EQ-2 | El documento no es alterado durante el mapeo")
    void should_preserveDocument_when_mapping() {
        Voter voter = new Voter();
        voter.setDocument("987654321");
        voter.setPollingStation("Mesa 2");
        voter.setHasVoted(false);

        VoterResponse response = mapper.toResponse(voter);

        assertEquals("987654321", response.getDocument());
    }

    // VM-05 | EQ-27 | El mapper es instanciable sin dependencias externas
    @Test
    @DisplayName("VM-05 | EQ-27 | El mapper es instanciable y testeable sin dependencias externas")
    void should_instantiate_when_noExternalDependencies() {
        assertNotNull(mapper);
    }
}

