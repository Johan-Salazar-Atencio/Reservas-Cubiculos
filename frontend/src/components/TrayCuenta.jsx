import { useEffect } from 'react'
import { LogOut, X } from 'lucide-react'
import { useAuth } from '../context/AuthContext.jsx'
import { iniciales } from '../utils/formato.js'

/** Bandeja de la cuenta: se abre desde "Cuenta" (como la bandeja "Cursos" de Canvas). */
export default function TrayCuenta({ onCerrar }) {
  const { usuario, esAdmin, salir } = useAuth()

  useEffect(() => {
    function alTeclear(evento) {
      if (evento.key === 'Escape') onCerrar()
    }
    document.addEventListener('keydown', alTeclear)
    return () => document.removeEventListener('keydown', alTeclear)
  }, [onCerrar])

  return (
    <>
      <div className="tray-fondo" onClick={onCerrar} aria-hidden="true" />
      <aside className="tray" aria-label="Cuenta">
        <div className="tray-cabecera">
          <h2>Cuenta</h2>
          <button type="button" className="tray-cerrar" aria-label="Cerrar la bandeja de la cuenta" onClick={onCerrar}>
            <X size={20} aria-hidden="true" />
          </button>
        </div>
        <div className="tray-usuario">
          <span className="avatar avatar-grande" aria-hidden="true">
            {iniciales(usuario?.nombre)}
          </span>
          <div>
            <p>
              <strong>{usuario?.nombre}</strong>
            </p>
            <p className="codigo">{usuario?.codigo}</p>
            <p>{usuario?.correo}</p>
          </div>
        </div>
        <p>
          <span className={`chip ${esAdmin ? 'chip-admin' : 'chip-ocupado'}`}>
            {esAdmin ? 'Administrador' : 'Estudiante'}
          </span>
        </p>
        <p style={{ marginTop: 24 }}>
          <button type="button" className="btn" onClick={() => salir()}>
            <LogOut size={18} aria-hidden="true" />
            Cerrar sesión
          </button>
        </p>
      </aside>
    </>
  )
}
