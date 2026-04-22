package com.electoral.citizen_query_service.repository;

// ============================================================
//  TIPO: Integración con BD Real — VoterRepository
//  Tecnología: Testcontainers + PostgreSQL 15
//  Atributos: Funcionalidad (EQ-2) | Seguridad (EQ-20)
//             Compatibilidad (EQ-8)
// ============================================================

import com.electoral.citizen_query_service.entity.Voter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@Testcontainers(disabledWithoutDocker = true)
@AutoConfigureTestDatabase(replace = Replace.NONE)
@DisplayName("VoterRepository — Integración con PostgreSQL real")
class VoterRepositoryTest {

    @Container
    static PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:15-alpine")
                    .withDatabaseName("electoral_test")
                    .withUsername("test")
                    .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        registry.add("spring.jpa.database-platform", () -> "org.hibernate.dialect.PostgreSQLDialect");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        registry.add("spring.flyway.enabled", () -> "false");
    }

    @Autowired private VoterRepository repository;

    @BeforeEach
    void setUp() {
        repository.deleteAll();
        Voter voter1 = new Voter();
        voter1.setDocument("123456789");
        voter1.setPollingStation("Colegio San José - Mesa 5");
        voter1.setHasVoted(false);
        Voter voter2 = new Voter();
        voter2.setDocument("987654321");
        voter2.setPollingStation("IE Distrital - Mesa 2");
        voter2.setHasVoted(true);
        Voter voter3 = new Voter();
        voter3.setDocument("111222333");
        voter3.setPollingStation("Centro Cívico - Mesa 1");
        voter3.setHasVoted(false);
        repository.saveAll(List.of(voter1, voter2, voter3));
    }

    // VR-01 | EQ-2 | Encuentra votante registrado en PostgreSQL
    @Test
    @DisplayName("VR-01 | EQ-2 | Encuentra votante por documento en PostgreSQL real")
    void should_findVoter_when_documentExistsInPostgres() {
        Optional<Voter> result = repository.findById("123456789");

        assertTrue(result.isPresent());
        assertEquals("123456789", result.get().getDocument());
        assertEquals("Colegio San José - Mesa 5", result.get().getPollingStation());
        assertFalse(result.get().isHasVoted());
    }

    // VR-02 | EQ-8 | Retorna vacío para documento no registrado
    @Test
    @DisplayName("VR-02 | EQ-8 | Retorna vacío para documento no registrado en el censo")
    void should_returnEmpty_when_documentNotInPostgres() {
        Optional<Voter> result = repository.findById("000000000");

        assertFalse(result.isPresent());
    }

    // VR-03 | EQ-20 | Datos persisten sin alteración en PostgreSQL
    @Test
    @DisplayName("VR-03 | EQ-20 | Los datos del votante persisten sin alteración en PostgreSQL")
    void should_persistDataIntact_when_voterIsSaved() {
        Voter nuevo = new Voter();
        nuevo.setDocument("555666777");
        nuevo.setPollingStation("IE La Candelaria - Mesa 3");
        nuevo.setHasVoted(false);
        repository.save(nuevo);

        Optional<Voter> result = repository.findById("555666777");

        assertTrue(result.isPresent());
        assertEquals("555666777", result.get().getDocument());
        assertEquals("IE La Candelaria - Mesa 3", result.get().getPollingStation());
        assertFalse(result.get().isHasVoted());
    }

    // VR-04 | EQ-20 | hasVoted=true persiste correctamente
    @Test
    @DisplayName("VR-04 | EQ-20 | hasVoted=true persiste correctamente en PostgreSQL")
    void should_persistHasVotedTrue_when_voterHasVoted() {
        Optional<Voter> result = repository.findById("987654321");

        assertTrue(result.isPresent());
        assertTrue(result.get().isHasVoted());
    }

    // VR-05 | EQ-20 | Actualización hasVoted false→true persiste
    @Test
    @DisplayName("VR-05 | EQ-20 | Actualización de hasVoted false→true persiste en PostgreSQL")
    void should_updateHasVoted_when_voterVotes() {
        Voter voter = repository.findById("123456789").orElseThrow();
        voter.setHasVoted(true);
        repository.save(voter);

        assertTrue(repository.findById("123456789").orElseThrow().isHasVoted());
    }

    // VR-06 | EQ-20 | Conteo exacto sin registros fantasma
    @Test
    @DisplayName("VR-06 | EQ-20 | El conteo de registros es exacto sin registros fantasma")
    void should_countExactRecords_when_queryingPostgres() {
        assertEquals(3, repository.count());
    }

    // VR-07 | EQ-2 | Múltiples votantes persisten y se recuperan correctamente
    @Test
    @DisplayName("VR-07 | EQ-2 | Múltiples votantes persisten y se recuperan correctamente")
    void should_findAllVoters_when_multipleAreSaved() {
        assertTrue(repository.findById("123456789").isPresent());
        assertTrue(repository.findById("987654321").isPresent());
        assertTrue(repository.findById("111222333").isPresent());
    }

    // VR-08 | EQ-8 | PostgreSQL acepta documentos de 20 dígitos
    @Test
    @DisplayName("VR-08 | EQ-8 | PostgreSQL acepta documentos con formato de censo colombiano")
    void should_acceptLongDocument_when_formatMatchesColombia() {
        String documentoLargo = "12345678901234567890";
        Voter voter = new Voter();
        voter.setDocument(documentoLargo);
        voter.setPollingStation("Mesa Rural");
        voter.setHasVoted(false);
        repository.save(voter);

        assertTrue(repository.findById(documentoLargo).isPresent());
    }

    // VR-09 | EQ-20 | Eliminar votante no deja registros huérfanos
    @Test
    @DisplayName("VR-09 | EQ-20 | Eliminar votante lo remueve completamente sin registros huérfanos")
    void should_removeVoterCompletely_when_deleted() {
        repository.deleteById("111222333");

        assertFalse(repository.findById("111222333").isPresent());
        assertEquals(2, repository.count());
    }

    // VR-10 | EQ-2 | El contenedor es PostgreSQL real, no H2
    @Test
    @DisplayName("VR-10 | EQ-2 | El contenedor es PostgreSQL real — no H2 ni BD sustituta")
    void should_haveRunningPostgresContainer_when_testsStart() {
        assertTrue(postgres.isRunning());
        assertTrue(postgres.getJdbcUrl().contains("postgresql"));
    }
}
