import { useState } from 'react'
import Modal from './Modal.jsx'

/** Pregunta "¿seguro?" antes de una acción. {onConfirmar} puede ser asíncrona; si falla, el error se muestra aquí. */
export default function ConfirmarModal({ titulo, children, etiqueta, peligro = false, onConfirmar, onCerrar }) {
  const [enviando, setEnviando] = useState(false)
  const [error, setError] = useState('')

  async function confirmar() {
    setEnviando(true)
    setError('')
    try {
      await onConfirmar()
    } catch (e) {
      setError(e.message)
      setEnviando(false)
    }
  }

  return (
    <Modal
      titulo={titulo}
      onCerrar={onCerrar}
      ancho={460}
      pie={
        <>
          <button type="button" className="btn" onClick={onCerrar}>
            Volver
          </button>
          <button
            type="button"
            className={`btn ${peligro ? 'btn-peligro-solido' : 'btn-primario'}`}
            disabled={enviando}
            onClick={confirmar}
          >
            {enviando ? 'Un momento…' : etiqueta}
          </button>
        </>
      }
    >
      <div>{children}</div>
      {error && (
        <p className="campo-error" role="alert">
          {error}
        </p>
      )}
    </Modal>
  )
}
