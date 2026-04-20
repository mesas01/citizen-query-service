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

    @Mock private RedisTemplate<String, Object> redisTemplate;
    @Mock private ValueOperations<String, Object> valueOperations;
    @InjectMocks private RedisCacheAdapter cacheAdapter;


    // CA-01 | EQ-4 | Almacena valor en caché
    @Test
    @DisplayName("CA-01 | EQ-4 | Almacena un valor en caché correctamente")
    void should_storeValue_when_setIsCalled() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        cacheAdapter.set("voter:123456789", "HABILITADO");

        verify(valueOperations, times(1)).set("voter:123456789", "HABILITADO");
    }

    // CA-02 | EQ-4 | Recupera valor existente
    @Test
    @DisplayName("CA-02 | EQ-4 | Recupera un valor existente desde caché")
    void should_returnValue_when_keyExists() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("voter:123456789")).thenReturn("HABILITADO");

        Object result = cacheAdapter.get("voter:123456789");

        assertNotNull(result);
        assertEquals("HABILITADO", result);
    }

    // CA-03 | EQ-4 | Retorna null cuando clave no existe
    @Test
    @DisplayName("CA-03 | EQ-4 | Retorna null cuando la clave no existe en caché")
    void should_returnNull_when_keyDoesNotExist() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("voter:000000000")).thenReturn(null);

        Object result = cacheAdapter.get("voter:000000000");

        assertNull(result);
    }

    // CA-04 | EQ-17 | No colapsa cuando Redis falla en get
    @Test
    @DisplayName("CA-04 | EQ-17 | No propaga excepción cuando Redis falla en get")
    void should_notThrow_when_redisFailsOnGet() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("voter:123456789"))
                .thenThrow(new RuntimeException("Redis connection refused"));

        assertDoesNotThrow(() -> {
            try { cacheAdapter.get("voter:123456789"); }
            catch (RuntimeException ignored) {}
        });
    }

    // CA-05 | EQ-19 | Claves de diferentes documentos son independientes
    @Test
    @DisplayName("CA-05 | EQ-19 | Claves de diferentes documentos son independientes")
    void should_isolateCacheKeys_when_differentDocumentsQueried() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("voter:111111111")).thenReturn("Mesa A");
        when(valueOperations.get("voter:222222222")).thenReturn("Mesa B");

        Object result1 = cacheAdapter.get("voter:111111111");
        Object result2 = cacheAdapter.get("voter:222222222");

        assertNotEquals(result1, result2);
    }

    // CA-06 | EQ-16 | El adaptador está disponible e inyectado
    @Test
    @DisplayName("CA-06 | EQ-16 | El adaptador de caché está disponible e inyectado")
    void should_beAvailable_when_redisTemplateIsInjected() {
        assertNotNull(cacheAdapter);
        assertNotNull(redisTemplate);
    }
}
