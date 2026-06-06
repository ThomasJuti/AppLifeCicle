# customers-api — Guía de pruebas locales

API de registro de clientes con **arquitectura hexagonal**, configuración multi-ambiente
(DEV/PROD), observabilidad y despliegue en AWS Free Tier. Esta guía te lleva paso a paso
para **probar todo lo construido en tu máquina** (Windows / PowerShell).

> Para cada bloque: abre **PowerShell** en la carpeta del proyecto:
> ```powershell
> cd C:\Users\thoma_vaulmzi\OneDrive\Documentos\Kata\customers-api
> ```

---

## 0. Prerrequisitos

Verifica que tienes todo (ya validado en este equipo):

```powershell
java --version          # 21.x
mvn --version           # 3.9.x
docker --version        # 28.x
terraform version       # 1.15.x  (abre una terminal NUEVA tras instalarlo)
```

> **Importante — Docker Desktop debe estar abierto** para las fases de Docker y los tests
> de Testcontainers. Si `docker info` da error de "pipe", abre Docker Desktop y espera a
> que el icono esté en verde.

> ℹ️ **Puertos:** el perfil **DEV usa el 8081** (estándar de este proyecto, ya configurado en
> `application-dev.yml`) y **PROD usa el 9090**. Se eligió 8081 para DEV porque en este equipo
> Apache (XAMPP) ocupa el 8080. Comprueba que el 8081 esté libre: `netstat -ano | findstr :8081`

---

## 1. Ejecutar todos los tests (unitarios + integración)

Levanta automáticamente un PostgreSQL real en Docker (Testcontainers).

```powershell
.\mvnw.cmd test
```

**Qué esperar:** `Tests run: 10, Failures: 0, Errors: 0`
- 3 tests del servicio de dominio (Mockito)
- 4 tests del controller REST (`@WebMvcTest`)
- 3 tests de persistencia contra PostgreSQL 16 (Testcontainers + migración Flyway real)

---

## 2. Probar el modo DEV (H2 en memoria, puerto 8081)

### Arrancar
```powershell
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=dev"
```

> El puerto 8081 ya está fijado en `application-dev.yml`, no hace falta pasar nada extra.
> Usa **`curl.exe`** (no `curl` a secas: en PowerShell es un alias distinto).

### Probar los endpoints (en OTRA terminal)

```powershell
# Health (H2 visible porque DEV expone detalles)
curl.exe http://localhost:8081/actuator/health

# Crear cliente -> 201
curl.exe -X POST http://localhost:8081/api/customers -H "Content-Type: application/json" -d "{\"name\":\"Juan Perez\",\"email\":\"juan@email.com\"}"

# Listar -> el cliente creado
curl.exe http://localhost:8081/api/customers

# Email duplicado -> 409 (ProblemDetail RFC 7807)
curl.exe -X POST http://localhost:8081/api/customers -H "Content-Type: application/json" -d "{\"name\":\"Juan Perez\",\"email\":\"juan@email.com\"}"

# Email inválido -> 400 con errors.email
curl.exe -X POST http://localhost:8081/api/customers -H "Content-Type: application/json" -d "{\"name\":\"Ana\",\"email\":\"no-es-email\"}"
```

### En el navegador
- **Swagger UI:** http://localhost:8081/swagger-ui.html
- **H2 Console:** http://localhost:8081/h2-console
  - JDBC URL: `jdbc:h2:mem:customersdb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=false`
  - User: `sa` · Password: *(vacío)*
- **OpenAPI JSON:** http://localhost:8081/api-docs

### Detener
En la terminal donde corre la app: `Ctrl + C`.

---

## 3. Probar el modo PROD (Docker Compose + PostgreSQL real)

Levanta PostgreSQL 16 + la app empaquetada en imagen Docker, en el **puerto 9090**.

### Construir y levantar
```powershell
docker compose up -d --build
```
Espera ~30-45 s a que la app quede *healthy*:
```powershell
docker compose ps
```

### Probar (puerto 9090)
```powershell
# Health (en PROD no muestra detalles a anónimos -> solo status:UP)
curl.exe http://localhost:9090/actuator/health

# Crear cliente -> 201 (se persiste en PostgreSQL)
curl.exe -X POST http://localhost:9090/api/customers -H "Content-Type: application/json" -d "{\"name\":\"Maria Lopez\",\"email\":\"maria@email.com\"}"

# Listar
curl.exe http://localhost:9090/api/customers
```

