import { AlertTriangle } from 'lucide-react'

export function Cargando({ texto = 'Cargando…' }) {
  return (
    <div className="estado-vacio" role="status">
      <span>{texto}</span>
    </div>
  )
}

export function ErrorCarga({ error, onReintentar }) {
  return (
    <div className="estado-vacio" role="alert">
      <AlertTriangle size={32} aria-hidden="true" />
      <p>{error?.message ?? 'No se pudo cargar la información.'}</p>
      {onReintentar && (
        <button type="button" className="btn" onClick={onReintentar}>
          Reintentar
        </button>
      )}
    </div>
  )
}
