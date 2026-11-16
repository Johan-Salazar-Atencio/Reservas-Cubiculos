import { NavLink } from 'react-router-dom'
import {
  BarChart3,
  BookOpenCheck,
  ClipboardList,
  FlaskConical,
  Gauge,
  LayoutDashboard,
  ListChecks,
} from 'lucide-react'
import { useAuth } from '../context/AuthContext.jsx'
import { iniciales } from '../utils/formato.js'

const NAV_ESTUDIANTE = [
  { a: '/', etiqueta: 'Tablero', Icono: LayoutDashboard, fin: true },
  { a: '/mis-reservas', etiqueta: 'Mis reservas', Icono: ListChecks },
  { a: '/normas', etiqueta: 'Normas', Icono: BookOpenCheck },
]

const NAV_ADMIN = [
  { a: '/admin', etiqueta: 'Monitoreo', Icono: Gauge, fin: true },
  { a: '/admin/reservas', etiqueta: 'Reservas', Icono: ListChecks },
  { a: '/admin/registro', etiqueta: 'Registro', Icono: ClipboardList },
  { a: '/admin/estadisticas', etiqueta: 'Estadísticas', Icono: BarChart3 },
  { a: '/admin/simulador', etiqueta: 'Simulador', Icono: FlaskConical },
]

/** Barra lateral azul, como la de Canvas: cada opción lleva el icono sobre su etiqueta. */
export default function GlobalNav({ cuentaAbierta, onCuenta }) {
  const { usuario, esAdmin } = useAuth()
  const items = esAdmin ? NAV_ADMIN : NAV_ESTUDIANTE

  return (
    <nav className="nav-global" aria-label="Navegación principal">
      <div className="nav-logo">
        {/* Aquí va el logo oficial de Tecsup: en este prototipo no se copia, hay un símbolo propio. */}
        <svg
          width="40"
          height="40"
          viewBox="0 0 64 64"
          fill="none"
          stroke="#fff"
          strokeWidth="5"
          strokeLinejoin="round"
          aria-hidden="true"
        >
          <rect x="17" y="10" width="30" height="44" rx="3" />
          <circle cx="39" cy="33" r="2.5" fill="#fff" stroke="none" />
        </svg>
      </div>
      <button type="button" className="nav-item" onClick={onCuenta} aria-expanded={cuentaAbierta}>
        <span className="avatar" aria-hidden="true">
          {iniciales(usuario?.nombre)}
        </span>
        Cuenta
      </button>
      {items.map(({ a, etiqueta, Icono, fin }) => (
        <NavLink
          key={a}
          to={a}
          end={fin}
          className={({ isActive }) => `nav-item${isActive ? ' activo' : ''}`}
        >
          <Icono size={26} aria-hidden="true" />
          {etiqueta}
        </NavLink>
      ))}
    </nav>
  )
}