### Ver los logs JSON estructurados (diferencia clave con DEV)
```powershell
docker compose logs app
```
En PROD verás logs en **JSON** (campos `@timestamp`, `level`, `app:customers-prod`, `env:PROD`),
mientras que DEV los muestra como texto legible con colores.

### Swagger en PROD
http://localhost:9090/swagger-ui.html

### Detener y limpiar
```powershell
docker compose down -v     # -v elimina también el volumen de datos de PostgreSQL
```

---

## 4. Validar la infraestructura (Terraform)

Valida la sintaxis y consistencia de toda la infra (VPC, ECR, RDS, ECS). **No crea nada en AWS.**

```powershell
cd infra
terraform init        # ya ejecutado; descarga el provider AWS
terraform validate    # -> Success! The configuration is valid.
terraform fmt -recursive
cd ..
```

> ⚠️ **No ejecutes `terraform apply`** salvo que quieras crear recursos reales en AWS
> (genera costos aunque sea Free Tier, y requiere credenciales `aws configure`).
> Para esta kata basta con `validate`.

---

## 5. Build + ejecución con el JAR (flujo de la kata)

Este es el flujo que pide la kata: construir un ejecutable y lanzarlo con `--spring.profiles.active`.

### 5.1 Construir el JAR
```powershell
.\mvnw.cmd clean package -DskipTests
```
Genera **`target\customers-api-1.0.0.jar`** (~59 MB, ejecutable y autocontenido).

> El enunciado usa `app.jar` como nombre de ejemplo. Si quieres ese nombre exacto:
> ```powershell
> Copy-Item target\customers-api-1.0.0.jar target\app.jar
> ```
> y sustituye `customers-api-1.0.0.jar` por `app.jar` en los comandos de abajo.

### 5.2 Ejecutar en modo DEV (H2 en memoria, puerto 8081)
No necesita base de datos externa.
```powershell
java -jar target\customers-api-1.0.0.jar --spring.profiles.active=dev
```
Verifica (en otra terminal):
```powershell
curl.exe http://localhost:8081/actuator/health
curl.exe -X POST http://localhost:8081/api/customers -H "Content-Type: application/json" -d "{\"name\":\"Juan Perez\",\"email\":\"juan@email.com\"}"
curl.exe http://localhost:8081/api/customers
```

### 5.3 Ejecutar en modo PROD (PostgreSQL, puerto 9090)
El perfil prod **necesita un PostgreSQL en `localhost:5432`**. Levanta solo ese contenedor
(sin la app, para que el 9090 quede libre para el JAR):
```powershell
docker compose up -d postgres
```
Luego lanza el JAR en prod:
```powershell
java -jar target\customers-api-1.0.0.jar --spring.profiles.active=prod
```
Verifica:
```powershell
curl.exe http://localhost:9090/actuator/health
curl.exe -X POST http://localhost:9090/api/customers -H "Content-Type: application/json" -d "{\"name\":\"Maria Lopez\",\"email\":\"maria@email.com\"}"
curl.exe http://localhost:9090/api/customers
```
Al terminar, detén el contenedor de PostgreSQL:
```powershell
docker compose down -v
```

> Las credenciales por defecto de prod (`customers_user` / `customers_pass`, BD `customersdb`)
> coinciden con las del contenedor `postgres` del `docker-compose.yml`, así que conectan sin
> configurar nada más. Puedes sobrescribirlas con variables de entorno
> `DATABASE_URL`, `DATABASE_USER`, `DATABASE_PASSWORD`.

### 5.4 La diferencia entre ambientes (lo que evalúa la kata)
| | DEV | PROD |
|---|---|---|
| Comando | `... --spring.profiles.active=dev` | `... --spring.profiles.active=prod` |
| Puerto | **8081** | **9090** |
| Nombre app | `customers-dev` | `customers-prod` |
| Base de datos | H2 (en memoria) | PostgreSQL |
| Logs | texto con colores | JSON estructurado |

