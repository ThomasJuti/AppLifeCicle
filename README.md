# LifeCicleApp — Consola de Gestión de Clientes

![Java](https://img.shields.io/badge/Java-21-orange?logo=openjdk)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3.5-brightgreen?logo=springboot)
![React](https://img.shields.io/badge/React-18-61DAFB?logo=react)
![AWS](https://img.shields.io/badge/AWS-Cloud-FF9900?logo=amazonaws)

**LifeCicleApp** es una solución fullstack para la administración de clientes en entornos bancarios y corporativos. Combina una API REST segura con arquitectura hexagonal, un frontend React moderno y un pipeline de despliegue automatizado en AWS.

---

## Tabla de Contenidos

- [Arquitectura del Sistema](#-arquitectura-del-sistema)
- [Características Principales](#-características-principales)
- [Ciclo de Vida (DEV → PROD)](#-ciclo-de-vida-dev--prod)
- [Historias de Usuario (Release 1)](#-historias-de-usuario-release-1)
- [Casos de Prueba (Release 1)](#-casos-de-prueba-release-1)
- [Tecnologías y Stack](#-tecnologías-y-stack)
- [Modelo de Datos](#-modelo-de-datos)
- [API Reference](#-api-reference)
- [Pruebas de API](#-pruebas-de-api)
- [Guía de Instalación Local](#-guía-de-instalación-local)
- [Despliegue en Nube (AWS)](#️-despliegue-en-nube-aws)
- [Flujo Git y Entrega](#-flujo-git-y-entrega)
- [CI/CD con GitHub Actions](#-cicd-con-github-actions)
- [Agente de Revisión de PR](#-agente-de-revisión-de-pr)
- [Solución de Problemas](#-solución-de-problemas)
- [Usuario de Acceso](#-usuario-de-acceso)
- [Estructura del Proyecto](#-estructura-del-proyecto)
- [Licencia](#-licencia)

---

## Arquitectura del Sistema

El sistema sigue una **arquitectura hexagonal (ports & adapters)** desacoplada, desplegada en AWS con un único punto de entrada HTTPS.

```
                    ┌─────────────────────────────────────┐
                    │         Usuario / Navegador         │
                    └──────────────────┬──────────────────┘
                                       │ HTTPS
                    ┌──────────────────▼──────────────────┐
                    │           CloudFront (CDN)          │
                    │   https://d2bmeirll4v0n0.cloudfront  │
                    └───┬──────────────────────────┬──────┘
                        │ /*                       │ /api/*
              ┌─────────▼─────────┐     ┌──────────▼──────────┐
              │   S3 (Frontend)   │     │   ALB (HTTP :80)    │
              │  React + Vite     │     └──────────┬──────────┘
              │  SPA estática     │                │
              └───────────────────┘     ┌──────────▼──────────┐
                                        │  ECS (EC2 t3.micro) │
                                        │  Spring Boot :9090  │
                                        └──────────┬──────────┘
                                                   │
                                        ┌──────────▼──────────┐
                                        │  RDS PostgreSQL 16  │
                                        │  (subred privada)   │
                                        └─────────────────────┘
```

### Componentes

| Capa | Tecnología | Responsabilidad |
|------|------------|-----------------|
| **Frontend (SPA)** | React 18 + Vite | UI de login y consola CRUD de clientes |
| **CDN / Proxy** | CloudFront | HTTPS, enrutamiento `/api/*` → backend, `/*` → S3 |
| **Backend (API)** | Spring Boot 3 en ECS/EC2 | Lógica de negocio, seguridad JWT, persistencia |
| **Base de Datos** | PostgreSQL 16 (RDS) | Persistencia transaccional con Flyway |
| **Secretos** | AWS Secrets Manager | Credenciales BD, JWT y admin |
| **Contenedores** | ECR + Docker | Imagen multi-stage optimizada (Java 21) |
| **CI/CD** | GitHub Actions + OIDC | Build, test, deploy automático en push a `main` |

### Enrutamiento CloudFront

| Ruta | Destino | Descripción |
|------|---------|-------------|
| `/*` | S3 | Frontend estático (React) |
| `/api/*` | ALB → ECS | API REST |
| `/actuator/*` | ALB → ECS | Health checks |
| `/swagger-ui/*` | ALB → ECS | Documentación interactiva |
| `/api-docs/*` | ALB → ECS | OpenAPI JSON |

---

## Características Principales

### Seguridad y Control de Acceso

- **Autenticación JWT**: sesiones stateless con tokens Bearer (JJWT 0.12).
- **Spring Security 6**: filtro JWT, rutas públicas para login y actuator.
- **Password Hashing**: BCrypt para credenciales del administrador.
- **CORS configurable**: orígenes permitidos vía variable de entorno.
- **Secrets Manager**: credenciales nunca en código ni en task definitions.

### Gestión de Clientes (CRUD)

- **Crear** clientes con validación de nombre y email.
- **Listar** toda la cartera con búsqueda en tiempo real.
- **Editar** nombre y email (unicidad garantizada).
- **Eliminar** con confirmación en la UI.
- **Email único**: constraint a nivel de dominio y base de datos.

### Observabilidad y Calidad

- **Actuator**: health, info, metrics.
- **OpenAPI / Swagger UI**: documentación interactiva de la API.
- **Logs estructurados**: JSON en PROD (Logstash encoder), texto en DEV.
- **Flyway**: migraciones versionadas de esquema.
- **Tests**: unitarios, `@WebMvcTest` y Testcontainers con PostgreSQL real.

### Frontend (Consola)

- Login con redirección declarativa post-autenticación.
- Dashboard con estadísticas, búsqueda y formulario inline.
- Token JWT en `localStorage` con interceptor Axios.

---

## Ciclo de Vida (DEV → PROD)

Es la **misma aplicación** (mismo JAR / misma imagen Docker). Lo que cambia es el **perfil de Spring** (`dev` o `prod`):

| Ambiente | Perfil | Backend | Base de datos | Frontend | Credenciales admin |
|----------|--------|---------|---------------|----------|-------------------|
| **DEV local** | `dev` | http://localhost:8081 | H2 en memoria | http://localhost:5173 | `admin` / `admin123` |
| **PROD simulado** | `prod` | http://localhost:9090 | PostgreSQL (Docker Compose) | http://localhost:5173 → proxy | `admin` / `admin123` |
| **PROD AWS** | `prod` | CloudFront `/api/*` → ALB → ECS | RDS PostgreSQL 16 | https://d2bmeirll4v0n0.cloudfront.net | `admin` / Secrets Manager |

### Banners por perfil

Al arrancar, Spring Boot muestra un banner distinto según el perfil (`spring.banner.location`):

| Perfil | Archivo | Mensaje |
|--------|---------|---------|
| DEV | `banner-dev.txt` | `Environment : DEV (H2 In-Memory)` |
| PROD | `banner-prod.txt` | `Environment : PROD (PostgreSQL)` |

### Comandos por ambiente

```bash
# DEV — backend H2
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev

# PROD simulado — Docker Compose
docker compose up -d --build

# PROD AWS — merge a main dispara GitHub Actions Deploy
git push origin main
```

### CORS y frontend en desarrollo

| Contexto | Configuración |
|----------|---------------|
| **DEV** | CORS `*` en `application-dev.yml`; proxy Vite (`/api` → `:8081`); `VITE_API_URL` vacío en `.env` |
| **PROD AWS** | CORS `*` vía `CORS_ALLOWED_ORIGINS`; frontend con rutas relativas (`/api/...`); CloudFront enruta `/api/*` al ALB |
| **Spring Security** | Si `allowed-origins` es `*`, usa `allowedOriginPatterns("*")` en `SecurityConfig` |

---

## Historias de Usuario (Release 1)

| | **Identificador (ID) de la historia** | **Rol** | **Característica / Funcionalidad** | **Razón / Resultado** | **Número (#) de escenario** | **Criterio de aceptación** | **Contexto** | **Evento** | **Resultado / Comportamiento esperado** |
|---|---|---|---|---|---|---|---|---|---|
| **Iniciar sesión en la consola** | HU-R1-001 | Como administrador del banco | Quiero iniciar sesión con usuario y contraseña | Para acceder de forma segura a la consola de gestión de clientes | 1 | Login exitoso | Dado que el administrador tiene credenciales válidas | Cuando ingrese usuario y contraseña correctos y presione "Entrar a la consola" | Entonces el sistema autenticará al usuario, emitirá un token JWT y lo redirigirá al Dashboard |
| | | | | | 2 | Credenciales inválidas | Dado que el administrador ingresa credenciales incorrectas | Cuando intente iniciar sesión | Entonces el sistema mostrará un mensaje de error y no permitirá el acceso |
| | | | | | 3 | Acceso a ruta protegida sin sesión | Dado que el usuario no ha iniciado sesión | Cuando intente acceder al Dashboard (`/`) | Entonces el sistema lo redirigirá a la pantalla de login |
| | | | | | 4 | Usuario ya autenticado | Dado que el usuario ya tiene una sesión activa | Cuando acceda a `/login` | Entonces el sistema lo redirigirá automáticamente al Dashboard |
| **Visualizar cartera de clientes** | HU-R1-002 | Como administrador autenticado | Quiero visualizar la lista de clientes registrados | Para consultar el estado actual de la cartera | 1 | Listado de clientes | Dado que el administrador ha iniciado sesión | Cuando acceda al Dashboard | Entonces el sistema mostrará la tabla de clientes con nombre, email y fecha de registro |
| | | | | | 2 | Estadísticas del panel | Dado que existen clientes registrados | Cuando se cargue el Dashboard | Entonces el sistema mostrará el total de clientes, el último registro y la cantidad visible |
| | | | | | 3 | Cartera vacía | Dado que no existen clientes registrados | Cuando acceda al Dashboard | Entonces el sistema mostrará la cartera vacía y estadísticas en cero |
| | | | | | 4 | Refrescar listado | Dado que el administrador está en el Dashboard | Cuando presione "Refrescar" | Entonces el sistema volverá a consultar la API y actualizará la lista de clientes |
| **Buscar clientes** | HU-R1-003 | Como administrador autenticado | Quiero buscar clientes por nombre o email | Para localizar rápidamente un cliente en la cartera | 1 | Búsqueda con coincidencias | Dado que existen clientes registrados | Cuando escriba un texto en el campo de búsqueda | Entonces el sistema filtrará la tabla mostrando solo los clientes cuyo nombre o email coincidan |
| | | | | | 2 | Búsqueda sin resultados | Dado que no hay clientes que coincidan con el criterio | Cuando realice una búsqueda | Entonces el sistema mostrará la tabla vacía e indicará 0 coincidencias |
| | | | | | 3 | Limpiar búsqueda | Dado que hay un filtro de búsqueda activo | Cuando borre el texto del buscador | Entonces el sistema mostrará nuevamente todos los clientes |
| **Registrar nuevo cliente** | HU-R1-004 | Como administrador autenticado | Quiero registrar un nuevo cliente | Para ampliar la cartera del banco | 1 | Creación exitosa | Dado que el administrador está en el Dashboard | Cuando complete nombre y email válidos y presione "Crear cliente" | Entonces el sistema registrará el cliente, mostrará confirmación y lo incluirá en la lista |
| | | | | | 2 | Validación de campos | Dado que el administrador intenta crear un cliente | Cuando deje campos obligatorios vacíos o con formato inválido | Entonces el sistema no enviará el formulario y solicitará datos válidos |
| | | | | | 3 | Email duplicado | Dado que ya existe un cliente con el mismo email | Cuando intente registrar un cliente con ese email | Entonces el sistema rechazará la operación con error 409 y mostrará un mensaje al usuario |
| **Editar cliente** | HU-R1-005 | Como administrador autenticado | Quiero modificar los datos de un cliente existente | Para mantener la información de la cartera actualizada | 1 | Edición exitosa | Dado que existe un cliente en la cartera | Cuando seleccione editar, modifique nombre o email y guarde | Entonces el sistema actualizará el cliente y reflejará los cambios en la tabla |
| | | | | | 2 | Cancelar edición | Dado que el administrador está editando un cliente | Cuando presione "Cancelar" | Entonces el sistema descartará los cambios y volverá al modo de creación |
| | | | | | 3 | Email duplicado en edición | Dado que otro cliente ya usa el email ingresado | Cuando intente guardar los cambios | Entonces el sistema rechazará la actualización y mostrará un mensaje de error |
| **Eliminar cliente** | HU-R1-006 | Como administrador autenticado | Quiero eliminar un cliente de la cartera | Para dar de baja registros que ya no aplican | 1 | Eliminación confirmada | Dado que existe un cliente en la cartera | Cuando seleccione eliminar y confirme la acción | Entonces el sistema eliminará el cliente y lo quitará de la lista |
| | | | | | 2 | Cancelar eliminación | Dado que el administrador inició la eliminación | Cuando cancele el diálogo de confirmación | Entonces el sistema no eliminará al cliente |
| | | | | | 3 | Cliente inexistente | Dado que el cliente ya fue eliminado por otro proceso | Cuando intente eliminarlo | Entonces el sistema mostrará un mensaje de error |
| **Cerrar sesión** | HU-R1-007 | Como administrador autenticado | Quiero cerrar mi sesión | Para proteger el acceso a la consola | 1 | Logout exitoso | Dado que el administrador tiene sesión activa | Cuando presione "Salir" | Entonces el sistema invalidará la sesión local, eliminará el token y redirigirá al login |
| | | | | | 2 | Acceso posterior al logout | Dado que el administrador cerró sesión | Cuando intente acceder al Dashboard | Entonces el sistema exigirá autenticación nuevamente |

---

## Casos de Prueba (Release 1)

Especificación Gherkin (Dado / Cuando / Entonces) en formato tabla.

| | **Identificador (ID) de la historia** | **Rol** | **Característica / Funcionalidad** | **Razón / Resultado** | **Número (#) de escenario** | **Criterio de aceptación** | **Contexto** | **Evento** | **Resultado / Comportamiento esperado** |
|---|---|---|---|---|---|---|---|---|---|
| **Iniciar sesión en la consola** | HU-R1-001 | Como administrador del banco | Quiero iniciar sesión con usuario y contraseña | Para acceder de forma segura a la consola de gestión de clientes | 1 | Login exitoso | Dado que el administrador está en la pantalla de login | Cuando ingresa usuario y contraseña correctos y presiona "Entrar a la consola" | Entonces el sistema autentica al administrador, emite un token JWT y redirige al Dashboard |
| | | | | | 2 | Credenciales inválidas | Dado que el administrador está en la pantalla de login | Cuando ingresa credenciales incorrectas e intenta iniciar sesión | Entonces el sistema muestra un mensaje de error y no permite el acceso |
| | | | | | 3 | Acceso a ruta protegida sin sesión | Dado que el usuario no ha iniciado sesión | Cuando intenta acceder al Dashboard (`/`) | Entonces el sistema lo redirige a la pantalla de login |
| | | | | | 4 | Usuario ya autenticado | Dado que el usuario tiene una sesión activa | Cuando accede a `/login` | Entonces el sistema lo redirige automáticamente al Dashboard |
| **Visualizar cartera de clientes** | HU-R1-002 | Como administrador autenticado | Quiero visualizar la lista de clientes registrados | Para consultar el estado actual de la cartera | 1 | Listado de clientes | Dado que el administrador ha iniciado sesión y existen clientes registrados | Cuando accede al Dashboard | Entonces el sistema muestra la tabla con nombre, email y fecha de registro |
| | | | | | 2 | Estadísticas del panel | Dado que existen clientes registrados | Cuando se carga el Dashboard | Entonces el sistema muestra el total de clientes, el último registro y la cantidad visible |
| | | | | | 3 | Cartera vacía | Dado que no existen clientes registrados | Cuando accede al Dashboard | Entonces el sistema muestra la cartera vacía y estadísticas en cero |
| | | | | | 4 | Refrescar listado | Dado que el administrador está en el Dashboard | Cuando presiona "Refrescar" | Entonces el sistema consulta la API y actualiza la lista de clientes |
| **Buscar clientes** | HU-R1-003 | Como administrador autenticado | Quiero buscar clientes por nombre o email | Para localizar rápidamente un cliente en la cartera | 1 | Búsqueda por nombre | Dado que existen clientes registrados | Cuando escribe un texto que coincide con un nombre en el buscador | Entonces el sistema filtra la tabla mostrando solo los clientes coincidentes |
| | | | | | 2 | Búsqueda por email | Dado que existen clientes registrados | Cuando escribe un texto que coincide con un email en el buscador | Entonces el sistema filtra la tabla mostrando solo los clientes coincidentes |
| | | | | | 3 | Búsqueda sin resultados | Dado que no hay clientes que coincidan con el criterio | Cuando realiza una búsqueda | Entonces el sistema muestra la tabla vacía e indica 0 coincidencias |
| | | | | | 4 | Limpiar búsqueda | Dado que hay un filtro de búsqueda activo | Cuando borra el texto del buscador | Entonces el sistema muestra nuevamente todos los clientes |
| **Registrar nuevo cliente** | HU-R1-004 | Como administrador autenticado | Quiero registrar un nuevo cliente | Para ampliar la cartera del banco | 1 | Creación exitosa | Dado que el administrador está en el Dashboard | Cuando completa nombre y email válidos y presiona "Crear cliente" | Entonces el sistema registra al cliente, muestra "Cliente creado" y lo incluye en la lista |
| | | | | | 2 | Validación de campos vacíos | Dado que el administrador intenta crear un cliente | Cuando deja el nombre o email vacíos | Entonces el sistema no envía el formulario y solicita datos válidos |
| | | | | | 3 | Validación de formato inválido | Dado que el administrador intenta crear un cliente | Cuando ingresa un email con formato inválido o nombre menor a 2 caracteres | Entonces el sistema no envía el formulario y solicita datos válidos |
| | | | | | 4 | Email duplicado | Dado que ya existe un cliente con el mismo email | Cuando intenta registrar un cliente con ese email | Entonces el sistema responde HTTP 409 y muestra un mensaje de error |
| **Editar cliente** | HU-R1-005 | Como administrador autenticado | Quiero modificar los datos de un cliente existente | Para mantener la información de la cartera actualizada | 1 | Edición exitosa | Dado que existe un cliente en la cartera | Cuando selecciona editar, modifica los datos y presiona "Guardar cambios" | Entonces el sistema actualiza al cliente, muestra "Cliente actualizado" y refleja los cambios en la tabla |
| | | | | | 2 | Cancelar edición | Dado que el administrador está editando un cliente | Cuando presiona "Cancelar" | Entonces el sistema descarta los cambios y vuelve al modo "Nuevo cliente" |
| | | | | | 3 | Email duplicado en edición | Dado que otro cliente ya usa el email ingresado | Cuando intenta guardar los cambios | Entonces el sistema rechaza la actualización y muestra un mensaje de error |
| **Eliminar cliente** | HU-R1-006 | Como administrador autenticado | Quiero eliminar un cliente de la cartera | Para dar de baja registros que ya no aplican | 1 | Eliminación confirmada | Dado que existe un cliente en la cartera | Cuando selecciona eliminar y confirma la acción | Entonces el sistema elimina al cliente, muestra confirmación y lo quita de la lista |
| | | | | | 2 | Cancelar eliminación | Dado que el administrador inició la eliminación | Cuando cancela el diálogo de confirmación | Entonces el sistema no elimina al cliente |
| | | | | | 3 | Cliente inexistente | Dado que el cliente ya fue eliminado previamente | Cuando intenta eliminarlo | Entonces el sistema muestra un mensaje de error |
| **Cerrar sesión** | HU-R1-007 | Como administrador autenticado | Quiero cerrar mi sesión | Para proteger el acceso a la consola | 1 | Logout exitoso | Dado que el administrador tiene sesión activa en el Dashboard | Cuando presiona "Salir" | Entonces el sistema elimina el token JWT y redirige al login |
| | | | | | 2 | Acceso posterior al logout | Dado que el administrador cerró sesión | Cuando intenta acceder al Dashboard | Entonces el sistema exige autenticación y redirige al login |

---

## Tecnologías y Stack

### Backend (Java Ecosystem)

| Componente | Versión |
|------------|---------|
| Framework | Spring Boot 3.3.5 |
| Lenguaje | Java 21 (LTS) |
| Arquitectura | Hexagonal (Ports & Adapters) |
| ORM | Hibernate / Spring Data JPA |
| Seguridad | Spring Security 6 + JJWT |
| Base de Datos (prod) | PostgreSQL 16 |
| Base de Datos (dev) | H2 en memoria |
| Migraciones | Flyway |
| Documentación | SpringDoc OpenAPI 2.6 |
| Tests | JUnit 5, Mockito, Testcontainers |
| Build | Maven 3.9 (wrapper incluido) |

### Frontend (Modern Web)

| Componente | Versión |
|------------|---------|
| Framework | React 18 |
| Build Tool | Vite 5 |
| Estilos | CSS custom (design system propio) |
| HTTP Client | Axios (interceptores JWT) |
| Routing | React Router DOM 6 |

### Infraestructura (AWS)

| Servicio | Uso |
|----------|-----|
| CloudFront | CDN + reverse proxy unificado |
| S3 | Hosting frontend estático |
| ALB | Balanceador hacia ECS |
| ECS (EC2) | Orquestación de contenedores |
| ECR | Registro de imágenes Docker |
| RDS | PostgreSQL gestionado |
| Secrets Manager | Gestión de secretos |
| CloudWatch | Logs de aplicación |
| IAM (OIDC) | Autenticación CI/CD sin access keys |

### IaC y Pipelines

| Herramienta | Uso |
|-------------|-----|
| Terraform 1.7+ | Infraestructura como código (módulos: VPC, ALB, ECS, RDS, S3, CloudFront, CI/CD) |
| GitHub Actions | CI (build + test) y CD (deploy automático) |
| Docker | Imagen multi-stage con layertools |

---

## Modelo de Datos

Esquema relacional simple, orientado a integridad y unicidad de email.

### Tabla `customers`

| Columna | Tipo | Restricción |
|---------|------|-------------|
| `id` | UUID | PK |
| `name` | VARCHAR(100) | NOT NULL |
| `email` | VARCHAR(150) | NOT NULL, UNIQUE |
| `created_at` | TIMESTAMP | NOT NULL |

```sql
CREATE TABLE customers (
    id         UUID         NOT NULL PRIMARY KEY,
    name       VARCHAR(100) NOT NULL,
    email      VARCHAR(150) NOT NULL UNIQUE,
    created_at TIMESTAMP    NOT NULL
);
```

---

## API Reference

### Autenticación

| Método | Endpoint | Auth | Descripción |
|--------|----------|------|-------------|
| `POST` | `/api/auth/login` | No | Inicia sesión y retorna JWT |
| `GET` | `/api/auth/me` | Bearer | Usuario autenticado actual |

**Request login:**
```json
{
  "username": "admin",
  "password": "admin123"
}
```

**Response login:**
```json
{
  "token": "eyJhbGciOiJIUzUxMiJ9...",
  "tokenType": "Bearer",
  "expiresIn": 3600,
  "username": "admin"
}
```

### Clientes

| Método | Endpoint | Auth | Descripción |
|--------|----------|------|-------------|
| `POST` | `/api/customers` | Bearer | Crea un nuevo cliente |
| `GET` | `/api/customers` | Bearer | Lista todos los clientes |
| `PUT` | `/api/customers/{id}` | Bearer | Actualiza un cliente |
| `DELETE` | `/api/customers/{id}` | Bearer | Elimina un cliente |

**Request crear cliente:**
```json
{
  "name": "Juan Pérez",
  "email": "juan@email.com"
}
```

**Response cliente:**
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "name": "Juan Pérez",
  "email": "juan@email.com",
  "createdAt": "2026-06-06T22:00:00Z"
}
```

### Operaciones

| Método | Endpoint | Descripción |
|--------|----------|-------------|
| `GET` | `/actuator/health` | Estado de salud de la aplicación |
| `GET` | `/swagger-ui.html` | Documentación interactiva Swagger |
| `GET` | `/api-docs` | Especificación OpenAPI en JSON |

---

## Pruebas de API

### OpenAPI

Especificación exportada en [`openapi.json`](openapi.json). También disponible en runtime:

| Ambiente | Swagger UI | OpenAPI JSON |
|----------|------------|--------------|
| DEV | http://localhost:8081/swagger-ui.html | http://localhost:8081/api-docs |
| PROD local | http://localhost:9090/swagger-ui.html | http://localhost:9090/api-docs |
| PROD AWS | https://d2bmeirll4v0n0.cloudfront.net/swagger-ui.html | https://d2bmeirll4v0n0.cloudfront.net/api-docs |

## Guía de Instalación Local

### Prerrequisitos

- Java JDK **21+**
- Node.js **18+** (recomendado 20)
- Maven (incluido via `mvnw`)
- Docker Desktop (para tests con Testcontainers y modo PROD local)

### 1. Clonar el repositorio

```bash
git clone https://github.com/ThomasJuti/AppLifeCicle.git
cd AppLifeCicle
```

### 2. Backend — Modo DEV (H2 en memoria, puerto 8081)

```bash
chmod +x mvnw
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

| Recurso | URL |
|---------|-----|
| API | http://localhost:8081 |
| Swagger UI | http://localhost:8081/swagger-ui.html |
| H2 Console | http://localhost:8081/h2-console |
| Health | http://localhost:8081/actuator/health |

> El puerto **8081** se usa en DEV para evitar conflicto con otros servicios en el 8080.

### 3. Frontend — Modo desarrollo

```bash
cd frontend
npm install
npm run dev
```

Crear `frontend/.env` (recomendado para DEV con proxy Vite):

```env
VITE_API_URL=
```

| Recurso | URL |
|---------|-----|
| Consola | http://localhost:5173 |

### 4. Backend — Modo PROD local (Docker Compose + PostgreSQL)

```bash
docker compose up -d --build
```

| Recurso | URL |
|---------|-----|
| API | http://localhost:9090 |
| Swagger UI | http://localhost:9090/swagger-ui.html |

```bash
docker compose down -v   # Detener y limpiar volúmenes
```

### 5. Ejecutar tests

```bash
./mvnw test
```

Incluye tests unitarios, de controller (`@WebMvcTest`) e integración con PostgreSQL real via Testcontainers.

---

## Despliegue en Nube (AWS)

El proyecto está desplegado y operativo en **AWS us-east-2**.

### URL de producción

**https://d2bmeirll4v0n0.cloudfront.net**

Frontend y API comparten el mismo dominio. El frontend usa rutas relativas (`/api/...`), sin configurar URL absoluta.

### Infraestructura desplegada

| Recurso | Identificador |
|---------|---------------|
| CloudFront | `E1LLV553EWBLUX` |
| S3 Frontend | `customers-api-prod-frontend` |
| ECR | `customers-api-prod` |
| ECS Cluster | `customers-api-cluster` |
| ECS Service | `customers-api-service` |
| Task Family | `customers-api-task` |
| ALB | `customers-api-alb` |
| RDS | PostgreSQL 16.14 |

### Desplegar infraestructura (primera vez)

```bash
cd infra
cp terraform.tfvars.example terraform.tfvars
# Editar terraform.tfvars con credenciales y configuración
terraform init
terraform plan
terraform apply
```

### Variables de entorno (producción)

Inyectadas en ECS via task definition y Secrets Manager:

| Variable | Origen | Descripción |
|----------|--------|-------------|
| `SPRING_PROFILES_ACTIVE` | Task definition | `prod` |
| `DATABASE_URL` | Task definition | JDBC URL de RDS |
| `DATABASE_USER` | Task definition | Usuario de BD |
| `DATABASE_PASSWORD` | Secrets Manager | Contraseña de BD |
| `JWT_SECRET` | Secrets Manager | Clave de firma JWT |
| `ADMIN_USERNAME` | Task definition | Usuario admin (`admin`) |
| `ADMIN_PASSWORD` | Secrets Manager | Contraseña del admin |
| `CORS_ALLOWED_ORIGINS` | Task definition | Orígenes CORS (`*` en prod) |

---

## Flujo Git y Entrega

```
rama feature / QA
      │
      ▼
 Pull Request → main
      │
      ├──► CI (build + test + terraform validate)
      │
      └──► PR Review Agent (reglas + IA Gemini)
              │
              ▼
         merge a main
              │
              ▼
         Deploy condicional → AWS
              ├── frontend/ cambió  → S3 + CloudFront
              └── src/ cambió       → ECR + ECS
```

Repositorio: **https://github.com/ThomasJuti/AppLifeCicle**

---

## CI/CD con GitHub Actions

### Workflows

| Workflow | Archivo | Trigger | Qué hace |
|----------|---------|---------|----------|
| **CI** | `ci.yml` | Push / PR a cualquier rama | Build + test backend, build frontend, `terraform validate` |
| **Deploy** | `deploy.yml` | Push a `main`, `workflow_dispatch` | Deploy condicional a AWS (ver abajo) |
| **PR Review Agent** | `pr-review.yml` | PR opened / synchronize / reopened | Revisión automática del PR (reglas + Gemini) |

### Deploy condicional

El workflow **Deploy** detecta qué cambió con `dorny/paths-filter` y solo ejecuta los jobs necesarios:

| Cambios en el push | Job que corre |
|--------------------|---------------|
| Solo `frontend/**` | Frontend → S3 + CloudFront |
| Solo `src/`, `Dockerfile`, `pom.xml`, `mvnw` | Backend → ECR + ECS |
| Ambos | Los dos en paralelo |
| Solo docs / infra / `.github/` (sin rutas anteriores) | Ningún deploy |
| **Run workflow** manual | Siempre backend + frontend |

### Variables de GitHub (Repository Variables)

Configurar en **Settings → Secrets and variables → Actions → Variables**:

| Variable | Ejemplo |
|----------|---------|
| `AWS_REGION` | `us-east-2` |
| `AWS_DEPLOY_ROLE_ARN` | `arn:aws:iam::659125558798:role/customers-api-github-deploy` |
| `ECR_REPOSITORY` | `customers-api-prod` |
| `CONTAINER_NAME` | `customers-api` |
| `ECS_TASK_FAMILY` | `customers-api-task` |
| `ECS_CLUSTER` | `customers-api-cluster` |
| `ECS_SERVICE` | `customers-api-service` |
| `S3_BUCKET` | `customers-api-prod-frontend` |
| `CLOUDFRONT_DISTRIBUTION_ID` | `E1LLV553EWBLUX` |
| `GEMINI_MODEL` | `gemini-2.5-flash` *(opcional, agente PR)* |

### Secrets de GitHub

| Secret | Uso |
|--------|-----|
| `GEMINI_API_KEY` | API key de Google AI Studio para revisión con IA en PRs |

> La API key debe estar en **Secrets**, no en Variables (las variables son visibles en el repo).

### Flujo de deploy

```
git push origin main
        │
        ├──► CI: build + test (paralelo en PRs y pushes)
        │
        └──► Deploy (solo en main)
              ├── Detectar cambios (paths-filter)
              ├── Frontend: npm build → S3 sync → CloudFront invalidation
              └── Backend: docker build → ECR push → ECS deploy
```

---

## Agente de Revisión de PR

Workflow automático que comenta en cada Pull Request con dos capas:

### Capa 1 — Reglas (`pr-review.sh`)

Análisis determinista del diff:

- Archivos sensibles (`.env`, `.tfvars`, `.pem`, credenciales)
- Patrones de secretos (AWS keys, passwords hardcodeados)
- Backend sin tests en `src/test/java`
- Cambios en ECS, Terraform o `deploy.yml`

Si detecta **bloqueantes**, el check del PR falla en rojo.

### Capa 2 — IA Gemini (`pr-review-ai.sh`)

Envía a Gemini el título, descripción y diff del PR (máx. ~30 KB) con un prompt orientado al stack del proyecto. Responde con:

- Resumen, riesgos, calidad/tests, seguridad, infra/deploy, sugerencias

**Configuración:**

1. Secret `GEMINI_API_KEY` en el repo (Google AI Studio)
2. *(Opcional)* Variable `GEMINI_MODEL` — default `gemini-2.5-flash`

Sin el secret, la capa 2 se omite y solo corren las reglas.

---



### Desarrollo local

| Usuario | Contraseña | Rol |
|---------|------------|-----|
| `admin` | `admin123` | Administrador (acceso total) |

### Producción (AWS)

| Usuario | Contraseña | Notas |
|---------|------------|-------|
| `admin` | *(Secrets Manager)* | Obtener con: |

```bash
aws secretsmanager get-secret-value \
  --secret-id customers-api-prod-admin-password \
  --region us-east-2 \
  --query SecretString --output text
```

---

## Estructura del Proyecto

```
customers-api/
├── src/main/java/com/bank/customers/
│   ├── domain/              # Modelo, puertos, servicios de dominio
│   ├── application/         # DTOs
│   └── infrastructure/      # Adapters (web, persistence), security, config
├── src/main/resources/
│   ├── application.yml      # Config base
│   ├── application-dev.yml  # Perfil DEV (H2, puerto 8081)
│   ├── application-prod.yml # Perfil PROD (PostgreSQL, puerto 9090)
│   └── db/migration/        # Scripts Flyway
├── frontend/
│   ├── src/
│   │   ├── pages/           # Login, Dashboard
│   │   ├── components/      # CustomerForm, CustomerTable
│   │   ├── auth/            # AuthContext, ProtectedRoute
│   │   └── api/             # Cliente Axios
│   └── .env.production      # VITE_API_URL vacío (rutas relativas en prod)
├── infra/
│   ├── main.tf              # Orquestación de módulos
│   └── modules/             # VPC, ALB, ECS, RDS, S3, CloudFront, ECR, CI/CD, Secrets
├── .github/
│   ├── workflows/
│   │   ├── ci.yml               # Build + test
│   │   ├── deploy.yml           # Deploy condicional a AWS
│   │   └── pr-review.yml        # Agente revisión PR (reglas + Gemini)
│   └── scripts/
│       ├── pr-review.sh         # Capa 1: reglas
│       └── pr-review-ai.sh      # Capa 2: IA Gemini
├── openapi.json               # Especificación OpenAPI exportada
├── Dockerfile               # Multi-stage (builder → layertools → runtime)
└── docker-compose.yml       # PostgreSQL + app para pruebas locales PROD
```

---

## Licencia

Proyecto académico — Kata LifeCicleApp. Uso libre con fines educativos.
