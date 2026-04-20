# citizen-query-service

## 1. Descripción

Microservicio de consultas del sistema ciudadano encargado de exponer
información pública de solo lectura, como el puesto de votación y el
estado del voto.

Este servicio sigue un enfoque CQRS (lado de lectura) y está optimizado
para alto rendimiento mediante Redis como cache, incluyendo mecanismos
de resiliencia para tolerar fallos del servicio de cache.

------------------------------------------------------------------------

## 2. Tecnologías

-   Java 21
-   Spring Boot 3.x
-   Spring Web
-   Spring Data JPA
-   PostgreSQL
-   Redis
-   Resilience4j (Circuit Breaker)
-   Springdoc OpenAPI (Swagger)
-   Maven

------------------------------------------------------------------------

## 3. Arquitectura

Arquitectura por capas:

-   Controller: Exposición de endpoints REST
-   Service: Lógica de negocio y orquestación
-   Repository: Acceso a datos con JPA
-   Cache Adapter: Integración con Redis
-   Circuit Breaker: Manejo de fallos en cache
-   Mapper: Transformación de entidades a DTOs
-   Exception Layer: Manejo global de errores

------------------------------------------------------------------------

## 4. Estrategia de Cache

Se implementa el patrón cache-aside con resiliencia:

1.  Se intenta obtener la información desde Redis
2.  Si falla o no existe, se consulta la base de datos
3.  Se intenta almacenar en cache
4.  En caso de fallo de Redis, el sistema continúa funcionando usando DB

------------------------------------------------------------------------

## 5. Resiliencia (Circuit Breaker)

Se implementa Circuit Breaker con Resilience4j:

-   Detecta fallos en Redis
-   Evita llamadas repetidas a un servicio caído
-   Permite fallback automático hacia base de datos
-   Mejora la latencia en escenarios de fallo

Estados:

-   CLOSED → funcionamiento normal
-   OPEN → Redis deshabilitado temporalmente
-   HALF-OPEN → prueba de recuperación

------------------------------------------------------------------------

## 6. Versionamiento de API

/api/v1/\*

------------------------------------------------------------------------

## 7. Variables de entorno

DB_URL=jdbc:postgresql://localhost:5432/citizen_db DB_USER=citizen_user
DB_PASSWORD=123456

REDIS_HOST=localhost REDIS_PORT=6379

PORT=8081

------------------------------------------------------------------------

## 8. Configuración PostgreSQL

CREATE DATABASE citizen_db;

CREATE USER citizen_user WITH PASSWORD '123456';

GRANT ALL PRIVILEGES ON DATABASE citizen_db TO citizen_user;

`\c c`{=tex}itizen_db

GRANT ALL ON SCHEMA public TO citizen_user;

------------------------------------------------------------------------

## 9. Configuración Redis

sudo apt update sudo apt install redis-server

sudo systemctl start redis-server sudo systemctl enable redis-server

redis-cli ping

------------------------------------------------------------------------

## 10. Ejecución

export \$(grep -v '\^#' .env \| xargs)

mvn spring-boot:run

------------------------------------------------------------------------

## 11. Swagger

http://localhost:8081/swagger-ui.html

------------------------------------------------------------------------

## 12. Endpoint

GET /api/v1/citizen/polling-station?document=1001

------------------------------------------------------------------------

## 13. Respuesta

{ "document": "1001", "pollingStation": "Mesa 01 - Bogotá", "status":
"NOT_VOTED" }

------------------------------------------------------------------------

## 14. Observabilidad

Logging estructurado:

-   CACHE HIT
-   CACHE MISS
-   CACHE STORE
-   CACHE FALLBACK
-   Circuit Breaker events (OPEN, CLOSED, HALF-OPEN)

------------------------------------------------------------------------

## 15. Estado

Servicio funcional con:

-   API REST
-   PostgreSQL
-   Redis
-   Circuit Breaker (Resilience4j)
-   Tolerancia a fallos
-   Documentación Swagger