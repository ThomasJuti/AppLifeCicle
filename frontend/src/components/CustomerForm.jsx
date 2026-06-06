import { useEffect, useState } from 'react'

const EMPTY = { name: '', email: '' }

export default function CustomerForm({ editing, onSubmit, onCancel, submitting }) {
  const [form, setForm] = useState(EMPTY)
  const isEdit = Boolean(editing)

  useEffect(() => {
    setForm(editing ? { name: editing.name, email: editing.email } : EMPTY)
  }, [editing])

  function handleSubmit(e) {
    e.preventDefault()
    onSubmit({ name: form.name.trim(), email: form.email.trim() })
  }

  return (
    <section className="panel">
      <div className="panel-head">
        <h2>
          {isEdit ? 'Editar cliente' : 'Nuevo cliente'}
          {isEdit && <span className="tag">Editando</span>}
        </h2>
      </div>

      <div className="panel-body">
        <form onSubmit={handleSubmit}>
          <div className="form-grid">
            <label className="field">
              <span>Nombre</span>
              <input
                type="text"
                value={form.name}
                onChange={(e) => setForm((f) => ({ ...f, name: e.target.value }))}
                placeholder="Juan Pérez"
                minLength={2}
                maxLength={100}
                required
              />
            </label>

            <label className="field">
              <span>Email</span>
              <input
                type="email"
                value={form.email}
                onChange={(e) => setForm((f) => ({ ...f, email: e.target.value }))}
                placeholder="juan@email.com"
                maxLength={150}
                required
              />
            </label>
          </div>

          <div className="form-actions">
            <button className="btn btn-primary" type="submit" disabled={submitting}>
              {submitting ? 'Guardando…' : isEdit ? 'Guardar cambios' : 'Crear cliente'}
            </button>
            {isEdit && (
              <button className="btn btn-ghost" type="button" onClick={onCancel} disabled={submitting}>
                Cancelar
              </button>
            )}
          </div>
        </form>
      </div>
    </section>
  )
}
