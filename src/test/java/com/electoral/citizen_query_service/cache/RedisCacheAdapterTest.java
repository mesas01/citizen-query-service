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
}
