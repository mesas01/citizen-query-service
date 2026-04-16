# citizen-query-service

## 1. Descripción

Microservicio de consultas del sistema ciudadano encargado de exponer
información pública de solo lectura, como el puesto de votación y el
estado del voto.

Este servicio forma parte del lado de lectura bajo el enfoque CQRS y
está optimizado para alto rendimiento mediante el uso de Redis como capa
de cache.

------------------------------------------------------------------------

## 2. Tecnologías

-   Java 21
-   Spring Boot 3.x
-   Spring Web
-   Spring Data JPA
-   PostgreSQL
-   Redis
-   Springdoc OpenAPI (Swagger)
-   Maven

------------------------------------------------------------------------

## 3. Arquitectura

Arquitectura por capas:

-   Controller: Exposición de endpoints REST
-   Service: Lógica de negocio y gestión de cache
-   Repository: Acceso a datos mediante JPA
-   Cache Adapter: Integración con Redis
-   Mapper: Transformación de entidades a DTOs
-   Exception Layer: Manejo global de errores

------------------------------------------------------------------------

## 4. Estrategia de Cache

Se implementa el patrón cache-aside:

1.  Se intenta obtener la información desde Redis
2.  Si no existe (cache miss), se consulta la base de datos
3.  Se almacena el resultado en cache
4.  Las siguientes consultas se resuelven desde cache

------------------------------------------------------------------------

## 5. Versionamiento de API

/api/v1/\*

------------------------------------------------------------------------

## 6. Variables de entorno

DB_URL=jdbc:postgresql://localhost:5432/citizen_db\
DB_USER=citizen_user\
DB_PASSWORD=123456

REDIS_HOST=localhost\
REDIS_PORT=6379

PORT=8081

------------------------------------------------------------------------

## 7. Configuración PostgreSQL

CREATE DATABASE citizen_db;

CREATE USER citizen_user WITH PASSWORD '123456';

GRANT ALL PRIVILEGES ON DATABASE citizen_db TO citizen_user;

`\c `citizen_db

GRANT ALL ON SCHEMA public TO citizen_user;

------------------------------------------------------------------------

## 8. Configuración Redis

sudo apt update\
sudo apt install redis-server

sudo systemctl start redis-server\
sudo systemctl enable redis-server

redis-cli ping

------------------------------------------------------------------------

## 9. Ejecución

export \$(grep -v '\^#' .env \| xargs)

mvn spring-boot:run

------------------------------------------------------------------------

## 10. Swagger

http://localhost:8081/swagger-ui.html

------------------------------------------------------------------------

## 11. Endpoint

GET /api/v1/citizen/polling-station?document=1001

------------------------------------------------------------------------

## 12. Respuesta

{ "document": "1001", "pollingStation": "Mesa 01 - Bogotá", "status":
"NOT_VOTED" }

------------------------------------------------------------------------

## 13. Observabilidad

Logging estructurado para cache hit, cache miss y persistencia en cache.

------------------------------------------------------------------------

## 14. Estado

Servicio funcional con API REST, PostgreSQL, Redis, validación, manejo
de errores y documentación Swagger.
