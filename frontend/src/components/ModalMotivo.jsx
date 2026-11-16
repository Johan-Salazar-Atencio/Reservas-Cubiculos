import { useState } from 'react'
import Modal from './Modal.jsx'

/** Acciones del administrador: siempre piden un motivo (mínimo 5 letras) y quedan en el registro. */
export default function ModalMotivo({
  titulo,
  descripcion,
  etiqueta,
  conHoras = false,
  maxHoras = 4,
  peligro = false,
  onConfirmar,
  onCerrar,
}) {
  const [motivo, setMotivo] = useState('')
  const [horas, setHoras] = useState(1)
  const [enviando, setEnviando] = useState(false)
  const [error, setError] = useState('')
  const valido = motivo.trim().length >= 5

  async function confirmar() {
    setEnviando(true)
    setError('')
    try {
      await onConfirmar({ motivo: motivo.trim(), horas })
    } catch (e) {
      setError(e.message)
      setEnviando(false)
    }
  }

  return (
    <Modal
      titulo={titulo}
      onCerrar={onCerrar}
      pie={
        <>
          <button type="button" className="btn" onClick={onCerrar}>
            Volver
          </button>
          <button
            type="button"
            className={`btn ${peligro ? 'btn-peligro-solido' : 'btn-primario'}`}
            disabled={!valido || enviando}
            onClick={confirmar}
          >
            {enviando ? 'Un momento…' : etiqueta}
          </button>
        </>
      }
    >
      <p>{descripcion}</p>
      {conHoras && (
        <div className="campo">
          <label htmlFor="horas-ocupar">¿Cuántas horas?</label>
          <select
            id="horas-ocupar"
            className="control"
            value={horas}
            onChange={(e) => setHoras(Number(e.target.value))}
          >
            {Array.from({ length: maxHoras }, (_, i) => i + 1).map((h) => (
              <option key={h} value={h}>
                {h === 1 ? '1 hora' : `${h} horas`} (desde el turno actual)
              </option>
            ))}
          </select>
        </div>
      )}
      <div className="campo">
        <label htmlFor="motivo">Motivo</label>
        <textarea
          id="motivo"
          className="control"
          value={motivo}
          maxLength={250}
          onChange={(e) => setMotivo(e.target.value)}
          placeholder="Escribe por qué haces esto (queda en el registro)"
        />
        <span className="campo-ayuda">Queda en el registro con tu código, la fecha y la hora.</span>
      </div>
      {error && (
        <p className="campo-error" role="alert">
          {error}
        </p>
      )}
    </Modal>
  )
}
