import { useLocation } from 'react-router-dom'
import { CalendarClock, ChevronRight } from 'lucide-react'
import { useServidor } from '../context/ServidorContext.jsx'
import { fechaLarga, horaDe, mayuscula } from '../utils/formato.js'

const SECCIONES = {
  '/': 'Tablero',
  '/mis-reservas': 'Mis reservas',
  '/normas': 'Normas',
  '/admin': 'Monitoreo',
  '/admin/reservas': 'Reservas',
  '/admin/registro': 'Registro',
  '/admin/estadisticas': 'Estadísticas',
  '/admin/simulador': 'Simulador',
}

/** Barra superior blanca: dónde estás, la hora del servidor (la simulada, si se está probando) y si hay conexión. */
export default function TopBar() {
  const { pathname } = useLocation()
  const { servidor } = useServidor()
  const seccion = SECCIONES[pathname.replace(/\/$/, '') || '/'] ?? ''

  return (
    <header className="topbar">
      <nav className="topbar-ruta" aria-label="Ubicación">
        <span>Reservas de cubículos</span>
        <ChevronRight size={16} aria-hidden="true" />
        <strong>{seccion}</strong>
      </nav>
      <div className="topbar-estado">
        {servidor.simulado && <span className="etiqueta-simulada">Hora simulada</span>}
        {servidor.ahora && (
          <span className="chip-reloj">
            <CalendarClock size={18} aria-hidden="true" />
            {mayuscula(fechaLarga(servidor.ahora))} · {horaDe(servidor.ahora)}
          </span>
        )}
        <span className={`en-vivo${servidor.conectado ? '' : ' sin-conexion'}`}>
          {servidor.conectado ? 'En vivo' : 'Sin conexión'}
        </span>
      </div>
    </header>
  )
}
