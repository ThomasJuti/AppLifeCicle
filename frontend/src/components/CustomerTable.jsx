import { formatDate, timeAgo } from '../lib/format'

function CustomerIcon() {
  return (
    <span className="customer-icon" aria-hidden="true">
      <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.75">
        <circle cx="12" cy="8" r="4" />
        <path d="M5 20v-1c0-3.5 3.1-5.5 7-5.5s7 2 7 5.5v1" />
      </svg>
    </span>
  )
}

function EmptyIcon({ filtered }) {
  if (filtered) {
    return (
      <svg className="empty-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.75" aria-hidden="true">
        <circle cx="11" cy="11" r="7" />
        <line x1="21" y1="21" x2="16.65" y2="16.65" />
      </svg>
    )
  }

  return (
    <svg className="empty-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.75" aria-hidden="true">
      <circle cx="9" cy="8" r="3.5" />
      <path d="M2.5 20v-1.2c0-2.8 2.9-4.3 6.5-4.3s6.5 1.5 6.5 4.3V20" />
      <circle cx="17" cy="9" r="2.5" />
      <path d="M21.5 20v-0.9c0-2-2-3.5-4.5-3.7" />
    </svg>
  )
}

function SkeletonRows({ rows = 4 }) {
  return (
    <div aria-hidden="true">
      {Array.from({ length: rows }).map((_, i) => (
        <div className="skeleton-row" key={i}>
          <div className="skeleton sk-avatar" />
          <div style={{ flex: 1 }}>
            <div className="skeleton sk-line" style={{ width: '40%', marginBottom: 8 }} />
            <div className="skeleton sk-line" style={{ width: '60%' }} />
          </div>
        </div>
      ))}
    </div>
  )
}

export default function CustomerTable({ customers, loading, filtered, onEdit, onDelete, busyId }) {
  if (loading) return <SkeletonRows />

  if (customers.length === 0) {
    return (
      <div className="empty">
        <div className="glyph">
          <EmptyIcon filtered={filtered} />
        </div>
        <div className="title">
          {filtered ? 'Sin coincidencias' : 'Todavía no hay clientes'}
        </div>
        <div>{filtered ? 'Prueba con otro término de búsqueda.' : 'Crea el primero con el formulario de arriba.'}</div>
      </div>
    )
  }

  return (
    <div className="table-scroll">
      <table className="ledger">
        <thead>
          <tr>
            <th>Cliente</th>
            <th>Email</th>
            <th>Alta</th>
            <th className="col-actions">Acciones</th>
          </tr>
        </thead>
        <tbody>
          {customers.map((c) => (
            <tr key={c.id}>
              <td>
                <div className="cell-customer">
                  <CustomerIcon />
                  <div>
                    <div className="name">{c.name}</div>
                    <div className="id">{String(c.id).slice(0, 8)}</div>
                  </div>
                </div>
              </td>
              <td className="cell-email">{c.email}</td>
              <td className="cell-date">
                {formatDate(c.createdAt)}
                <span className="ago">{timeAgo(c.createdAt)}</span>
              </td>
              <td className="col-actions">
                <div className="row-actions">
                  <button
                    className="btn btn-sm btn-ghost"
                    onClick={() => onEdit(c)}
                    disabled={busyId === c.id}
                  >
                    Editar
                  </button>
                  <button
                    className="btn btn-sm btn-danger"
                    onClick={() => onDelete(c)}
                    disabled={busyId === c.id}
                    aria-label={`Eliminar ${c.name}`}
                  >
                    {busyId === c.id ? '…' : 'Eliminar'}
                  </button>
                </div>
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  )
}
