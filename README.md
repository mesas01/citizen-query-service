# citizen-query-service

## 1. Descripción

El Citizen Query Service es responsable de exponer endpoints públicos de solo lectura relacionados con información del ciudadano, como el puesto de votación y el estado del voto.

El servicio sigue un enfoque CQRS (lado de lectura) y utiliza Redis como capa de cache para optimizar el rendimiento mediante el patrón cache-aside.

---

## 2. Tecnologías

* Java 21
* Spring Boot 3.x
* Spring Web
* Spring Data JPA
* PostgreSQL
* Redis
* Springdoc OpenAPI (Swagger)
* Maven

---

## 3. Arquitectura

El servicio está diseñado como un microservicio de solo lectura dentro del sistema ciudadano.

Sigue una arquitectura por capas:

* Controller: Manejo de solicitudes HTTP
* Service: Lógica de negocio y manejo de cache
* Repository: Acceso a datos mediante JPA
* Cache Adapter: Integración con Redis
* Mapper: Transformación de entidades a DTOs

### Estrategia de cache

Se implementa el patrón cache-aside:

1. Se intenta obtener la información desde Redis
2. Si no existe (cache miss), se consulta la base de datos
3. Se almacena el resultado en cache con TTL

---

## 4. Versionamiento de API

Todos los endpoints están versionados bajo:

```
/api/v1/*
```

---

## 5. Variables de entorno

Crear un archivo `.env` en la raíz del proyecto:

```bash
DB_URL=jdbc:postgresql://localhost:5432/citizen_db
DB_USER=citizen_user
DB_PASSWORD=123456

REDIS_HOST=localhost
REDIS_PORT=6379

PORT=8081
```

---

## 6. Configuración de la base de datos (PostgreSQL)

### Crear base de datos

```sql
CREATE DATABASE citizen_db;
```

### Crear usuario

```sql
CREATE USER citizen_user WITH PASSWORD '123456';
```

### Asignar permisos

```sql
GRANT ALL PRIVILEGES ON DATABASE citizen_db TO citizen_user;
```

### Permisos sobre esquema

```sql
\c citizen_db
GRANT ALL ON SCHEMA public TO citizen_user;
```

---

## 7. Configuración de Redis

Instalar Redis:

```bash
sudo apt update
sudo apt install redis-server
```

Iniciar servicio:

```bash
sudo systemctl start redis-server
sudo systemctl enable redis-server
```

Verificar funcionamiento:

```bash
redis-cli ping
```

Respuesta esperada:

```
PONG
```

---

## 8. Ejecución del proyecto

Cargar variables de entorno:

```bash
export $(grep -v '^#' .env | xargs)
```

Ejecutar la aplicación:

```bash
mvn spring-boot:run
```

---

## 9. Documentación API (Swagger)

Disponible en:

```
http://localhost:8081/swagger-ui.html
```

---

## 10. Endpoint principal

### Consultar puesto de votación

```http
GET /api/v1/citizen/polling-station?document=123
```

### Respuesta

```json
{
  "document": "123",
  "pollingStation": "Station A",
  "status": "VOTED"
}
```

---

## 11. Base de datos

El servicio utiliza una base de datos PostgreSQL como modelo de lectura.

Tabla principal:

* voter

---

## 12. Consideraciones

* El servicio es de solo lectura
* No requiere autenticación
* Los datos provienen de sistemas externos (sincronización)
* Está optimizado para alto volumen de consultas

---

## 13. Estado del proyecto

Implementación inicial del servicio de consultas del sistema ciudadano, con integración a base de datos y cache en Redis.
