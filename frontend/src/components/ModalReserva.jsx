import { useState } from 'react'
import Modal from './Modal.jsx'
import { reservasApi } from '../services/endpoints.js'
import { horasTexto, sumarHoras } from '../utils/formato.js'

const MAX_PERSONAS = 6

/** Cuántos turnos libres seguidos hay desde la posición {desde} de la lista. */
function libresSeguidos(slots, desde) {
  let n = 0
  while (desde >= 0 && desde + n < slots.length && slots[desde + n].estado === 'LIBRE') n += 1
  return n
}

/**
 * Ventana para reservar. {cubiculo} es un cubículo de la agenda (con sus turnos). Se puede reservar una hora
 * para uno mismo o un relevo: varias personas suman sus horas y apartan un bloque seguido en el mismo cubículo.
 */
export default function ModalReserva({ cubiculo, horaInicial, usuario, onCerrar, onReservado }) {
  const slots = cubiculo.slots
  const libres = slots.filter((s) => s.estado === 'LIBRE').map((s) => s.hora)

  const [hora, setHora] = useState(libres.includes(horaInicial) ? horaInicial : (libres[0] ?? ''))
  const [modo, setModo] = useState('solo')
  const [personas, setPersonas] = useState(2)
  const [otros, setOtros] = useState(Array(MAX_PERSONAS - 1).fill(''))
  const [error, setError] = useState('')
  const [enviando, setEnviando] = useState(false)

  const indice = slots.findIndex((s) => s.hora === hora)
  const maxPersonas = Math.min(MAX_PERSONAS, libresSeguidos(slots, indice))
  const n = modo === 'grupo' ? Math.max(2, Math.min(personas, maxPersonas)) : 1

  const ids = otros.slice(0, n - 1).map((v) => v.trim())
  const faltan = modo === 'grupo' && ids.some((v) => v === '')
  const escritos = ids.filter(Boolean).map((v) => v.toLowerCase())
  const repetido =
    new Set(escritos).size !== escritos.length ||
    escritos.includes(usuario.codigo.toLowerCase()) ||
    escritos.includes(usuario.correo.toLowerCase())

  function cambiarOtro(i, valor) {
    setOtros((lista) => lista.map((v, j) => (j === i ? valor : v)))
  }

  async function confirmar() {
    setEnviando(true)
    setError('')
    try {
      const reserva = await reservasApi.crear({ cubiculoId: cubiculo.id, horaInicio: hora, integrantes: ids })
      onReservado(reserva)
    } catch (e) {
      setError(e.message)
      setEnviando(false)
    }
  }

  const pie = (
    <>
      <button type="button" className="btn" onClick={onCerrar}>
        Cancelar
      </button>
      <button
        type="button"
        className="btn btn-primario"
        disabled={!hora || enviando || faltan || repetido}
        onClick={confirmar}
      >
        {enviando ? 'Reservando…' : 'Confirmar reserva'}
      </button>
    </>
  )

  return (
    <Modal titulo={`Reservar ${cubiculo.nombre}`} onCerrar={onCerrar} pie={libres.length > 0 ? pie : null}>
      {libres.length === 0 ? (
        <p>Ya no quedan turnos libres hoy en este cubículo.</p>
      ) : (
        <>
          <div className="campo">
            <label htmlFor="hora-inicio">Hora de inicio</label>
            <select id="hora-inicio" className="control" value={hora} onChange={(e) => setHora(e.target.value)}>
              {libres.map((h) => (
                <option key={h} value={h}>
                  {h}
                </option>
              ))}
            </select>
          </div>

          <fieldset className="campo">
            <legend>¿Cómo vas a usarlo?</legend>
            <label className="opcion">
              <input type="radio" name="modo" value="solo" checked={modo === 'solo'} onChange={() => setModo('solo')} />
              <span>
                Solo yo (1 hora)
                <small>Gastas tu hora de hoy.</small>
              </span>
            </label>
            <label className="opcion">
              <input
                type="radio"
                name="modo"
                value="grupo"
                checked={modo === 'grupo'}
                disabled={maxPersonas < 2}
                onChange={() => setModo('grupo')}
              />
              <span>
                Relevo en grupo
                <small>
                  Varias personas suman sus horas y apartan un bloque seguido en este cubículo. Cada una gasta su hora
                  de hoy.
                </small>
              </span>
            </label>
          </fieldset>

          {modo === 'grupo' && (
            <>
              <div className="campo">
                <label htmlFor="num-personas">Cantidad de personas (contándote)</label>
                <select
                  id="num-personas"
                  className="control"
                  value={n}
                  onChange={(e) => setPersonas(Number(e.target.value))}
                >
                  {Array.from({ length: maxPersonas - 1 }, (_, i) => i + 2).map((v) => (
                    <option key={v} value={v}>
                      {v} personas · {horasTexto(v)}
                    </option>
                  ))}
                </select>
                <span className="campo-ayuda">Cada persona debe estar en la lista de estudiantes de Tecsup.</span>
              </div>
              {ids.map((_, i) => (
                <div className="campo" key={i}>
                  <label htmlFor={`persona-${i}`}>Código o correo de la persona {i + 2}</label>
                  <input
                    id={`persona-${i}`}
                    className="control"
                    value={otros[i]}
                    onChange={(e) => cambiarOtro(i, e.target.value)}
                    placeholder="Código o correo institucional"
                    autoComplete="off"
                  />
                </div>
              ))}
              {faltan && <p className="campo-ayuda">Escribe el código o correo de todas las personas del grupo.</p>}
              {repetido && <p className="campo-error">Hay una persona repetida en el grupo.</p>}
            </>
          )}

          <p className="resumen-bloque">
            Bloque: <strong>{hora}</strong> a <strong>{hora ? sumarHoras(hora, n) : ''}</strong> ({horasTexto(n)}) en el{' '}
            {cubiculo.nombre}. Si nadie llega en 10 minutos, la sala se libera.
          </p>
          {error && (
            <p className="campo-error" role="alert">
              {error}
            </p>
          )}
        </>
      )}
    </Modal>
  )
}
