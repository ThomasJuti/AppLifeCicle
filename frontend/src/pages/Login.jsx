import { useState } from 'react'
import { Navigate, useLocation } from 'react-router-dom'
import { useAuth } from '../auth/AuthContext'
import { apiErrorMessage } from '../api/client'

export default function Login() {
  const { login, isAuthenticated } = useAuth()
  const location = useLocation()
  const from = location.state?.from?.pathname || '/'

  const [username, setUsername] = useState('admin')
  const [password, setPassword] = useState('')
  const [error, setError] = useState(null)
  const [loading, setLoading] = useState(false)

  if (isAuthenticated) {
    return <Navigate to={from} replace />
  }

  async function handleSubmit(e) {
    e.preventDefault()
    setError(null)
    setLoading(true)
    try {
      await login(username, password)
    } catch (err) {
      setError(apiErrorMessage(err, 'No se pudo iniciar sesión'))
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="auth">
      <aside className="auth-brand">
        <div className="auth-brand-hero">
          <h1 className="auth-headline">
            Life<em>Cicle</em>App PROD
          </h1>
          <p className="auth-tagline">Gestión de clientes</p>
        </div>
      </aside>

      <section className="auth-panel">
        <form className="auth-card" onSubmit={handleSubmit}>
          <h1>Iniciar sesión</h1>
          <p className="sub">Introduce tus credenciales para acceder a la consola.</p>

          {error && (
            <div className="alert alert-error" role="alert">
              {error}
            </div>
          )}

          <label className="field">
            <span>Usuario</span>
            <input
              type="text"
              value={username}
              onChange={(e) => setUsername(e.target.value)}
              autoComplete="username"
              placeholder="admin"
              required
            />
          </label>

          <label className="field">
            <span>Contraseña</span>
            <input
              type="password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              autoComplete="current-password"
              placeholder="••••••••"
              required
            />
          </label>

          <button className="btn btn-primary btn-block" type="submit" disabled={loading}>
            {loading ? 'Verificando…' : 'Entrar a la consola'}
          </button>
        </form>
      </section>
    </div>
  )
}
