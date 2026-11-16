import { useEffect, useId, useRef } from 'react'
import { X } from 'lucide-react'

/** Ventana emergente: se cierra con Esc, con la X o al pulsar fuera; el foco empieza en el primer campo. */
export default function Modal({ titulo, onCerrar, children, pie = null, ancho = 520 }) {
  const idTitulo = useId()
  const panel = useRef(null)
  const cerrarRef = useRef(onCerrar)

  useEffect(() => {
    cerrarRef.current = onCerrar
  })

  // Solo al abrir: así el foco no salta cada vez que la pantalla de atrás se actualiza.
  useEffect(() => {
    const previo = document.activeElement
    const primero = panel.current?.querySelector('input, select, textarea')
    ;(primero ?? panel.current)?.focus()

    function alTeclear(evento) {
      if (evento.key === 'Escape') cerrarRef.current()
    }
    document.addEventListener('keydown', alTeclear)
    return () => {
      document.removeEventListener('keydown', alTeclear)
      previo?.focus?.()
    }
  }, [])

  return (
    <div
      className="modal-fondo"
      onMouseDown={(evento) => {
        if (evento.target === evento.currentTarget) onCerrar()
      }}
    >
      <div
        className="modal"
        role="dialog"
        aria-modal="true"
        aria-labelledby={idTitulo}
        ref={panel}
        tabIndex={-1}
        style={{ maxWidth: ancho }}
      >
        <header className="modal-cabecera">
          <h2 id={idTitulo}>{titulo}</h2>
          <button type="button" className="modal-cerrar" aria-label="Cerrar" onClick={onCerrar}>
            <X size={20} aria-hidden="true" />
          </button>
        </header>
        <div className="modal-cuerpo">{children}</div>
        {pie && <footer className="modal-pie">{pie}</footer>}
      </div>
    </div>
  )
}
