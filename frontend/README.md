# Customers — Frontend (React + Vite)

SPA que consume la **Customers API** con autenticación **JWT** y un **dashboard** para
crear, listar, editar y eliminar clientes.

## Stack

- **React 18** + **Vite 5**
- **react-router-dom** (rutas + ruta protegida)
- **axios** (cliente HTTP con interceptores de token y manejo de 401)

## Requisitos

- Node.js 18+ (probado con 22)
- El backend `customers-api` corriendo en modo **dev** (puerto **8081**)

## Configuración

La URL del backend se lee de `.env`:

```
VITE_API_URL=http://localhost:8081
```

El backend ya permite el origen `http://localhost:5173` por CORS (ver
`application.yml` → `app.security.cors.allowed-origins`).

## Arrancar en desarrollo

```powershell
# 1. Backend (en la carpeta raíz del proyecto, otra terminal)
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=dev"

# 2. Frontend (en la carpeta frontend/)
cd frontend
npm install
npm run dev
```

Abre **http://localhost:5173**.

## Credenciales por defecto (DEV)

| Usuario | Contraseña |
|---------|------------|
| `admin` | `admin123` |

Se configuran en el backend con `ADMIN_USERNAME` / `ADMIN_PASSWORD`
(ver `application.yml`). En producción cámbialas por variables de entorno.

## Build de producción

```powershell
npm run build      # genera dist/
npm run preview    # sirve dist/ localmente para verificar
```

## Diseño

Tema visual **"Vault"**: consola financiera oscura y refinada — lienzo de tinta
profunda con atmósfera (glow radial + grano), acento dorado y tipografía con
carácter (**Bricolage Grotesque** display, **Hanken Grotesk** cuerpo, **JetBrains
Mono** para datos). Login a pantalla partida, app shell con topbar fija, tira de
estadísticas, búsqueda en vivo, monogramas por cliente, skeletons de carga y
toasts. Solo CSS + fuentes de Google (sin librerías de UI extra), build estático
listo para servir desde cualquier CDN / hosting estático.

## Estructura

```
src/
  api/
    client.js        # axios: baseURL, interceptor Bearer, manejo de 401, parseo de errores
    customers.js     # funciones de los endpoints (login + CRUD)
  auth/
    AuthContext.jsx  # estado de sesión (token), login/logout, escucha de 401
    ProtectedRoute.jsx
  components/
    CustomerForm.jsx   # crear / editar (panel)
    CustomerTable.jsx  # tabla "ledger": monogramas, datos en mono, skeleton, vacío
  lib/
    format.js        # iniciales, color de avatar, fecha y "hace X"
  pages/
    Login.jsx        # pantalla partida (marca + formulario)
    Dashboard.jsx    # app shell: topbar, stats, búsqueda, toasts
  App.jsx            # rutas
  main.jsx           # bootstrap + router
  styles.css         # sistema de diseño "Vault" (tokens + componentes)
```

## Cómo funciona la sesión

1. `Login` envía `POST /api/auth/login` y recibe `{ token, tokenType, expiresIn, username }`.
2. El token se guarda en `localStorage` y se inyecta como `Authorization: Bearer <token>`
   en cada petición vía el interceptor de axios.
3. Si el backend responde **401** (token expirado/ inválido), el interceptor limpia la
   sesión y la app redirige al login automáticamente.
4. La ruta `/` está protegida: sin token, redirige a `/login`.