> Nota sobre el puerto: el enunciado pone 8080 como *ejemplo* para dev; aquí se usa **8081**
> porque en este equipo Apache ocupa el 8080. Lo que se evalúa es la **diferenciación** entre
> ambientes, que queda clara igual.

---

## Diferencias DEV vs PROD (para la demo)

| Característica   | DEV (8081)             | PROD (9090)              |
|-----------------|------------------------|--------------------------|
| Base de datos   | H2 en memoria          | PostgreSQL 16            |
| Esquema         | Hibernate `create-drop`| Flyway + Hibernate `validate` |
| Formato de logs | Texto con colores      | JSON (Logstash)          |
| Nivel de logs   | DEBUG                  | WARN / INFO              |
| H2 Console      | ✅ `/h2-console`       | ❌                        |
| SQL en consola  | ✅                     | ❌                        |
| Health details  | `always`               | `when-authorized`        |

---

## Checklist rápido de verificación

- [ ] `.\mvnw.cmd test` → 10/10 verdes
- [ ] DEV arranca, POST 201 / GET / 409 / 400 responden, Swagger y H2 console abren
- [ ] `docker compose up -d --build` → app *healthy* en 9090
- [ ] PROD: POST/GET funcionan y `docker compose logs app` muestra JSON
- [ ] `docker compose down -v` limpia todo
- [ ] `terraform validate` → Success

---

## Endpoints de la API

> Todos los endpoints `/api/customers/**` requieren un **JWT** en el header
> `Authorization: Bearer <token>`. El token se obtiene en `/api/auth/login`.

| Método | Ruta                  | Auth | Descripción                          |
|--------|-----------------------|:----:|--------------------------------------|
| POST   | `/api/auth/login`     |  —   | Autentica y devuelve un JWT          |
| GET    | `/api/auth/me`        |  ✅  | Usuario autenticado actual           |
| POST   | `/api/customers`      |  ✅  | Crear cliente (email único)          |
| GET    | `/api/customers`      |  ✅  | Listar todos los clientes            |
| PUT    | `/api/customers/{id}` |  ✅  | Editar cliente (email único)         |
| DELETE | `/api/customers/{id}` |  ✅  | Eliminar cliente                     |
| GET    | `/actuator/health`    |  —   | Estado de salud                      |
| GET    | `/swagger-ui.html`    |  —   | Documentación interactiva            |

### Códigos de respuesta

| Código | Cuándo                                                        |
|--------|---------------------------------------------------------------|
| 200    | Login / listado / edición correctos                           |
| 201    | Cliente creado                                                |
| 204    | Cliente eliminado                                             |
| 400    | Datos inválidos (validación) o cuerpo JSON malformado         |
| 401    | Credenciales inválidas, o token ausente/expirado en el login  |
| 403    | Petición a `/api/customers/**` sin token válido               |
| 404    | Cliente inexistente (editar/eliminar)                         |
| 409    | Email ya registrado por otro cliente                          |

## Autenticación (JWT)

```powershell
# 1. Obtener un token (usuario por defecto en DEV: admin / admin123)
$login = Invoke-RestMethod http://localhost:8081/api/auth/login -Method Post `
  -Body (@{username='admin';password='admin123'} | ConvertTo-Json) -ContentType 'application/json'
$tok = $login.token

# 2. Usar el token en los endpoints protegidos
Invoke-RestMethod http://localhost:8081/api/customers -Headers @{ Authorization = "Bearer $tok" }
```

> **Configuración de seguridad** (variables de entorno, con valores por defecto para DEV):
> - `JWT_SECRET` (mín. 32 caracteres), `JWT_EXPIRATION_SECONDS` (def. 3600)
> - `ADMIN_USERNAME` / `ADMIN_PASSWORD` (def. `admin` / `admin123`)
> - `CORS_ALLOWED_ORIGINS` (def. `http://localhost:5173,http://127.0.0.1:5173`)
> En PROD **siempre** sobrescribe `JWT_SECRET` y las credenciales con secretos reales.

## Frontend

SPA en React + Vite bajo [`frontend/`](frontend/README.md): login con JWT y dashboard
para crear / listar / editar / eliminar clientes. Arranca con `npm install && npm run dev`
(puerto 5173) con el backend dev en el 8081. Ver `frontend/README.md`.
