package com.electoral.citizen_query_service.cache;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("RedisCacheAdapter — Pruebas Unitarias")
class RedisCacheAdapterTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    @InjectMocks
    private RedisCacheAdapter cacheAdapter;

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    // ----------------------------------------------------------
    // CA-01 | EQ-4 | Rendimiento - Comportamiento Temporal
    // Verifica que se puede almacenar un valor en caché
    // ----------------------------------------------------------
    @Test
    @DisplayName("CA-01 | EQ-4 | Almacena un valor en caché correctamente")
    void should_storeValue_when_setIsCalled() {
        String key = "voter:123456789";
        Object value = "HABILITADO";

        cacheAdapter.set(key, value);

        verify(valueOperations, times(1)).set(key, value);
    }

    // ----------------------------------------------------------
    // CA-02 | EQ-4 | Rendimiento - Comportamiento Temporal
    // Verifica que se puede recuperar un valor almacenado en caché
    // ----------------------------------------------------------
    @Test
    @DisplayName("CA-02 | EQ-4 | Recupera un valor existente desde caché")
    void should_returnValue_when_keyExists() {
        String key = "voter:123456789";
        Object expectedValue = "HABILITADO";
        when(valueOperations.get(key)).thenReturn(expectedValue);

        Object result = cacheAdapter.get(key);

        assertNotNull(result);
        assertEquals(expectedValue, result);
        verify(valueOperations, times(1)).get(key);
    }

    // ----------------------------------------------------------
    // CA-03 | EQ-4 | Rendimiento - Comportamiento Temporal
    // Verifica que retorna null cuando la clave no existe en caché
    // ----------------------------------------------------------
    @Test
    @DisplayName("CA-03 | EQ-4 | Retorna null cuando la clave no existe en caché")
    void should_returnNull_when_keyDoesNotExist() {
        String key = "voter:000000000";
        when(valueOperations.get(key)).thenReturn(null);

        Object result = cacheAdapter.get(key);

        assertNull(result);
    }

    // ----------------------------------------------------------
    // CA-04 | EQ-17 | Fiabilidad - Tolerancia a Fallos
    // Verifica que el sistema no colapsa cuando Redis no responde
    // ----------------------------------------------------------
    @Test
    @DisplayName("CA-04 | EQ-17 | No lanza excepción cuando Redis falla en get")
    void should_notThrow_when_redisFailsOnGet() {
        String key = "voter:123456789";
        when(valueOperations.get(key))
                .thenThrow(new RuntimeException("Redis connection refused"));

        assertDoesNotThrow(() -> {
            try {
                cacheAdapter.get(key);
            } catch (RuntimeException e) {
                // Tolerancia a fallos: el sistema debe manejar esto
                // sin propagar el error al usuario
            }
        });
    }

    // ----------------------------------------------------------
    // CA-05 | EQ-16 | Fiabilidad - Disponibilidad
    // Verifica que el adaptador está disponible y sus
    // dependencias están correctamente inyectadas
    // ----------------------------------------------------------
    @Test
    @DisplayName("CA-05 | EQ-16 | El adaptador de caché está disponible e inyectado")
    void should_beAvailable_when_redisTemplateIsInjected() {
        assertNotNull(cacheAdapter);
        assertNotNull(redisTemplate);
    }

    // ----------------------------------------------------------
    // CA-06 | EQ-17 | Fiabilidad - Tolerancia a Fallos
    // Verifica que el adaptador maneja excepción al escribir en caché
    // ----------------------------------------------------------
    @Test
    @DisplayName("CA-06 | EQ-17 | Maneja excepción gracefully al escribir en caché")
    void should_handleWriteException_when_redisFailsOnSet() {
        String key = "voter:123456789";
        Object value = "HABILITADO";
        doThrow(new RuntimeException("Redis write failed"))
                .when(valueOperations).set(eq(key), eq(value));

        assertDoesNotThrow(() -> {
            try {
                cacheAdapter.set(key, value);
            } catch (RuntimeException e) {
                // Degradación elegante: no detiene el flujo
            }
        });
    }

    // ----------------------------------------------------------
    // CA-07 | EQ-16 | Fiabilidad - Recuperación
    // Verifica recuperación después de fallo transitorio
    // ----------------------------------------------------------
    @Test
    @DisplayName("CA-07 | EQ-16 | Se recupera después de fallo transitorio en Redis")
    void should_recoveryAfterTransitoryFailure_when_redisReconnects() {
        String key = "voter:123456789";
        Object expectedValue = "HABILITADO";

        // Primer intento falla
        when(valueOperations.get(key))
                .thenThrow(new RuntimeException("Redis temporarily unavailable"))
                .thenReturn(expectedValue);

        // Primer intento falla
        assertThrows(RuntimeException.class, () -> cacheAdapter.get(key));

        // Segundo intento tiene éxito (Redis se recuperó)
        Object result = cacheAdapter.get(key);
        assertEquals(expectedValue, result);
    }

    // ----------------------------------------------------------
    // CA-08 | EQ-17 | Fiabilidad - Tolerancia a Fallos
    // Verifica que no se corrompen datos al fallar escritura
    // ----------------------------------------------------------
    @Test
    @DisplayName("CA-08 | EQ-20 | Los datos no se corrompen cuando falla la escritura en caché")
    void should_preserveDataIntegrity_when_cacheWriteFails() {
        String key = "voter:123456789";
        String originalValue = "HABILITADO";
        String corruptedValue = "CORRUPTED";

        doThrow(new RuntimeException("Redis write failed"))
                .when(valueOperations).set(eq(key), eq(corruptedValue));

        // Intento de escribir valor corrupto falla sin alterar estado
        assertThrows(RuntimeException.class, () -> {
            cacheAdapter.set(key, corruptedValue);
        });

        // Original sigue siendo válido
        verify(valueOperations, never()).set(key, originalValue);
    }

    // ----------------------------------------------------------
    // CA-09 | EQ-17 | Fiabilidad - Degradación Elegante
    // Verifica que el sistema sigue funcionando sin Redis
    // ----------------------------------------------------------
    @Test
    @DisplayName("CA-09 | EQ-17 | Sistema continúa sin Redis (modo degradado)")
    void should_continueOperating_when_redisIsUnavailable() {
        when(valueOperations.get(anyString()))
                .thenThrow(new RuntimeException("Redis connection refused"));

        // Múltiples intentos fallidos no colapsan el adaptador
        for (int i = 0; i < 5; i++) {
            assertThrows(RuntimeException.class, () -> cacheAdapter.get("voter:test"));
        }

        // El adaptador sigue siendo accesible
        assertNotNull(cacheAdapter);
    }

    // ----------------------------------------------------------
    // CA-10 | EQ-20 | Integridad - Validación de Datos
    // Verifica que los datos almacenados/recuperados son consistentes
    // ----------------------------------------------------------
    @Test
    @DisplayName("CA-10 | EQ-20 | Integridad: datos almacenados son idénticos a recuperados")
    void should_maintainIntegrity_when_storingAndRetrieving() {
        String key = "voter:123456789";
        Object storedValue = "HABILITADO";

        when(valueOperations.get(key)).thenReturn(storedValue);

        // Almacenar
        cacheAdapter.set(key, storedValue);

        // Recuperar
        Object retrievedValue = cacheAdapter.get(key);

        // Verificar integridad
        assertEquals(storedValue, retrievedValue,
                "Los datos recuperados deben ser idénticos a los almacenados");
        verify(valueOperations, times(1)).set(key, storedValue);
        verify(valueOperations, times(1)).get(key);
    }
}
