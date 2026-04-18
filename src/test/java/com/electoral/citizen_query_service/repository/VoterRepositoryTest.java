package com.electoral.citizen_query_service.repository; 

import com.electoral.citizen_query_service.entity.Voter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@DisplayName("VoterRepository — Pruebas de Integración con BD")
class VoterRepositoryTest {

    @Autowired
    private VoterRepository repository;

    @BeforeEach
    void setUp() {
        repository.deleteAll();

        Voter voter = new Voter();
        voter.setDocument("123456789");
        voter.setPollingStation("Colegio San José - Mesa 5");
        voter.setHasVoted(false);
        repository.save(voter);
    }

    // ----------------------------------------------------------
    // VR-01 | EQ-2 | Funcional - Corrección
    // Verifica que se puede encontrar un votante por su documento
    // ----------------------------------------------------------
    @Test
    @DisplayName("VR-01 | EQ-2 | Encuentra votante por documento registrado")
    void should_findVoter_when_documentExists() {
        Optional<Voter> result = repository.findById("123456789");

        assertTrue(result.isPresent());
        assertEquals("123456789", result.get().getDocument());
        assertEquals("Colegio San José - Mesa 5", result.get().getPollingStation());
        assertFalse(result.get().isHasVoted());
    }

    // ----------------------------------------------------------
    // VR-02 | EQ-8 | Compatibilidad - Interoperabilidad
    // Verifica que retorna vacío para documento no registrado
    // ----------------------------------------------------------
    @Test
    @DisplayName("VR-02 | EQ-8 | Retorna vacío para documento no registrado en censo")
    void should_returnEmpty_when_documentNotExists() {
        Optional<Voter> result = repository.findById("999999999");

        assertFalse(result.isPresent());
    }

    // ----------------------------------------------------------
    // VR-03 | EQ-20 | Seguridad - Integridad
    // Verifica que los datos del votante no se alteran al persistir
    // ----------------------------------------------------------
    @Test
    @DisplayName("VR-03 | EQ-20 | Los datos del votante se persisten sin alteraciones")
    void should_persistDataIntact_when_voterIsSaved() {
        Voter newVoter = new Voter();
        newVoter.setDocument("987654321");
        newVoter.setPollingStation("IE Distrital - Mesa 2");
        newVoter.setHasVoted(false);
        repository.save(newVoter);

        Optional<Voter> result = repository.findById("987654321");

        assertTrue(result.isPresent());
        assertEquals("987654321", result.get().getDocument());
        assertEquals("IE Distrital - Mesa 2", result.get().getPollingStation());
        assertFalse(result.get().isHasVoted());
    }

    // ----------------------------------------------------------
    // VR-04 | EQ-20 | Seguridad - Integridad
    // Verifica que el estado hasVoted se actualiza correctamente
    // para prevenir voto duplicado
    // ----------------------------------------------------------
    @Test
    @DisplayName("VR-04 | EQ-20 | El estado hasVoted se actualiza correctamente")
    void should_updateHasVoted_when_voterVotes() {
        Voter voter = repository.findById("123456789").get();
        voter.setHasVoted(true);
        repository.save(voter);

        Optional<Voter> updated = repository.findById("123456789");

        assertTrue(updated.isPresent());
        assertTrue(updated.get().isHasVoted(),
                "El estado hasVoted debe ser true tras ejercer el voto");
    }

    // ----------------------------------------------------------
    // VR-05 | EQ-2 | Funcional - Corrección
    // Verifica que el repositorio no retorna registros fantasma
    // ----------------------------------------------------------
    @Test
    @DisplayName("VR-05 | EQ-2 | El repositorio no retorna registros inexistentes")
    void should_returnOnlyExistingRecords_when_queried() {
        long count = repository.count();

        assertEquals(1, count,
                "Solo debe existir el registro creado en setUp");
    }
}
