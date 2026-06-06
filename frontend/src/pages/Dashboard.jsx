import { useCallback, useEffect, useMemo, useState } from 'react'
import { useAuth } from '../auth/AuthContext'
import { apiErrorMessage } from '../api/client'
import {
  listCustomers,
  createCustomer,
  updateCustomer,
  deleteCustomer,
} from '../api/customers'
import CustomerForm from '../components/CustomerForm'
import CustomerTable from '../components/CustomerTable'
import { timeAgo } from '../lib/format'

let toastSeq = 0

export default function Dashboard() {
  const { username, logout } = useAuth()

  const [customers, setCustomers] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)
  const [toasts, setToasts] = useState([])

  const [editing, setEditing] = useState(null)
  const [submitting, setSubmitting] = useState(false)
  const [busyId, setBusyId] = useState(null)
  const [query, setQuery] = useState('')

  const load = useCallback(async () => {
    setLoading(true)
    setError(null)
    try {
      setCustomers(await listCustomers())
    } catch (err) {
      setError(apiErrorMessage(err, 'No se pudieron cargar los clientes'))
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    load()
  }, [load])

  function toast(type, msg) {
    const id = ++toastSeq
    setToasts((t) => [...t, { id, type, msg }])
    setTimeout(() => setToasts((t) => t.filter((x) => x.id !== id)), 3600)
  }

  async function handleSubmit(payload) {
    setSubmitting(true)
    setError(null)
    try {
      if (editing) {
        await updateCustomer(editing.id, payload)
        toast('ok', 'Cliente actualizado')
        setEditing(null)
      } else {
        await createCustomer(payload)
        toast('ok', 'Cliente creado')
      }
      await load()
    } catch (err) {
      const msg = apiErrorMessage(err, 'No se pudo guardar el cliente')
      setError(msg)
      toast('err', msg)
    } finally {
      setSubmitting(false)
    }
  }

  async function handleDelete(customer) {
    if (!window.confirm(`¿Eliminar a ${customer.name}? Esta acción no se puede deshacer.`)) return
    setBusyId(customer.id)
    setError(null)
    try {
      await deleteCustomer(customer.id)
      toast('ok', `${customer.name} eliminado`)
      if (editing?.id === customer.id) setEditing(null)
      await load()
    } catch (err) {
      const msg = apiErrorMessage(err, 'No se pudo eliminar el cliente')
      setError(msg)
      toast('err', msg)
    } finally {
      setBusyId(null)
    }
  }

  const filtered = useMemo(() => {
    const q = query.trim().toLowerCase()
    if (!q) return customers
    return customers.filter(
      (c) => c.name.toLowerCase().includes(q) || c.email.toLowerCase().includes(q),
    )
  }, [customers, query])

  const latest = useMemo(() => {
    if (customers.length === 0) return null
    return customers.reduce((a, b) => (a.createdAt > b.createdAt ? a : b))
  }, [customers])

  const isFiltering = query.trim().length > 0

  return (
    <div className="shell">
      <header className="topbar">
        <span className="wordmark">Consola</span>
        <div className="topbar-right">
          {username && (
            <span className="user-chip">{username}</span>
          )}
          <button className="btn btn-ghost btn-sm" onClick={logout}>
            Salir
          </button>
        </div>
      </header>

      <main className="content">
        <div className="hero">
          <div>
            <p className="eyebrow">Panel de operaciones</p>
            <h1>Clientes</h1>
          </div>
          <button className="btn btn-ghost btn-sm" onClick={load} disabled={loading}>
            {loading ? 'Cargando…' : '↻ Refrescar'}
          </button>
        </div>

        <div className="stats">
          <div className="stat">
            <div className="label">Total de clientes</div>
            <div className="value">{loading ? '—' : customers.length}</div>
          </div>
          <div className="stat">
            <div className="label">Último registro</div>
            <div className="value mono">{latest ? timeAgo(latest.createdAt) : '—'}</div>
          </div>
          <div className="stat">
            <div className="label">{isFiltering ? 'Resultados' : 'Mostrando'}</div>
            <div className="value">
              {filtered.length}
              <span className="unit">{isFiltering ? 'coincidencias' : 'visibles'}</span>
            </div>
          </div>
        </div>

        {error && (
          <div className="alert alert-error" role="alert">
            {error}
          </div>
        )}

        <CustomerForm
          editing={editing}
          onSubmit={handleSubmit}
          onCancel={() => setEditing(null)}
          submitting={submitting}
        />

        <section className="panel">
          <div className="panel-head">
            <h2>
              Cartera <span className="count-pill">{customers.length}</span>
            </h2>
            <div className="search">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.4">
                <circle cx="11" cy="11" r="7" />
                <line x1="21" y1="21" x2="16.65" y2="16.65" />
              </svg>
              <input
                type="search"
                value={query}
                onChange={(e) => setQuery(e.target.value)}
                placeholder="Buscar por nombre o email…"
                aria-label="Buscar clientes"
              />
            </div>
          </div>

          <CustomerTable
            customers={filtered}
            loading={loading}
            filtered={isFiltering}
            onEdit={setEditing}
            onDelete={handleDelete}
            busyId={busyId}
          />
        </section>
      </main>

      <div className="toast-stack" aria-live="polite" aria-atomic="false">
        {toasts.map((t) => (
          <div className={`toast ${t.type}`} key={t.id} role="status">
            <span className="dot" />
            {t.msg}
          </div>
        ))}
      </div>
    </div>
  )
}
