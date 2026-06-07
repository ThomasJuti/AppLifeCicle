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
- [Tecnologías y Stack](#-tecnologías-y-stack)
- [Modelo de Datos](#-modelo-de-datos)
- [API Reference](#-api-reference)
- [Guía de Instalación Local](#-guía-de-instalación-local)
- [Despliegue en Nube (AWS)](#️-despliegue-en-nube-aws)
- [CI/CD con GitHub Actions](#-cicd-con-github-actions)
- [Usuario de Acceso](#-usuario-de-acceso)
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

## Guía de Instalación Local

### Prerrequisitos

- Java JDK **21+**
- Node.js **18+** (recomendado 20)
- Maven (incluido via `mvnw`)
- Docker Desktop (para tests con Testcontainers y modo PROD local)

### 1. Clonar el repositorio

```bash
git clone https://github.com/ThomasJuti/AppLifeCicle.git
cd customers-api
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

Crear `frontend/.env` (opcional, ya apunta a 8081 por defecto):

```env
VITE_API_URL=http://localhost:8081
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

## CI/CD con GitHub Actions

### Workflows

| Workflow | Trigger | Jobs |
|----------|---------|------|
| **CI** | Push / PR a cualquier rama | Backend (build + test), Frontend (build), Terraform (validate) |
| **Deploy** | Push a `main` | Backend (ECR + ECS), Frontend (S3 + CloudFront) |

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

### Flujo de deploy

```
git push origin main
        │
        ├──► CI: build + test (paralelo)
        │
        └──► Deploy (paralelo)
              ├── Frontend: npm build → S3 sync → CloudFront invalidation (~20s)
              └── Backend: docker build → ECR push → ECS rolling deploy (~3min)
```


## Usuario de Acceso

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
├── .github/workflows/
│   ├── ci.yml               # Build + test
│   └── deploy.yml           # Deploy a AWS
├── Dockerfile               # Multi-stage (builder → layertools → runtime)
└── docker-compose.yml       # PostgreSQL + app para pruebas locales PROD
```
