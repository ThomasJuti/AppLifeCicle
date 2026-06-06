export function initials(name) {
  if (!name) return '?'
  const parts = name.trim().split(/\s+/).slice(0, 2)
  return parts.map((p) => p[0]?.toUpperCase() ?? '').join('') || '?'
}

export function avatarColor(name) {
  let h = 0
  for (let i = 0; i < (name?.length ?? 0); i++) {
    h = (h * 31 + name.charCodeAt(i)) % 360
  }
  return `linear-gradient(150deg, hsl(${h} 70% 68%), hsl(${(h + 28) % 360} 65% 56%))`
}

export function formatDate(iso) {
  if (!iso) return '—'
  const d = new Date(iso)
  if (Number.isNaN(d.getTime())) return iso
  return d.toLocaleDateString(undefined, { day: '2-digit', month: 'short', year: 'numeric' })
}

export function timeAgo(iso) {
  if (!iso) return ''
  const d = new Date(iso)
  if (Number.isNaN(d.getTime())) return ''
  const secs = Math.floor((Date.now() - d.getTime()) / 1000)
  if (secs < 60) return 'hace un momento'
  const mins = Math.floor(secs / 60)
  if (mins < 60) return `hace ${mins} min`
  const hrs = Math.floor(mins / 60)
  if (hrs < 24) return `hace ${hrs} h`
  const days = Math.floor(hrs / 24)
  if (days < 30) return `hace ${days} d`
  const months = Math.floor(days / 30)
  if (months < 12) return `hace ${months} mes${months > 1 ? 'es' : ''}`
  return `hace ${Math.floor(months / 12)} a`
}
