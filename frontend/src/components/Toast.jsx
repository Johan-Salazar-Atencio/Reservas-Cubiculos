import { createContext, useCallback, useContext, useMemo, useRef, useState } from 'react'
import { AlertTriangle, CheckCircle2, X } from 'lucide-react'

const ToastContext = createContext({ mostrar: () => {} })

/** Avisos cortos arriba de la página: "Reserva confirmada", errores del servidor, etc. */
export function ToastProvider({ children }) {
  const [avisos, setAvisos] = useState([])
  const siguiente = useRef(1)

  const quitar = useCallback((id) => {
    setAvisos((lista) => lista.filter((a) => a.id !== id))
  }, [])

  const mostrar = useCallback(
    (mensaje, tipo = 'exito') => {
      const id = siguiente.current++
      setAvisos((lista) => [...lista, { id, mensaje, tipo }])
      setTimeout(() => quitar(id), tipo === 'error' ? 7000 : 4500)
    },
    [quitar],
  )

  const valor = useMemo(() => ({ mostrar }), [mostrar])

  return (
    <ToastContext.Provider value={valor}>
      {children}
      <div className="toasts">
        {avisos.map((a) => (
          <div key={a.id} className={`toast toast-${a.tipo}`} role={a.tipo === 'error' ? 'alert' : 'status'}>
            {a.tipo === 'error' ? (
              <AlertTriangle size={20} aria-hidden="true" />
            ) : (
              <CheckCircle2 size={20} aria-hidden="true" />
            )}
            <span>{a.mensaje}</span>
            <button type="button" className="toast-cerrar" aria-label="Cerrar aviso" onClick={() => quitar(a.id)}>
              <X size={16} aria-hidden="true" />
            </button>
          </div>
        ))}
      </div>
    </ToastContext.Provider>
  )
}

export function useToast() {
  return useContext(ToastContext)
}
