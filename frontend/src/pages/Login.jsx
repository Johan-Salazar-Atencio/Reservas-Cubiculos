import { useEffect, useState } from 'react'
import { Navigate } from 'react-router-dom'
import { Clock, DoorOpen, Users } from 'lucide-react'
import { useAuth } from '../context/AuthContext.jsx'
import { authApi } from '../services/endpoints.js'

export default function Login() {
  const { sesion, entrar, aviso } = useAuth()
  const [pestana, setPestana] = useState('estudiante')
  const [identificador, setIdentificador] = useState('')
  const [clave, setClave] = useState('')
  const [error, setError] = useState('')
  const [enviando, setEnviando] = useState(false)
  const [demo, setDemo] = useState([])

  // Solo el perfil dev del servidor tiene cuentas de demostración; en producción esta llamada falla y no se muestra nada.
  useEffect(() => {
    authApi.cuentasDemo().then(setDemo).catch(() => setDemo([]))
  }, [])

  if (sesion) return <Navigate to={sesion.usuario.rol === 'ADMIN' ? '/admin' : '/'} replace />

  async function enviar(evento) {
    evento.preventDefault()
    setEnviando(true)
    setError('')
    try {
      entrar(await authApi.login({ identificador: identificador.trim(), clave: pestana === 'admin' ? clave : '' }))
    } catch (e) {
      setError(e.message)
      setEnviando(false)
    }
  }

  async function entrarDemo(codigo) {
    setEnviando(true)
    setError('')
    try {
      entrar(await authApi.entrarDemo(codigo))
    } catch (e) {
      setError(e.message)
      setEnviando(false)
    }
  }

  function cambiarPestana(nueva) {
    setPestana(nueva)
    setError('')
  }

  const estudiantes = demo.filter((c) => c.rol === 'ESTUDIANTE').slice(0, 6)
  const administrador = demo.find((c) => c.rol === 'ADMIN')

  return (
    <div className="login">
      <section className="login-marca" aria-label="Presentación">
        <svg
          width="56"
          height="56"
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
        <h1>Reservas de cubículos</h1>
        <p>Mira en tiempo real qué cubículos están libres y reserva tu hora de hoy.</p>
        <ul>
          <li>
            <DoorOpen size={22} aria-hidden="true" />
            <span>4 cubículos, atención de 7:00 a 19:00</span>
          </li>
          <li>
            <Clock size={22} aria-hidden="true" />
            <span>1 hora por día para cada estudiante, solo para el mismo día</span>
          </li>
          <li>
            <Users size={22} aria-hidden="true" />
            <span>Relevos: suma horas con tus compañeros y aparta un bloque seguido</span>
          </li>
        </ul>
      </section>

      <section className="login-panel">
        <h2>Ingresar</h2>
        <div className="pestanas" role="tablist" aria-label="Tipo de cuenta">
          <button
            type="button"
            role="tab"
            id="tab-estudiante"
            className="pestana"
            aria-selected={pestana === 'estudiante'}
            aria-controls="panel-ingreso"
            onClick={() => cambiarPestana('estudiante')}
          >
            Estudiante
          </button>
          <button
            type="button"
            role="tab"
            id="tab-admin"
            className="pestana"
            aria-selected={pestana === 'admin'}
            aria-controls="panel-ingreso"
            onClick={() => cambiarPestana('admin')}
          >
            Administrador
          </button>
        </div>

        <form
          id="panel-ingreso"
          role="tabpanel"
          aria-labelledby={`tab-${pestana}`}
          className="login-form"
          onSubmit={enviar}
        >
          {aviso && (
            <div className="aviso aviso-aviso" role="status">
              {aviso}
            </div>
          )}
          <div className="campo">
            <label htmlFor="identificador">
              {pestana === 'estudiante' ? 'Código o correo institucional' : 'Usuario del administrador'}
            </label>
            <input
              id="identificador"
              className="control"
              value={identificador}
              onChange={(e) => setIdentificador(e.target.value)}
              autoComplete="username"
              required
            />
            {pestana === 'estudiante' && (
              <span className="campo-ayuda">Debes estar en la lista de estudiantes de Tecsup.</span>
            )}
          </div>
          {pestana === 'admin' && (
            <div className="campo">
              <label htmlFor="clave">Contraseña</label>
              <input
                id="clave"
                type="password"
                className="control"
                value={clave}
                onChange={(e) => setClave(e.target.value)}
                autoComplete="current-password"
                required
              />
            </div>
          )}
          {error && (
            <p className="campo-error" role="alert">
              {error}
            </p>
          )}
          <button type="submit" className="btn btn-primario" disabled={enviando}>
            {enviando ? 'Ingresando…' : 'Ingresar'}
          </button>
        </form>

        {(estudiantes.length > 0 || administrador) && (
          <div className="demo-caja">
            <h3>Modo demostración</h3>
            <p>Cuentas de prueba que solo existen en desarrollo. Entra con un clic:</p>
            <div className="grupo-botones">
              {estudiantes.map((c) => (
                <button
                  key={c.codigo}
                  type="button"
                  className="btn btn-pequeno"
                  disabled={enviando}
                  onClick={() => entrarDemo(c.codigo)}
                >
                  {c.nombre}
                </button>
              ))}
              {administrador && (
                <button
                  type="button"
                  className="btn btn-pequeno btn-primario"
                  disabled={enviando}
                  onClick={() => entrarDemo(administrador.codigo)}
                >
                  Entrar como administrador
                </button>
              )}
            </div>
          </div>
        )}
      </section>
    </div>
  )
}
